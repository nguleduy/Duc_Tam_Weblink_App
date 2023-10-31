/****************************************************************************
 *
 * @file MediaPlayerServiceConstants.java
 * @brief
 *
 * Contains the MediaPlayerServiceConstants class.
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

/**
 * Contains an assortment of constants used with the Media Services.
 */
public class MediaPlayerServiceConstants {

    /**
     * Default protocol for Media Services.
     */
    public static final String MEDIA_SERVICE_PROTOCOL_NAME = "io.wlservice.mediaplayer";

    /**
     * Default notification resource path for Media Services.
     */
    public static final String MEDIA_SERVICE_NOTIFICATION_PATH = "/media/player";

    /**
     * Resource path for requesting information about the current media item.
     * */
    public static final String MEDIA_SERVICE_ITEM_PATH = "/media/item/";

    /**
     * Retrieve the ID of the current media item.
     * */
    public static final String MEDIA_SERVICE_CURRENT_ITEM_PATH = "/media/player/item";

    /**
     * Resource path for requesting the album artwork of the current media item.
     */
    public static final String MEDIA_SERVICE_IMAGE_PATH = "/media/image/";
    /**
     * Resource path fir sending commands to the Media Services.
     **/
    public static final String MEDIA_SERVICE_CONTROL_COMMAND_PATH = "/media/player?command=";

    /**
     * Playing media status.
     */
    public static final String STATUS_PLAYING = "PLAYING";

    /**
     * Paused media status.
     */
    public static final String STATUS_PAUSED = "PAUSED";

    /**
     * UI focus key in the Media Status Notification JSON.
     */
    public static final String KEY_HAS_FOCUS = "hasUIFocus";

    /**
     * Media status key in the Media Status Notification JSON.
     */
    public static final String KEY_MEDIA_STATUS = "mediaStatus";

    /**
     * Audio status key in the Media Status Notification JSON.
     */
    public static final String KEY_AUDIO_STATUS = "audioStatus";

    /**
     * Video status key in the Media Status Notification JSON.
     */
    public static final String KEY_VIDEO_STATUS = "videoStatus";

    /**
     * Playback position key in the Media Status Notification JSON.
     */
    public static final String KEY_PLAYBACK_POSITION = "playbackPosition";

    /**
     * Media item ID key in the Media Status Notification JSON.
     */
    public static final String KEY_MEDIA_ITEM_ID = "mediaItemId";

    /**
     * Media item ID key in the Media Item Information JSON.
     */
    public static final String KEY_ID = "id";

    /**
     * Media item length key in the Media Status Notification JSON.
     */
    public static final String KEY_MEDIA_ITEM_LENGTH = "mediaItemLength";

    /**
     * Media item name key Media Item Information JSON.
     */
    public static final String KEY_MEDIA_ITEM_NAME = "name";

    /**
     * Album artwork imageID key in the Media Item Information JSON.
     */
    public static final String KEY_IMAGE_ID = "imageId";

    /**
     * Artist information key in the Media Item Information JSON.
     */
    public static final String KEY_ARTIST = "artist";

    /**
     * Artist name key in the Artist information JSON.
     */
    public static final String KEY_ARTIST_NAME = "name";

    /**
     * Album information key in the Media Item Information JSON.
     */
    public static final String KEY_ALBUM = "album";

    /**
     * Album name key in the Album information JSON.
     */
    public static final String KEY_ALBUM_NAME = "name";

    /**
     * Previous media item command.
     */
    public static final String COMMAND_PREV = "PREV";

    /**
     * Next media item command.
     */
    public static final String COMMAND_NEXT = "NEXT";

    /**
     * Play current media item command.
     */
    public static final String COMMAND_PLAY = "PLAY";

    /**
     * Pause current media item command.
     */
    public static final String COMMAND_PAUSE = "PAUSE";

    /**
     * Seek current media item command.
     */
    public static final String COMMAND_SEEK = "SEEK";

    /**
     * Template for commands that have arguments.
     */
    public static final String ARGUMENT_TEMPLATE = "&value=%s";

    /**
     * Default key for the artist and album names JSON.
     */
    public static final String DEFAULT_ID = "default";

    /**
     * ID constant for an invalid request.
     */
    public static final int INVALID_REQUEST_ID = -1;
}
