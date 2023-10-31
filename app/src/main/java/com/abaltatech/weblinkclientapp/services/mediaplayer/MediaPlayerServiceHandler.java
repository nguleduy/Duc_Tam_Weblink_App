/****************************************************************************
 *
 * @file MediaPlayerServiceHandler.java
 * @brief
 *
 * Contains the MediaPlayerServiceHandler class.
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
package com.abaltatech.weblinkclientapp.services.mediaplayer;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.abaltatech.wlappservices.ERequestMethod;
import com.abaltatech.wlappservices.EServiceErrorCode;
import com.abaltatech.wlappservices.IServiceNotificationHandler;
import com.abaltatech.wlappservices.IServiceResponseNotification;
import com.abaltatech.wlappservices.ServiceProxy;
import com.abaltatech.wlappservices.ServiceRequest;
import com.abaltatech.wlappservices.ServiceResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Helper class that wraps the communication with a Media Service.
 *
 * This handler can send commands to the service and will subscribe for notifications.
 *
 * This handler will forward notifications the Media Service Manager and will append the name of
 * the service that generated the notification as this piece of informations is not contained in
 * the notification itself.
 */
public class MediaPlayerServiceHandler implements IServiceNotificationHandler, IServiceResponseNotification {

    // The Service Proxy that is used to send commands to the service
    private final ServiceProxy m_service;

    // Consumer of notifications from the service
    private final IServiceHandlerNotification m_notification;

    // Name of the service
    private final String m_serviceName;

    // List of IDs of requests for the current media status.
    private final Set<Integer> m_statusRequestIDs = new LinkedHashSet<Integer>();
    // List of IDs of requests for the current media item information.
    private final Set<Integer> m_mediaInfoRequestIDs = new LinkedHashSet<Integer>();
    /// List of IDs of requests for the album artwork of the current media item.
    private final Set<Integer> m_imageRequestIDs = new LinkedHashSet<Integer>();

    // ID of the current media item
    private String m_mediaItemId = "";

    // The current media status, i.e. Playing/Paused
    private String m_mediaStatus = "";

    /**
     * Interface for consumer of service notifications.
     */
    public interface IServiceHandlerNotification {

        /**
         * Called when the current media status has changed.
         *
         * @param serviceName Name of the service that generated the notification
         * @param hasFocus true if the service has UI focus, false otherwise
         * @param mediaStatus media status
         * @param audioStatus audio status
         * @param videoStatus video status
         * @param playbackPosition playback position (in seconds) of the current media item
         * @param mediaItemLength length (in seconds) of the current media item
         */
        void onMediaStatusChanged(String serviceName, boolean hasFocus,
                                  String mediaStatus, String audioStatus, String videoStatus,
                                  int playbackPosition, int mediaItemLength);

        /**
         * Called when the current media item has changed.
         *
         * @param serviceName Name of the service that changed the media item
         * @param title Name of the new media item
         * @param artist Artist name of the new media item
         * @param albumName Name of the album of which the new media item belongs to
         */
        void onMediaItemChanged(String serviceName, String title, String artist, String albumName);

        /**
         * Called when the artwork for the album has been received.
         *
         * @param serviceName Name of the service that sent the artwork
         * @param image Artwork bitmap
         */
        void onImageDownloaded(String serviceName, Bitmap image);

        /**
         * Called when a request failed.
         *
         * @param serviceName Name of the service that the request was for
         * @param requestID ID of the request
         * @param code Error code
         * @param response Service response or null if no response
         */
        void onRequestFailed(String serviceName, int requestID, EServiceErrorCode code, JSONObject response);

        /**
         * Called when a response from the service has been received
         *
         * @param serviceName Name of the service that sent the response
         * @param requestID ID of the request to which the service is responding
         * @param response The service response or null if no response
         */
        void onRequestResponseReceived(String serviceName, int requestID, JSONObject response);
    }

    /**
     * Default constructor.
     *
     * @param service Sevice Proxy object
     * @param notification Consumer of service notifications
     */
    public MediaPlayerServiceHandler(ServiceProxy service, IServiceHandlerNotification notification) {
        m_service = service;
        m_notification = notification;
        m_serviceName = service.getServiceName();
    }

    /**
     * Initialize the handler.
     *
     * After this call , the handler will start receiving notifications from the service and will
     * request the current media status of the service.
     */
    public void init() {
        m_service.registerForNotification(MediaPlayerServiceConstants.MEDIA_SERVICE_NOTIFICATION_PATH, this);
        requestCurrentStatus();
    }

    /**
     * Request the current media status of the service.
     *
     * The result is delivered through the {@link IServiceHandlerNotification} interface.
     */
    public void requestCurrentStatus() {
        ServiceRequest request = new ServiceRequest();
        request.setRequestBody(null);
        request.setRequestMethod(ERequestMethod.GET);
        String path = MediaPlayerServiceConstants.MEDIA_SERVICE_NOTIFICATION_PATH;
        int requestID = m_service.sendRequest(path, request, this);
        m_statusRequestIDs.add(requestID);
    }

    /**
     * Requests the ID of the current media item.
     *
     * The result is delivered through the
     * {@link IServiceHandlerNotification#onMediaItemChanged(String, String, String, String)}
     * notification.
     *
     * @return The request ID or {@link MediaPlayerServiceConstants#INVALID_REQUEST_ID} on error.
     */
    public int requestCurrentMediaItemInfo() {
        m_mediaItemId = "";
        ServiceRequest request = new ServiceRequest();
        request.setRequestBody(null);
        request.setRequestMethod(ERequestMethod.GET);
        String path = MediaPlayerServiceConstants.MEDIA_SERVICE_CURRENT_ITEM_PATH;
        int requestID = m_service.sendRequest(path, request, this);
        m_mediaInfoRequestIDs.add(requestID);
        return requestID;
    }

    @Override
    public void onResponseReceived(ServiceRequest request, ServiceResponse response) {
        String jsonStr = new String(response.getResponseBody(), Charset.forName("utf-8"));
        int requestID = request.getRequestID();
        if (m_imageRequestIDs.contains(requestID)) {
            onImageDownloaded(response.getResponseBody());
            m_imageRequestIDs.remove(requestID);
            return;
        }

        JSONObject root;
        try {
            root = new JSONObject(jsonStr);
            if (m_statusRequestIDs.contains(requestID)) {
                onMediaStatusUpdated(root);
                m_statusRequestIDs.remove(requestID);
            } else if (m_mediaInfoRequestIDs.contains(request.getRequestID())) {
                onMediaItemInfoReceived(root);
                m_mediaInfoRequestIDs.remove(requestID);
            } else {
                if (m_notification != null) {
                    m_notification.onRequestResponseReceived(m_serviceName, requestID, root);
                }
            }
        } catch (JSONException e) {
            if (m_notification != null) {
                m_notification.onRequestResponseReceived(m_serviceName, requestID, null);
            }
        }
    }

    @Override
    public void onRequestFailed(ServiceRequest request, EServiceErrorCode code, ServiceResponse response) {
        if (m_notification != null) {
            byte[] responseBody = request.getRequestBody();
            JSONObject responseJSON = null;
            if (responseBody != null && responseBody.length > 0) {
                try {
                    String jsonStr = new String(responseBody, Charset.forName("utf-8"));
                    responseJSON = new JSONObject(jsonStr);
                } catch (JSONException e) {
                    responseJSON = null;
                }
            }
            m_notification.onRequestFailed(m_serviceName, request.getRequestID(), code, responseJSON);
        }
    }

    @Override
    public void onNotification(String resourcePath, byte[] notificationData) {
        if (resourcePath.equals(MediaPlayerServiceConstants.MEDIA_SERVICE_NOTIFICATION_PATH)) {
            String jsonStr = new String(notificationData, Charset.forName("utf-8"));
            try {
                JSONObject root = new JSONObject(jsonStr);
                onMediaStatusUpdated(root);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Helper method that parses the media status notification from the media service.
     *
     * This method will read the media status, request the media item information if the media item
     * has changed since the last notification and will notify the consumer that the media status
     * has changed.
     *
     * @param root Notification data
     */
    private void onMediaStatusUpdated(JSONObject root) {
        try {
            String mediaItemId = root.getString(MediaPlayerServiceConstants.KEY_MEDIA_ITEM_ID);

            String mediaStatus = root.optString(MediaPlayerServiceConstants.KEY_MEDIA_STATUS, "");
            String audioStatus = root.optString(MediaPlayerServiceConstants.KEY_AUDIO_STATUS, "");
            String videoStatus = root.optString(MediaPlayerServiceConstants.KEY_VIDEO_STATUS, "");
            boolean hasFocus = root.optBoolean(MediaPlayerServiceConstants.KEY_HAS_FOCUS, false);
            int playbackPosition = root.optInt(MediaPlayerServiceConstants.KEY_PLAYBACK_POSITION, 0);
            int mediaItemLength = root.optInt(MediaPlayerServiceConstants.KEY_MEDIA_ITEM_LENGTH, 0);

            boolean mediaItemChanged = !m_mediaItemId.equals(mediaItemId);

            m_mediaItemId = mediaItemId;
            m_mediaStatus = mediaStatus;

            if (mediaItemChanged) {
                if (!mediaItemId.isEmpty()) {
                    ServiceRequest request = new ServiceRequest();
                    request.setRequestBody(null);
                    request.setRequestMethod(ERequestMethod.GET);
                    String path = MediaPlayerServiceConstants.MEDIA_SERVICE_ITEM_PATH + m_mediaItemId;
                    int requestID = m_service.sendRequest(path, request, this);
                    m_mediaInfoRequestIDs.add(requestID);
                }
            }
            if (m_notification != null) {
                m_notification.onMediaStatusChanged(m_serviceName, hasFocus, mediaStatus, audioStatus,
                        videoStatus,playbackPosition, mediaItemLength);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Helper method that parses the media item information, requests the album artwork and notifies
     * the consumer if the media item has changed since the last notification.
     *
     * @param root Notification data
     */
    private void onMediaItemInfoReceived(JSONObject root) {
        String mediaItemId = root.optString(MediaPlayerServiceConstants.KEY_ID, "");
        if (!mediaItemId.isEmpty()) {

            String title = "";
            String artistName = "";
            String albumName = "";

            JSONObject object = root.optJSONObject(MediaPlayerServiceConstants.KEY_MEDIA_ITEM_NAME);
            if (object != null) {
                title = object.optString(MediaPlayerServiceConstants.DEFAULT_ID);
                if (title.isEmpty()) {
                    title = object.toString();
                }
            } else {
                title = root.optString(MediaPlayerServiceConstants.KEY_MEDIA_ITEM_NAME, "");
            }

            object = root.optJSONObject(MediaPlayerServiceConstants.KEY_ARTIST);
            if (object != null) {
                JSONObject artist = object.optJSONObject(MediaPlayerServiceConstants.KEY_ARTIST_NAME);
                if (artist != null) {
                    artistName = artist.optString(MediaPlayerServiceConstants.DEFAULT_ID, "");
                    if (artistName.isEmpty()) {
                        albumName = artist.toString();
                    }
                }
            }

            object = root.optJSONObject(MediaPlayerServiceConstants.KEY_ALBUM);
            if (object != null) {
                JSONObject album = object.optJSONObject(MediaPlayerServiceConstants.KEY_ALBUM_NAME);
                if (album != null) {
                    albumName = album.optString(MediaPlayerServiceConstants.DEFAULT_ID, "");
                    if (albumName.isEmpty()) {
                        albumName = album.toString();
                    }
                }
            }

            if (m_notification != null) {
                m_notification.onMediaItemChanged(m_serviceName, title, artistName, albumName);
            }

            String imageId = root.optString(MediaPlayerServiceConstants.KEY_IMAGE_ID);
            if (!imageId.isEmpty()) {
                ServiceRequest request = new ServiceRequest();
                request.setRequestBody(null);
                request.setRequestMethod(ERequestMethod.GET);
                String path = MediaPlayerServiceConstants.MEDIA_SERVICE_IMAGE_PATH + imageId;
                int requestID = m_service.sendRequest(path, request, this);
                m_imageRequestIDs.add(requestID);
            }
        }
    }

    /**
     * Helper method that is called when the album artwork is received.
     *
     * This method notifies the consumer that the artwork was received.
     *
     * @param responseBody Image data
     */
    private void onImageDownloaded(byte[] responseBody) {
        if (responseBody.length < 4) {
            return;
        }

        if (m_notification != null) {
            m_notification.onImageDownloaded(m_serviceName,
                    BitmapFactory.decodeByteArray(responseBody, 0, responseBody.length));
        }
    }

    /**
     * Helper method that sends a control command to the service and returns the ID of the reqest.
     *
     * @param command Command to send
     * @param args Arguments to the command
     * @return Request ID
     */
    private int sendControlCommand(String command, String args) {
        if (m_service != null) {
            ServiceRequest request = new ServiceRequest();
            request.setRequestBody(null);
            request.setRequestMethod(ERequestMethod.PUT);
            String path = MediaPlayerServiceConstants.MEDIA_SERVICE_CONTROL_COMMAND_PATH + command;
            if (args != null) {
                path += String.format(Locale.US, MediaPlayerServiceConstants.ARGUMENT_TEMPLATE, args);
            }
            return m_service.sendRequest(path, request, this);
        }
        return MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    }

    /**
     * Send a next command to the service.
     *
     * @return Request ID
     */
    public int next() {
        return sendControlCommand(MediaPlayerServiceConstants.COMMAND_NEXT, null);
    }

    /**
     * Send a previous command to the service.
     *
     * @return Request ID
     */
    public int prev() {
        return sendControlCommand(MediaPlayerServiceConstants.COMMAND_PREV, null);
    }

    /**
     * Send a play command to the service.
     *
     * @return Request ID
     */
    public int play() {
        return sendControlCommand(MediaPlayerServiceConstants.COMMAND_PLAY, null);
    }

    /**
     * Send a pause command to the service.
     *
     * @return Request ID
     */
    public int pause() {
        return sendControlCommand(MediaPlayerServiceConstants.COMMAND_PAUSE, null);
    }

    /**
     * Checks if the service is paused.
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        return MediaPlayerServiceConstants.STATUS_PAUSED.equals(m_mediaStatus);
    }

    /**
     * Checks if the service is playing.
     *
     * @return true if playing, false otherwise
     */
    public boolean isPlaying() {
        return MediaPlayerServiceConstants.STATUS_PLAYING.equals(m_mediaStatus);
    }

    /**
     * Send a next command to the service.
     *
     * @param progress New position (in seconds)
     *
     * @return Request ID
     */
    public int seek(int progress) {
        return sendControlCommand(MediaPlayerServiceConstants.COMMAND_SEEK, String.valueOf(progress));
    }

    /**
     * Terminates the handler.
     *
     * Will unregister from notifications from the service  and will clean the pending requests.
     */
    public void terminate() {
        m_mediaItemId = "";
        m_service.unregisterFromNotification(MediaPlayerServiceConstants.MEDIA_SERVICE_NOTIFICATION_PATH, this);
      
        for (int requestID : m_statusRequestIDs) {
            m_service.cancelRequest(requestID);
        }
        for (int requestID : m_mediaInfoRequestIDs) {
            m_service.cancelRequest(requestID);
        }
        for (int requestID : m_imageRequestIDs) {
            m_service.cancelRequest(requestID);
        }
      
        m_statusRequestIDs.clear();
        m_mediaInfoRequestIDs.clear();
        m_imageRequestIDs.clear();
    }

}
