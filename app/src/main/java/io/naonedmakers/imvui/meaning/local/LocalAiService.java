package io.naonedmakers.imvui.meaning.local;

import android.util.Log;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.HashMap;

import ai.api.AIServiceException;
import io.naonedmakers.imvui.meaning.MeanResponse;
import io.naonedmakers.imvui.meaning.MeanService;


/**
 * Created by dbatiot on 29/09/17.
 */

public class LocalAiService implements MeanService {
    private static final String TAG = LocalAiService.class.getSimpleName();
    MqttClient sampleClient;

    /**
     * Find the intent behind the stringRequest
     *
     * @param stringRequest request object to the service. Cannot be <code>null</code>
     * @return response object from service. Never <code>null</code>
     */
    @Override
    public MeanResponse request(String stringRequest) throws AIServiceException {

        MeanResponse meanResponse = new MeanResponse();
        meanResponse.source = "im-vui";
        meanResponse.statusCode = 0;
        meanResponse.actionIncomplete=false;
        meanResponse.resolvedQuery=stringRequest;
        stringRequest = stringRequest.toLowerCase();
        if (stringRequest.contains("tête")|stringRequest.contains("êtes")) {
            meanResponse.action = "head/move";
            meanResponse.speech = "ok je bouge la tête";
            meanResponse.intentName = "headmove";
        } else if (stringRequest.contains("bras")) {
            meanResponse.action = "leftarm/move";
            meanResponse.speech = "ok je bouge les bras";
            meanResponse.intentName = "sidepartmove";
        } else if (stringRequest.contains("main") | stringRequest.contains("ama") ) {
            meanResponse.action = "lefthand/move";
            meanResponse.speech = "ok je bouge les mains";
            meanResponse.intentName = "sidepartmove";
        } else if (stringRequest.contains("bonjour")) {
            meanResponse.action = null;
            meanResponse.speech = "Bonjour tony";
            meanResponse.intentName = "greetings";
        } else if (stringRequest.contains("ieu")) {
            meanResponse.action = null;
            meanResponse.speech = "ok j'active mes yeux";
            meanResponse.intentName = "eyes";
        } else {
            meanResponse.action = null;
            meanResponse.speech = "Je ne comprends pas";
            meanResponse.intentName =null;
            //meanResponse.intentName = "none";
            //HashMap<String, JsonElement> params = new HashMap<String, JsonElement>();
            //params.put("wipparam", new JsonPrimitive("wipvalue"));
            //meanResponse.parameters = params;
        }
        return meanResponse;
    }

    public void publish(String topic, String payLoadStr) {
        Log.d(TAG, "publish " + topic + "->" + payLoadStr);
        int qos = 0;
        try {
            MqttMessage message = new MqttMessage(payLoadStr.getBytes());
            message.setQos(qos);
            sampleClient.publish(topic, message);
        } catch (MqttException me) {
            Log.d(TAG, "reason " + me.getReasonCode() + " msg " + me.getMessage());
        }
    }

    /**
     * Create new service with unique context for given configuration
     *
     * @param brokerIp
     * @throws IllegalArgumentException If config parameter is null
     */
    public LocalAiService(String brokerIp) {
        Log.d(TAG, "LocalAiService " + brokerIp);
        String broker = "tcp://" + brokerIp + ":1883";
        String clientId = "vui_" + (Math.random() * 1000000000 + "").substring(2, 8);
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            Log.d(TAG, "Connecting to broker: " + broker);
            sampleClient.connect(connOpts);
        } catch (MqttException me) {
            Log.d(TAG, "reason " + me.getReasonCode() + "msg " + me.getMessage() + "cause " + me.getCause());
        }
    }

    public void onDestroy() {
        Log.d(TAG, "onDestroy ");
        if (sampleClient != null) {
            try {
                sampleClient.disconnect();
            } catch (MqttException me) {
                Log.d(TAG, "reason " + me.getReasonCode() + "msg " + me.getMessage() + "cause " + me.getCause());
            }
        }
    }


}
