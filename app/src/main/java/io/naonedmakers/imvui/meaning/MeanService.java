package io.naonedmakers.imvui.meaning;

import ai.api.AIServiceException;
import ai.api.RequestExtras;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

/**
 * Created by dbatiot on 29/09/17.
 */

public interface MeanService {

    /**
     * Make request to the AI service.
     *
     * @param request request object to the service. Cannot be <code>null</code>
     * @return response object from service. Never <code>null</code>
     */
    public MeanResponse request(final String request) throws AIServiceException ;
}
