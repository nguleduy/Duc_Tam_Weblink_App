/****************************************************************************
 *
 * @file Services.java
 * @brief
 *
 * Contains the Services class.
 *
 * @author Abalta Technologies, Inc.
 * @date Dec, 2016
 *
 * @cond Copyright
 *
 * COPYRIGHT 2016 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
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
package com.abaltatech.weblinkclientapp.services;

import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.wlappservices.IServiceHandler;
import com.abaltatech.wlappservices.IServiceNotificationHandler;
import com.abaltatech.wlappservices.ServiceManager;
import com.abaltatech.wlappservices.WLServicesHTTPProxy;

import java.util.ArrayList;
import java.util.List;

/**
 * Example implementation of a WEBLINK services manager.
 */
public class Services {
	private static final String TAG                        = "Services";
	private static final String SERVICE_NAME               = "com.abaltatech.weblink.test.sampleTimer";
	private static final String CLIENT_SERVICE_NAME        = "com.abaltatech.weblink.test.sampleTimer.client";
	private static final String PROXY_SERVICE_NAME         = "com.abaltatech.weblink.test.sampleTimer.proxy";
	

	private Handler m_handler = new Handler();
	protected boolean m_timerStarted = false;
	protected boolean m_isResetEnabled = false;
	protected IServiceHandler m_service = null;
	protected String m_proxyConfigFile = null;
	protected TextView m_tvTimer = null;
	protected IServiceNotificationHandler m_notificationHandler = new IServiceNotificationHandler(){

		@Override
		public void onNotification(String resourcePath, byte[] notificationData) {
		  if(resourcePath.compareTo(TestAppService_Timer.getNotificationResourcePath()) == 0)
		  {
			String notification = new String(notificationData);
			int pos = notification.indexOf("\"time\":");
			int timerValue = 0;
			if(pos>=0) {
				String value = notification.substring(pos+"\"time\":".length());
				
				value = value.split(",", 2)[0];
				try {
					timerValue = Integer.valueOf(value);
				} catch(NumberFormatException e) {
					timerValue = 0;
				}
			}


			boolean isStarted = false;
			pos = notification.indexOf("\"status\":\"");
		    if(pos >=0)
		    {
		    	String value = notification.substring(pos+ "\"status\":\"".length());
		    	value = value.trim();
		    	if(value.startsWith("STARTED")) {
		    		isStarted = true;
		    	}
		    }


		    // Format a time string
		    int seconds = timerValue % 60;
		    timerValue = timerValue / 60;
		    int minutes = timerValue % 60;
		    timerValue = timerValue / 60;
		    int hours = timerValue % 24;
		    timerValue = timerValue / 24;
		    
			final String f = String.format("TIMER(%S) %02d:%02d:%02d",(isStarted?"started":"paused"), hours, minutes, seconds);
//		    Log.d(TAG,f);
		    
		    if(m_tvTimer != null) {
		    	m_handler.post(new Runnable(){
		    		@Override
		    		public void run() {
		    			m_tvTimer.setText(f);
		    		}
		    	});
		    }
		  }
		}
	};


		/**
		 * Updates the UI
		 */
		void UpdateUI()
		{
		}


		/**
		 * Handles the "Connect Service" button click
		 */
		public void OnRegisterService()
		{
			if(m_service != null) {
				ServiceManager.getInstance().unregisterService(CLIENT_SERVICE_NAME,m_service);
				((TestAppService_Timer)m_service).stop();
				m_service = null;
			} else {
				m_service = new TestAppService_Timer();
				((TestAppService_Timer)m_service).start();
				List<String> protocols = new ArrayList<String>();
				protocols.add("TestProtocol111");
				protocols.add("TestProtocol222");
				boolean res = ServiceManager.getInstance().registerService(CLIENT_SERVICE_NAME, protocols, m_service);
				m_service.registerForNotification(TestAppService_Timer.getNotificationResourcePath(), m_notificationHandler);
				Log.d(TAG,"OnRegisterService res="+res);
			}
			UpdateUI();
		}
		public void setTimerTextView(TextView tv) {
			m_tvTimer = tv;
		}
		public boolean getTimerStarted(){ 
			return m_timerStarted;
		}
		public boolean getProxyServerStarted(){ 
			return WLServicesHTTPProxy.getInstance().isWLServicesServerRunning();
		}
		public boolean getProxyClientStarted(){ 
			return WLServicesHTTPProxy.getInstance().isWLServicesClientRunning();
		}
		public boolean getServiceRegistered(){ 
			return m_service != null;
		}

		/**
		 * Handles the "Start" button click
		 */
		public void onStart()
		{
			if(m_service != null)
			{
				if(!m_timerStarted)
				{
					((TestAppService_Timer)m_service).startTimer(-1, null, null);
					m_timerStarted = true;
				}
				else
				{
					((TestAppService_Timer)m_service).stopTimer(-1, null, null, false);
					m_timerStarted = false;
				}
			}
		}


		/**
		 * Handles the "Reset" button click
		 */
		public void onReset()
		{
			if(m_service != null)
			{
				((TestAppService_Timer)m_service).stopTimer(-1, null, null, true);
				m_timerStarted = false;
			}
		}

		/**
		 * Handles the "Start/Stop HTTP Proxy Server" button click
		 */
		public void onHTTPProxyServer() {
			if(!WLServicesHTTPProxy.getInstance().isWLServicesServerRunning()) {
				String path = m_proxyConfigFile;
				if(!WLServicesHTTPProxy.getInstance().startWLServicesServer(path)) {
					MCSLogger.log(MCSLogger.eWarning, TAG,"Could not start WL Services Client HTTP Proxy");
				}
			} else {
				WLServicesHTTPProxy.getInstance().stopWLServicesServer();
			}
		}

		/**
		 * Handles the "Start/Stop HTTP Proxy Client" button click
		 */
		public void onHTTPProxyClient()
		{
			if(!WLServicesHTTPProxy.getInstance().isWLServicesClientRunning()) {
				if(!WLServicesHTTPProxy.getInstance().startWLServicesClient()){
					MCSLogger.log(MCSLogger.eWarning, TAG,"Could not start WL Services Client HTTP Proxy");
					return;
				}

			} else {
				WLServicesHTTPProxy.getInstance().stopWLServicesClient();
			}
		}

}
