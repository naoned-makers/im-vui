package io.naonedmakers.imvui;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.nsd.NsdManager;
import android.net.nsd.NsdServiceInfo;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.widget.ImageView;

import java.util.HashMap;
import java.util.Map;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
public class TouchActivity extends MqttActivity {

    private static final String TAG = TouchActivity.class.getSimpleName();

    /**
     * Whether or not the system UI should be auto-hidden after
     * {@link #AUTO_HIDE_DELAY_MILLIS} milliseconds.
     */
    private static final boolean AUTO_HIDE = true;

    /**
     * If {@link #AUTO_HIDE} is set, the number of milliseconds to wait after
     * user interaction before hiding the system UI.
     */
    private static final int AUTO_HIDE_DELAY_MILLIS = 3000;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private ImageView mImView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mImView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    //private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            //mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };
    /**
     * Touch listener to use for in-layout UI controls to delay hiding the
     * system UI. This is to prevent the jarring behavior of controls going away
     * while interacting with activity UI.
     */
    private final View.OnTouchListener mDelayHideTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
            if (AUTO_HIDE) {
                delayedHide(AUTO_HIDE_DELAY_MILLIS);
            }
            return false;
        }
    };

    private class Finger {
        String originZone;
        float originX;
        float originY;
        float lastX;
        float lastY;
    }

    private int moveThreshold = 5;
    private Map<Integer, Finger> fingerMap = new HashMap<Integer, Finger>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_touch);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mVisible = true;
        mImView = findViewById(R.id.imtouch);

        mImView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                int action = motionEvent.getActionMasked();
                if (MotionEvent.ACTION_DOWN == action || MotionEvent.ACTION_POINTER_DOWN == action) {
                    int mActivePointerIndex = motionEvent.getActionIndex();
                    int mActivePointerID = motionEvent.getPointerId(mActivePointerIndex);
                    // Get the pointer's current position
                    float x = motionEvent.getX(mActivePointerIndex);
                    float y = motionEvent.getY(mActivePointerIndex);
                    Finger curFinger = new Finger();
                    if (y < view.getHeight() / 2.5) {//450
                        curFinger.originZone = "up";
                    } else if (x < view.getWidth() / 2) {
                        curFinger.originZone = "right";
                    } else {
                        curFinger.originZone = "left";
                    }
                    curFinger.originX = x;
                    curFinger.originY = y;
                    Log.i(TAG, "originX " + curFinger.originX + "originY" + curFinger.originY + " zone" + curFinger.originZone);
                    fingerMap.put(mActivePointerID, curFinger);
                    moveMqttEvent(curFinger); //mqtt starting position in middle: 50 & 50
                } else if (MotionEvent.ACTION_MOVE == action) {
                    int pointerCount = motionEvent.getPointerCount();
                    for (int i = 0; i < pointerCount; ++i) {
                        int pointerId = motionEvent.getPointerId(i);
                        // Get the pointer's current position
                        float x = motionEvent.getX(i);
                        float y = motionEvent.getY(i);
                        Finger curFinger = fingerMap.get(pointerId);
                        if (moveThreshold < Math.abs(curFinger.lastX - x) || moveThreshold < Math.abs(curFinger.lastY - y)) {
                            curFinger.lastX = x;
                            curFinger.lastY = y;
                            moveMqttEvent(curFinger);
                        }
                    }
                } else if (MotionEvent.ACTION_UP == action || MotionEvent.ACTION_POINTER_UP == action) {
                    int mActivePointerIndex = motionEvent.getActionIndex();
                    int mActivePointerID = motionEvent.getPointerId(mActivePointerIndex);
                    fingerMap.remove(mActivePointerID);
                }
                return true;
            }
        });


    }

    private void moveMqttEvent(Finger finger) {
        float deltaX = finger.lastX - finger.originX;
        float deltaY = finger.lastY - finger.originY;
        //curFinger
        float range = 150;
        int xpos = Math.round((Math.min(Math.max(deltaX, -range), range) / range + 1) * 50);
        int ypos = Math.round((Math.min(Math.max(deltaY, -range), range) / range + 1) * 50);
        //mqtt set position=
        Log.d(TAG, "MOVE " + finger.originZone + " TO x:" + xpos + " y:" + ypos);
        if ("right".equals(finger.originZone)) {
            publish("im/command/righthand/set", "{\"origin\":\"im-vui\",\"absPosition\":" + xpos + "}");
            publish("im/command/rightarm/set", "{\"origin\":\"im-vui\",\"absPosition\":" + ypos + "}");
        } else if ("left".equals(finger.originZone)) {
            publish("im/command/lefthand/set", "{\"origin\":\"im-vui\",\"absPosition\":" + xpos + "}");
            publish("im/command/leftarm/set", "{\"origin\":\"im-vui\",\"absPosition\":" + ypos + "}");
        } else if ("up".equals(finger.originZone)) {
            publish("im/command/head/set", "{\"origin\":\"im-vui\",\"absPosition\":" + xpos + "}");
            publish("im/command/helmet/set", "{\"origin\":\"im-vui\",\"absPosition\":" + ypos + "}");
        }

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            // This ID represents the Home or Up button.
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        //mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mImView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in delay milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }


    protected void onResume() {
        Log.i(TAG, "onResume ");
        super.onResume();
        findAndConnectToLanMqttBroker();
        //discoverMqttService(this);
    }

    NsdManager mNsdManager;
    String SERVICE_TYPE = "_mqtt._tcp.";
    //String SERVICE_TYPE = "_googlezone._tcp.";

    public void discoverMqttService(Context context) {
        mNsdManager = (NsdManager) context.getSystemService(Context.NSD_SERVICE);
        NsdManager.DiscoveryListener mDiscoveryListener = initializeDiscoveryListener();
        mNsdManager.discoverServices(
                SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, mDiscoveryListener);

    }

    public NsdManager.DiscoveryListener initializeDiscoveryListener() {


        final NsdManager.ResolveListener mResolveListener = new NsdManager.ResolveListener() {
            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);
            }
        };


        // Instantiate a new DiscoveryListener
        final NsdManager.DiscoveryListener mDiscoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success " + service);
                mNsdManager.resolveService(service, mResolveListener);
            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                mNsdManager.stopServiceDiscovery(this);
            }
        };
        return mDiscoveryListener;
    }


}
