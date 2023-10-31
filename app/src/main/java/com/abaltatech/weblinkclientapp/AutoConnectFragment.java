/****************************************************************************
 *
 * @file AutoConnectFragment.java
 * @brief
 *
 * Contains the AutoConnectFragment class.
 *
 * @author Abalta Technologies, Inc.
 * @date October/2020
 *
 * @cond Copyright
 *
 * COPYRIGHT 2020 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
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

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.abaltatech.mcs.connectionmanager.EConnectionResult;
import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.socket.android.AndroidSocketConnectionMethod;
import com.abaltatech.mcs.usbhost.android.ConnectionMethodAOA;
import com.abaltatech.weblinkclient.IClientNotification.ServerInfo;
import com.testabalta.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Fragment that is displayed to the user when the Auto-Connect UI has been switched on.
 *
 * This fragment starts a thread that will periodically scan for devices and try to establish
 * a connection with any device that is discovered.
 *
 * The thread scans once every couple of seconds to reduce overhead. If we are connected to a
 * WebLink Host the scanning is stopped. When the connection is closed, the scanning is started
 * again.
 */
public class AutoConnectFragment extends Fragment implements IServerUpdateNotification, IConnectionStatusNotification {

    private static final String TAG = "AutoConnectFragment";
    private final int MAX_SERVERS_QUEUE_CAPACITY = 10;

    // Used to display the clock widget
    private TextView m_clockTextView;

    // List of servers that have been discovered during the scanning process.
    private final Queue<ServerInfo> m_serversQueue = new ArrayBlockingQueue<ServerInfo>(MAX_SERVERS_QUEUE_CAPACITY);

    // Thread that will periodically scan for devices.
    private Thread m_deviceScanThread;

    private ConnectionThread m_connectionThread;

    // Amount of time to wait in milliseconds between consecutive server scans.
    private static final long DEVICE_SCANNING_INTERVAL_MS = 3000; // 3 seconds

    // Clock update rate
    private static final long CLOCK_TICK_MS = 1000; // 1 second

    // Format of the time for the clock widget
    private final SimpleDateFormat m_clockFormat = new SimpleDateFormat("HH:mm:ss", Locale.US);

    // Will update the clock widget periodically
    private Timer m_clockTimer;

    // Shared preferences
    private PreferenceHelper m_sharedPrefs;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        m_sharedPrefs = ((HomeActivity)getActivity()).getSharedPreferences();
        m_connectionThread = new ConnectionThread();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.auto_connect_fragment,
            container, false);
        m_clockTextView = (TextView)view.findViewById(R.id.tv_clock);
        m_clockTextView.setText(m_clockFormat.format(new Date()));
        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        MCSLogger.log(MCSLogger.eDebug, TAG, "Start");

        // Start scanning for devices
        startListening();

        // Start the clock update task
        m_clockTimer = new Timer();
        m_clockTimer.scheduleAtFixedRate(new UpdateClockTask(), 0, CLOCK_TICK_MS);
    }

    @Override
    public void onStop() {
        MCSLogger.log(MCSLogger.eDebug, TAG, "Stop");

        // Stop listening for devices
        stopListening();

        // Stop the clock update task
        m_clockTimer.cancel();
        m_clockTimer = null;

        super.onStop();
    }

    /**
     * Starts listening for available WebLink Servers.
     */
    private void startListening() {
        MCSLogger.log(MCSLogger.eDebug, TAG, "Start scanning for devices");

        WebLinkClient wlClient = App.instance().getWLClient();
        wlClient.registerConnectionListener(this);
        wlClient.registerServerUpdateListener(this);

        if (m_deviceScanThread != null && m_deviceScanThread.isAlive()) {
            m_deviceScanThread.interrupt(); //stop old thread
        }
        m_deviceScanThread = new Thread(){
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        MCSLogger.log(MCSLogger.eDebug, TAG, "Reset server list");
                        App.instance().getWLClientCore().resetServerList();
                        MCSLogger.log(MCSLogger.eDebug, TAG, "Scan devices");
                        App.instance().getWLClientCore().scanDeviceList();
                        Thread.sleep(DEVICE_SCANNING_INTERVAL_MS);
                    }
                } catch(InterruptedException ex){
                    //do nothing
                }
            }
        };
        m_deviceScanThread.start();
    }

    /**
     * Stops listening for available WebLink Servers.
     */
    private void stopListening() {
        MCSLogger.log(MCSLogger.eDebug, TAG, "Stop scanning for devices");

        // Unregister from notifications
        WebLinkClient wlClient = App.instance().getWLClient();
        wlClient.unregisterServerUpdateListener(this);
        wlClient.unregisterConnectionListener(this);

        // Stop the device scanning thread
        if (m_deviceScanThread != null) {
            m_deviceScanThread.interrupt();
            m_deviceScanThread = null;
        }
    }

    /**
     * Called when the list with available WebLink servers has changed.
     *
     * @param servers the list of active devices
     */
    @Override
    public void onServerListUpdated(ServerInfo[] servers) {
        MCSLogger.log(MCSLogger.eDebug, TAG, "On server list updated");

        synchronized (this) {
            for (ServerInfo serverInfo : servers) {
                if (!m_serversQueue.contains(serverInfo)) {
                    try {
                        m_serversQueue.add(serverInfo);
                    } catch (IllegalStateException e) {
                        MCSLogger.log(MCSLogger.eWarning, TAG, "Fail to add server, queue is full");
                    }
                }
            }
            if (!App.instance().getWLClientCore().isConnected() &&
                    m_connectionThread != null && !m_connectionThread.isAlive()) {
                m_connectionThread.start();
            }
        }

        for (ServerInfo info : servers) {
            MCSLogger.log(MCSLogger.eDebug, TAG, "Device: "
                    + info.m_peerDevice.getName() + ", Address:" + info.m_peerDevice.getAddress());
        }
    }

    /**
     * Called when a connection with the WebLink Host has been established.
     */
    @Override
    public void onConnectionEstablished(PeerDevice peerDevice) {
        MCSLogger.log(TAG, "Connection Established");
        final Activity activity = getActivity();
        activity.runOnUiThread(new Runnable(){
            public void run() {
                startClient();
            }
        });

        if(m_connectionThread != null && m_connectionThread.isAlive()) {
            m_connectionThread.connectionEstablished(peerDevice);
        }
    }

    /**
     * Received when connection request failed.
     */
    @Override
    public void onConnectionFailed(PeerDevice peerDevice, EConnectionResult result) {
        MCSLogger.log(MCSLogger.eWarning, TAG, "Failed to establish a connection, reason: " + result);
        if(m_connectionThread != null && m_connectionThread.isAlive()) {
            m_connectionThread.connectionFailed(peerDevice);
        }
    }

    /**
     * Called when the connection with the WebLink Host has been closed.
     */
    @Override
    public void onConnectionClosed(PeerDevice peerDevice) {
        MCSLogger.log(MCSLogger.eWarning, TAG, "Connection closed!");
        if(m_connectionThread != null && m_connectionThread.isAlive()) {
            m_connectionThread.connectionClosed(peerDevice);
        }
    }

    /**
     * Requests that the Home Activity displays the WebLink Fragment.
     */
    private void startClient() {
        HomeActivity activity = (HomeActivity) getActivity();
        activity.startWebLink();
    }

    /**
     * Helper class that update the clock widget.
     */
    class UpdateClockTask extends TimerTask {

        @Override
        public void run() {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    m_clockTextView.setText(m_clockFormat.format(new Date()));
                }
            });
        }

    }

    /**
     * Thread is provide implementation of auto-connect functionality.
     * Used approach is using queue instead of signaling mechanism because of
     * blocking mechanism used by some of ConnectionMethods, which would prevent
     * the thread of being interrupted properly.
     */
    private class ConnectionThread extends Thread {

        PeerDevice m_deviceConnectionRequested = null;
        PeerDevice m_deviceConnected = null;

        @Override
        public void run() {
            MCSLogger.log(TAG, "ConnectionThread started!");

            if (m_sharedPrefs.getConnectionMode().equals(PreferenceHelper.VALUE_USB)) {
                autoConnect(ConnectionMethodAOA.ID);
            } else {
                autoConnect(AndroidSocketConnectionMethod.ID);
            }

            MCSLogger.log(TAG, "ConnectionThread end!");
        }

        /**
         * Notify ConnectionThread for connection fails
         *
         * @param peerDevice PeerDevice which fails to connect
         */
        void connectionFailed(PeerDevice peerDevice) {
            synchronized (AutoConnectFragment.this) {
                if (peerDevice.equals(m_deviceConnectionRequested)) {
                    m_deviceConnectionRequested = null;
                } else {
                    MCSLogger.log(MCSLogger.eWarning, TAG, "PeerDevice mismatch! " +
                            "Connection Failed with different than requested device!");
                }
            }
        }

        /**
         * Notify ConnectionThread for connection established
         *
         * @param peerDevice PeerDevice which connects successfully
         */
        void connectionEstablished(PeerDevice peerDevice) {
            synchronized (AutoConnectFragment.this) {
                if (peerDevice.equals(m_deviceConnectionRequested)) {
                    m_deviceConnected = peerDevice;
                } else {
                    MCSLogger.log(MCSLogger.eWarning, TAG, "PeerDevice mismatch! " +
                            "Connection Established with different than requested device!");
                }
            }
        }

        /**
         * Notify ConnectionThread for connection closed
         *
         * @param peerDevice PeerDevice which connection was closed
         */
        void connectionClosed(PeerDevice peerDevice) {
            synchronized (AutoConnectFragment.this) {
                if (peerDevice.equals(m_deviceConnected)) {
                    m_deviceConnected = null;
                } else {
                    MCSLogger.log(MCSLogger.eWarning, TAG, "PeerDevice mismatch! " +
                            "Connection closed with different than requested device!");
                }
            }
        }


        /**
         * Attempt to automatically connect to those devices in the available servers list that
         * are connected using corresponding connection method.
         *
         * @param connectionMethodID The ID of the connection method which should be used
         */
        private void autoConnect(String connectionMethodID) {

            while (!isInterrupted()) {
                synchronized (AutoConnectFragment.this) {
                    if (m_deviceConnectionRequested != null) {

                        // Confirm that connection with requested device is successful and exit
                        if (m_deviceConnectionRequested.equals(m_deviceConnected)) {
                            m_deviceConnectionRequested = null;
                            return;
                        }
                        continue;
                    }
                }

                // Get next element in the queue if it is available.
                ServerInfo info;
                synchronized (AutoConnectFragment.this) {
                    info = m_serversQueue.poll();
                }

                if (info != null && info.m_peerDevice.getConnectionMethodID().equals(connectionMethodID)) {
                    HomeActivity activity = (HomeActivity) getActivity();
                    if (activity != null) {
                        synchronized (AutoConnectFragment.this) {
                            m_deviceConnectionRequested = info.m_peerDevice;
                        }

                        // Initiate connection process with the corresponding PeerDevice.
                        if (!activity.connectRequest(info.m_peerDevice)) {
                            synchronized (AutoConnectFragment.this) {
                                m_deviceConnectionRequested = null;
                            }
                            MCSLogger.log(MCSLogger.eWarning, TAG, "Connection request failed, device Name: " + info.m_peerDevice.getName());
                        }
                    }
                }
            }
        }

    }
}
