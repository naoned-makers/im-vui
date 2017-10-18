package io.naonedmakers.imvui.meaning.local;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

import java.util.HashMap;
import java.util.Random;

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
        meanResponse.actionIncomplete = false;
        meanResponse.resolvedQuery = stringRequest;
        stringRequest = stringRequest.toLowerCase();
        if (stringRequest.contains("tête") | stringRequest.contains("êtes")) {
            meanResponse.action = "head/move";
            meanResponse.speech = "ok je bouge la tête";
            meanResponse.intentName = "headmove";
        } else if (stringRequest.contains("casque") | stringRequest.contains("visière")) {
            meanResponse.action = "helmet/move";
            meanResponse.speech = "ok je bouge mon casque";
            meanResponse.intentName = "helmetmove";
        }
        else if (stringRequest.contains("bras") | stringRequest.contains("quoi") | stringRequest.contains("lebrun")) {
            Random random = new Random();
            if (random.nextBoolean()) {
                meanResponse.action = "leftarm/move";
            } else {
                meanResponse.action = "rightarm/move";
            }
            meanResponse.speech = "ok je bouge les bras";
            meanResponse.intentName = "sidepartmove";
        } else if (stringRequest.contains("main") | stringRequest.contains("ama") | stringRequest.contains("gamin")) {
            Random random = new Random();
            if (random.nextBoolean()) {
                meanResponse.action = "lefthand/move";
            } else {
                meanResponse.action = "righthand/move";
            }
            meanResponse.speech = "ok je bouge les mains";
            meanResponse.intentName = "sidepartmove";
        } else if (stringRequest.contains("bonjour")||stringRequest.contains("salut")) {
            meanResponse.action = null;
            meanResponse.speech = "Bonjour Tony";
            meanResponse.intentName = "greetings";
        } else if (stringRequest.contains("ieu") | stringRequest.contains("mk2")) {
            meanResponse.action = "eyes";
            meanResponse.speech = "ok j'active mes yeux";
            meanResponse.intentName = "eyes";
        } else if (stringRequest.contains("bleu")
                | stringRequest.contains("rouge")
                | stringRequest.contains("vert")
                | stringRequest.contains("jaune")
                | stringRequest.contains("orange")
                | stringRequest.contains("rose")
                | stringRequest.contains("violet")) {
            meanResponse.action = "im/color";
            meanResponse.speech = "J'aime bien cette couleur";
            meanResponse.intentName = "color";
            HashMap<String, JsonElement> params = new HashMap<String, JsonElement>();
            if(stringRequest.contains("bleu")) {
                params.put("rgba", new JsonPrimitive("0000FFFF"));
            }else if(stringRequest.contains("rouge")) {
                params.put("rgba", new JsonPrimitive("FF0000FF"));
            }else if(stringRequest.contains("vert")) {
                params.put("rgba", new JsonPrimitive("00FF00FF"));
            }else if(stringRequest.contains("jaune")) {
                params.put("rgba", new JsonPrimitive("FFFF00FF"));
            }else if(stringRequest.contains("orange")) {
                params.put("rgba", new JsonPrimitive("FFA500FF"));
            }else if(stringRequest.contains("rose")) {
                params.put("rgba", new JsonPrimitive("FFC0CBFF"));
            }else if(stringRequest.contains("violet")) {
                params.put("rgba", new JsonPrimitive("800080FF"));
            }
            meanResponse.parameters = params;
        } else {
            meanResponse.action = null;
            meanResponse.speech = "Je ne comprends pas";
            meanResponse.intentName = null;
            //meanResponse.intentName = "none";
            //HashMap<String, JsonElement> params = new HashMap<String, JsonElement>();
            //params.put("wipparam", new JsonPrimitive("wipvalue"));
            //meanResponse.parameters = params;
        }
        return meanResponse;
    }


}
