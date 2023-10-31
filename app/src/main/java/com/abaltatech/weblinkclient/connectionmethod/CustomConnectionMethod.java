/****************************************************************************
 *
 * @file CustomConnectionMethod.java
 * @brief
 *
 * Implementation of ConnectionMethod which manages android accessory connections.
 *
 * @author Abalta Technologies, Inc.
 * @date July, 2015
 *
 * @cond Copyright
 *
 * COPYRIGHT 2015 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
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

package com.abaltatech.weblinkclient.connectionmethod;

import android.content.Context;

import com.abaltatech.mcs.common.IMCSConnectionAddress;
import com.abaltatech.mcs.common.IMCSDataLayer;
import com.abaltatech.mcs.connectionmanager.ConnectionMethod;
import com.abaltatech.mcs.connectionmanager.IDeviceScanningNotification;
import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;

import java.util.ArrayList;
import java.util.List;

/**
 * Skeleton Custom Connection Method.
 */
public class CustomConnectionMethod extends ConnectionMethod implements CustomListener.ICustomNotification {
    // Debugging
    private static final String TAG = CustomConnectionMethod.class.getSimpleName();

    private List<CustomAddress> m_availableDevices = new ArrayList<CustomAddress>();

    private final Context m_context;

    private IMCSDataLayer m_dataLayer; //previous data layer

    private CustomListener m_listener;


    // Key names received from the Handler
    private static final String SYSTEM_NAME = "USB";
    public static final String ID = "USB";

    /**
     * Constructs a new instance of {@link CustomConnectionMethod}.
     *
     * This constructor creates an {@link CustomListener} which
     * monitors for accessories. The listener will notify us when an accessory
     * becomes available. The listener will only monitor for a default
     * accessory.
     *
     * @param context The UI Activity Context
     */
    public CustomConnectionMethod(Context context) {
        m_context = context;
        if (m_context == null) {
            throw new RuntimeException("Context cannot be null!");
        }

        try {
            m_listener = new CustomListener(m_context);

        } catch (Throwable tr) {
            MCSLogger.log(TAG,
                    "Error connecting to the UsbManager! The AOA connection will not work! See details below...");
            MCSLogger.log(TAG, tr.toString());
        }
    }

    /**
     * Start the {@link CustomConnectionMethod}.
     *
     */
    public synchronized boolean start() {
        MCSLogger.log(TAG, "START");

        // Cancel any thread currently running a connection
        if (m_dataLayer != null) {
            MCSLogger.log(TAG,"Warning! connection method start is stopping an existing data layer!");
            m_dataLayer.closeConnection();
            m_dataLayer = null;
        }
        if(m_listener != null) {
            m_listener.registerUpdateListener(this);
            // Start the CustomListener
            if (m_listener.startServerScan()) {
                return true;
            }

        }

        return false;
    }

    /**
     * Stop the {@link CustomConnectionMethod}.
     *
     */
    public synchronized void stop() {
        MCSLogger.log(TAG, "END");

        if (m_dataLayer != null) {
            removeConnection(m_dataLayer);
        }

        if (m_listener != null) {
            m_listener.stopServerScan();
            m_listener.unregisterUpdateListener(this);
        }
    }

    @Override
    protected String getSystemName() {
        return SYSTEM_NAME;
    }

    @Override
    public String getConnectionMethodID() {
        return ID;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    /**
     *
     * @param notification object to receive scan device status
     * @param waitForCompletion whether to wait for the scan to complete (true) or return immediately.
     *
     * @return
     */
    @Override
    public boolean scanDevices(IDeviceScanningNotification notification, boolean waitForCompletion) {
        if (notification != null) {
            //start scan notification
            if (notification.onDeviceScanningBegin(getConnectionMethodID())) {
                List<CustomAddress> devices = null;
                synchronized (this) {
                    devices = m_availableDevices;
                }

                for (CustomAddress device : devices) {
                    if (/*is it valid*/device != null) {
                        notification.onDeviceFound(getDeviceForAddress(device));
                    }
                }
                //notification of scanning end.
                notification.onDeviceScanningEnd(getConnectionMethodID());
                return true;
            }
        }

        return false;
    }

    @Override
    protected boolean stopScan() {
        return true;
    }

    @Override
    public boolean connectDevice(PeerDevice device) {
        final CustomAddress address = findAccessoryForDevice(device);
        if (address != null) {
            //perform the actual connection.
            MCSLogger.log(TAG, "Opening connection to " + address);
            CustomLayer layer = new CustomLayer();
            if (layer.connect(address)) {
                //connected!
                MCSLogger.log(TAG, "USB layers attached! =" + address);

                return addConnection(device, layer);
            } else {
            }
        }

        MCSLogger.log(TAG, "UsbAccessory for device " + device.getName() + " not found! ");
        return false;
    }

    /**
     *
     * @param device the device to disconnect from
     *
     * @return
     */
    @Override
    protected boolean disconnectDevice(PeerDevice device) {
        boolean result = removeConnection(device);
        return result;
    }

    /**
     * Normally not needed for connection methods.
     * @param device the device to authorize
     * @param authKey the authentication key
     *
     * @return true if authorized.
     */
    @Override
    protected boolean authorizeDevice(PeerDevice device, String authKey) {
        boolean result = false;
        if (device.getConnectionMethodID().equals(getConnectionMethodID())) {
            // Do nothing. AOA does not require SW authorization
            result = true;
        }
        return result;
    }

    /**
     *  Normally not needed for connection methods.
     * @param device device to deauthorize
     *
     * @return true if successful.
     */
    @Override
    protected boolean deauthorizeDevice(PeerDevice device) {
        boolean result = false;
        if (device.getConnectionMethodID().equals(getConnectionMethodID())) {
            // Do nothing. AOA does not require SW authorization
            result = true;
        }
        return result;
    }

    /**
     *
     * @param address the address class
     *
     * @return
     */
    @Override
    public PeerDevice getDeviceForAddress(IMCSConnectionAddress address) {
        if(address instanceof CustomAddress) {
            CustomAddress addr = (CustomAddress)address;
            PeerDevice device = new PeerDevice();
            //TODO - convert to the peer device.
            device.setName(addr.getAddress());
            device.setAddress(addr.getAddress());
            device.setConnMethodID(getConnectionMethodID());
            return device;
        }
        return null;
    }

    /**
     * Check the list of attached devices if they contain an entry for a
     * PeerDevice.
     *
     * @param device Device being searched for
     * @return UsbAccessory entry for the PeerDevice or null if no entry found
     */
    private CustomAddress findAccessoryForDevice(PeerDevice device) {
        synchronized (this) {
            List<CustomAddress> availableDevices = m_availableDevices;
            for (CustomAddress addr : availableDevices) {
                //implement your custom comparisons to match the conversion from
                // PeerDevice to CustomAddress
                if (device.getAddress().compareTo( addr.getAddress() ) == 0) {
                    return addr;
                }
            }
        }
        return null;
    }

    /**
     * Called when the listener finds a new custom device
     * @param address
     */
    @Override
    public void onCustomDetected(CustomAddress address) {
        synchronized (m_availableDevices) {
            m_availableDevices.add(address);
        }
    }

}
