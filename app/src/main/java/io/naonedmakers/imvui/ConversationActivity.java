package io.naonedmakers.imvui;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.JsonElement;

import java.util.HashMap;
import java.util.Map;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Metadata;
import ai.api.model.Result;
import ai.api.model.Status;
import ai.api.util.StringUtils;
import io.naonedmakers.imvui.meaning.MeanResponse;
import io.naonedmakers.imvui.meaning.MeanService;
import io.naonedmakers.imvui.meaning.apiai.ApiAiService;
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
        } else {
            meanService = null;
        }
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        speechSynthetizer.destroy();
        speechSynthetizer = null;
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
     * /**
     * Event fires when recognition engine finish listening
     */
    public void onListeningFinished(String queryText) {

        //Don't call super flow, but only track the queryText
        updateLog(queryText, "black");


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


    void onMeaningResult(MeanResponse aiResponse) {
        if (aiResponse != null) {
            Log.i(TAG, "Status code: " + aiResponse.statusCode);
//            Log.i(TAG, "Status type: " + status.getErrorType());

            //          final Result result = aiResponse.getResult();
            //        if (result != null) {
            Log.i(TAG, "Resolved query: " + aiResponse.resolvedQuery);
            Log.i(TAG, "Action: " + aiResponse.action);
            //Get speech
            final String speech = aiResponse.speech;
            Log.i(TAG, "Speech: " + speech);
            //Get metadata

//                    Log.i(TAG, "Intent id: " + aiResponse.getIntentId());
            Log.i(TAG, "Intent name: " + aiResponse.intentName);

            //Get parameters
            final HashMap<String, JsonElement> params = aiResponse.parameters;
            if (params != null && !params.isEmpty()) {
                Log.i(TAG, "Parameters: ");
                for (final Map.Entry<String, JsonElement> entry : params.entrySet()) {
                    Log.i(TAG, String.format("%s: %s", entry.getKey(), entry.getValue().toString()));
                }
            }

            //Show the ai ai response
            updateLog(speech, "#FF69B4");


            if (speechSynthetizer != null) {
                speechSynthetizer.speak(speech, aiResponse.actionIncomplete);
            } else if (aiResponse.actionIncomplete) {
                onPartialResponseDone();
            } else {
                onFinalResponseDone();
            }

        } else {
            Log.i(TAG, "Api.ai no result: " + aiResponse);
        }

    }

    ;

}
