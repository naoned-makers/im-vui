package io.naonedmakers.imvui.meaning.local;

import android.app.backup.FullBackupDataOutput;
import android.util.Log;

import ai.api.AIServiceException;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.Fulfillment;
import ai.api.model.Metadata;
import ai.api.model.Result;
import io.naonedmakers.imvui.meaning.MeanResponse;
import io.naonedmakers.imvui.meaning.MeanService;

/**
 * Created by dbatiot on 29/09/17.
 */

public class LocalAiService implements MeanService {
    /**
     * Make request to the AI service.
     *
     * @param stringRequest request object to the service. Cannot be <code>null</code>
     * @return response object from service. Never <code>null</code>
     */
    @Override
    public MeanResponse request(String stringRequest) throws AIServiceException {

        MeanResponse meanResponse = new MeanResponse();


        meanResponse.source= "im-vui";
        meanResponse.action="";
        meanResponse.speech="";
        //metadata.setIntentId("");
        meanResponse.intentName="";
        meanResponse.parameters=null;

        return meanResponse;
    }
}
