/****************************************************************************
 *
 * @file CustomAddress.java
 * @brief 
 *
 * Contains definition of USB address connection point.
 *
 * @author Abalta Technologies, Inc.
 * @date October, 2014
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
package com.abaltatech.weblinkclient.connectionmethod;

import android.annotation.TargetApi;
import android.os.Build;

import com.abaltatech.mcs.common.IMCSConnectionAddress;

/**
 * Represents an {@link IMCSConnectionAddress} for the UsbHost implementation for connecting over
 * USB as an Android Accessory to an android device.
 */
@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
public class CustomAddress implements IMCSConnectionAddress {
    @SuppressWarnings("unused")
    private static final String TAG = CustomAddress.class.getSimpleName();


    protected  String m_address;

    /**
     * Custom address that contains all information neeeded for the custom layer to connect
     */
    public CustomAddress(String address) {
        m_address = address;
    }

    /**
     * Get the address.
     * @return
     */
    public String getAddress() {
        return m_address;
    }

    @Override
    public boolean equals (Object object) {
        boolean isSame = object == this;
        if( !isSame && object != null && object instanceof CustomAddress) {
            CustomAddress usbAddress = (CustomAddress)object;

            isSame = m_address.equals(usbAddress.m_address) ;
        }
        return isSame;
    }

    @Override 
    public int hashCode() {
        // Start with a non-zero constant.
        int result = 17;

        result = 31 * result + (m_address == null ? 0 : m_address.hashCode());
        return result;
    }

    /**
     * TODO - implement
     * @param address
     * @return
     */
    @Override
    public boolean isSameAs(IMCSConnectionAddress address) {
        return equals(address);
    }

    /**
     * TODO - implement.
     * @return
     */
    @Override 
    public String toString() {
        String res = (m_address == null ? "*null*" : m_address);
        return res;
    }

    public boolean isSubsetOf(IMCSConnectionAddress address) {
        return false;
    }
}
