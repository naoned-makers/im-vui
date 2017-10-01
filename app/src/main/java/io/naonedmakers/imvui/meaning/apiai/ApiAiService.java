package io.naonedmakers.imvui.meaning.apiai;

import ai.api.AIConfiguration;
import ai.api.AIDataService;
import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import io.naonedmakers.imvui.meaning.MeanResponse;
import io.naonedmakers.imvui.meaning.MeanService;

/**
 * Created by dbatiot on 29/09/17.
 */

public class ApiAiService extends AIDataService implements MeanService {
    /**
     * Make request to the AI service.
     *
     * @param queryText request object to the service. Cannot be <code>null</code>
     * @return response object from service. Never <code>null</code>
     */
    @Override
    public MeanResponse request(String queryText) throws AIServiceException {
        final AIRequest aiRequest = new AIRequest();
        aiRequest.setQuery(queryText);
        AIResponse aiResponse= super.request(aiRequest);
        MeanResponse meanResponse = new MeanResponse();

        meanResponse.source= aiResponse.getResult().getSource();
        meanResponse.action=aiResponse.getResult().getAction();
        meanResponse.speech=aiResponse.getResult().getFulfillment().getSpeech();
        meanResponse.intentName=aiResponse.getResult().getMetadata().getIntentName();
        meanResponse.parameters=aiResponse.getResult().getParameters();

        return meanResponse;
    }

    public void publish(String topic, String payLoadStr){
        //do nothing as it ia already handle by firebase
    }

    public void onDestroy(){}

    /**
     * Create new service with unique context for given configuration
     *
     * @param config Service configuration data. Cannot be <code>null</code>
     * @throws IllegalArgumentException If config parameter is null
     */
    public ApiAiService(AIConfiguration config) {
        super(config);
    }
}
