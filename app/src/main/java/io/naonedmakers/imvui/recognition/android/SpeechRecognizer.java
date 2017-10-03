package io.naonedmakers.imvui.recognition.android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;

import io.naonedmakers.imvui.MsgEnum;

public class SpeechRecognizer {

    private static final String TAG = "SpeechRecognizer";

    private Handler activityCallBack;
    private android.speech.SpeechRecognizer sr;
    private boolean preferOffline;


    public SpeechRecognizer(Handler activityCallBack,boolean preferOffline) {
        this.activityCallBack = activityCallBack;
        this.preferOffline = preferOffline;
    }

    /**
     * Take speech input and convert it back as text
     */
    public void startRecognizing(Context appContext) {
        //Prepeare Intent 
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toString());
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE,preferOffline);
        //intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Parle TODO REMOVE");
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false);
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 0);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.getPackageName());

        // secret parameters that when added provide audio url in the result
        intent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
        intent.putExtra("android.speech.extra.GET_AUDIO", true);


        //Add listeners
        CustomRecognitionListener listener = new CustomRecognitionListener();
        sr = android.speech.SpeechRecognizer.createSpeechRecognizer(appContext);
        sr.setRecognitionListener(listener);
        sr.startListening(intent);

        //sr.cancel();
        //sr.destroy();
        //sr.stopListening();

    }
    public void stopRecognizing() {
        if(sr!=null){
            //sr.cancel();
            sr.destroy();
            sr=null;
        }
    }




    class CustomRecognitionListener implements RecognitionListener {


        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "onReadyForSpeech");
        }

        public void onBeginningOfSpeech() {
            Log.d(TAG, "onBeginningOfSpeech");
        }

        public void onRmsChanged(float rmsdB) {
            //Log.d(TAG, "onRmsChanged"+(rmsdB*10));
            sendMessage(MsgEnum.MSG_STT_LEVEL,Math.max(0,Math.min(Math.round(rmsdB*10),100)));
        }

        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "onBufferReceived");
        }

        public void onEndOfSpeech() {
            Log.d(TAG, "onEndofSpeech");
        }

        public void onError(int error) {
            Log.e(TAG, "error " + error);
            sendMessage(MsgEnum.MSG_STT_ERROR, ErrorTranslator.getErrorText(error));
        }

        public void onResults(Bundle results) {
            String str = new String();
            ArrayList data = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
            Log.d(TAG, "onResults RECOGNITION" + data);
            //Log.d(TAG, "onResults EXTRA" + results.getStringArrayList(RecognizerIntent.EXTRA_RESULTS));

            Log.d(TAG, "onResults CONFIDENCE_SCORES" + Arrays.asList(results.getFloatArray(android.speech.SpeechRecognizer.CONFIDENCE_SCORES)));


            if(data.size()>0){
                //We take best score match
                sendMessage(MsgEnum.MSG_STT_TEXT, data.get(0).toString());
            }else{
                sendMessage(MsgEnum.MSG_STT_ERROR, ErrorTranslator.getErrorText(android.speech.SpeechRecognizer.ERROR_NO_MATCH));
            }
        }

        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults"+partialResults);
        }

        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "onEvent " + eventType);
        }


    }


    private void sendMessage(MsgEnum what, Object obj){
        if (null != activityCallBack) {
            Message msg = activityCallBack.obtainMessage(what.ordinal(), obj);
            activityCallBack.sendMessage(msg);
        }
    }

} 