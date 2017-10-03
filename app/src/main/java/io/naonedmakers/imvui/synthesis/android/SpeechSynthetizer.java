package io.naonedmakers.imvui.synthesis.android;

import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

import io.naonedmakers.imvui.MsgEnum;
import io.naonedmakers.imvui.R;

public class SpeechSynthetizer extends UtteranceProgressListener implements TextToSpeech.OnInitListener {

    private static final String TAG = "SpeechSynthetizer";

    private TextToSpeech textToSpeech;
    private Handler synthHandler;
    private boolean isReady = false;

    public SpeechSynthetizer(final Context context, Handler pSynthHandler) {
        this.isReady = false;
        textToSpeech = new TextToSpeech(context.getApplicationContext(), this);
        //arams.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(audioManager.STREAM_ALARM);
        textToSpeech.setOnUtteranceProgressListener(this);
        synthHandler = pSynthHandler;

        AudioManager audioManager = (AudioManager) context.getApplicationContext().getSystemService(context.AUDIO_SERVICE);
        audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM ,AudioManager.ADJUST_MUTE, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM ,AudioManager.ADJUST_MUTE, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC ,AudioManager.ADJUST_RAISE, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION ,AudioManager.ADJUST_MUTE, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_VOICE_CALL ,AudioManager.ADJUST_MUTE, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING ,AudioManager.ADJUST_MUTE, 0);
        audioManager.adjustStreamVolume(AudioManager.STREAM_ACCESSIBILITY ,AudioManager.ADJUST_MUTE, 0);


        amStreamVoiceCallMaxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL);

        if (audioManager.isBluetoothScoAvailableOffCall()) {
            audioManager.startBluetoothSco();
        }
        if (!audioManager.isSpeakerphoneOn()) {
            audioManager.setSpeakerphoneOn(true);
        }
        audioManager.requestAudioFocus(null, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);
    }

    int amStreamVoiceCallMaxVol = 10;

    /**
     * OnInitListener
     *
     * @param status
     */
    @Override
    public void onInit(int status) {
        this.isReady = (status != TextToSpeech.ERROR);
        Log.d(TAG, "onInit " + isReady + " " + !textToSpeech.isSpeaking());
        if (this.isReady) {
            //textToSpeech.setLanguage(Locale.getDefault());
            //textToSpeech.setPitch(1.3f);
            //textToSpeech.setSpeechRate(1f);
            //textToSpeech.setLanguage(Locale.FRENCH);
            //Log.e("XgetDefaultEngine",""+textToSpeech.getDefaultEngine());
            //Log.e("XgetDefaultVoice",""+textToSpeech.getDefaultVoice());
            //Log.e("XgetAvailableLanguages",""+textToSpeech.getAvailableLanguages());
            //Log.e("XgetEngines",""+textToSpeech.getEngines());
            //Log.e("XgetVoice",""+textToSpeech.getVoice());
            //Log.e("XgetVoices",""+textToSpeech.getVoices());
            //Log.e("XgetAvailableLanguages",""+textToSpeech.getAvailableLanguages());
            //textToSpeech.addEarcon("bonjour", "io.naonedmakers.imvui", R.raw.goodmorn1);
        } else {
            sendMessage(MsgEnum.MSG_TTS_ERROR, null);
            Log.e("XgetDefaultEngine", "FAILED_TO_INITILIZE_TTS_ENGINE");
        }
    }

    /**
     * Called when an utterance "starts" as perceived by the caller.
     *
     * @param utteranceId
     */
    @Override
    public void onStart(String utteranceId) {
        Log.d(TAG, "onStart " + utteranceId);
        sendMessage(MsgEnum.MSG_TTS_START, null);
    }

    /**
     * Called when an utterance has successfully completed processing.
     *
     * @param utteranceId
     */
    @Override
    public void onDone(String utteranceId) {
        Log.d(TAG, "onDone " + utteranceId);
        if (utteranceId.startsWith("FINAL")) {
            sendMessage(MsgEnum.MSG_TTS_FINAL_DONE, null);
        } else {
            sendMessage(MsgEnum.MSG_TTS_PARTIAL_DONE, null);
        }

    }

    /**
     * @param s
     * @deprecated
     */
    @Override
    public void onError(String s) {
        Log.d(TAG, "onError " + s);
        sendMessage(MsgEnum.MSG_TTS_ERROR, null);
    }

    public void destroy() {
        Log.d(TAG, "onDestroy ");
        this.isReady = false;
        if (this.textToSpeech != null) {
            this.textToSpeech.stop();
            this.textToSpeech.shutdown();
            this.textToSpeech = null;
        }
    }


    public void speak(final String message, boolean isPartial) {
        Log.d(TAG, "speak " + message + " " + isReady);
        if (isReady && !textToSpeech.isSpeaking() && message != null) {
            String utteranceId = isPartial ? "PARTIAL" : "FINAL" + message.hashCode();
            //Bundle params = null;
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (float) amStreamVoiceCallMaxVol);
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL);
            textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        } else {
            sendMessage(MsgEnum.MSG_TTS_ERROR, null);
        }
    }

    public void playEarcon(final String earcon) {
        Log.d(TAG, "speak " + earcon + "  " + isReady);
        if (isReady && !textToSpeech.isSpeaking() && earcon != null) {
            String utteranceId = earcon.hashCode() + "";
            Bundle params = new Bundle();
            params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, (float) amStreamVoiceCallMaxVol);
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_VOICE_CALL);
            textToSpeech.playEarcon(earcon, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
        }
    }

    public void playSilence(long milis) {
        Log.d(TAG, "playSilence " + isReady + "" + !textToSpeech.isSpeaking());
        if (isReady && !textToSpeech.isSpeaking()) {
            textToSpeech.playSilentUtterance(milis, TextToSpeech.QUEUE_FLUSH, "silent");
        }
    }


    private void sendMessage(MsgEnum what, Object obj) {
        if (null != synthHandler) {
            Message msg = synthHandler.obtainMessage(what.ordinal(), obj);
            synthHandler.sendMessage(msg);
        }
    }

}
