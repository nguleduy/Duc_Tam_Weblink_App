/****************************************************************************
 *
 * @file IConnectionStatusNotification.java
 * @brief
 *
 * Contains the IConnectionStatusNotification interface.
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

import com.abaltatech.mcs.connectionmanager.EConnectionResult;
import com.abaltatech.mcs.connectionmanager.PeerDevice;

/**
 * The WEBLINK connection status notification interface.
 */
public interface IConnectionStatusNotification {
	
	/**
	 * Called when connection to the server has been established
     *
     * @param peerDevice PeerDevice which connects successfully
	 */
    void onConnectionEstablished(PeerDevice peerDevice);

    /**
     * Called when failed to connect to the server
     *
     * @param peerDevice PeerDevice which fails to connect
     * @param result Reason for connection failing
     */
    void onConnectionFailed(PeerDevice peerDevice, EConnectionResult result);
    
    /**
     * Called when connection to the server has been closed
     *
     * @param peerDevice PeerDevice which connection is closed
     */
    void onConnectionClosed(PeerDevice peerDevice);
}
