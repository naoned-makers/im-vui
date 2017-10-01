package io.naonedmakers.imvui;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

import ai.api.AIServiceException;
import ai.api.util.StringUtils;
import io.naonedmakers.imvui.meaning.MeanResponse;

public class UiBaseActivity extends AppCompatActivity {
    private static final String TAG = UiBaseActivity.class.getSimpleName();

    static String strLog = null;
    private TextView log;
    protected FloatingActionButton fab;
    private ScrollView logView;
    //TODO change with https://github.com/zagum/SpeechRecognitionView
    private ProgressBar soundLevel;

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
        if (ContextCompat.checkSelfPermission(this,Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED) {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO},REQUEST_AUDIO_PERMISSIONS_ID);
            return false;
        }else{
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
        findLanMqttBrokerIp();
        strLog="";
    }

    /**
     * Find in the local network an host that has mqtt port open
     * @return
     */
    public void findLanMqttBrokerIp() {

        SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.pref_file),Context.MODE_PRIVATE);
        String lastBrokerIp = sharedPref.getString(getString(R.string.broker_ip),null);

        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String ... params) {
                String brokerIp=params[0];
                try {
                    if(brokerIp!=null && !StringUtils.isEmpty(brokerIp)) {
                        //test the last Ip first
                        if(isPortOpen(brokerIp,1883,100)){
                            return brokerIp;
                        }
                    }
                    List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
                    for (NetworkInterface nif : all) {
                        if (!nif.getName().equalsIgnoreCase("wlan0")) continue;
                        if (!nif.isUp()) continue;
                        for (InterfaceAddress intAddress : nif.getInterfaceAddresses()) {
                            if (intAddress.getAddress() instanceof Inet4Address) {
                                //found the wifi ipv4 adress
                                Log.i(TAG, "wifi ipv4 address: " +intAddress);
                                Log.i(TAG, "wifi ipv4 getBroadcast: " +intAddress.getBroadcast());
                                Log.i(TAG, "wifi ipv4 getNetworkPrefixLength: " +intAddress.getNetworkPrefixLength());
                                if(intAddress.getNetworkPrefixLength()<=24){
                                    //not test more than 256 ip
                                    byte[] currentIp = intAddress.getBroadcast().getAddress();
                                    Log.i(TAG, "wifi ipv4 bytes: " +currentIp.length);
                                    for(int i=1;i<256;i++){
                                        currentIp[3]=((Integer)i).byteValue();
                                        InetAddress addr = InetAddress.getByAddress(currentIp);
                                        if(addr.isReachable(40)){
                                            Log.i(TAG, "address isReachable: " +addr.getHostAddress());
                                            if(isPortOpen(addr.getHostAddress(),1883,100)){
                                                return addr.getHostAddress();
                                            }
                                        }
                                    }
                                }
                            }
                        }


                    }
                } catch (Exception ex) {
                    //handle exception
                }
                return null;
            }

            @Override
            protected void onPostExecute(String brokerIp) {
                storeBrokerIp(brokerIp);
            }
        }.execute(lastBrokerIp);
    }

    public void storeBrokerIp(String brokerIp){
        if(brokerIp!=null && !StringUtils.isEmpty(brokerIp)) {
            Log.i(TAG, "new Broker Ip: " +brokerIp);
            SharedPreferences sharedPref = this.getSharedPreferences(getString(R.string.pref_file),Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(getString(R.string.broker_ip), brokerIp);
            editor.commit();
        }else{
            Log.i(TAG, "no Broker Found");
        }
    }
    public static boolean isPortOpen(final String ip, final int port, final int timeout) {

        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(ip, port), timeout);
            socket.close();
            return true;
        }

        catch(ConnectException ce){
            //Log.i(TAG, "isPortOpen"+ce.getMessage());
            return false;
        }

        catch (Exception ex) {
            Log.i(TAG, "isPortOpen"+ex.getMessage());
            return false;
        }
    }
}
