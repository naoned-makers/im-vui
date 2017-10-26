package io.naonedmakers.imvui;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.net.ConnectException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.util.Collections;
import java.util.List;

import ai.api.util.StringUtils;

/**
 * Created by dbatiot on 23/10/17.
 */

public class MqttActivity extends AppCompatActivity {
    private static final String TAG = MqttActivity.class.getSimpleName();

    private MqttClient sampleClient;
    private String mqttStatus = "false";

    public void publish(String topic, String payLoadStr) {
        if (sampleClient != null && sampleClient.isConnected()) {
            Log.d(TAG, "publish " + topic + "->" + payLoadStr);
            int qos = 0;
            try {
                MqttMessage message = new MqttMessage(payLoadStr.getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
            } catch (MqttException me) {
                Log.d(TAG, "reason " + me.getReasonCode() + " msg " + me.getMessage());
            }
        }
    }


    /**
     * Find in the local network an host that has mqtt port open
     * @return
     */
    public void findAndConnectToLanMqttBroker() {
        this.setMqttStatus("");
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
        String lastBrokerIp = sharedPref.getString(SettingsActivity.MeanPreferenceFragment.BROKER_IP,null);
        Log.d(TAG, "findAndConnectToLanMqttBroker lastBrokerIp:" + lastBrokerIp);
        new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String ... params) {
                String brokerIp=params[0];
                try {
                    if(brokerIp!=null && !StringUtils.isEmpty(brokerIp)) {
                        //test the last Ip first
                        if(isPortOpen(brokerIp,1883,1000)){
                            //storeAndConnectToLanMqttBroker(brokerIp);
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
                                Log.d(TAG, "wifi ipv4 address: " +intAddress);
                                Log.d(TAG, "wifi ipv4 getBroadcast: " +intAddress.getBroadcast());
                                Log.d(TAG, "wifi ipv4 getNetworkPrefixLength: " +intAddress.getNetworkPrefixLength());
                                Log.d(TAG, "wifi ipv4 getNetworkPrefixLength: " +intAddress.getNetworkPrefixLength());
                                if(intAddress.getNetworkPrefixLength()>=24){
                                    //not test more than 256 ip
                                    byte[] currentIp = intAddress.getBroadcast().getAddress();
                                    //Log.i(TAG, "wifi ipv4 bytes: " +currentIp.length);
                                    for(int i=1;i<256;i++){
                                        currentIp[3]=((Integer)i).byteValue();
                                        InetAddress addr = InetAddress.getByAddress(currentIp);
                                        if(addr.isReachable(100)){
                                            Log.i(TAG, "address isReachable: " +addr.getHostAddress());
                                            if(isPortOpen(addr.getHostAddress(),1883,100)){
                                                //storeAndConnectToLanMqttBroker(addr.getHostAddress());
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
                storeAndConnectToLanMqttBroker(brokerIp);
            }
        }.execute(lastBrokerIp);
    }

    public void storeAndConnectToLanMqttBroker(String brokerIp){
        if(brokerIp!=null && !StringUtils.isEmpty(brokerIp)) {
            //Log.i(TAG, "new Broker Ip: " +brokerIp);
            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(SettingsActivity.MeanPreferenceFragment.BROKER_IP, brokerIp);
            editor.commit();
            mqttConnect(brokerIp);
        }else{
            setMqttStatus(""+false);
            Log.i(TAG, "no Broker Found");
        }
    }


    /**
     * Create new service with unique context for given configuration
     *
     * @throws IllegalArgumentException If config parameter is null
     */
    public void mqttConnect(String lastBrokerIp) {
        String broker = "tcp://" + lastBrokerIp + ":1883";
        String clientId = "vui_" + (Math.random() * 1000000000 + "").substring(2, 8);
        try {
            MemoryPersistence persistence = new MemoryPersistence();
            sampleClient = new MqttClient(broker, clientId, persistence);
            sampleClient.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.d(TAG, "Broker complete: " +reconnect+" "+ sampleClient.getServerURI());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setMqttStatus(""+true);
                        }
                    });
                }

                @Override
                public void connectionLost(Throwable cause) {
                    Log.d(TAG, "Broker lost"+cause);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setMqttStatus(""+false);
                        }
                    });
                }
                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {}
                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {}
            });

            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            connOpts.setConnectionTimeout(2);
            Log.d(TAG, "Connecting to broker: " + sampleClient.getServerURI());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setMqttStatus(""+false);
                }
            });
            sampleClient.connect(connOpts);

        } catch (MqttException me) {
            Log.d(TAG, "reason " + me.getReasonCode() + "msg " + me.getMessage() + "cause " + me.getCause());
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


    public void setMqttStatus(String connected) {
        mqttStatus = connected;
    }

    public String getMqttStatusValue() {
        return mqttStatus;
    }

    /**
     * Dispatch onPause() to fragments.
     */
    @Override
    protected void onPause() {
        Log.i(TAG, "onPause ");
        if (sampleClient != null) {
            try {
                sampleClient.disconnect();
            } catch (MqttException me) {
                Log.d(TAG, "reason " + me.getReasonCode() + "msg " + me.getMessage() + "cause " + me.getCause());
            }
            sampleClient = null;
        }
        super.onPause();
    }
}
