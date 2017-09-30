package io.naonedmakers.imvui.meaning;

import com.google.gson.JsonElement;
import com.google.gson.annotations.SerializedName;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ai.api.model.AIContext;
import ai.api.model.AIEvent;
import ai.api.model.AIOriginalRequest;
import ai.api.model.AIOutputContext;
import ai.api.model.Fulfillment;
import ai.api.model.Metadata;
import ai.api.model.ResponseMessage;
import ai.api.model.Result;
import ai.api.model.Status;

/**
 * Created by dbatiot on 30/09/17.
 */

public class MeanResponse {

    private static final long serialVersionUID = 1L;

    /**
     * Unique identifier of the result.
     */
    //private String id;
    //private Date timestamp;
    //private String lang;
    //private String sessionId;
    //private float score;
    //private List<AIOutputContext> contexts;


    public Integer statusCode;
    public String action;
    public String source;
    public HashMap<String, JsonElement> parameters;
    public String resolvedQuery;
    public boolean actionIncomplete;
    public String intentName;


    public String speech;
    public List<ResponseMessage> messages;
    public String displayText;
    //private Map<String, JsonElement> data;




}
