package io.naonedmakers.imvui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

public class UiBaseActivity extends AppCompatActivity {
    private static final String TAG = UiBaseActivity.class.getSimpleName();

    static String strLog = null;
    private TextView log;
    protected FloatingActionButton fab;
    private ScrollView logView;
    //TODO change with https://github.com/zagum/SpeechRecognitionView
    private ProgressBar soundLevel;
    private Menu menu;
    private boolean headSetStatus = false;
    private boolean mqttStatus = false;
    private static final int REQUEST_AUDIO_PERMISSIONS_ID = 33;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        log = (TextView) findViewById(R.id.log);
        logView = (ScrollView) findViewById(R.id.logView);
        soundLevel = (ProgressBar) findViewById(R.id.progressbar_level);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_home, menu);
        this.menu = menu;
        displayBTHeadSetStatus();
        displayMqtttStatus();
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_admin) {
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            String lastBrokerIp = sharedPref.getString(SettingsActivity.MeanPreferenceFragment.BROKER_IP, null);
            if (lastBrokerIp != null) {
                Toast.makeText(this, "Opening Web admin", Toast.LENGTH_LONG).show();
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("http://" + lastBrokerIp + ":8081/"));
                startActivity(browserIntent);
            } else {
                Toast.makeText(this, "No Server yet found", Toast.LENGTH_LONG).show();
            }
            return true;
        } else if (id == R.id.action_touch) {
            Intent intent = new Intent(this, WebTouchActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_mqtt) {
            //findAndConnectToLanMqttBroker() {
            return true;
        } else if (id == R.id.action_headset) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    //##############################################################


    static int MAX_LOG_LINE_NUM = 200;
    static int currLogLineNum = 0;

    public void updateLog(final String text, final String color) {
        log.post(new Runnable() {
            @Override
            public void run() {
                if (currLogLineNum >= MAX_LOG_LINE_NUM) {
                    int st = strLog.indexOf("<br>");
                    strLog = strLog.substring(st + 4);
                } else {
                    currLogLineNum++;
                }
                String str = "<font color='" + color + "'>" + text + "</font>" + "<br>";
                strLog = (strLog == null || strLog.length() == 0) ? str : strLog + str;
                log.setText(Html.fromHtml(strLog));
            }
        });
        logView.post(new Runnable() {
            @Override
            public void run() {
                logView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }

    public void updateSoundLevel(final int level) {
        soundLevel.post(new Runnable() {
            @Override
            public void run() {
                soundLevel.setProgress(level);
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static Spanned fromHtml(String html) {
        Spanned result;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            result = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
        } else {
            result = Html.fromHtml(html);
        }
        return result;
    }

    protected boolean checkAudioRecordPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_AUDIO_PERMISSIONS_ID);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_AUDIO_PERMISSIONS_ID: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    this.finish();
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        initBT();
        //initMediaSession();
        //successfullyRetrievedAudioFocus();
        strLog = "";
    }


    public void setMqttStatus(boolean connected) {
        mqttStatus = connected;
        displayMqtttStatus();
    }

    public void displayMqtttStatus() {
        if (menu != null) {
            int colorId = 0;
            if (mqttStatus) {
                colorId = getResources().getColor(R.color.colorConnected);
            } else {
                colorId = getResources().getColor(R.color.colorAccent);
            }
            menu.findItem(R.id.action_mqtt).getIcon().mutate();
            menu.findItem(R.id.action_mqtt).getIcon().setColorFilter(colorId, PorterDuff.Mode.SRC_ATOP);
            menu.findItem(R.id.action_admin).getIcon().mutate();
            menu.findItem(R.id.action_admin).getIcon().setColorFilter(colorId, PorterDuff.Mode.SRC_ATOP);
        }
    }


    /*******************************************************************************************
     * ******************************************************************************************
     *                                   BLUETOOTH
     * ******************************************************************************************
     *******************************************************************************************/
    public void setBTHeadSetStatus(boolean connected) {
        headSetStatus = connected;
        displayBTHeadSetStatus();
    }

    public void displayBTHeadSetStatus() {
        //at startup the menu is not present
        if (menu != null) {
            int colorId = 0;
            if (headSetStatus) {
                colorId = getResources().getColor(R.color.colorConnected);
            } else {
                colorId = getResources().getColor(R.color.colorAccent);
            }
            menu.findItem(R.id.action_headset).getIcon().mutate();
            menu.findItem(R.id.action_headset).getIcon().setColorFilter(colorId, PorterDuff.Mode.SRC_ATOP);
        }
    }


    MediaSessionCompat mediaSession;

    private void initMediaSession() {


/**
 BroadcastReceiver remoteReceiver = new MediaButtonReceiver(){
@Override public void onReceive(Context context, Intent intent) {
if (Intent.ACTION_MEDIA_BUTTON.equals(intent.getAction())) {
final KeyEvent event = intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
Log.i(TAG, "onMediaButtonRecevier "+ event.getAction());
}else if (Intent.ACTION_VOICE_COMMAND.equals(intent.getAction())) {
Log.i(TAG, "onMediaButtonRecevier ACTION_VOICE_COMMAND");
}else{
Log.i(TAG, "onMediaButtonRecevier"+intent.getAction());
};
abortBroadcast();
}
};
 IntentFilter filter = new IntentFilter();
 filter.addAction(Intent.ACTION_MEDIA_BUTTON);
 filter.addAction(Intent.ACTION_VOICE_COMMAND);
 filter.setPriority(999);
 if(mediaSession!=null) {
 this.unregisterReceiver(remoteReceiver);
 }
 this.registerReceiver(remoteReceiver,filter);
 */

        //ComponentName mediaButtonReceiver = new ComponentName(getApplicationContext(), MediaButtonReceiver.class);
        mediaSession = new MediaSessionCompat(this.getApplicationContext(), TAG);
        mediaSession.setMediaButtonReceiver(null);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public boolean onMediaButtonEvent(Intent mediaButtonEvent) {
                Log.i(TAG, "onMediaButtonEvent ");
                return super.onMediaButtonEvent(mediaButtonEvent);
            }
        });
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
        mediaSession.setActive(true);
    }


    BluetoothHeadset mBluetoothHeadset;

    private void initBT() {
        BluetoothAdapter mBluetoothAdapter;

        // Get the default adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 0);
        }

        BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
            public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    Log.d(TAG, "Connecting HeadsetService...");
                    mBluetoothHeadset = (BluetoothHeadset) proxy;
                    List<BluetoothDevice> devices = mBluetoothHeadset.getConnectedDevices();
                    Log.d(TAG, "HeadsetService..." + devices.size());
                    setBTHeadSetStatus((devices.size() > 0));
                }
            }

            public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HEADSET) {
                    Log.d(TAG, "Unexpected Disconnect of HeadsetService...");
                    mBluetoothHeadset = null;
                    setBTHeadSetStatus(false);
                }
            }
        };
        // Establish connection to the proxy.
        mBluetoothAdapter.getProfileProxy(this, mProfileListener, BluetoothProfile.HEADSET);


        //Monitor profile events
        IntentFilter filter = new IntentFilter();
        //filter.addAction(BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED);
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);
        if (mProfileReceiver != null) {
            try {
                unregisterReceiver(mProfileReceiver);
            } catch (Exception e) {//DO NOTING
            }
            try {
                registerReceiver(mProfileReceiver, filter);
            } catch (Exception e) {//DO NOTING
            }
        }


    }

    private boolean successfullyRetrievedAudioFocus() {
        AudioManager audioManager = (AudioManager) this.getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(new AudioManager.OnAudioFocusChangeListener() {
            @Override
            public void onAudioFocusChange(int i) {
                Log.i(TAG, "onAudioFocusChange " + (AudioManager.AUDIOFOCUS_GAIN == i));
            }
        }, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN);

        return result == AudioManager.AUDIOFOCUS_GAIN;
    }


    private BroadcastReceiver mProfileReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            mBluetoothHeadset = null;
            //if (BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED.equals(action)) {
            //    notifyAudioState(intent);
            //}
            if (BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED.equals(action)) {
                notifyConnectState(intent);
            }
        }
    };
/*
    private void notifyAudioState(Intent intent) {
        final int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
        String message;
        switch (state) {
            case BluetoothHeadset.STATE_AUDIO_CONNECTED:
                message = "Audio Connected";
                this.setBTHeadSetStatus(true);
                break;
            case BluetoothHeadset.STATE_AUDIO_CONNECTING:
                message = "Audio Connecting";
                break;
            case BluetoothHeadset.STATE_AUDIO_DISCONNECTED:
                message = "Audio Disconnected";
                this.setBTHeadSetStatus(false);
                break;
            default:
                message = "Audio Unknown";
                break;
        }
        Log.d(TAG, " HeadsetnotifyAudioState..."+message);
        //Toast.makeText(this, message, Toast.LENGTH_SHORT).show();

    }*/

    private void notifyConnectState(Intent intent) {
        final int state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1);
        String message;
        switch (state) {
            case BluetoothHeadset.STATE_CONNECTED:
                message = "Connected";
                this.setBTHeadSetStatus(true);
                break;
            case BluetoothHeadset.STATE_CONNECTING:
                message = "Connecting";
                break;
            case BluetoothHeadset.STATE_DISCONNECTING:
                message = "Disconnecting";
                break;
            case BluetoothHeadset.STATE_DISCONNECTED:
                message = "Disconnected";
                this.setBTHeadSetStatus(false);
                break;
            default:
                message = "Connect Unknown";
                break;
        }
        Log.d(TAG, " HeadsetnotifyConnectState..." + message);
        //Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
