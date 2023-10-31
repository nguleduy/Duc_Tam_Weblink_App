/****************************************************************************
 *
 * @file MediaPlayerServiceManager.java
 * @brief
 *
 * Contains the MediaPlayerServiceManager class.
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

import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.wlappservices.EServiceDiscoveryErrorCode;
import com.abaltatech.wlappservices.EServiceErrorCode;
import com.abaltatech.wlappservices.IServiceDiscoveryNotification;
import com.abaltatech.wlappservices.IServiceStatusNotification;
import com.abaltatech.wlappservices.ServiceManager;
import com.abaltatech.wlappservices.ServiceProxy;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Helper class that manages periodically tries to discover new media services, detect which is the
 * active media service and sends commands to the active service.
 *
 * A service is considered active if it is in the foreground or is playing a media item.
 *
 * This class is implemented as a singleton.
 */
public class MediaPlayerServiceManager implements
        IServiceDiscoveryNotification, IServiceStatusNotification,
        MediaPlayerServiceHandler.IServiceHandlerNotification {
          
    private static final String TAG = MediaPlayerServiceManager.class.getSimpleName();

    // Notification Handler. In this example, this is the sample UI
    private IMediaNotificationHandler m_notificationHandler;

    // Reference to the ServiceManager from the WebLink Client SDK.
    // The ServiceManager allows us to discover and control application services.
    private ServiceManager m_appServiceManager = ServiceManager.getInstance();

    // Map that contains a handler for each media service that is registered and discovered by
    // this manager.
    private Map<String, MediaPlayerServiceHandler> m_mediaServiceHandlersMap
            = new HashMap<String, MediaPlayerServiceHandler>();

    // Thread that tries to periodically discover new media services.
    private Thread m_discoveryThread;

    // Flag, indicating if this manager should use logic to determine which is the active service
    // (true) or if a specific service should be explicitly set from the outside to be the active
    // service (false)
    private boolean m_isAutoManagedEnabled = false;

    // Media service that has UI focus or empty string if no service has UI focus
    private String m_serviceWithFocus = "";
    // Last media service that had UI focus or empty string if no such had UI focus previously
    private String m_lastServiceWithFocus = "";
    // Current active media service or empty string if no service is active
    private String m_activeService = "";
    // Explicitly set active media service or empty string if no media service is configured
    private String m_currentService = "";

    // Map that contains the last timestamp when a media service was playing media
    private Map<String, Long> m_lastPlayingTimestampsMap = new HashMap<String, Long>();

    /**
     * Interface for consumers of notifications about the Active Media Services.
     */
    public interface IMediaNotificationHandler {

        /**
         * Called when a new Media Service has been discovered.
         * @param serviceName Name of the service that was discovered
         */
        void onMediaServiceAvailable(String serviceName);

        /**
         * Called when a Media Service is no longer valid, i.e. it was unregistered.
         *
         * Such a service will not send notifications and will not accept request.
         *
         * @param serviceName Name of the service that became invalid
         */
        void onMediaServiceNotAvailable(String serviceName);

        /**
         * Called when the current Active Media Service has changed.
         *
         * @param serviceName New active media service name
         */
        void onCurrentServiceChanged(String serviceName);

        /**
         * Called when the media item, played by the current active media service has changed.
         *
         * @param title Title of the new media item
         * @param artist Artist name of the new media item
         * @param album Album name of the new media item
         */
        void onMediaItemChanged(String title, String artist, String album);

        /**
         * Called when the artwork for a media item album has been received.
         *
         * @param albumArt The album artwork
         */
        void onAlbumArtReceived(Bitmap albumArt);

        /**
         * Called when the media status of the current active media service has changed.
         *
         * This method is called as a result of the media service sending a notification.
         * The notification includes the playback status of the service, the audio and video statuses
         * and information about the length an playback position of the current media item.
         *
         * @param mediaStatus - Media status: {@link MediaPlayerServiceConstants#STATUS_PLAYING} if
         *                    the service is playing a media item,
         *                    {@link MediaPlayerServiceConstants#STATUS_PAUSED} if the media service
         *                    is paused or an empty string if the status is unknown.
         * @param audioStatus - Audio status. Empty string if unknown.
         * @param videoStatus - Video status. Empty string if unknown.
         * @param playbackPosition - Current media item playback position (in seconds)
         * @param mediaItemLength - Current media item length (in seconds) or 0 if unknown or is a
         *                        stream
         */
        void onMediaStatusChanged(String mediaStatus, String audioStatus, String videoStatus,
                                  int playbackPosition, int mediaItemLength);

        /**
         * Called when a request sent to the active service has failed.
         *
         * @param requestID ID of the request that failed
         * @param code Error code
         * @param response Service response or null if no response
         */
        void onRequestFailed(int requestID, EServiceErrorCode code, JSONObject response);

        /**
         * Called when a response from the active media service was received.
         *
         * @param requestID ID of the request to which the service is responding
         * @param response Service response
         */
        void onResponseReceived(int requestID, JSONObject response);
    }

    // singleton instance
    private static MediaPlayerServiceManager s_Instance;

    // Hide the constructor - singleton pattern
    private MediaPlayerServiceManager() {
        // No-op
    }

    /**
     * Returns the unique instance of the Media Player Service Manager.
     *
     * @return MediaPlayerServiceManager singeton instance
     */
    public static MediaPlayerServiceManager getInstance() {
        if (s_Instance == null) {
            s_Instance = new MediaPlayerServiceManager();
        }

        return s_Instance;
    }

    /**
     * Sets a consumer to receive notifications with the state of the active service.
     *
     * @param handler Consumer of notifications
     */
    public void setMediaNotificationHandler(IMediaNotificationHandler handler) {
        m_notificationHandler = handler;
    }

    /**
     * Initialize the Media Service Manager.
     *
     * This method will start a thread that will discover Media Services by protocol once every
     * three seconds. The protocol is used to avoid hardcoding service names.
     */
    public void init() {
        if (m_discoveryThread == null || !m_discoveryThread.isAlive()) {
            // Start a thread that will try to discover new media player services once every 3 seconds
            m_discoveryThread = new Thread() {

                private static final long SLEEP_TIME_MS = 3000; // 3 seconds sleep

                @Override
                public void run() {
                    while (!isInterrupted()) {
                        m_appServiceManager.findServiceByProtocol(
                                MediaPlayerServiceConstants.MEDIA_SERVICE_PROTOCOL_NAME, MediaPlayerServiceManager.this, false);

                        try {
                            Thread.sleep(SLEEP_TIME_MS);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }

            };

            m_discoveryThread.start();

            // Register to receive notifications when a service is no longer available
            m_appServiceManager.registerForServiceStatusNotification(this);
        }
    }

    /**
     * Enables or disables auto management.
     *
     * When auto management is enabled, the Media Service Manager will automatically select the
     * active service and will forward notifications from the active service to the configured
     * consumer of notifications.
     *
     * If auto-management is disabled, to receive notifications, a service needs to be explicitly
     * marked as active using the {@link #setCurrentService(String)} method.
     *
     * @param enabled true to enable auto-management, false to disable it
     */
    public void enableAutoManagement(boolean enabled) {
        m_activeService = "";
        m_currentService = "";
        m_isAutoManagedEnabled = enabled;

        if (enabled) {
            for (MediaPlayerServiceHandler handler : m_mediaServiceHandlersMap.values()) {
                handler.init();
            }
        } else {
            for (MediaPlayerServiceHandler handler : m_mediaServiceHandlersMap.values()) {
                handler.terminate();
            }
        }
    }

    /**
     * Returns if auto-management is enabled.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isAutoManagementEnabled() {
        return m_isAutoManagedEnabled;
    }

    /**
     * Returns if there is an explicitly configured media service.
     *
     * If the service is not registered, if auto-management is enabled or if a media service
     * is not explicitly configured using the {@link #setCurrentService(String)} method.
     *
     * @return true if there is an explicitly configured service, false otherwise
     */
    public boolean isConnected() {
        String currentService = getCurrentService();
        return !m_isAutoManagedEnabled && !currentService.isEmpty();
    }

    /**
     * Excplicitly configures a media service as active.
     *
     * @param selectedService Name of the service.
     *
     * @return true if the active service has been configured, false otherwise
     */
    public boolean setCurrentService(String selectedService) {
        if (!m_currentService.isEmpty()) {
            MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(m_currentService);
            if (handler != null) {
                handler.terminate();
            }
            m_currentService = "";
        }

        if (m_mediaServiceHandlersMap.containsKey(selectedService)) {
            MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(selectedService);
            if (handler != null) {
                handler.init();
                m_currentService = selectedService;
                m_isAutoManagedEnabled = false;
                if (m_notificationHandler != null) {
                    m_notificationHandler.onCurrentServiceChanged(selectedService);
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Requests the ID of the current media item.
     *
     * The result is delivered through the
     * {@link IMediaNotificationHandler#onMediaItemChanged(String, String, String, String)}
     * notification.
     *
     * @return The request ID or {@link MediaPlayerServiceConstants#INVALID_REQUEST_ID} on error.
     */
    public int requestCurrentMediaItemInfo() {
        String activeService = getActiveService();
        if (activeService.isEmpty()) {
            return MediaPlayerServiceConstants.INVALID_REQUEST_ID;
        }
        MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(activeService);
        if (handler != null) {
            return handler.requestCurrentMediaItemInfo();
        }

        return MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    }

    /**
     * Send a `next` command to the active media service.
     *
     * @return ID of the request for tracking or {@link MediaPlayerServiceConstants#INVALID_REQUEST_ID}
     * on error.
     */
    public int next() {
        MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(getCurrentService());
        if (handler != null) {
            return handler.next();
        }
        return MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    }

    /**
     * Send a `previous` command to the active media service.
     *
     * @return ID of the request for tracking or {@link MediaPlayerServiceConstants#INVALID_REQUEST_ID}
     * on error.
     */
    public int prev() {
        MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(getCurrentService());
        if (handler != null) {
            return handler.prev();
        }
        return MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    }

    /**
     * Send a `play` command to the active media service.
     *
     * @return ID of the request for tracking or {@link MediaPlayerServiceConstants#INVALID_REQUEST_ID}
     * on error.
     */
    public int play() {
        MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(getCurrentService());
        if (handler != null) {
            return handler.play();
        }
        return MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    }

    /**
     * Send a `pause` command to the active media service.
     *
     * @return ID of the request for tracking or {@link MediaPlayerServiceConstants#INVALID_REQUEST_ID}
     * on error.
     */
    public int pause() {
        MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(getCurrentService());
        if (handler != null) {
            return handler.pause();
        }
        return MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    }

    /**
     * Send a `seek to position` command to the active media service.
     *
     * @param progress New position in seconds
     *
     * @return ID of the request for tracking or {@link MediaPlayerServiceConstants#INVALID_REQUEST_ID}
     * on error.
     */
    public int seek(int progress) {
        MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(getCurrentService());
        if (handler != null) {
            return handler.seek(progress);
        }
        return MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    }

    /**
     * Checks if the current service is paused.
     *
     * @return true if paused, false otherwise
     */
    public boolean isPaused() {
        MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(getCurrentService());
        if (handler != null) {
            return handler.isPaused();
        }
        return false;
    }

    /**
     * Checks if the current service is playing.
     *
     * @return true if playing, false otherwise
     */
    public boolean isPlaying() {
        MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(getCurrentService());
        if (handler != null) {
            return handler.isPlaying();
        }
        return false;
    }


    /**
     * Checks if the active service has changed and if it has notifies the consumer.
     *
     * @param serviceName Name of the candidate active service
     * @return true if the candidate name differs from the active service name, false otherwise
     */
    private boolean activeServiceChanged(String serviceName) {
        if (!m_activeService.equals(serviceName)) {
            if (m_notificationHandler != null) {
                m_notificationHandler.onCurrentServiceChanged(serviceName);
            }
            MediaPlayerServiceHandler handler = m_mediaServiceHandlersMap.get(serviceName);
            if (handler != null) {
                handler.requestCurrentStatus();
            }
            return true;
        }

        return false;
    }

    /**
     * Returns the name of the active service.
     *
     * This method is used when auto-management is enabled to determine the active service.
     *
     * The following logic is employed:
     *
     * * If there is a service that has UI focus, i.e. the application that registered the
     *   service is in the foreground, then that is the active service.
     * * If no service has UI focus currently, then the service that last played a media item is
     *   returned
     * * If no service is playing media, then the service that last had UI focus is returned
     * * If no service had UI focus before, the first discovered service is returned
     * * If no services are discovered, then an empty string is returned
     *
     * @return Name of the active service
     */
    private String getActiveService() {
        long maxTimestamp = 0;
        String lastSeenService = "";
        for (Map.Entry<String, Long> entry : m_lastPlayingTimestampsMap.entrySet()) {
            if (entry.getValue() > maxTimestamp) {
                maxTimestamp = entry.getValue();
                lastSeenService = entry.getKey();
            }
        }

        if (!lastSeenService.isEmpty()) {
            return lastSeenService;
        }

        if (!m_serviceWithFocus.isEmpty()) {
            return m_serviceWithFocus;
        }

        if (!m_lastServiceWithFocus.isEmpty()) {
            return m_lastServiceWithFocus;
        }

        if (!m_mediaServiceHandlersMap.isEmpty()) {
            return m_mediaServiceHandlersMap.keySet().iterator().next();
        }

        return "";
    }

    /**
     * Returns a list with all discovered media services.
     *
     * @return List of discovered media services
     */
    public Set<String> getAvailableServices() {
        return m_mediaServiceHandlersMap.keySet();
    }

    /**
     * Returns the name of the currently active service if auto-managemenet is enabled or an empty
     * string if no service is active.
     *
     * If auto-management is not enabled, returns the name of the explicitly configured active
     * service or an empty string if not configured.
     *
     * @return Name of the current active service
     */
    public String getCurrentService() {
        if (m_isAutoManagedEnabled) {
            return getActiveService();
        } else {
            return m_currentService;
        }
    }

    /**
     * Called when a service has been discovered.
     *
     * @param service a proxy object that represents the service
     * @param index sequential index of the service
     *
     * @return false to stop the discovery process, true to continue
     */
    @Override
    public boolean onServiceFound(ServiceProxy service, int index) {
        String serviceName = service.getServiceName();
        // Check if we have already discovered this service
        if (!m_mediaServiceHandlersMap.containsKey(serviceName)) {
            // Create the handler
            MediaPlayerServiceHandler handler = new MediaPlayerServiceHandler(service, this);
            m_mediaServiceHandlersMap.put(serviceName, handler);
            if (m_isAutoManagedEnabled) {
                handler.init();
            }
            // Notify that a new service is available
            if (m_notificationHandler != null) {
                m_notificationHandler.onMediaServiceAvailable(serviceName);
            }
        }

        // We want to discover all services
        return true;
    }

    /**
     *  Called when the service discovery process has completed
     * @param foundCount the number of services found
     */
    @Override
    public void onServiceDiscoveryCompleted(int foundCount) {
        // Do nothing
    }

    /**
     * Called when the service discovery process has failed
     * @param code the error code
     */
    @Override
    public void onServiceDiscoveryFailed(EServiceDiscoveryErrorCode code) {
        //  TODO: Add error handling
    }

    /**
     * Called when an application has published a service.
     *
     * @param serviceName name of the service
     * @param protocols list of protocols that it supports
     */
    @Override
    public void onServiceRegistered(String serviceName, List<String> protocols) {

    }

    /**
     * Called when an application has unpublished a service.
     *
     * The service will no longer deliver notifications and will no longer accept requests.
     *
     * @param serviceName name of the service that was unregistered
     */
    @Override
    public void onServiceUnregistered(String serviceName) {
        // Clean-up
        if (m_mediaServiceHandlersMap.containsKey(serviceName)) {
            MediaPlayerServiceHandler serviceHandler = m_mediaServiceHandlersMap.get(serviceName);
            if (serviceHandler != null) {
                serviceHandler.terminate();
            }
            m_mediaServiceHandlersMap.remove(serviceName);
            m_lastPlayingTimestampsMap.remove(serviceName);
            if (m_currentService.equals(serviceName)) {
                m_currentService = "";
            }
            if (m_serviceWithFocus.equals(serviceName)) {
                m_serviceWithFocus = "";
            }
            if (m_lastServiceWithFocus.equals(serviceName)) {
                m_lastServiceWithFocus = "";
            }
            if (m_activeService.equals(serviceName)) {
                m_activeService = getActiveService();
            }
            if (m_notificationHandler != null) {
                m_notificationHandler.onMediaServiceNotAvailable(serviceName);
            }
        }
    }

    /**
     * Called when there is a notification from a media service,
     *
     * @param serviceName Name of the service that sent the notification
     * @param hasFocus true if the service has UI focus
     * @param mediaStatus the media status of the service
     * @param audioStatus the audio status of the service
     * @param videoStatus the video status of the service
     * @param playbackPosition the playback position (in seconds) of the current media item
     * @param mediaItemLength the length (in seconds) of the current media item
     */
    @Override
    public void onMediaStatusChanged(String serviceName, boolean hasFocus,
                                     String mediaStatus, String audioStatus, String videoStatus,
                                     int playbackPosition, int mediaItemLength) {
        if (m_isAutoManagedEnabled) {
            if (mediaStatus.equals(MediaPlayerServiceConstants.STATUS_PLAYING)) {
                m_lastPlayingTimestampsMap.put(serviceName, System.currentTimeMillis());
            }

            if (hasFocus && !m_serviceWithFocus.equals(serviceName)) {
                if (!m_serviceWithFocus.isEmpty()) {
                    m_lastServiceWithFocus = m_serviceWithFocus;
                }
                m_serviceWithFocus = serviceName;
            }

            if (!hasFocus && serviceName.equals(m_serviceWithFocus)) {
                if (!m_serviceWithFocus.isEmpty()) {
                    m_lastServiceWithFocus = m_serviceWithFocus;
                }
                m_serviceWithFocus = "";
            }

            String activeService = getActiveService();
            if (activeServiceChanged(activeService)) {
                m_activeService = activeService;
            }

            if (m_activeService.equals(serviceName)) {
                if (m_notificationHandler != null) {
                    m_notificationHandler.onMediaStatusChanged(mediaStatus, audioStatus, videoStatus,
                            playbackPosition, mediaItemLength);
                }
            }
        } else if (m_currentService.equals(serviceName)) {
            if (m_notificationHandler != null) {
                m_notificationHandler.onMediaStatusChanged(mediaStatus, audioStatus, videoStatus,
                        playbackPosition, mediaItemLength);
            }
        }
    }

    /**
     * Called when the media item of a media service has changed.
     *
     * @param serviceName Name of the service playing the item
     * @param title Title of the media item
     * @param artist Artist name of the media item
     * @param albumName Album name of the media item
     */
    @Override
    public void onMediaItemChanged(String serviceName, String title, String artist, String albumName) {
        if (!m_isAutoManagedEnabled && !m_currentService.equals(serviceName)) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Ignore notification from non-active service " + serviceName);
            return;
        }
        if (m_isAutoManagedEnabled && !m_activeService.equals(serviceName)) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Ignore notification from non-active service " + serviceName);
            return;
        }
        if (m_notificationHandler != null) {
            m_notificationHandler.onMediaItemChanged(title.trim(), artist.trim(), albumName.trim());
        }
    }

    /**
     * Called when the artwork for the album of a media item was received.
     *
     * @param serviceName Name of the service playing the item
     * @param image Artwork bitmap
     */
    @Override
    public void onImageDownloaded(String serviceName, Bitmap image) {
        if (!m_isAutoManagedEnabled && !m_currentService.equals(serviceName)) {
           MCSLogger.log(MCSLogger.eWarning, TAG, "Ignore notification from non-active service " + serviceName);
            return;
        }
        if (m_isAutoManagedEnabled && !m_activeService.equals(serviceName)) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Ignore notification from non-active service " + serviceName);
            return;
        }

        if (m_notificationHandler != null) {
            m_notificationHandler.onAlbumArtReceived(image);
        }
    }

    @Override
    public void onRequestFailed(String serviceName, int requestID, EServiceErrorCode code, JSONObject response) {
        if (!m_isAutoManagedEnabled && !m_currentService.equals(serviceName)) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Ignore notification from non-active service " + serviceName);
            return;
        }
        if (m_isAutoManagedEnabled && !m_activeService.equals(serviceName)) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Ignore notification from non-active service " + serviceName);
            return;
        }

        if (m_notificationHandler != null) {
            m_notificationHandler.onRequestFailed(requestID, code, response);
        }
    }

    @Override
    public void onRequestResponseReceived(String serviceName, int requestID, JSONObject response) {
        if (!m_isAutoManagedEnabled && !m_currentService.equals(serviceName)) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Ignore notification from non-active service " + serviceName);
            return;
        }
        if (m_isAutoManagedEnabled && !m_activeService.equals(serviceName)) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Ignore notification from non-active service " + serviceName);
            return;
        }

        if (m_notificationHandler != null) {
            m_notificationHandler.onResponseReceived(requestID, response);
        }
    }

    /**
     * Terminates the Media Service Manager.
     */
    public void terminate() {
        if (m_discoveryThread != null && m_discoveryThread.isAlive()) {
            m_discoveryThread.interrupt();
        }
        m_discoveryThread = null;
        for (Map.Entry<String, MediaPlayerServiceHandler> entry : m_mediaServiceHandlersMap.entrySet()) {
            entry.getValue().terminate();
        }
        m_mediaServiceHandlersMap.clear();
    }
}
