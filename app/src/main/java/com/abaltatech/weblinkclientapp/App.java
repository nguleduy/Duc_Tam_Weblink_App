/****************************************************************************
 *
 * @file App.java
 * @brief
 *
 * Contains the App class.
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

import android.app.Application;
import android.content.Context;

import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.mcs.logger.android.LoggerAndroid;
import com.abaltatech.weblinkclient.WebLinkClientCore;

public class App extends Application {
    static final String TAG = "WLClientApp";
    private static App ms_instance = null;
    
    private WebLinkClient m_wlClient;
    
    @Override
    public void onCreate() {
        super.onCreate();
        ms_instance = this;
        /*if (AudioDecoder.isSupported(IAudioDecoder.CODEC_ID_AAC)) {
            AudioDecoderFactory.instance()
                    .registerDecoder(IAudioDecoder.CODEC_ID_AAC, AudioDecoder.class);
        }*/
        m_wlClient = new WebLinkClient(this);

        //register MCSLogger. used for internal logs.
        MCSLogger.registerLogger(new LoggerAndroid());
        MCSLogger.setLogLevel(MCSLogger.eAll);
    }

    /**
     * Get the Application instance.
     */
    public static App instance() {
        return ms_instance;
    }
    /**
     * Get the application context.
     */
    public static Context getAppContext() {
        return ms_instance != null ? ms_instance.getApplicationContext() : null;
    }
    
    /**
     * Get the WebLinkClient object.
     */
    public WebLinkClient getWLClient() {
        return m_wlClient;
    }

    /**
     * Get the WebLinkClientCore object
     * @return
     */
    public WebLinkClientCore getWLClientCore() {
        return m_wlClient.getWLClientCore();
    }

}
