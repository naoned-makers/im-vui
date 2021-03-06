package io.naonedmakers.imvui;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import ai.api.AIConfiguration;
import ai.api.AIServiceException;
import ai.api.util.StringUtils;
import io.naonedmakers.imvui.meaning.MeanResponse;
import io.naonedmakers.imvui.meaning.MeanService;
import io.naonedmakers.imvui.meaning.apiai.ApiAiService;
import io.naonedmakers.imvui.meaning.local.LocalAiService;
import io.naonedmakers.imvui.synthesis.android.SpeechSynthetizer;

/**
 * Created by dbatiot on 23/09/17.
 */

public class ConversationActivity extends HotWordActivity {
    private static final String TAG = ConversationActivity.class.getSimpleName();

    MeanService meanService;
    private String meanMode;
    private String synthMode;

    private SpeechSynthetizer speechSynthetizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        meanMode = getString(R.string.pref_default_mean_mode);
        synthMode = getString(R.string.pref_default_synth_mode);
    }


    @Override
    protected void onResume() {
        Log.i(TAG, "onResume ");
        super.onResume();
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            meanMode = sharedPref.getString(SettingsActivity.MeanPreferenceFragment.MEAN_MODE, meanMode);
            synthMode = sharedPref.getString(SettingsActivity.SynthesisPreferenceFragment.SYNTH_MODE, synthMode);
        } catch (Exception e) {
            Toast.makeText(this, "SharedPreferences Exception" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (!StringUtils.isEmpty(synthMode)) {
            //android tts is quite long to init, so we do a warmup here
            speechSynthetizer = new SpeechSynthetizer(this, synthHandler);
        }
        if (meanMode.equals("api.ai")) {
            //APIAI_ACESS_TOKEN system env must be set in android studio startup script
            final AIConfiguration config = new AIConfiguration(BuildConfig.APIAI_ACESS_TOKEN);
            meanService = (MeanService) new ApiAiService(config);
            findAndConnectToLanMqttBroker();
        } else {
            meanService = new LocalAiService();
            findAndConnectToLanMqttBroker();
        }
    }


    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause ");
        if (speechSynthetizer != null) {
            speechSynthetizer.destroy();
            speechSynthetizer = null;
        }
        if (meanService != null) {
            meanService = null;
        }
        super.onPause();
    }

    private Handler synthHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MsgEnum message = MsgEnum.getMsgEnum(msg.what);
            switch (message) {
                //######################
                //      SYNTHETIZER
                //######################
                case MSG_TTS_PARTIAL_DONE:
                    //If the speech uterance is complete, listen to the next user input
                    onPartialResponseDone();
                    break;
                case MSG_TTS_FINAL_DONE:
                    //If the speech uterance is complete, go back to waiting the hot word
                    onFinalResponseDone();
                    break;
                case MSG_TTS_ERROR:
                    //If the speech uterance is complete, go back to waiting the hot word
                    onFinalResponseDone();
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };


    private void onPartialResponseDone() {
        switchToListenning();
    }

    private void onFinalResponseDone() {
        switchToHotWordDetection();
    }


    /**
     * Dispatch onListenningStart() to fragments.
     */
    @Override
    protected void onListenningStart() {
        super.onListenningStart();
        this.publish("im/command/chat/listenstart", "{\"origin\":\"im-vui\"}");
    }

    public void onListeningError(String errorText) {
        super.onListeningError(errorText);
        this.publish("im/command/chat/listenerror", "{\"origin\":\"im-vui\"}");
    }

    /**
     * /**
     * Event fires when recognition engine finish listening
     */
    public void onListeningFinished(String queryText) {
        this.publish("im/command/chat/request", "{\"origin\":\"im-vui\",\"text\":\"" + queryText + "\"}");
        //Don't call super flow, but only track the queryText
        updateLog(queryText,TextType.REQ);


        new AsyncTask<String, Void, MeanResponse>() {
            @Override
            protected MeanResponse doInBackground(String... requests) {
                final String request = requests[0];
                try {
                    final MeanResponse response = meanService.request(request);
                    return response;
                } catch (AIServiceException e) {
                }
                return null;
            }

            @Override
            protected void onPostExecute(MeanResponse aiResponse) {
                // process aiResponse here
                onMeaningResult(aiResponse);
            }
        }.execute(queryText);
    }


    void onMeaningResult(MeanResponse meanResponse) {
        if (meanResponse != null && meanResponse.statusCode == 0) {

            if (meanResponse.action != null) {
                this.publish("im/command/" + meanResponse.action, buildCommandPayload(meanResponse));
            }
            if (meanResponse.speech != null) {
                //Show the ai ai response
                this.publish("im/command/chat/response", "{\"origin\":\"im-vui\",\"text\":\"" + meanResponse.speech + "\"}");
                updateLog(meanResponse.speech,TextType.RES);
            }
            //myTts.speak(meanResponse.speech, TextToSpeech.QUEUE_FLUSH, null,""+meanResponse.speech.hashCode());
            if (speechSynthetizer != null && meanResponse.speech != null) {
                speechSynthetizer.speak(meanResponse.speech, meanResponse.actionIncomplete);
            } else if (meanResponse.actionIncomplete) {
                onPartialResponseDone();
            } else {
                onFinalResponseDone();
            }

        } else {
            Log.i(TAG, "ai no result: " + meanResponse);
        }

    }

    /**
     * serialize command payload based on ai repsonse
     *
     * @param meanResponse
     * @return
     */
    private String buildCommandPayload(MeanResponse meanResponse) {
        Log.i(TAG, "Status code: " + meanResponse.statusCode);
        JsonObject payload = new JsonObject();
        payload.addProperty("origin", meanResponse.source);
        payload.addProperty("intent", meanResponse.intentName);
        payload.addProperty("request", meanResponse.resolvedQuery);
        payload.addProperty("response", meanResponse.speech);
        JsonObject parameter = new JsonObject();
        if (meanResponse.parameters != null && !meanResponse.parameters.isEmpty()) {
            for (final Map.Entry<String, JsonElement> entry : meanResponse.parameters.entrySet()) {
                parameter.addProperty(entry.getKey(), entry.getValue().toString());
            }
        }
        payload.add("parameter", parameter);

        JsonArray messages = new JsonArray();
        //messages.put("messages");
        payload.add("messages", messages);

        final GsonBuilder gsonBuilder = new GsonBuilder();
        final Gson gson = gsonBuilder.create();
        return gson.toJson(payload);
    }
}
