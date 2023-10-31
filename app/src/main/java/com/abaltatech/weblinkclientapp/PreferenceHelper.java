/****************************************************************************
 *
 * @file PreferenceHelper.java
 * @brief
 *
 * Contains the PreferenceHelper class.
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
package com.abaltatech.weblinkclientapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.preference.PreferenceManager;

import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.weblink.core.WLTypes;
import com.testabalta.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class that reads the users preferences.
 */
public class PreferenceHelper {

    private static final String TAG = PreferenceHelper.class.getSimpleName();

    private static final String RESOLUTION_STRING_SEPARATOR = "x";

    public static final String KEY_VERSION_INFO = getPreferenceString(R.string.key_version_info);
    public static final String KEY_VIDEO_RESOLUTION = getPreferenceString(R.string.key_video_resolution);
    public static final String KEY_DECODER_SURFACE = getPreferenceString(R.string.key_decoder_surface);
    public static final String KEY_FRAME_RATE = getPreferenceString(R.string.key_frame_rate);
    public static final String KEY_KEYFRAME_INTERVAL = getPreferenceString(R.string.key_keyframe_interval);
    public static final String KEY_BITRATE = getPreferenceString(R.string.key_bit_rate);
    public static final String KEY_APP_SWITCH_TIMEOUT = getPreferenceString(R.string.key_app_switch_timeout);
    public static final String KEY_SHOW_STATISTICS = getPreferenceString(R.string.key_show_statistics);
    public static final String KEY_VIDEO_DECODER = getPreferenceString(R.string.key_video_decoder);
    public static final String KEY_FRAME_SKIPPING = getPreferenceString(R.string.key_frame_skipping);
    public static final String KEY_AUTO_CONNECT_UI = getPreferenceString(R.string.key_auto_connect_ui);
    public static final String KEY_SHOW_CMD_BAR = getPreferenceString(R.string.key_show_cmdbar);
    public static final String KEY_AUTO_START_PROXY = getPreferenceString(R.string.key_auto_start_proxy);
    public static final String KEY_ENABLE_FPS_MANAGER = getPreferenceString(R.string.key_enable_fps_manager);
    public static final String KEY_CONNECTION_MODE = getPreferenceString(R.string.key_connect_mode);

    public static final String VALUE_I420_DECODER = getPreferenceString(R.string.value_i420_decoder);
    public static final String VALUE_YUV_DECODER = getPreferenceString(R.string.value_yuv_decoder);
    public static final String VALUE_H264_CUSTOM_DECODER = getPreferenceString(R.string.value_h264_custom_decoder);
    public static final String VALUE_H264_DECODER = getPreferenceString(R.string.value_h264_decoder);

    public static final String VALUE_SURFACE_VIEW = getPreferenceString(R.string.value_surface_view);
    public static final String VALUE_TEXTURE_VIEW = getPreferenceString(R.string.value_texture_view);

    public static final String VALUE_USB = getPreferenceString(R.string.value_usb);
    public static final String VALUE_SOCKET = getPreferenceString(R.string.value_socket);

    private static final boolean DEFAULT_ENABLE_FRAMESKIP = false; // Disabled by default
    private static final boolean DEFAULT_SHOW_STATISTICS = false; // Disabled by default
    private static final boolean DEFAULT_ENABLE_AUTO_CONNECT_UI = true; // Enabled by default
    private static final boolean DEFAULT_SHOW_CMD_BAR = false;
    private static final boolean DEFAULT_AUTO_START_PROXY = false;
    private static final boolean DEFAULT_ENABLE_FPS_MANAGER = false;

    private static final int DEFAULT_DECODER_WIDTH      = 800;
    private static final int DEFAULT_DECODER_HEIGHT     = 480;
    private static final int DEFAULT_FRAME_RATE         = 30; // 30 frames per second
    private static final int DEFAULT_KEYFRAME_INTERVAL  = 30; // 1 key-frame every 30 frames
    private static final int DEFAULT_BITRATE            = 2000000; // 2 MBPS
    private static final int DEFAULT_APP_SWITCH_TIMEOUT = 5; // 5 seconds

    private static final String DEFAULT_VIDEO_DECODER   = VALUE_H264_CUSTOM_DECODER;
    private static final String DEFAULT_CONNECTION_MODE = VALUE_USB;
    private static final String DEFAULT_DECODER_SURFACE = VALUE_TEXTURE_VIEW;

    // The Shared Preferences
    private SharedPreferences m_sharedPreferences;

    // Keeps track of preferences that change when the Prefs fragment is shown
    private List<String> m_changedPreferences = new ArrayList<String>();

    /**
     * Default constructor.
     *
     * @param context Context to use when reading the shared preferences
     */
    public PreferenceHelper(Context context) {
        m_sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    /**
     * Returns the configured decoder resolution.
     * @return Decoder resolution
     */
    public Point getDecoderResolution() {
        Point resolution = new Point(DEFAULT_DECODER_WIDTH, DEFAULT_DECODER_HEIGHT);
        String resolutionPreference = m_sharedPreferences.getString(KEY_VIDEO_RESOLUTION, "");
        if (!resolutionPreference.isEmpty()) {
            if (resolutionPreference.contains(RESOLUTION_STRING_SEPARATOR)) {
                try {
                    String[] tokens = resolutionPreference.split(RESOLUTION_STRING_SEPARATOR);
                    resolution.x = Integer.parseInt(tokens[0]);
                    resolution.y = Integer.parseInt(tokens[1]);
                } catch (NumberFormatException e) {
                    MCSLogger.log(MCSLogger.eWarning, TAG, "Invalid resolution configured!");
                }
            }
        }
        return resolution;
    }

    public int getFrameRate() {
        String frameRate = m_sharedPreferences.getString(KEY_FRAME_RATE, "");
        if (!frameRate.isEmpty()) {
            try {
                return Integer.parseInt(frameRate);
            } catch (NumberFormatException e) {
                MCSLogger.log(MCSLogger.eWarning, "Invalid frame-rate configured!");
            }
        }
        return DEFAULT_FRAME_RATE;
    }

    /**
     * Returns the configured key-frame interval.
     * @return Key-frame interval
     */
    public int getKeyFrameInterval() {
        String keyFrameInterval = m_sharedPreferences.getString(KEY_KEYFRAME_INTERVAL, "");
        if (!keyFrameInterval.isEmpty()) {
            try {
                return Integer.parseInt(keyFrameInterval);
            } catch (NumberFormatException e) {
                MCSLogger.log(MCSLogger.eWarning, "Invalid key-frame interval configured!");
            }
        }
        return DEFAULT_KEYFRAME_INTERVAL;
    }

    /**
     * Returns the configured bitrate.
     * @return Bitrate in bits per second
     */
    public int getBitrate() {
        String bitrate = m_sharedPreferences.getString(KEY_BITRATE, "");
        if (!bitrate.isEmpty()) {
            try {
                return Integer.parseInt(bitrate);
            } catch (NumberFormatException e) {
                MCSLogger.log(MCSLogger.eWarning, "Invalid bitrate configured!");
            }
        }
        return DEFAULT_BITRATE;
    }

    /**
     * Returns the configured application switch timeout.
     * @return Application switch timeout in seconds
     */
    public int getAppSwitchTimeout() {
        String appSwitchTimeout = m_sharedPreferences.getString(KEY_APP_SWITCH_TIMEOUT, "");
        if (!appSwitchTimeout.isEmpty()) {
            try {
                return Integer.parseInt(appSwitchTimeout);
            } catch (NumberFormatException e) {
                MCSLogger.log(MCSLogger.eWarning, "Invalid app-switch timeout configured!");
            }
        }
        return DEFAULT_APP_SWITCH_TIMEOUT;
    }

    /**
     * Returns the name of the configured video decoder.
     * @return Video decoder
     */
    public String getVideoDecoder() {
        String videoDecoder = m_sharedPreferences.getString(KEY_VIDEO_DECODER, "");
        if (!videoDecoder.isEmpty()) {
            return videoDecoder;
        }
        return DEFAULT_VIDEO_DECODER;
    }

    /**
     * Returns the type of the configured video decoder.
     * @return Video decoder type
     */
    public int getDecoderType() {
        String videoDecoder = m_sharedPreferences.getString(KEY_VIDEO_DECODER, "");
        if (!videoDecoder.isEmpty()) {
            if (videoDecoder.equals(PreferenceHelper.VALUE_I420_DECODER)) {
                return WLTypes.FRAME_ENCODING_I420;
            }
            else if (videoDecoder.equals(PreferenceHelper.VALUE_YUV_DECODER)) {
                return WLTypes.FRAME_ENCODING_YUV;
            }
            else if (videoDecoder.equals(PreferenceHelper.VALUE_H264_CUSTOM_DECODER)){
                return WLTypes.FRAME_ENCODING_H264;
            }
        }

        return WLTypes.FRAME_ENCODING_H264;
    }

    /**
     * Returns the view that should be used to render the decoded projection.
     * @return View that shold be used to render the projection
     */
    public String getDecoderSurface() {
        String decoderSurface = m_sharedPreferences.getString(KEY_DECODER_SURFACE, "");
        if (!decoderSurface.isEmpty()) {
            return decoderSurface;
        }
        return DEFAULT_DECODER_SURFACE;
    }

    /**
     * Returns the configured type of connection to be used in Auto-Connect Mode.
     *
     * @return Connection type for the Auto-Connect mode
     */
    public Object getConnectionMode() {
        String connectionMode = m_sharedPreferences.getString(KEY_CONNECTION_MODE, "");
        if (!connectionMode.isEmpty()) {
            return connectionMode;
        }
        return DEFAULT_CONNECTION_MODE;
    }

    /**
     * Checks if statistics are enabled.
     * @return true if enabled, false otherwise
     */
    public boolean areStatisticsEnabled() {
        return m_sharedPreferences.getBoolean(KEY_SHOW_STATISTICS, DEFAULT_SHOW_STATISTICS);
    }

    /**
     * Checks if the command bar should be visible.
     * @return true to be visible, false otherwise
     */
    public boolean shouldShowCmdBar() {
        return m_sharedPreferences.getBoolean(KEY_SHOW_CMD_BAR, DEFAULT_SHOW_CMD_BAR);
    }

    /**
     * Checks if the Service Proxy should be started automatically when the a connection
     * with the WebLink Host is established.
     * @return true to auto-start the proxy, false otherwise
     */
    public boolean shouldAutoStartProxy() {
        return m_sharedPreferences.getBoolean(KEY_AUTO_START_PROXY, DEFAULT_AUTO_START_PROXY);
    }

    /**
     * Checks if frame skipping is enabled
     * @return true if enabled, false otherwise
     */
    public boolean isFrameSkippingEnabled() {
        return m_sharedPreferences.getBoolean(KEY_FRAME_SKIPPING, DEFAULT_ENABLE_FRAMESKIP);
    }

    /**
     * Checks if the auto-connect UI or the manual-connection UI should be displayed when not
     * connected to a WebLink Host
     * @return true to show the auto-connect UI, false otherwise
     */
    public boolean isAutoConnectEnabled() {
        return m_sharedPreferences.getBoolean(KEY_AUTO_CONNECT_UI, DEFAULT_ENABLE_AUTO_CONNECT_UI);
    }

    /**
     * Checks if automatic FPS adjustment through the FPS Manager is enabled.
     * @return true if enable, false otherwise
     */
    public boolean isFPSManagementEnabled() {
        return m_sharedPreferences.getBoolean(KEY_ENABLE_FPS_MANAGER, DEFAULT_ENABLE_FPS_MANAGER);
    }

    /**
     * Helper method to check if a preference was changed by the user.
     * @param preference Preference to check
     * @return true if changed, false otherwise
     */
    public boolean hasPreferenceChanged(String preference) {
        synchronized (this) {
            if (m_changedPreferences.contains(preference)) {
                m_changedPreferences.remove(preference);
                return true;
            }
        }

        return false;
    }

    /**
     * Reset the list of preferences that were changed by the user.
     */
    public void resetChanges() {
        synchronized (this) {
            m_changedPreferences.clear();
        }
    }

    /**
     * Called when a preference was changed by the user.
     * @param key Preference that changed
     */
    public void onPreferenceChanged(String key) {
        synchronized (this) {
            if (!m_changedPreferences.contains(key)) {
                m_changedPreferences.add(key);
            }
        }
    }

    /**
     * Helper method that extracts a string from the strings.xml file.
     *
     * Used to initialize the static preference key constants.
     * @param resId String resource identifier
     * @return String value
     */
    private static String getPreferenceString(int resId) {
        Context context = App.getAppContext();
        if (context != null) {
            return context.getString(resId);
        }
        return "";
    }
}
