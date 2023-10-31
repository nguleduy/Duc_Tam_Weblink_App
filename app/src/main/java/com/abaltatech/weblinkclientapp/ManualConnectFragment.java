/****************************************************************************
 *
 * @file ManualConnectFragment.java
 * @brief
 *
 * Contains the ManualConnectFragment class.
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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.abaltatech.mcs.connectionmanager.EConnectionResult;
import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.socket.android.AndroidSocketConnectionMethod;
import com.abaltatech.weblinkclient.IClientNotification.ServerInfo;
import com.abaltatech.weblinkclient.WebLinkClientCore;
import com.testabalta.R;

/**
 * This fragment scans for WebLink Servers periodically and presents all devices that were
 * discovered in a list view.
 *
 * The user can choose any of the devices in the list to connect with.
 */
public class ManualConnectFragment
        extends
            Fragment
        implements
            IServerUpdateNotification,
            IConnectionStatusNotification,
            AdapterView.OnItemClickListener {

    private static final String TAG = "ManualConnectFragment";

    private ArrayAdapter<String> m_adapter;

    private Handler m_handler = new Handler();
    private Thread m_deviceScanThread;
    private AlertDialog m_connectFailedDialog = null;

    private static final long DEVICE_SCANNING_INTERVAL_MS = 3000; // 3 seconds

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.manual_connect_fragment,
                container, false);


        ImageView ivRefreshButton = view.findViewById(R.id.iv_refresh);
        ivRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                App.instance().getWLClientCore().resetServerList();
                App.instance().getWLClientCore().scanDeviceList();
            }
        });

        m_adapter = new ArrayAdapter<String>(inflater.getContext(), android.R.layout.simple_list_item_1);

        for (ServerInfo info : App.instance().getWLClientCore().getServerList()) {
            m_adapter.add(info.m_peerDevice.toString());
        }

        ListView lvAvailableServers = view.findViewById(R.id.lv_available_servers);
        lvAvailableServers.setAdapter(m_adapter);
        lvAvailableServers.setOnItemClickListener(this);

        // For debugging purposes, when the status bar is long-pressed, a dialog is presented,
        // that allows manually entering the IP address of a WebLink Host to connect to.
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                builder.setTitle("Enter phone IP address");
                final EditText editText = new EditText(getContext());
                builder.setView(editText);
                builder.setPositiveButton(R.string.connect, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String address = editText.getEditableText().toString();
                        PeerDevice device = new PeerDevice("Device", AndroidSocketConnectionMethod.ID, address);
                        connectToHost(device.toString());
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                builder.show();
                return true;
            }
        });

        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        startListening();
    }

    @Override
    public void onStop() {
        stopListening();
        super.onStop();
    }

    /**
     * Start listening for WebLink servers.
     */
    public void startListening(){
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
                        App.instance().getWLClientCore().scanDeviceList();
                        Thread.sleep(DEVICE_SCANNING_INTERVAL_MS);
                    }
                } catch(InterruptedException ex){
                    MCSLogger.log(MCSLogger.eDebug, "Device scanning interrupted!");
                }
            }
        };
        m_deviceScanThread.start();
    }

    /**
     * Stops listening for WebLink servers.
     */
    public void stopListening() {
        WebLinkClient wlClient = App.instance().getWLClient();
        wlClient.unregisterConnectionListener(this);
        wlClient.unregisterServerUpdateListener(this);

        if (m_deviceScanThread != null) {
            m_deviceScanThread.interrupt();
            m_deviceScanThread = null;
        }
    }

    /**
     * Called when the server list has been updated.
     */
    @Override
    public void onServerListUpdated(final ServerInfo[] servers) {
        m_handler.post(new Runnable(){
            public void run(){
                m_adapter.clear();
                for (ServerInfo info : servers) {
                    m_adapter.add(info.m_peerDevice.toString());
                }
                m_adapter.notifyDataSetChanged();
            }
        });
    }

    /**
     * Called when a connection with the WebLink Server has been established.
     */
    @Override
    public void onConnectionEstablished(PeerDevice peerDevice) {
        stopListening();
        final Activity activity = getActivity();
        activity.runOnUiThread(new Runnable(){
            public void run() {
                startClient();
            }
        });
    }

    /**
     * Called when the connection attempt failed.
     */
    @Override
    public void onConnectionFailed(PeerDevice peerDevice, EConnectionResult result) {
        startListening();
        getActivity().runOnUiThread(new Runnable(){
            public void run(){
                if (m_connectFailedDialog == null) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setTitle(getString(R.string.info));
                    builder.setMessage(getString(R.string.failed_to_connect));
                    builder.setIcon(android.R.drawable.ic_dialog_alert);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener(){
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            WebLinkClientCore wlClient = App.instance().getWLClientCore();
                            wlClient.disconnect();
                            m_connectFailedDialog = null;
                        }});
                    m_connectFailedDialog = builder.create();
                    m_connectFailedDialog.show();
                }
            }
        });
    }

    /**
     * Called when the connection closes.
     */
    @Override
    public void onConnectionClosed(PeerDevice peerDevice) {
        MCSLogger.log(MCSLogger.eDebug, "The connection was closed.");
        startListening();
    }


    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String item = (String) parent.getItemAtPosition(position);
        if (item != null) {
            if (!connectToHost(item)) {
                Toast.makeText(getActivity(), getString(R.string.failed_to_connect), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Helper method that tries to establish a connection with a Peer Device.
     *
     * The result is delivered through the {@link IConnectionStatusNotification} notification.
     *
     * @param address Serialized Peer Device
     * @return true if the request was accepted, false otherwise
     */
    private boolean connectToHost(String address) {
        PeerDevice device = new PeerDevice();
        if (device.fromString(address)) {
            HomeActivity activity = (HomeActivity)getActivity();
            if (activity != null) {
                if (activity.connectRequest(device)) {
                    stopListening();
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Requests that the Home Activity display the WebLink fragment.
     */
    private void startClient() {
        HomeActivity activity = (HomeActivity) getActivity();
        activity.startWebLink();
    }

}
