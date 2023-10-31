/****************************************************************************
 *
 * @file ServiceClient.java
 * @brief
 *
 * Contains the ServiceClient class.
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

import android.annotation.SuppressLint;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.abaltatech.wlappservices.ERequestMethod;
import com.abaltatech.wlappservices.EServiceDiscoveryErrorCode;
import com.abaltatech.wlappservices.EServiceErrorCode;
import com.abaltatech.wlappservices.IServiceDiscoveryNotification;
import com.abaltatech.wlappservices.IServiceNotificationHandler;
import com.abaltatech.wlappservices.IServiceResponseNotification;
import com.abaltatech.wlappservices.ServiceManager;
import com.abaltatech.wlappservices.ServiceProxy;
import com.abaltatech.wlappservices.ServiceRequest;
import com.abaltatech.wlappservices.ServiceResponse;

/**
 * Example implementation for a WEBLINK app services client.
 */
public class ServiceClient  implements IServiceNotificationHandler{
	static final String SERVICE_NAME               = "com.abaltatech.weblink.test.sampleTimer";
	static final String CLIENT_SERVICE_NAME        = "com.abaltatech.weblink.test.sampleTimer.client";
	static final String PROXY_SERVICE_NAME         = "com.abaltatech.weblink.test.sampleTimer.proxy";

	
	private static final String TAG = ServiceClient.class.getSimpleName();
	
	
	private Handler m_handler = new Handler();
	private ServiceProxy  m_serviceProxy=null;
	private int m_statusRequestID=0;
	private int m_startRequestID=0;
	private DiscoveryNotification m_discoveryNotification = new DiscoveryNotification();
	private ServiceResponseNotification m_responseNotification = new ServiceResponseNotification();
	private TextView m_tvTimer = null;
	
	public class DiscoveryNotification implements IServiceDiscoveryNotification
	{
		//	  public:
		public boolean onServiceFound(ServiceProxy service, int index) {
			Log.d(TAG,"onServiceFound " + service);
			m_serviceProxy = service;
			//			  UpdateUI();
			//			  m_ui.btnConnectService.setText("Disconnect Service");

			// Register for service notifications
			boolean res = m_serviceProxy.registerForNotification(TestAppService_Timer.getNotificationResourcePath(), ServiceClient.this);
			
			// Get the initial timer value
			ServiceRequest request = new ServiceRequest();
			request.setRequestBody(null);
			request.setRequestMethod(ERequestMethod.GET);
			m_statusRequestID = m_serviceProxy.sendRequest(TestAppService_Timer.getTimerResourcePath(), request, m_responseNotification);
			Log.d(TAG,"onServiceFound: register for notifications res = "+ res + " sent request id="+m_statusRequestID);
			
			return false; // Abort searching as we don't need the other services
		}
		public void onServiceDiscoveryCompleted( int foundCount) {
			Log.d(TAG,"onServiceDiscoveryCompleted " + foundCount);
		}
		public void onServiceDiscoveryFailed(EServiceDiscoveryErrorCode code) {
			Log.d(TAG,"onServiceDiscoveryFailed " + code);
		}

	};
	@SuppressLint("DefaultLocale")
    @Override
	public void onNotification(String resourcePath, byte[] notificationData) {

		// Check the resource path of the notification
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

	public class ServiceResponseNotification implements IServiceResponseNotification
	{
		public void onResponseReceived(ServiceRequest request, ServiceResponse response) {
			// Check which request is this
			//Log.d(TAG,"====> requestID: "+request.getRequestID()+" , response:"+ response);
			if(request.getRequestID() == m_statusRequestID || request.getRequestID() == m_startRequestID)
			{
				// The notification has the same format as the status response!
				onNotification(TestAppService_Timer.getNotificationResourcePath(), response.getResponseBody());
			}
		}
		public void onRequestFailed(ServiceRequest request, EServiceErrorCode code, ServiceResponse response) {
			//Log.d(TAG,"====> requestID: "+request.getRequestID()+" , error code:"+ code);
		}

	};

	public boolean getIsServiceConnected() {
		return m_serviceProxy != null;
	}


	/**
	 * Handles the "Connect Service" button click
	 * @param index selected index.
	 */
	public void OnConnectService(int index)
	{
		if(m_serviceProxy!=null)
		{
			// Disconnect service
			boolean res = m_serviceProxy.unregisterFromNotification(TestAppService_Timer.getNotificationResourcePath(),this);
			m_serviceProxy = null;

			Log.d(TAG,"OnConnectService STOP res = " + res);
			//UpdateUI();
			//m_ui.btnConnectService.setText("Connect Service");
		}
		else
		{
			ServiceManager manager = ServiceManager.getInstance();
			String pServiceName = null;
			switch(index)
			{
			case 0:
				pServiceName = SERVICE_NAME;
				break;

			case 1:
				pServiceName = CLIENT_SERVICE_NAME;
				break;

			case 2:
				pServiceName = PROXY_SERVICE_NAME;
				break;

			default:
				//ASSERT(FALSE);
				return;
			}

			Log.d(TAG,"OnConnectService START name = " + pServiceName);
			manager.findServiceByName(pServiceName, m_discoveryNotification);
		}
		//UpdateUI();
	}

	/**
	 * Handles the "Start" button click
	 * @param toggle state of the toggle
	 */
	public void onStart(boolean toggle)
	{
		if(m_serviceProxy!=null)
		{
			// Send a Start/Stop request
			ServiceRequest request = new ServiceRequest();
			request.setRequestBody(null);
			request.setRequestMethod(ERequestMethod.PUT);
			String path = new String(TestAppService_Timer.getTimerResourcePath());
			path += "?start=";
			//QString str = m_ui.btnStart.text();

			// If we currently display "Start" then we have to send a start request; otherwise we have to send a stop request
			path += toggle ? "false" : "true"; // "false" to stop the timer; "true" to start the timer

			// Send the request
			m_startRequestID = m_serviceProxy.sendRequest(path, request, m_responseNotification);
		}
	}

	/**
	 * Handles the "Reset" button click
	 */
	public void onReset()
	{
		if(m_serviceProxy!=null)
		{
			// Send a Reset request
			ServiceRequest request = new ServiceRequest();
			request.setRequestBody(null);
			request.setRequestMethod(ERequestMethod.DELETE);
			m_startRequestID = m_serviceProxy.sendRequest(TestAppService_Timer.getTimerResourcePath(), request, m_responseNotification);
		}
	}

	public void setTimerTextView(TextView tv) {
		m_tvTimer = tv;
	}

}
