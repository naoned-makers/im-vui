package io.naonedmakers.imvui.meaning.local;

import ai.api.AIServiceException;
import io.naonedmakers.imvui.meaning.MeanResponse;
import io.naonedmakers.imvui.meaning.MeanService;


/**
 * Created by dbatiot on 29/09/17.
 */

public class LocalAiService implements MeanService {
    private static final String TAG = LocalAiService.class.getSimpleName();

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
        } else if (stringRequest.contains("bras")| stringRequest.contains("quoi")| stringRequest.contains("lebrun") ) {
            meanResponse.action = "leftarm/move";
            meanResponse.speech = "ok je bouge les bras";
            meanResponse.intentName = "sidepartmove";
        } else if (stringRequest.contains("main") | stringRequest.contains("ama")| stringRequest.contains("gamin") ) {
            meanResponse.action = "lefthand/move";
            meanResponse.speech = "ok je bouge les mains";
            meanResponse.intentName = "sidepartmove";
        } else if (stringRequest.contains("bonjour")) {
            meanResponse.action = null;
            meanResponse.speech = "Bonjour tony";
            meanResponse.intentName = "greetings";
        } else if (stringRequest.contains("ieu") |stringRequest.contains("mk2") ) {
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





}
