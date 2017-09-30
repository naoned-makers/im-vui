package io.naonedmakers.imvui.hotword.snowboy;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import ai.kitt.snowboy.SnowboyDetect;
import ai.kitt.snowboy.snowboy;
import ai.kitt.snowboy.snowboyJNI;

import io.naonedmakers.imvui.MsgEnum;


public class HotWordThread {
    static { System.loadLibrary("snowboy-detect-android"); }

    private static final String TAG = HotWordThread.class.getSimpleName();
    public static final int SAMPLE_RATE = 16000;

    private boolean shouldContinue;
    private Handler activityCallBack = null;
    private Thread thread;
    private SnowboyDetect detector = null;


    public HotWordThread(Handler activityCallBack,String activeModel, String commonRes,String sensitivity,float audioGain) {
        this.activityCallBack = activityCallBack;

        detector = new SnowboyDetect(commonRes, activeModel);

        //Log.v(TAG, "GetSensitivity:"+detector.GetSensitivity() + "  NumHotwords:"+detector.NumHotwords());
        // GetSensitivity:0.4  NumHotwords:1
        //Log.v(TAG, "  BitsPerSample:"+detector.BitsPerSample()+ "  NumChannels:"+detector.NumChannels()+ "  SampleRate:"+detector.SampleRate());
        //BitsPerSample:16  NumChannels:1  SampleRate:16000


        // Sets the sensitivity string for the loaded hotwords. A <sensitivity_str> is
        // a list of floating numbers between 0 and 1, and separated by comma. For
        // example, if there are 3 loaded hotwords, your string should looks something
        // like this:
        //   0.4,0.5,0.8
        // Make sure you properly align the sensitivity value to the corresponding
        // hotword.
        detector.SetSensitivity(sensitivity);//0.5
        // When sensitiviy is higher, the hotword gets more easily triggered. But you might get more false alarms.

        // Applied a fixed gain to the input audio. In case you have a very weak
        // microphone, you can use this function to boost input audio level.
        // whether to increase (>1) or decrease (<1) input volume.
        detector.SetAudioGain(audioGain);
        detector.ApplyFrontend(true);

    }

    private void sendMessage(MsgEnum what, Object obj){
        if (null != activityCallBack) {
            Message msg = activityCallBack.obtainMessage(what.ordinal(), obj);
            activityCallBack.sendMessage(msg);
        }
    }

    public void startDetecting() {
        if (thread != null)
            return;

        shouldContinue = true;
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                record();
            }
        });
        thread.start();
    }

    public void stopDetecting() {
        if (thread == null)
            return;

        shouldContinue = false;
        try {
            Thread.sleep(100);
        } catch (Exception e) {
        }
        thread = null;
    }

    public void cleanDetecting() {
        detector.delete();
        detector=null;
    }



    private void record() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

        // Buffer size in bytes: for 0.1 second of audio
        int bufferSize = (int)(SAMPLE_RATE * 0.1 * 2);
        if (bufferSize == AudioRecord.ERROR || bufferSize == AudioRecord.ERROR_BAD_VALUE) {
            bufferSize = SAMPLE_RATE * 2;
        }

        byte[] audioBuffer = new byte[bufferSize];
        AudioRecord record = new AudioRecord(
            MediaRecorder.AudioSource.DEFAULT,
                SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize);

        if (record.getState() != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "Audio Record can't initialize!");
            return;
        }
        record.startRecording();
        Log.v(TAG, "Start detecting");

        long shortsRead = 0;
        detector.Reset();
        while (shouldContinue) {
            record.read(audioBuffer, 0, audioBuffer.length);
            // Converts to short array.
            short[] audioData = new short[audioBuffer.length / 2];
            ByteBuffer.wrap(audioBuffer).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(audioData);

            shortsRead += audioData.length;

            // Snowboy hotword detection.
            int result = detector.RunDetection(audioData, audioData.length);

            if (result == -2) {
                // NO SPEECH
                sendMessage(MsgEnum.MSG_HOT_LEVEL, 10);
            } else if (result == -1) {
                sendMessage(MsgEnum.MSG_HOT_ERROR, "Unknown Detection Error");
            } else if (result == 0) {
                // SPEECH but not the hotworld
                //recorder.getMaxAmplitude()
                sendMessage(MsgEnum.MSG_HOT_LEVEL,60);
            } else if (result > 0) {
                sendMessage(MsgEnum.MSG_HOT_DETECTED, null);
                Log.i("Snowboy: ", "Hotword " + Integer.toString(result) + " detected!");
            }
        }
        //detector.Reset();
        record.stop();
        //record.release();
        Log.v(TAG, String.format("Recording stopped. Samples read: %d", shortsRead));
    }





}
