package io.naonedmakers.imvui.synthesis.android;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

import io.naonedmakers.imvui.MsgEnum;
import io.naonedmakers.imvui.R;

public class SpeechSynthetizer  extends UtteranceProgressListener implements TextToSpeech.OnInitListener{

    private static final String TAG = "SpeechSynthetizer";

    private TextToSpeech textToSpeech;
    private Handler synthHandler;
    private boolean isReady=false;

    public SpeechSynthetizer(final Context context,Handler pSynthHandler) {
        this.isReady = false;
        textToSpeech = new TextToSpeech(context,this);
        textToSpeech.setOnUtteranceProgressListener(this);
        synthHandler = pSynthHandler;

    }

    /**
     * OnInitListener
     * @param status
     */
    @Override
    public void onInit(int status) {
        this.isReady = (status != TextToSpeech.ERROR);
        Log.d(TAG, "onInit");
        if(this.isReady) {
            textToSpeech.setLanguage(Locale.getDefault());
            //textToSpeech.setPitch(1.3f);
            //textToSpeech.setSpeechRate(1f);
            textToSpeech.setLanguage(Locale.FRENCH);
            //Log.e("XgetDefaultEngine",""+textToSpeech.getDefaultEngine());
            //Log.e("XgetDefaultVoice",""+textToSpeech.getDefaultVoice());
            //Log.e("XgetAvailableLanguages",""+textToSpeech.getAvailableLanguages());
            //Log.e("XgetEngines",""+textToSpeech.getEngines());
            //Log.e("XgetVoice",""+textToSpeech.getVoice());
            //Log.e("XgetVoices",""+textToSpeech.getVoices());
            //Log.e("XgetAvailableLanguages",""+textToSpeech.getAvailableLanguages());
            textToSpeech.addEarcon("bonjour", "io.naonedmakers.imvui", R.raw.goodmorn1);
        }else {
            Log.e("XgetDefaultEngine","FAILED_TO_INITILIZE_TTS_ENGINE");
        }
    }

    /**
     * Called when an utterance "starts" as perceived by the caller.
     * @param utteranceId
     */
    @Override
    public void onStart(String utteranceId) {
        Log.d(TAG, "onStart "+utteranceId);
        sendMessage(MsgEnum.MSG_TTS_START,null);
    }

    /**
     *
     * Called when an utterance has successfully completed processing.
     * @param utteranceId
     */
    @Override
    public void onDone(String utteranceId) {
        Log.d(TAG, "onDone "+utteranceId);
        if(utteranceId.startsWith("FINAL")){
            sendMessage(MsgEnum.MSG_TTS_FINAL_DONE,null);
        }else{
            sendMessage(MsgEnum.MSG_TTS_PARTIAL_DONE,null);
        }

    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public void onError(String s) {

    }

    public void destroy(){
        this.isReady = false;
        if(this.textToSpeech!=null){
            this.textToSpeech.stop();
            this.textToSpeech.shutdown();
            this.textToSpeech=null;
        }
    }



    public void speak(final String message,boolean isPartial) {
        if (isReady && !textToSpeech.isSpeaking() && message !=null) {
            String utteranceId = isPartial? "PARTIAL":"FINAL" +message.hashCode();
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
        }
    }
    public void playEarcon(final String message) {
        if (isReady && !textToSpeech.isSpeaking() && message !=null) {
            String utteranceId = message.hashCode() + "";
            Bundle params = null;
            //params.putString(TextToSpeech.Engine.KEY_PARAM_STREAM,String.valueOf(AudioManager.STREAM_ALARM));
            textToSpeech.playEarcon(message, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        }
    }


    private void sendMessage(MsgEnum what, Object obj){
        if (null != synthHandler) {
            Message msg = synthHandler.obtainMessage(what.ordinal(), obj);
            synthHandler.sendMessage(msg);
        }
    }

}
