/****************************************************************************
 *
 * @file WebLinkClient.java
 * @brief
 *
 * Contains the WebLinkClient class.
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


import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Build;

import com.abaltatech.mcs.common.IMCSConnectionClosedNotification;
import com.abaltatech.mcs.common.IMCSDataLayer;
import com.abaltatech.mcs.connectionmanager.ConnectionManager;
import com.abaltatech.mcs.connectionmanager.EConnectionResult;
import com.abaltatech.mcs.connectionmanager.IDeviceStatusNotification;
import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.socket.android.AndroidSocketConnectionMethod;
import com.abaltatech.mcs.usbhost.android.AOALayer;
import com.abaltatech.mcs.usbhost.android.ConnectionMethodAOA;
import com.abaltatech.mcs.utils.android.WLSerializer;
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblink.core.audioconfig.WLAudioChannelMapping;
import com.abaltatech.weblink.core.authentication.DeviceIdentity;
import com.abaltatech.weblink.core.commandhandling.Command;
import com.abaltatech.weblink.core.commandhandling.hid.HIDRequestProperties;
import com.abaltatech.weblinkclient.IClientNotification;
import com.abaltatech.weblinkclient.WebLinkClientCore;
import com.abaltatech.weblinkclient.appcatalog.WLAppCatalogManager;
import com.abaltatech.weblinkclient.audiodecoding.AudioDecoder_MediaCodec;
import com.abaltatech.weblinkclient.audiodecoding.AudioOutput;
import com.abaltatech.weblinkclient.audiodecoding.IAudioDecoder;
import com.abaltatech.weblinkclient.audiodecoding.IAudioOutput;
import com.abaltatech.weblinkclient.hid.EHIDCapability;
import com.abaltatech.weblinkclient.hid.HIDController_AOA;
import com.abaltatech.weblinkclient.hid.HIDController_TCPIP;
import com.abaltatech.weblinkclient.hid.HIDController_USB;
import com.abaltatech.weblinkclient.hid.HIDInputManager;
import com.abaltatech.weblinkclient.hid.IHIDController;
import com.abaltatech.weblinkclient.hid.IHIDUSBConnection;
import com.abaltatech.weblinkclient.hid.IUSBDeviceConnection;
import com.abaltatech.weblinkclient.hid.TCPIPHIDUtils;
import com.abaltatech.weblinkclientapp.services.ServiceClient;
import com.abaltatech.weblinkclientapp.services.Services;
import com.testabalta.R;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Wrapper class for the notification interfaces to be able to register multiple listeners to certain
 * callbacks.
 */
public class WebLinkClient implements IClientNotification {

    private static final String TAG = "WebLinkClient";

    private static final String DEFAULT_APP_NAME_LANGUAGE = "en";
    private static final short DEFAULT_APP_IMAGE_WIDTH = 96;
    private static final short DEFAULT_APP_IMAGE_HEIGHT = 96;

    /**
     * Predefined consumer controls in the order they are defined in the HID Report Descriptor.
     * These should match the order and presence in the platform's Consumer Report.
     */
    private static final short[] PREDEFINED_CONSUMER_CONTROLS = new short[] {
        HIDRequestProperties.CU_Menu_Home, //first bit
        HIDRequestProperties.CU_Snapshot_Screenshot,
        HIDRequestProperties.CU_AC_Search_Spotlight,
        HIDRequestProperties.CU_AL_Keyboard_Layout_Toggle_Onscreen_Keyboard,
        HIDRequestProperties.CU_Scan_Previous_Track_Transport_Left,
        HIDRequestProperties.CU_Play_Pause_Play_Pause,
        HIDRequestProperties.CU_Scan_Next_Track_Transport_Right,
        HIDRequestProperties.CU_Mute_Mute,
        HIDRequestProperties.CU_Volume_Increment_Louder,
        HIDRequestProperties.CU_Volume_Decrement_Softer,
        HIDRequestProperties.CU_Power_Lock, //final bit
    };

    private final Context m_context;
    private ConnectionManager m_connManager = null;
    private WebLinkClientCore m_client;
    private ServiceClient m_serviceClient;
    private Services m_services;
    //
    private HIDInputManager m_inputManager;
    private HIDController_AOA m_aoaController;
    private HIDController_USB m_usbController;
    private HIDController_USB m_consumerController;
    private HIDController_TCPIP m_tcpController;

    private AOALayer m_aoaLayer;

    private IClientNotification m_listener = null;
    private final List<IConnectionStatusNotification> m_connListeners = new ArrayList<IConnectionStatusNotification>();
    private final List<IServerUpdateNotification> m_serverListeners = new ArrayList<IServerUpdateNotification>();
    private IPingHandler m_pingHandler;

    /**
     * Setup the client wrapper, which acts as the main receiver for WebLinkClientCore notifications.
     * This allows multiple UI classes register/unregister the notifications they care about.
     * @param context
     */
    WebLinkClient(Context context) {
        //create a custom connection manager that is easier to configure than accessing the internal version.
        m_connManager = createDefaultConnectionManager(context);

        DeviceIdentity myIdentity = new DeviceIdentity();
        myIdentity.setDisplayNameEn(Build.DISPLAY);
        myIdentity.setManufacturer(Build.MANUFACTURER);
        myIdentity.setModel(Build.MODEL);
        myIdentity.setApplication("WebLink Android Reference Client");
        myIdentity.setApplicationVendor("com.abaltatech");
        String versionName;
        final PackageManager packageManager = context.getPackageManager();
        if (packageManager != null) {
            try {
                PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
                versionName = packageInfo.versionName;
            } catch (PackageManager.NameNotFoundException e) {
                versionName = null;
            }
            myIdentity.setAppVersion(versionName);
        }
        myIdentity.setCountryCodes( "us" );
        myIdentity.setDisplayNameMultiLanguage( "{\"en\":\"" + Build.DISPLAY + "\"}" );
        myIdentity.setOs("Android");
        myIdentity.setOsVersion(String.valueOf(Build.VERSION.SDK_INT));
        myIdentity.setSerialNumber(Build.SERIAL);
        myIdentity.setSystemId(Build.MANUFACTURER + Build.MODEL);

        // Create the input manager and register all hid controllers
        m_inputManager = new HIDInputManager(context);
        m_aoaController = new HIDController_AOA(m_usbDeviceConnection);
        m_usbController = new HIDController_USB(m_hidUSBConnection);
        //Specify the capabilities for the two USB controllers, allowing each to focus on a single feature.
        //for the usbController (iOS support) we support AssistiveTouch mouse pointer and Stylus.
        m_usbController.setCapabilities(EHIDCapability.CAP_MOUSE, EHIDCapability.CAP_STYLUS);

        //The secondary HIDController_USB will only be used for the Consumer Controls support. This
        //provides WebLink with a way to trigger select actions on the phone through HID controls.
        m_consumerController = new HIDController_USB(m_consumerControlHID);
        m_consumerController.setCapabilities(EHIDCapability.CAP_CONSUMER);
        //Predefine the consumer controls that the platform supports.
        m_consumerController.setSupportedConsumerControls( PREDEFINED_CONSUMER_CONTROLS, PREDEFINED_CONSUMER_CONTROLS.length, 2 );

        m_tcpController = new HIDController_TCPIP(); //this is demo only.

        m_inputManager.registerHIDController(m_aoaController);
        m_inputManager.registerHIDController(m_usbController);
        m_inputManager.registerHIDController(m_consumerController);
        //
        m_inputManager.registerHIDController(m_tcpController); //this is demo only.

        // Create and initialize the WebLink Application Catalog Manager
        WLAppCatalogManager appCatalogManager = new WLAppCatalogManager();
        appCatalogManager.init(context.getFilesDir().getAbsolutePath(),
                DEFAULT_APP_IMAGE_WIDTH,
                DEFAULT_APP_IMAGE_HEIGHT,
                DEFAULT_APP_NAME_LANGUAGE,
                true,
                new AndroidFileManager(context));

        //create the client core
        m_client = new WebLinkClientCore(context, this, myIdentity, appCatalogManager, m_connManager, m_inputManager){
            @Override
            protected void onPingResponseTimeout() {
                super.onPingResponseTimeout(); //Always call super for this function.
                MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onPingResponseTimeout called!");
                //the communication is blocked!  do your own connection shutdown / stopping to reset!

                if (m_pingHandler != null) {
                    m_pingHandler.onPingResponseTimeout();
                }
            }
            
            @Override
            protected void onPingResponseReceived(boolean isSenderInactive) {
                super.onPingResponseReceived(isSenderInactive);//Always call super for this function.
                //MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onPingResponseReceived "+isSenderInactive);
                if(isSenderInactive) {
                    //Do your own restart of the connection here! (if not auto-reconfiguring).
                }

                if (m_pingHandler != null) {
                    m_pingHandler.onPingResponseReceived(isSenderInactive);
                }
            }
        };

        //Set the periodic ping parameters. Ping helps test the server for responsiveness.
        m_client.setPeriodicPingParams(500,2500);
        //set the reconfiguration delay. this happens if the phone app loses state (killed).
        m_client.setReconfigureDelay(2000);

        //optional features: client services.
        m_serviceClient = new ServiceClient();
        m_services = new Services();
        m_context = context;

        setupAudio();
    }

    public void setupAudio() {
        AudioConfigFileParser parser = null;
        InputStream is;
        try {
            is = m_context.getAssets().open("AudioChannelsConfig.ini");
            parser = new AudioConfigFileParser(is);
        } catch (IOException e) {
            MCSLogger.log(MCSLogger.ELogType.eError, TAG, "Failed to load default configuration file!", e);
            return;
        }

        try {
            if (parser != null) {
                parser.parse();
                for (WLAudioChannelMapping mapping : parser.m_channels) {
                    IAudioDecoder decoder = new AudioDecoder_MediaCodec();
                    IAudioOutput output = new AudioOutput();
                    decoder.setAudioOutput(output);
                    m_client.addAudioChannel(mapping, decoder);
                }
            }
        } catch (Exception e) {
            MCSLogger.printStackTrace(TAG,e);
        }
    }

    void startAudio() {
        m_client.startAudio(0);
    }

    void stopAudio() {
        m_client.stopAudio(0);
    }

    /**
     * Create a custom configuration for the WEBLINK connection manager, which can be passed to
     * the client core to override the default internal version.
     * @param context android context
     * @return the default connection manager.
     */
    protected ConnectionManager createDefaultConnectionManager(Context context){
        ConnectionManager connectionManager = new ConnectionManager();
        connectionManager.registerNotification(new IDeviceStatusNotification() {
            @Override
            public boolean onFavoriteDeviceAvailable(PeerDevice favoriteDevice) {
                //nothing to do
                return false;
            }

            @Override
            public boolean onDeviceConnected(PeerDevice device, IMCSDataLayer dataLayer) {
                //detect what type of device is connecting.
                if(dataLayer instanceof AOALayer) {
                    m_aoaLayer = (AOALayer) dataLayer;
                    //detect when the data layer is closed to remove it.
                    m_aoaLayer.registerCloseNotification(new IMCSConnectionClosedNotification(){
                        @Override
                        public void onConnectionClosed(IMCSDataLayer connection) {
                            connection.unregisterCloseNotification(this);
                            m_aoaLayer = null;
                        }
                    });
                }
                return true; //should return true if we want to keep the device.
            }

            @Override
            public void onDeviceConnectFailed(PeerDevice device, EConnectionResult result) {
                //nothing to do
            }

            @Override
            public void onDeviceDisconnected(PeerDevice device) {
                //nothing to do
            }

            @Override
            public void onAutoconnectFailed(EConnectionResult result) {
                //nothing to do
            }
        });
        connectionManager.registerConnectionMethod(new ConnectionMethodAOA(context,
                context.getString(R.string.aoa_manufacturer),
                context.getString(R.string.aoa_model),
                context.getString(R.string.aoa_version),
                context.getString(R.string.aoa_description),
                context.getString(R.string.aoa_url),
                context.getString(R.string.aoa_serial)));
        connectionManager.registerConnectionMethod(new AndroidSocketConnectionMethod(12345, WLTypes.SERVER_DEFAULT_BROADCAST_PORT));
        //Bluetooth isn't normally used for WEBLINK, in order to use, it will be necessary
        //to heavily tweak the video bitrate / settings to reduce the bandwidth to one that can be
        //handled by BT.
        //connectionManager.registerConnectionMethod(new BluetoothConnectionMethod(context));

        //add your custom connection methods.
        //connectionManager.registerConnectionMethod(new CustomConnectionMethod(context));

        connectionManager.setSerializer(new WLSerializer(context, "ConnectionManager"));
        connectionManager.init();
        return connectionManager;
    }

    /**
     * Access the weblink client core object.
     * @return
     */
    public WebLinkClientCore getWLClientCore() {
        return m_client;
    }

    /**
     * Set the client notification listener.
     * @param listener the listener, or null to remove.
     */
    public void setClientListener(IClientNotification listener) {
        m_listener = listener;
    }

    /**
     * add the listener from the list of listeners to connection updates.
     * @param listener
     */
    public void registerConnectionListener(IConnectionStatusNotification listener) {
        synchronized (m_connListeners) {
            if(!m_connListeners.contains(listener)) {
                m_connListeners.add(listener);
            }
        }
    }

    /**
     * Remove the listener from the list of listeners to connection updates.
     * @param listener
     */
    public void unregisterConnectionListener(IConnectionStatusNotification listener) {
        synchronized (m_connListeners) {
            m_connListeners.remove(listener);
        }
    }

    /**
     * add the listener from the list of listeners to server list updates.
     * @param listener
     */
    public void registerServerUpdateListener(IServerUpdateNotification listener) {
        synchronized (m_serverListeners) {
            if(!m_serverListeners.contains(listener)) {
                m_serverListeners.add(listener);
            }
        }
    }

    /**
     * Remove the listener from the list of listeners to server list updates.
     * @param listener
     */
    public void unregisterServerUpdateListener(IServerUpdateNotification listener) {
        synchronized (m_serverListeners) {
            m_serverListeners.remove(listener);
        }
    }
    /**
     * Get the service client.
     */
    public ServiceClient getServiceClient() {
        return m_serviceClient;
    }

    /**
     * Get the service object.
     */
    public Services getServices() {
        return m_services;
    }

    ///

    /**
     * HID USB Connection is used with the HIDController_USB.  This is used to bridge to the native
     * platform's iOS support.
     */
    private final IHIDUSBConnection m_hidUSBConnection = new IHIDUSBConnection() {
        private IMCSDataLayer m_dataLayer;
        @Override
        public boolean prepare(IHIDController controller) {
            //TODO prepare the native platform references
            m_dataLayer = TCPIPHIDUtils.getInstance().getLayer();
            return m_dataLayer != null; //for now is a placeholder. returning false here will prevent this controller from activating.
        }

        @Override
        public boolean release(IHIDController controller) {
            //TODO free up native platform references (if required)
            m_dataLayer = null;
            return false; //for now is a placeholder.
        }

        @Override
        public boolean isAvailable(IHIDController controller) {
            //TODO query the native platform's device state to see if connection.
            return m_connManager.getConnectedDevices().size() != 0; //for now is a placeholder.
        }

        /**
         *
         * @param deviceId the HID session unique identifier specific to this HID descriptor
         * @param capability the specific HID feature type being started with this deviceId.
         * @param descriptorData the array containing the HID class descriptor for this HID session
         * @param descriptorLength number of bytes to use for the descriptor.
         *
         * @return
         */
        @Override
        public boolean startHIDDescriptor(int deviceId, EHIDCapability capability, byte[] descriptorData, int descriptorLength) {
            //TODO do native platform's actions to start a HID descriptor.
            MCSLogger.log(TAG,"[HID USB]startHIDDescriptor: "+deviceId + "cap=" + capability + " d="+Arrays.toString(descriptorData));
            return m_dataLayer != null; //for now is a placeholder.
        }

        @Override
        public boolean stopHIDDescriptor(int deviceId, EHIDCapability capability) {
            //TODO do native platform's actions to stop a HID descriptor (if required).
            MCSLogger.log(TAG,"[HID USB]stopHIDDescriptor: "+deviceId + "cap=" + capability);
            return true; //for now is a placeholder.
        }

        @Override
        public boolean sendHIDReport(int deviceId, EHIDCapability capability, byte[] reportData, int reportLength) {
            //TODO do native platform's actions to send a HID input report.
            MCSLogger.log(TAG,"[HID USB]sendHIDReport: "+deviceId+ "cap=" + capability + " d="+Arrays.toString(reportData));
            TCPIPHIDUtils.getInstance().sendBufferEvent(reportData,reportLength);
            return m_dataLayer != null; //for now is a placeholder.
        }
    };

    /**
     * USB Device Connection is used with the HIDController_AOA to check the current state
     */
    private final IUSBDeviceConnection m_usbDeviceConnection = new IUSBDeviceConnection() {
        @Override
        public boolean prepare(IHIDController controller) {
            // we are prepared if the aoaLayer is not-null!
            return m_aoaLayer != null;
        }

        @Override
        public boolean release(IHIDController controller) {
            //TODO free up native platform references (if needed)
            return true;
        }

        @Override
        public boolean isAvailable(IHIDController controller) {
            return m_aoaLayer != null;
        }

        @Override
        public boolean sendControlTransfer(int requestType, int requestId, int value, byte[] payload, int payloadLength, int timeOutMs) {
            if(m_aoaLayer != null) {
                return m_aoaLayer.sendControlTransfer(requestType, requestId, value, payload, payloadLength, timeOutMs);
            }
            return false;
        }

        @Override
        public boolean sendControlTransfer(int requestType, int requestId, int value, int index, int timeOutMs) {
            if(m_aoaLayer != null) {
                return m_aoaLayer.sendControlTransfer(requestType, requestId, value, index, timeOutMs);
            }
            return false;
        }
    };

    /**
     * HID USB Connection is used with the HIDController_USB.  This is used to bridge to the native
     * platform's interface for Consumer Control actions.
     */
    private final IHIDUSBConnection m_consumerControlHID = new IHIDUSBConnection() {
        @Override
        public boolean prepare(IHIDController controller) {
            //TODO prepare the native platform references
            MCSLogger.log(TAG,"prepare: "+controller);
            return true; //for now is a placeholder.  returning false here will prevent this controller from activating.
        }

        @Override
        public boolean release(IHIDController controller) {
            //TODO free up native platform references (if required)
            MCSLogger.log(TAG,"release: "+controller);
            return false; //for now is a placeholder.
        }

        @Override
        public boolean isAvailable(IHIDController controller) {
            //TODO query the native platform's device state to see if connection.
            return m_client.isConnected(); //for now is a placeholder.
        }

        @Override
        public boolean startHIDDescriptor(int deviceId, EHIDCapability capability, byte[] descriptorData, int descriptorLength) {
            //TODO do native platform's actions to start a HID descriptor.
            // The platform may want to declare the USB HID report descriptor at start of the USB setup. In that case this value can be ignored.
            MCSLogger.log(TAG,"[HID CC]startHIDDescriptor: "+deviceId + "cap=" + capability + " d="+Arrays.toString(descriptorData));
            return m_client.isConnected(); //TODO - return the actual state.
        }

        @Override
        public boolean stopHIDDescriptor(int deviceId, EHIDCapability capability) {
            //TODO do native platform's actions to stop a HID descriptor (if required).
            MCSLogger.log(TAG,"[HID CC]stopHIDDescriptor: "+deviceId + "cap=" + capability);
            return m_client.isConnected(); //TODO - return the actual result.
        }

        @Override
        public boolean sendHIDReport(int deviceId, EHIDCapability capability, byte[] reportData, int reportLength) {
            //TODO do native platform's actions to send a HID input report.
            MCSLogger.log(TAG,"[HID CC]sendHIDReport: "+deviceId + "cap=" + capability + " d="+ Arrays.toString(reportData));
            return m_client.isConnected();
        }
    };

    ///

    @Override
    public void onServerListUpdated(ServerInfo[] servers) {
        if(m_listener != null) {
            m_listener.onServerListUpdated(servers);
        }
        synchronized (m_serverListeners) {
            for(IServerUpdateNotification listener : m_serverListeners) {
                listener.onServerListUpdated(servers);
            }
        }
    }

    @Override
    public void onConnectionEstablished(PeerDevice peerDevice) {
        if(m_listener != null) {
            m_listener.onConnectionEstablished(peerDevice);
        }
        synchronized (m_connListeners) {
            for(IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionEstablished(peerDevice);
            }
        }
        startAudio();
    }

    @Override
    public void onConnectionFailed(PeerDevice peerDevice, EConnectionResult result) {
        if(m_listener != null) {
            m_listener.onConnectionFailed(peerDevice, result);
        }
        synchronized (m_connListeners) {
            for(IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionFailed(peerDevice, result);
            }
        }
    }

    @Override
    public void onConnectionClosed(PeerDevice peerDevice) {
        if(m_listener != null) {
            m_listener.onConnectionClosed(peerDevice);
        }
        synchronized (m_connListeners) {
            for(IConnectionStatusNotification listener : m_connListeners) {
                listener.onConnectionClosed(peerDevice);
            }
        }
        stopAudio();
    }

    @Override
    public void onApplicationChanged(int appID) {
        if(m_listener != null) {
            m_listener.onApplicationChanged(appID);
        }
    }

    @Override
    public void onFrameRendered() {
        if(m_listener != null) {
            m_listener.onFrameRendered();
        }
    }

    @Override
    public boolean canProcessFrame() {
        if(m_listener != null) {
            return m_listener.canProcessFrame();
        }
        return true;
    }

    @Override
    public void onShowKeyboard(short type) {
        if(m_listener != null) {
            m_listener.onShowKeyboard(type);
        }
    }

    @Override
    public void onHideKeyboard() {
        if(m_listener != null) {
            m_listener.onHideKeyboard();
        }
    }

    @Override
    public void onWaitIndicator(boolean showWaitIndicator) {
        if(m_listener != null) {
            m_listener.onWaitIndicator(showWaitIndicator);
        }
    }

    @Override
    public void onAppImageChanged(int appID, Bitmap image) {
        if(m_listener != null) {
            m_listener.onAppImageChanged(appID,image);
        }
    }

    @Override
    public void onConnectionLost() {
        if(m_listener != null) {
            m_listener.onConnectionLost();
        }
    }

    @Override
    public void onConnectionResumed() {
        if(m_listener != null) {
            m_listener.onConnectionResumed();
        }
    }

    @Override
    public boolean onCommandReceived(Command command) {
        return true;
    }

    @Override
    public void onAudioChannelStarted(final int channelID) {
        if (m_listener != null) {
            m_listener.onAudioChannelStarted(channelID);
        }
    }

    @Override
    public void onAudioChannelStopped(final int channelID) {
        if (m_listener != null) {
            m_listener.onAudioChannelStopped(channelID);
        }
    }

    public void setPingHandler(IPingHandler pingHandler) {
        m_pingHandler = pingHandler;
    }
}
