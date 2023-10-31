/****************************************************************************
 *
 * @file HomeActivity.java
 * @brief
 *
 * Contains the HomeActivity class.
 *
 * @author Abalta Technologies, Inc.
 * @date Jan, 2014
 *
 * @cond Copyright
 *
 * COPYRIGHT 2014 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @endcond
 *****************************************************************************/
package com.abaltatech.weblinkclientapp;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.abaltatech.mcs.connectionmanager.EConnectionResult;
import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.weblink.core.WLClientFeatures;
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblink.core.clientactions.EClientResponse;
import com.abaltatech.weblinkclient.IClientNotification;
import com.abaltatech.weblinkclient.WLClientDisplay;
import com.abaltatech.weblinkclient.WebLinkClientCore;
import com.abaltatech.weblinkclient.appcatalog.WLAppCatalogManager;
import com.abaltatech.weblinkclient.clientactions.EAppLaunchStatus;
import com.abaltatech.weblinkclient.clientactions.ICARHLaunchApp;
import com.abaltatech.weblinkclient.clientactions.ICARHWebLinkClientState;
import com.abaltatech.weblinkclient.clientactions.LaunchAppRequest;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoderFactory;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoder_H264;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoder_H264_Custom;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoder_I420;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoder_YUV;
import com.abaltatech.weblinkclient.framedecoding.IFrameDecoder;
import com.testabalta.R;

import java.util.List;
import java.util.Locale;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * This is the main activity of the WebLink Client.
 *
 * It manages a set of fragments that represent the various states.
 *
 * The WebLink Projection is rendered in the WebLink Fragment Container, while examples,
 * configuration menus and other UI is held by the Fragment Container.
 */
public class HomeActivity extends AppCompatActivity {

    private static final String TAG = "HomeActivity";

    private static final int READ_EXTERNAL_STORAGE_PERMISSION_REQUEST_ID = 0;

    private static final int APP_LAUNCH_WAIT_COUNT = 10;             // 10 times
    private static final int APP_LAUNCH_WAIT_TIME_MS = 1000;        // 1 second

    /**
     * The default DPI values of the screen. These depend on the shape of the pixel of the
     * system's display and the WebLink Projection Resolution.
     *
     * TODO: CHANGE THESE FOR PRODUCTION.
     */
    private static final int DEFAULT_XDPI = 120;
    private static final int DEFAULT_YDPI = 120;

    // Contains all fragments with examples and the Preferences Fragment.
    protected FrameLayout m_fragmentContainer;
    protected FrameLayout m_webLinkContainer;

    // Contains the view with the surface on which the WebLink Projection is rendered.
    protected WebLinkFragment m_webLinkFragment;

    // The Auto-Connect Fragment
    protected AutoConnectFragment m_autoConnectFragment;
    // The Manual-Connect Fragment
    protected ManualConnectFragment m_manualConnectFragment;
    // The HID Sample Fragment
    protected HIDOverTCPIPFragment m_hidFragment;
    // The Media Player Services Sample Fragment
    protected MediaPlayerFragment m_mediaPlayerFragment;
    // The App Service Example fragment
    protected ServicesFragment m_serviceFragment;
    // The Application Catalog Fragment
    protected AppCatalogFragment m_appCatalogFragment;

    // Shared Preferences Helper
    protected PreferenceHelper m_sharedPref = null;

    // Dummy views for capturing keyboard input
    protected DummyInputView m_dummyInput;
    protected View.OnKeyListener m_inputListener = null;

    // The main client display object
    private WLClientDisplay m_clientDisplay;

    // Dialog for presenting messages to the user
    private AlertDialog m_infoDialog = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set activity to full screen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.home_activity);

        ImageButton settingsButton = findViewById(R.id.ib_settings);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPreferences();
            }
        });

        ImageButton mediaPlayerButton = findViewById(R.id.ib_media_player);
        mediaPlayerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebLinkClientCore wlClient = App.instance().getWLClientCore();
                if (!wlClient.isConnected()) {
                    showInfoDialog(getString(R.string.info), getString(R.string.connection_required));
                } else {
                    showMediaPlayer();
                }
            }
        });

        ImageButton appCatalogButton = findViewById(R.id.ib_app_catalog);
        appCatalogButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAppCatalog();
            }
        });

        ImageButton serviceButton = findViewById(R.id.ib_services);
        serviceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebLinkClientCore wlClient = App.instance().getWLClientCore();
                if (!wlClient.isConnected()) {
                    showInfoDialog(getString(R.string.info), getString(R.string.connection_required));
                } else {
                    showServices();
                }
            }
        });

        ImageButton hidButton = findViewById(R.id.ib_hid);
        hidButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WebLinkClientCore wlClient = App.instance().getWLClientCore();
                if (wlClient.isConnected()) {
                    showInfoDialog(getString(R.string.info), getString(R.string.disconnect_required));
                } else {
                    showTCPIPHID();
                }
            }
        });

        m_sharedPref = new PreferenceHelper(this);
        m_dummyInput = (DummyInputView) findViewById(R.id.dummyInput);
        m_fragmentContainer = (FrameLayout) findViewById(R.id.fragment_container);
        m_webLinkContainer = (FrameLayout) findViewById(R.id.weblink_container);

        final WebLinkClientCore wlClient = App.instance().getWLClientCore();

        initWLClientDisplay();

        // Demonstrates how to setup handlers for client actions
        wlClient.getClientActionHandlerManager().registerRequestHandler(new ICARHWebLinkClientState() {
            @Override
            public boolean onWebLinkClientExitRequested() {
                // The client is free to decide what to do when an exit is requested. In this example
                // the connection is terminated.
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onWebLinkClientExitRequested");
                if (wlClient.isConnected()) {
                    wlClient.disconnect();
                    return true;
                }

                return false;
            }
        });

        wlClient.getClientActionHandlerManager().registerRequestHandler(new ICARHLaunchApp() {
            @Override
            public boolean processAppLaunchRequest(final LaunchAppRequest request) {
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "processAppLaunchRequest: " + request.getApplicationID());

                // Check if we can launch the application
                if (canLaunchApp(request.getApplicationID()) == EAppLaunchStatus.ALS_CAN_LAUNCH) {
                    // Launch the application
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(request.getApplicationID());
                    startActivity(launchIntent);

                    // Wait for the application to launch
                    new Handler(Looper.getMainLooper()).post(new Runnable() {

                        @Override
                        public void run() {
                            // Periodically check if the application is running.
                            for (int i = 0; i < APP_LAUNCH_WAIT_COUNT; ++i) {
                                try {
                                    Thread.sleep(APP_LAUNCH_WAIT_TIME_MS);
                                } catch (InterruptedException e) {
                                    MCSLogger.log(MCSLogger.eWarning, TAG, "Waiting for app launch interrupted!", e);
                                }

                                if (isAppRunning(HomeActivity.this, request.getApplicationID())) {
                                    // Return a success response.
                                    request.sendResponse(EClientResponse.CR_SUCCESS, "");
                                    return;
                                }
                            }

                            // Error response. The launch timed out.
                            request.sendResponse(EClientResponse.CR_TIMEOUT, "The application failed to start within 5 seconds!");
                        }
                    });
                } else {
                    request.sendResponse(EClientResponse.CR_FAILED, "Application not installed or launching it is not supported!");
                }

                return true;
            }

            @Override
            public EAppLaunchStatus canLaunchApp(String applicationID) {
                // In this example the client will check if the specified application is installed.
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "canLaunchApp: " + applicationID);

                // Obtain information about the package
                PackageManager pm = HomeActivity.this.getPackageManager();
                try {
                    PackageInfo info = pm.getPackageInfo(applicationID, 0);
                    ApplicationInfo appInfo = pm.getApplicationInfo(applicationID, 0);
                    // If the package exists, get the application name and respond with a can launch
                    if (info != null) {
                        String applicationName = (String) (appInfo != null ? pm.getApplicationLabel(appInfo) : "(unknown)");
                        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "canLaunchApp: " + applicationName + " v(" + info.versionName + ") can be launched.");
                        return EAppLaunchStatus.ALS_CAN_LAUNCH;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    return EAppLaunchStatus.ALS_UNSUPPORTED_ACTION;
                }

                return EAppLaunchStatus.ALS_UNSUPPORTED_ACTION;
            }
        });

        showConnect(true);
    }

    private void showInfoDialog(String title, String message) {
        dismissInfoDialog();
        AlertDialog.Builder builder = new AlertDialog.Builder(HomeActivity.this);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                m_infoDialog = null;
            }
        });
        m_infoDialog = builder.show();
    }

    private void dismissInfoDialog() {
        if (m_infoDialog != null) {
            m_infoDialog.dismiss();
            m_infoDialog = null;
        }
    }

    /**
     * Helper method that initializes the WebLink client display. This method is called
     * upon the initial activity creation and if display related preferences have changed.
     *
     * Based on the settings it creates a display that uses an internal surface that can be attached
     * a TextureView or use an external surface (provided from a SurfaceView).
     */
    private void initWLClientDisplay() {

        final WebLinkClientCore wlClient = App.instance().getWLClientCore();

        // Terminate any previous session
        wlClient.terminate();

        Point resolution = m_sharedPref.getDecoderResolution();

        // The width and height of the the WebLink Client Display.
        // Automatically scaled to fit the size of the SurfaceView or TextureView
        // on the client side.
        int clientWidth = resolution.x;
        int clientHeight = resolution.y;

        // The resolution at which applications, captured by the WebLink Host are
        // rendered at. Scaling is done at the Host-side.
        int sourceWidth = resolution.x;
        int sourceHeight = resolution.y;

        // The resolution of the video projection streamed by the WebLink Host.
        // Scaling is done on the Host-side.
        int encodeWidth = resolution.x;
        int encodeHeight = resolution.y;

        int clientFeatures = getClientFeatureFlags();
        String clientFeaturesString = getClientFeaturesString();

        if(isDecoderTargetSurfaceView()) {
            // We are using a custom display with an external encoder surface
            // Register it with the client before it is initialized
            wlClient.registerClientDisplay(new WLClientDisplay(null/* Has external encoder surface*/));
        } else {
            // We are using a custom display with an internal encoder surface
            wlClient.registerClientDisplay(new WLClientDisplay(/* Uses internal encoder texture surface*/));
        }

        wlClient.setEncoderParams(getEncoderParams(
                m_sharedPref.getDecoderType(), m_sharedPref.getKeyFrameInterval(), m_sharedPref.getBitrate()));

        wlClient.setMaximumFrameRate(m_sharedPref.getFrameRate());

        wlClient.enableAutoFPSManagement(m_sharedPref.isFPSManagementEnabled());

        if (!wlClient.init(
                sourceWidth, sourceHeight,
                encodeWidth, encodeHeight,
                clientWidth, clientHeight,
                clientFeatures, clientFeaturesString)) {
            Toast.makeText(
                this,
                "Could not initialize WLClient",
                Toast.LENGTH_LONG).show();
        }

        m_clientDisplay = wlClient.getDefaultDisplay();

        String videoDecoder = m_sharedPref.getVideoDecoder();
        Class<? extends IFrameDecoder> decoderClass = null;
        if (videoDecoder.isEmpty() || !FrameDecoder_H264.isSupported()) {
            Toast.makeText(
                    this,
                    "Hardware H264 decoder is not supported by this platform.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Decoder set in Shared Prefs: " + videoDecoder);

        if (videoDecoder.equals(PreferenceHelper.VALUE_I420_DECODER)) {
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Chosen decoder: Software I420");
            decoderClass = FrameDecoder_I420.class;
        }
        else if (videoDecoder.equals(PreferenceHelper.VALUE_YUV_DECODER)) {
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Chosen decoder: Software YUV");
            decoderClass = FrameDecoder_YUV.class;
        } else if (videoDecoder.equals(PreferenceHelper.VALUE_H264_CUSTOM_DECODER)){
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Chosen decoder: Custom Hardware H264");
            decoderClass = FrameDecoder_H264_Custom.class;
        } else {
            MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Chosen decoder: Hardware H264");
            decoderClass = FrameDecoder_H264.class;
        }

        //unregister all other decoders, re-register only the selected version.
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_H264);
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_I420);
        FrameDecoderFactory.instance().unregisterDecoder(WLTypes.FRAME_ENCODING_YUV);
        FrameDecoderFactory.instance().registerDecoder(m_sharedPref.getDecoderType(), decoderClass);

        if (m_clientDisplay != null) {
            int decoderMask = FrameDecoderFactory.instance().getRegisteredDecodersMask();
            m_clientDisplay.setSupportedDecodersMask(decoderMask);
        }
    }

    /**
     * Returns the Client Feature Flags.
     *
     * Report that this Client supports Client Action and that the Server-side keyboard should
     * be used.
     *
     * @return Client feature flags
     */
    private int getClientFeatureFlags() {
        return WLClientFeatures.SUPPORTS_CLIENT_ACTIONS | WLClientFeatures.SERVER_KEYBOARD;
    }

    /**
     * Returns the string that defines the client features.
     *
     * This string only configures the DPI of the IVU screen.
     * @return Client features string
     */
    private String getClientFeaturesString() {
        return String.format(Locale.US, "xdpi=%d|ydpi=%d", DEFAULT_XDPI, DEFAULT_YDPI);
    }

    /**
     * Generates the encoder parameters string.
     *
     * @param decoderType Type of decoder that the WebLink Client uses
     * @param keyFrameInterval Key-frame interval
     * @param bitRate Bitrate
     * @return
     */
    private String getEncoderParams(int decoderType, int keyFrameInterval, int bitRate) {
        return String.format(Locale.US, "%d:maxKeyFrameInterval=%d,bitrate=%d",
                decoderType, keyFrameInterval, bitRate);
    }

    /**
     * Notification called by the {@link PrefsFragment} that the preference fragment was closed.
     */
    void OnPreferencesFragmentClosed() {
        boolean reinitWLClientDisplay = false;
        boolean clientSDKConfigurationChanged = false;

        // If the type of surface used by the Decoder has been changed or the Decoder resolution
        // was changed, we need to reinitialize the client
        if (m_sharedPref.hasPreferenceChanged(PreferenceHelper.KEY_DECODER_SURFACE)
                || m_sharedPref.hasPreferenceChanged(PreferenceHelper.KEY_VIDEO_RESOLUTION)
                || m_sharedPref.hasPreferenceChanged(PreferenceHelper.KEY_VIDEO_DECODER)
                || m_sharedPref.hasPreferenceChanged(PreferenceHelper.KEY_KEYFRAME_INTERVAL)
                || m_sharedPref.hasPreferenceChanged(PreferenceHelper.KEY_BITRATE)) {
            reinitWLClientDisplay = true;
        }

        // Changes to the Client SDK configuration
        if (m_sharedPref.hasPreferenceChanged(PreferenceHelper.KEY_APP_SWITCH_TIMEOUT)
                || m_sharedPref.hasPreferenceChanged(PreferenceHelper.KEY_FRAME_SKIPPING)) {
            clientSDKConfigurationChanged = true;
        }

        if (reinitWLClientDisplay) {
            // Note this is only done for this sample app. For production you will have the
            // desired decoder surface and settings hardcoded, and initialized once. The
            // preference would not changed and this logic will not be needed.
            initWLClientDisplay();
        }

        if (clientSDKConfigurationChanged) {
            WebLinkClientCore wlClient   = App.instance().getWLClientCore();
            int appSwitchTimeout = m_sharedPref.getAppSwitchTimeout();
            boolean frameSkipping = m_sharedPref.isFrameSkippingEnabled();

            wlClient.setFrameSkipping(frameSkipping);
            wlClient.setAppSwitchTimeout(appSwitchTimeout);
        }
    }


    /**
     * Helper method which checks if an application is running.
     *
     * @param context Context
     * @param packageName Package name of the application to check
     * @return true if the application is running, false otherwise
     */
    public static boolean isAppRunning(final Context context, final String packageName) {
        final ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
            if (activityManager != null) {
                final List<ActivityManager.RunningAppProcessInfo> procInfos = activityManager.getRunningAppProcesses();
                if (procInfos != null) {
                    for (final ActivityManager.RunningAppProcessInfo processInfo : procInfos) {
                        if (processInfo.processName.equals(packageName)) {
                            return true;
                        }
                    }
                }
            }
        } else {
            UsageStatsManager usm = (UsageStatsManager)context.getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm != null) {
                long time = System.currentTimeMillis();
                List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 1000, time);
                if (appList != null && appList.size() > 0) {
                    SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                    for (UsageStats usageStats : appList) {
                        mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                    }
                    if (!mySortedMap.isEmpty()) {
                        if (mySortedMap.get(mySortedMap.lastKey()).getPackageName().equals(packageName)) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
    
    /**
     * Tries to connect to the specified Peer Device.
     *
     * The result of this operation is delivered through the {@link IClientNotification}
     * interface.
     *
     * This method returns true if the connect request was accepted by the WebLink Client SDk
     * and false otherwise.
     *
     * @see IClientNotification#onConnectionEstablished(PeerDevice)
     * @see IClientNotification#onConnectionFailed(PeerDevice, EConnectionResult)
     *
     * @param device Peer device
     *
     * @return true if request was accepted, false otherwise
     */
    public boolean connectRequest(PeerDevice device) {
        if (device == null) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Fail to connect to device, PeerDevice is null");
            return false;
        }

        WebLinkClientCore wlClient = App.instance().getWLClientCore();
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "Connecting to device " + device.toString());
        return wlClient.connect(device, IClientNotification.EProtocolType.ePT_WL, -1);
    }

    /**
     * Show the App Services fragment.
     */
    public void showServices() {
        if (m_serviceFragment == null) {
            m_serviceFragment = new ServicesFragment();
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, m_serviceFragment);
        ft.commit();
        m_fragmentContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Shows the Media Services Example Fragment.
     */
    public void showMediaPlayer() {
        if (m_mediaPlayerFragment == null) {
            m_mediaPlayerFragment = new MediaPlayerFragment();
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, m_mediaPlayerFragment);
        ft.commit();
        m_fragmentContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Shows the Application Catalog Example Fragment.
     */
    public void showAppCatalog() {
        if (m_appCatalogFragment == null) {
            WLAppCatalogManager appCatalogManager = App.instance().getWLClientCore().getAppCatalogManager();
            m_appCatalogFragment = new AppCatalogFragment();
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, m_appCatalogFragment);
        ft.commit();
        m_fragmentContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Show preferences fragment.
     */
    public void showPreferences() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        m_sharedPref.resetChanges();
        ft.replace(R.id.fragment_container, new PrefsFragment());
        ft.commit();
        m_fragmentContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Shows the fragment to configure the TCPIP HID controls.
     */
    public void showTCPIPHID() {
        if (m_hidFragment == null) {
            m_hidFragment = new HIDOverTCPIPFragment();
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(R.id.fragment_container, m_hidFragment);
        ft.commit();
        m_fragmentContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Helper method that returns 'true' if the preferences are set to use an external SurfaceView
     * for the decoder.
     * @return true if SurfaceView was selected in the Preferences for the Encoder Surface
     */
    protected boolean isDecoderTargetSurfaceView() {
        String decoderVideoView = m_sharedPref.getDecoderSurface();
        return decoderVideoView.equals(PreferenceHelper.VALUE_SURFACE_VIEW);
    }

    /**
     * Starts the WebLink fragment.
     *
     * The first time this method is called, it will create the WebLink Fragment.
     *
     * This method attaches the WebLink fragment to the WebLink Container layout.
     */
    public void startWebLink(){
        // Instantiate the correct fragment, depending on whether a SurfaceView or TextureView
        // is used to render the decoded WebLink Projection.
        if (m_webLinkFragment == null) {
            if  (isDecoderTargetSurfaceView()) {
                m_webLinkFragment = new WebLinkFragment_SurfaceView();
                ((WebLinkFragment_SurfaceView)m_webLinkFragment).setDefaultDisplay(m_clientDisplay);
            } else {
                m_webLinkFragment = new WebLinkFragment_TextureView();
            }

            FragmentTransaction wft = getSupportFragmentManager().beginTransaction();
            wft.replace(R.id.weblink_container, m_webLinkFragment);
            wft.commit();
        }

        // This will force the HID fragment to be destroyed
        m_hidFragment = null;

        showWebLink();
    }

    /**
     * Shows the WebLink Projection fragment and makes the non-projection fragment
     * container invisible.
     */
    public void showWebLink(){
        // Hide the fragment container
        m_fragmentContainer.setVisibility(View.GONE);

        // Remove the fragment
        Fragment f = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (f != null) {
            FragmentTransaction wft = getSupportFragmentManager().beginTransaction();
            wft.remove(f);
            wft.commit();
        }

        m_webLinkContainer.setVisibility(View.VISIBLE);
    }

    /**
     * Detaches the WebLink Fragment and destroys it.
     */
    public void stopWebLink() {
        hideWebLink();
        if (m_webLinkFragment != null) {
            FragmentTransaction wft = getSupportFragmentManager().beginTransaction();
            wft.remove(m_webLinkFragment);
            wft.commit();
            m_webLinkFragment = null;
        }

        // This will force the Media Player Fragment to be destroyed
        m_mediaPlayerFragment = null;
        // This will force the Service Fragment to be destroyed
        m_serviceFragment = null;

        //  This will force the App Catalog Fragment to be destroyed
        m_appCatalogFragment = null;
    }

    /**
     * Hides the WebLink Projection fragment and makes the non-projection fragment
     * container visible.
     */
    public void hideWebLink(){
        m_webLinkContainer.setVisibility(View.INVISIBLE);
        showConnect(false);
    }

    @Override
    protected void onDestroy() {
        WebLinkClientCore wlClient = App.instance().getWLClientCore();
        wlClient.terminate();
        super.onDestroy();
    }

    /**
     * Handles the system back button.
     */
    @Override
    public void onBackPressed() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fragment != null) {
            if (m_webLinkFragment == null) {
                showConnect(false);
            } else {
                showWebLink();
            }
        } else {
            if (m_webLinkFragment != null) {
                m_webLinkFragment.onBackPressed();
            }
        }
    }

    /**
     * Transition to the connect UI fragment, depending on what is the current app settings.
     * @param isFirstShow for non-frst showing, allow the fragments to retain their enable values.
     */
    public void showConnect(boolean isFirstShow) {
        m_fragmentContainer.setVisibility(View.VISIBLE);
        boolean showAutoConnectUI = m_sharedPref.isAutoConnectEnabled();
        if (isFirstShow) {
            m_autoConnectFragment = new AutoConnectFragment();
            m_manualConnectFragment = new ManualConnectFragment();
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (showAutoConnectUI) {
            if (m_autoConnectFragment != null) {
                ft.replace(R.id.fragment_container, m_autoConnectFragment);
            } else {
                MCSLogger.log(MCSLogger.eDebug, "This should not happen!");
            }
        } else {
            if (m_manualConnectFragment != null) {
                ft.replace(R.id.fragment_container, m_manualConnectFragment);
            } else {
                MCSLogger.log(MCSLogger.eDebug, "This should not happen!");
            }
        }
        ft.commit();
    }
    
    /**
     * Show soft input for the dummy edit text.  While shown, the events will be forwarded
     * to the provided Key listener.
     * @param listener Key listener for the duration of this keyboard.
     */
    public void showKeyboard(View.OnKeyListener listener) {
        m_inputListener = listener;
        // Show soft input
        InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        m_dummyInput.requestFocus();
        m_dummyInput.requestFocusFromTouch();
        im.showSoftInput(m_dummyInput, InputMethodManager.SHOW_FORCED);
    }
    
    /**
     * Hide the soft input keyboard.
     */
    public void hideKeyboard() {
        m_inputListener = null;
        InputMethodManager im = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        im.hideSoftInputFromWindow(m_dummyInput.getWindowToken(), 0);
    }

    public PreferenceHelper getSharedPreferences() {
        return m_sharedPref;
    }
    
    /**
     * With dummy text, the key events need to be caught at the dispatch level as
     * they will no longer be available on the key listener of the EditText.
     */
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(m_inputListener != null) {
            return m_inputListener.onKey(m_dummyInput, event.getKeyCode(), event);
        } else {
            return super.dispatchKeyEvent(event);
        }
    }

    //-- Preference

    /**
     * Preferences fragment
     */
    public static class PrefsFragment
            extends PreferenceFragmentCompat
            implements SharedPreferences.OnSharedPreferenceChangeListener {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            //load up version information
            Preference version = findPreference(PreferenceHelper.KEY_VERSION_INFO);
            if(version != null) {
                version.setSummary(WebLinkClientCore.getClientVersion());
            }
        }

        @Override
        public void onResume() {
            super.onResume();
            getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        }

        @Override
        public void onPause() {
            getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
            super.onPause();
            HomeActivity homeActivity = (HomeActivity)getActivity();
            homeActivity.OnPreferencesFragmentClosed();
        }

        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            // Notify the parent activity
            ((HomeActivity)getActivity()).OnPreferenceChanged(sharedPreferences, key);
        }
    }

    private void OnPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        m_sharedPref.onPreferenceChanged(key);
    }
}
