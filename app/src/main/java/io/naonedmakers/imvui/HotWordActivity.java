package io.naonedmakers.imvui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

import io.naonedmakers.imvui.hotword.snowboy.AppResCopy;
import io.naonedmakers.imvui.hotword.snowboy.HotWordThread;
import io.naonedmakers.imvui.recognition.android.SpeechRecognizer;

/**
 * Created by dbatiot on 19/09/17.
 */

public class HotWordActivity extends UiBaseActivity {
    private static final String TAG = HotWordActivity.class.getSimpleName();

    //private MediaPlayer player;
    private HotWordThread hotWordThread;
    private SpeechRecognizer speechRecognizer;
    private String sensitivity;
    private float audioGain;
    private String activeModel;
    private  boolean hotWordActivated=false;
    private String recoMode;

    @Override
    protected void onDestroy() {
        //player.release();
        //player=null;
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //default value
        sensitivity = getString(R.string.pref_default_sensitivity);
        audioGain = Float.parseFloat(getString(R.string.pref_default_audio_gain));
        activeModel = getString(R.string.pref_default_snowboy_model);
        recoMode = getString(R.string.pref_default_reco_mode);
        AppResCopy.copyResFromAssetsToSD(this);
        /**
         player = new MediaPlayer();
         try {
         player.setDataSource(getResources().openRawResourceFd(R.raw.startlistening));
         player.prepare();
         } catch (IOException e) {
         Log.e(TAG, "Playing ding sound error", e);
         }*/
    }


    protected void startHotWordDetection() {
        Log.v(TAG, "startHotWordDetection "+hotWordActivated);
        if(hotWordActivated){
            hotWordThread.startDetecting();
            updateLog(" ----> Waiting HotWord", "blue");
        }else{
            updateLog(" ----> Waiting touch", "blue");
        }
        fab.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_btn_speak_now));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switchToListenning();
            }
        });

    }

    protected void stopHotWordDetection() {
        Log.v(TAG, "stopHotWordDetection");
        if (hotWordThread != null) {
            hotWordThread.stopDetecting();
        }
    }

    protected void switchToListenning() {
        Log.v(TAG, "switchToListenning");
        updateLog(" ----> Please Speak", "green");
        stopHotWordDetection();

        speechRecognizer = new SpeechRecognizer(handle, recoMode.equals("offline_android"));//preferOffline true or false
        speechRecognizer.startRecognizing(this);

        Snackbar.make(fab, "Listenning to speech", Snackbar.LENGTH_SHORT);
        fab.setImageDrawable(ContextCompat.getDrawable(this, android.R.drawable.ic_menu_close_clear_cancel));
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startHotWordDetection();
            }
        });
    }

    protected void switchToHotWordDetection() {
        Log.v(TAG, "switchToHotWordDetection");
        stopListenning();
        startHotWordDetection();
    }

    protected void stopListenning() {
        Log.v(TAG, "stopListenning");
        if (speechRecognizer != null) {
            speechRecognizer.stopRecognizing();
            speechRecognizer = null;
        }
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        stopListenning();
        stopHotWordDetection();
        if (hotWordThread != null) {
            hotWordThread.cleanDetecting();
            hotWordThread=null;
        }
        updateLog(" ----> Pause", "black");
        super.onPause();
    }

    /**
     * Dispatch onResume() to fragments.  Note that for better inter-operation
     * with older versions of the platform, at the point of this call the
     * fragments attached to the activity are <em>not</em> resumed.  This means
     * that in some cases the previous state may still be saved, not allowing
     * fragment transactions that modify the state.  To correctly interact
     * with fragments in their proper state, you should instead override
     * {@link #onResumeFragments()}.
     */
    @Override
    protected void onResume() {
        super.onResume();
        try {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            sensitivity = sharedPref.getString(SettingsActivity.HotWordPreferenceFragment.HOT_WORD_SENSITIVITY, sensitivity);
            audioGain = Float.parseFloat(sharedPref.getString(SettingsActivity.HotWordPreferenceFragment.HOT_WORD_AUDIO_GAIN, "" + audioGain));
            activeModel = sharedPref.getString(SettingsActivity.HotWordPreferenceFragment.HOT_WORD_MODEL, activeModel);
            hotWordActivated = sharedPref.getBoolean(SettingsActivity.HotWordPreferenceFragment.HOT_WORD_ACTIVATED, false);
            recoMode = sharedPref.getString(SettingsActivity.RecognitionPreferenceFragment.RECO_MODE, recoMode);
        } catch (Exception e) {
            Toast.makeText(this, "SharedPreferences Exception" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
        if (checkAudioRecordPermission()) {

            String commonRes = this.getFilesDir().getAbsolutePath() + "/common.res";
            hotWordThread = new HotWordThread(handle, this.getFilesDir().getAbsolutePath() + "/" + activeModel, commonRes, sensitivity, audioGain);

            startHotWordDetection();
        }
    }

    public Handler handle = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            MsgEnum message = MsgEnum.getMsgEnum(msg.what);
            switch (message) {
                //######################
                //      HOT WORD
                //######################
                case MSG_HOT_DETECTED:
                    switchToListenning();
                    break;
                case MSG_HOT_LEVEL:
                    updateSoundLevel((int) msg.obj);
                    break;
                case MSG_HOT_ERROR:
                    updateLog(" ----> " + msg.toString(), "red");
                    stopHotWordDetection();
                    break;
                //######################
                //          SST
                //######################
                case MSG_STT_TEXT:
                    onListeningFinished((String) msg.obj);
                    break;
                case MSG_STT_ERROR:
                    onListeningError((String) msg.obj);
                    break;
                case MSG_STT_LEVEL:
                    //updateLog(" ----> " + message, "black");
                    updateSoundLevel((int) msg.obj);
                    break;
                default:
                    super.handleMessage(msg);
                    break;
            }
        }
    };

    /**
     * Event fires when recognition engine finish listening
     */
    public void onListeningFinished(String queryText) {
        updateLog(queryText, "black");
    }
    /**
     * Event fires when recognition engine error
     */
    public void onListeningError(String errorText) {
        updateLog(errorText, "red");
        switchToHotWordDetection();
    }

}