/****************************************************************************
 *
 * @file TestAppService_Timer.java
 * @brief
 *
 * Contains the TestAppService_Timer class.
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

import android.util.Log;
import android.util.SparseArray;

import com.abaltatech.wlappservices.ERequestMethod;
import com.abaltatech.wlappservices.EServiceErrorCode;
import com.abaltatech.wlappservices.IServiceHandler;
import com.abaltatech.wlappservices.IServiceNotificationHandler;
import com.abaltatech.wlappservices.IServiceResponseNotification;
import com.abaltatech.wlappservices.ServiceRequest;
import com.abaltatech.wlappservices.ServiceResponse;
import com.abaltatech.wlappservices.WLServicesHTTPProxyServiceHandler.NotificationHandler;
import com.abaltatech.wlappservices.WLServicesHTTPProxyServiceHandler.RequestWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Example implementation of a WEBLINK app service.
 */
public class TestAppService_Timer implements IServiceHandler {
	private static final String TAG = TestAppService_Timer.class.getSimpleName();
	private static final String NotificationID = "Notification1";
	private static final String RESOURCE_PATH = "timer";
	private static final String RESOURCE_PATH_TIMEOUT = "timeout";
	private static final String RESOURCE_PATH_STARTSTOP_TIMER = "timer?start=";
	private static final String RESOURCE_PATH_NOTIFICATION = "notification";
	public static String getNotificationResourcePath() {
		return RESOURCE_PATH_NOTIFICATION;
	}
	public static String getTimerResourcePath() {
		return RESOURCE_PATH;
	};
	
	private final Object m_lock = new Object();
	protected List<IServiceNotificationHandler> m_notificationList = new ArrayList<IServiceNotificationHandler>();
	protected List<RequestWrapper> m_requestsQueue = new ArrayList<RequestWrapper>();
	protected SparseArray<RequestWrapper> m_pendingRequests = new SparseArray<RequestWrapper>();
	public enum ETimerStatus {
		ETS_UNKNOWN,
		ETS_STARTED,
		ETS_STOPPED,
		ETS_RESET,
	};
	protected boolean  m_isStopped = false;
	protected int m_nextRequestID = 1;
	protected ETimerStatus m_timerStatus = ETimerStatus.ETS_STOPPED;
	protected long m_startTime = 0;
	protected long m_timerValue = 0;
	protected int m_sendNotificationID = 0;
	private HandlerThread m_thread = null;

	@Override
	public int onProcessRequest(String resourcePath, ServiceRequest request,
			IServiceResponseNotification notification) {
		synchronized(m_lock) {
			  int requestID = m_nextRequestID;
			  m_nextRequestID++;

			  // Put the request into the list of pending requests
			  RequestWrapper wrapper = new RequestWrapper(requestID, request, resourcePath, notification);
			  m_pendingRequests.put(requestID, wrapper);
			  m_requestsQueue.add(wrapper);

			  return requestID;
		}
	}
	@Override
	public boolean onCancelRequest(int requestID) {
		synchronized(m_lock) {
			if(m_pendingRequests.get(requestID) != null) {
				m_pendingRequests.remove(requestID);
				return true;
			}
			return false;
		}
	}
	@Override
	public void registerForNotification(String resourcePath,
			IServiceNotificationHandler handler) {
		if(resourcePath.equals(RESOURCE_PATH_NOTIFICATION)) {
			synchronized(m_lock) {
				m_notificationList.add(handler);
			}
		}
	}
	@Override
	public void unregisterFromNotification(String resourcePath,
			IServiceNotificationHandler handler) {
		if(resourcePath.equals(RESOURCE_PATH_NOTIFICATION)) {
			synchronized(m_lock) {
				m_notificationList.remove(handler);
			}
		}
	}
	@Override
	public void removeAllNotifications() {
		synchronized(m_lock) {
		}
	}

	public void start() {
		if(m_thread == null) {
			m_thread = new HandlerThread();
			m_thread.start();
		}
	}
	public void stop() {
		if(m_thread != null) {
			m_thread.cancel();
			m_thread.interrupt();
			m_thread = null;
		}
	}
	private class HandlerThread extends Thread {
		private boolean m_exitFlag = false;
		public void cancel(){ 
			m_exitFlag = true;
		}
		@Override
		public void run() {
			setName(TAG+"Thread");
			Log.d(TAG,"START "+TAG+"Thread");
			long curTime;
			List<NotificationHandler> curList;

			while(!m_exitFlag && !isInterrupted())
			{
				// Update timer value
				boolean notify = false;
				long timerValue=0;
				curTime = System.currentTimeMillis()/1000;
				ETimerStatus status = ETimerStatus.ETS_UNKNOWN;
				synchronized(m_lock){
					if(m_timerStatus == ETimerStatus.ETS_STARTED)
					{
						timerValue = curTime - m_startTime;
						if(m_timerValue != timerValue)
						{
							m_timerValue = timerValue;
							notify = true;
							status = m_timerStatus;
						}
					}
				}
				if(notify)
				{
					notifyTimerState(status, timerValue);
				}

				// Search for a pending request
				RequestWrapper request = null;
				synchronized(m_lock){
					if(m_requestsQueue.size() > 0)
					{
						request = m_requestsQueue.get(0);
						m_requestsQueue.remove(0);
					}
				}

				// Process a request
				if(request != null)
				{
					String path = request.m_resourcePath;
					if(path.compareTo(RESOURCE_PATH) == 0 && request.m_request.getRequestMethod() == ERequestMethod.GET)
					{
						processCurrentTimeRequest(request);
					}
					else if(path.compareTo(RESOURCE_PATH_TIMEOUT) == 0 && request.m_request.getRequestMethod() == ERequestMethod.GET)
					{
						processTimeoutRequest(request);
					}
					else if(path.startsWith(RESOURCE_PATH_STARTSTOP_TIMER) && request.m_request.getRequestMethod() == ERequestMethod.PUT)
					{
						processStartStopRequest(request);
					}
					else if(path.compareTo(RESOURCE_PATH) == 0 && request.m_request.getRequestMethod() == ERequestMethod.DELETE)
					{
						stopTimer(request.m_requestID, request.m_request, request.m_notification, true);
					}
					// TODO: add support for other requests
					else
					{
						request.m_notification.onRequestFailed(request.m_request, EServiceErrorCode.UnsupportedRequest, null);
					}
				}
				else
				{
					try {
						Thread.sleep(50);
					} catch (InterruptedException e) {
					}
				}
			}

			Log.d(TAG,"STOP "+TAG+"Thread");
		}
	}


	void processStartStopRequest(RequestWrapper request)
	{
		// Check if the request was canceled
		boolean isCanceled = false;
		synchronized(m_lock) {	  
			if(m_pendingRequests.get(request.m_requestID) != null) {
				m_pendingRequests.remove(request.m_requestID);
			} else {
				isCanceled = true;
			}

		}

		if(!isCanceled)
		{
			String pResourcePath = request.m_resourcePath.substring(RESOURCE_PATH_STARTSTOP_TIMER.length());
			if(pResourcePath.startsWith("true"))
			{
				startTimer(request.m_requestID, request.m_request, request.m_notification);
			}
			else if(pResourcePath.startsWith("false"))
			{
				stopTimer(request.m_requestID, request.m_request, request.m_notification, false);
			}
			else
			{
				request.m_notification.onRequestFailed(request.m_request, EServiceErrorCode.InvalidArgument, null);
			}
		}
	}

	void processCurrentTimeRequest(RequestWrapper request)
	{
		//time_t rawtime;
		//struct tm * timeinfo;
		//char buffer [80];
		//int myRequestID = request.m_requestID;

		//time (&rawtime);
		//timeinfo = localtime (&rawtime);

		//strftime (buffer, 80, "%c",timeinfo);

		// Check if the request was canceled
		boolean isCanceled = false;
		long timerValue = 0;
		ETimerStatus status;
		synchronized(m_lock) {
			if(m_pendingRequests.get(request.m_requestID) != null) {
				m_pendingRequests.remove(request.m_requestID);
			} else {
				isCanceled = true;
			}

			timerValue = m_timerValue;
			status = m_timerStatus;
		}

		if(!isCanceled)
		{
			ServiceResponse response = new ServiceResponse();
			response.setRequestID(request.m_requestID);
			String responseBody = formatResponse(timerValue, status);
			response.setResponseBody(responseBody.getBytes());
			request.m_notification.onResponseReceived(request.m_request, response);
		}
	}

	void processTimeoutRequest(RequestWrapper request)
	{
		//const byte* source = request.m_request.GetRequestBody();
		byte[] source = request.m_request.getRequestBody();
		int timeout = (int)source[0] | (int)source[1]<<8 | (int)source[2]<<16 | (int)source[3]<<24;
		if(timeout <= 0 || source.length != 4)
		{
			request.m_notification.onRequestFailed(request.m_request, EServiceErrorCode.InvalidArgument, null);
		}
		else
		{
			TimeoutHandler handler = new TimeoutHandler(timeout, request); // This will start a thread; the THIS pointer will be deleted at the end of the thread func 
			handler.start();
		}
	}

	void onTimeout(RequestWrapper request, TimeoutHandler handler)
	{
		// Check if the request was canceled
		boolean isCanceled = false;
		synchronized(m_lock) {
			if(m_pendingRequests.get(request.m_requestID) != null) {
				m_pendingRequests.remove(request.m_requestID);
			} else {
				isCanceled = true;
			}
		}

		if(!isCanceled)
		{
			ServiceResponse response = new ServiceResponse();
			response.setRequestID(request.m_requestID);
			response.setResponseBody(null); // Nothing to return, just notify that the timeout has elapsed
			request.m_notification.onResponseReceived(request.m_request, response);
		}

	}

	void startTimer(int requestID, ServiceRequest request, IServiceResponseNotification notification)
	{
		long timerValue = 0;
		boolean notify = false;
		synchronized(m_lock) {
			// Initialize timer
			if(m_timerStatus != ETimerStatus.ETS_STARTED)
			{
				m_timerStatus = ETimerStatus.ETS_STARTED;
				notify = true;
				if(0 == m_timerValue)
				{
					m_startTime = System.currentTimeMillis()/1000;
				}
				else
				{
					// Shift the start time to the current time minus elapsed time
					long rawtime = System.currentTimeMillis()/1000;
					m_startTime = rawtime - m_timerValue;
				}
			}
			timerValue = m_timerValue;
		}

		if(request != null && notification != null)
		{
			ServiceResponse response = new ServiceResponse();
			response.setRequestID(requestID);
			String responseBody = formatResponse(timerValue, ETimerStatus.ETS_STARTED);
			response.setResponseBody(responseBody.getBytes());
			notification.onResponseReceived(request, response);
		}
		if(notify) {
			notifyTimerState(ETimerStatus.ETS_STARTED, timerValue);
		}

	}

	void stopTimer(int requestID, ServiceRequest request, IServiceResponseNotification notification, boolean resetTimer)
	{
		long timerValue = 0;
		boolean notify = false;
		synchronized(m_lock) {
			if(m_timerStatus != ETimerStatus.ETS_STOPPED) {
				m_timerStatus = ETimerStatus.ETS_STOPPED;
				notify = true;
			}
			if(resetTimer) {
				m_timerValue = 0;//System.currentTimeMillis()/1000;
				notify = true;
			}
			timerValue = m_timerValue;
		}
		ETimerStatus state = resetTimer ? ETimerStatus.ETS_RESET : ETimerStatus.ETS_STOPPED;
		if(request != null && notification != null) {
			ServiceResponse response = new ServiceResponse();
			response.setRequestID(requestID);
			String responseBody = formatResponse(timerValue, state);
			response.setResponseBody(responseBody.getBytes());
			notification.onResponseReceived(request, response);
		}
		if(notify) {
			notifyTimerState(state, timerValue);
		}
	}

	String formatResponse(long timerValue, ETimerStatus status)
	{
		String buffer = null;

		String statusText = null;
		switch(status)
		{
		case ETS_STARTED:
			statusText = "STARTED";
			break;

		case ETS_STOPPED:
			statusText = "STOPPED";
			break;

		case ETS_RESET:
			statusText = "RESET";
			break;

		default:
			// Do nothing
			statusText = null;
			break;
		}

		int notID = 0;
		synchronized(m_lock) {
			m_sendNotificationID++;
			notID = m_sendNotificationID;
		}

		if(statusText != null)
		{
			buffer = String.format( "{ \"time\":%d, \"status\":\"%s\", \"NotificationID\": %d }", timerValue, statusText, notID);
		}
		else
		{
			buffer = String.format("{ \"time\":%d, \"NotificationID\": %d }", timerValue, notID);
		}

		return buffer;
	}

	void notifyTimerState(ETimerStatus status, long timerValue)
	{
		// Copy the list of notification handlers
		List<IServiceNotificationHandler> handlers;
		synchronized(m_lock) {
			handlers = new ArrayList<IServiceNotificationHandler>(m_notificationList);
		}

		// Prepare a notification data
		String responseBody = formatResponse(timerValue, status);
//		Log.d(TAG,"Send notification: "+responseBody);

		// Notify all handlers
		for(IServiceNotificationHandler handler : handlers) {
			handler.onNotification(RESOURCE_PATH_NOTIFICATION, responseBody.getBytes());
		}
	}


	private class TimeoutHandler extends Thread {
		//public:
		public TimeoutHandler(int timeout, RequestWrapper wrapper)
		{
			m_timeout = timeout;
			m_wrapper = wrapper;
		}

		public void run()
		{
			try {
				Thread.sleep(m_timeout);
			} catch (InterruptedException e) {
			}
			onTimeout(m_wrapper, this);
		}

		//private:
		private RequestWrapper m_wrapper;
		private int m_timeout;
	}


}
