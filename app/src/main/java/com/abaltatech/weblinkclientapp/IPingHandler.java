/****************************************************************************
 *
 * @file IPingHandler.java
 * @brief
 *
 * Defines the IPingHandler class.
 *
 * @author Abalta Technologies, Inc.
 * @date May/2019
 *
 * @cond Copyright
 *
 * COPYRIGHT 2019 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
 * This program may not be reproduced, in whole or in part in any form
 * or any means whatsoever without the written permission of ABALTA TECHNOLOGIES.
 *
 * @endcond
 *****************************************************************************/
package com.abaltatech.weblinkclientapp;

/**
 * Helper interface for handlers of the ping timeout.
 */
interface IPingHandler {

    /**
     * Called when the host app is no longer communicating with the client actively.
     */
    void onPingResponseTimeout();

    /**
     * Called while communication is active and the host app is responding.
     *
     * The host app reports its status in this call, so if needed the client may want to handle the
     * case where the host app is connected and responsive, but reports as inactive.
     *
     * @param isSenderInactive the host app's reported activity status.
     *                         If true, the host app is not configured.
     */

    void onPingResponseReceived(boolean isSenderInactive);
    
}
