/****************************************************************************
 *
 * @file MediaPlayerFragment.java
 * @brief
 *
 * Contains the MediaPlayerFragment class.
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

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.weblinkclientapp.services.mediaplayer.MediaPlayerServiceConstants;
import com.abaltatech.weblinkclientapp.services.mediaplayer.MediaPlayerServiceManager;
import com.abaltatech.wlappservices.EServiceErrorCode;
import com.testabalta.R;

import org.json.JSONObject;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * Fragment that contains the UI that implements a very basic media player that uses WebLink
 * Application Services to control the Media Applications, running on the WebLink Host.
 */
public class MediaPlayerFragment
        extends Fragment
        implements MediaPlayerServiceManager.IMediaNotificationHandler {

    private static final String TAG = "MediaPlayerFragment";

    // Limit the number of characters displayed for the song title, album name and artist name
    // so that the UI does not get too large.
    public static final int MAX_STRING_LENGTH = 36;

    private CheckBox m_cbAutoConnect;
    private Spinner m_spAvailableServices;
    private Button m_btConnect;
    private Button m_btPlay;
    private Button m_btPrev;
    private Button m_btNext;
    private SeekBar m_sbSeek;
    private TextView m_tvTitle;
    private TextView m_tvArtist;
    private TextView m_tvAlbum;
    private TextView m_tvMediaStatus;
    private TextView m_tvAudioStatus;
    private TextView m_tvVideoStatus;
    private TextView m_tvPlaybackPosition;
    private TextView m_tvMediaItemLength;
    private ImageView m_ivArtwork;

    // Holds the available services
    private ArrayAdapter<String> m_spAvailableServicesAdapter;

    // Handles all of the interactions with the services, published by the Media Applications.
    // This Fragment only contains the UI part of the example. All of the logic is located in the
    // manager.
    private final MediaPlayerServiceManager m_mediaPlayerServiceManager
            = MediaPlayerServiceManager.getInstance();

    // Prevent changes to the seek bar from notifications while the user is seeking
    private boolean m_isProgressBarDragged = false;

    // Used to dispatch code on the main thread
    private Handler m_handler = new Handler(Looper.getMainLooper());

    // Store information about the current media item so that it can be
    // restored when the Fragment is reattached to the Activity
    private int m_mediaItemLength = 0;
    private int m_playbackPosition = 0;
    private String m_activeService = "";
    private String m_title = "";
    private String m_artist = "";
    private String m_album = "";
    private String m_mediaStatus = "";
    private String m_audioStatus = "";
    private String m_videoStatus = "";
    private Bitmap m_artwork = null;

    // Is a media item playing at the moment
    private boolean m_isPlaying = false;

    private int m_seekRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    private int m_playPauseRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    private int m_nextRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
    private int m_prevRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize the manager
        m_mediaPlayerServiceManager.init();
        // Subscribe for notifications
        m_mediaPlayerServiceManager.setMediaNotificationHandler(this);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Restore state
        m_isPlaying = m_mediaStatus.equals(MediaPlayerServiceConstants.STATUS_PLAYING);
        m_spAvailableServicesAdapter.addAll(m_mediaPlayerServiceManager.getAvailableServices());
        m_spAvailableServicesAdapter.notifyDataSetChanged();

        m_activeService = m_mediaPlayerServiceManager.getCurrentService();
        m_mediaPlayerServiceManager.requestCurrentMediaItemInfo();

        updateUI();

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.media_player_fragment,
                container, false);

        m_cbAutoConnect = view.findViewById(R.id.cb_auto_connect);
        m_spAvailableServices = view.findViewById(R.id.sp_available_services);
        m_btConnect = view.findViewById(R.id.bt_connect);
        m_btPlay = view.findViewById(R.id.bt_play);
        m_btNext = view.findViewById(R.id.bt_next);
        m_btPrev = view.findViewById(R.id.bt_prev);
        m_sbSeek = view.findViewById(R.id.sb_seek);
        m_tvTitle = view.findViewById(R.id.tv_title);
        m_tvArtist = view.findViewById(R.id.tv_artist);
        m_tvAlbum = view.findViewById(R.id.tv_album);
        m_tvMediaStatus = view.findViewById(R.id.tv_media_status);
        m_tvAudioStatus = view.findViewById(R.id.tv_audio_status);
        m_tvVideoStatus = view.findViewById(R.id.tv_video_status);
        m_tvMediaItemLength = view.findViewById(R.id.tv_media_item_length);
        m_tvPlaybackPosition = view.findViewById(R.id.tv_playback_position);
        m_ivArtwork = view.findViewById(R.id.iv_artwork);

        m_spAvailableServicesAdapter = new ArrayAdapter<String>(
                inflater.getContext(), android.R.layout.simple_spinner_dropdown_item);
        m_spAvailableServices.setAdapter(m_spAvailableServicesAdapter);

        m_btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleConnectButtonClicked();
            }
        });

        m_cbAutoConnect.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                handleAutoConnectCheckedChanged(isChecked);
            }
        });

        m_btPlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePlayButtonPressed();
            }
        });

        m_btPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handlePrevButtonPressed();
            }
        });

        m_btNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                handleNextButtonPressed();
            }
        });

        m_sbSeek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                m_isProgressBarDragged = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                m_isProgressBarDragged = false;
                handleSeek(seekBar.getProgress());
            }
        });

        return view;
    }

    /**
     * Called when the Connect button is clicked.
     */
    private void handleConnectButtonClicked() {
        if (m_activeService.isEmpty()) {
            String selectedService = (String) m_spAvailableServices.getSelectedItem();
            if (selectedService != null) {
                if (m_mediaPlayerServiceManager.setCurrentService(selectedService)) {
                    reset();
                }
            }
        } else {
            m_mediaPlayerServiceManager.setCurrentService("");
            m_activeService = "";
            reset();
        }

        updateUI();
    }

    /**
     * Called when the Auto Connect checkbox was checked and unchecked.
     * @param isChecked true if checked, false otherwise
     */
    private void handleAutoConnectCheckedChanged(boolean isChecked) {
        m_activeService = "";
        reset();
        m_mediaPlayerServiceManager.enableAutoManagement(isChecked);
        updateUI();
    }

    /**
     * Called when the Play button is pressed.
     */
    private void handlePlayButtonPressed() {
        if (m_isPlaying) {
            m_playPauseRequestID = m_mediaPlayerServiceManager.pause();
        } else {
            m_playPauseRequestID = m_mediaPlayerServiceManager.play();
        }

        if (m_playPauseRequestID != MediaPlayerServiceConstants.INVALID_REQUEST_ID) {
            m_btPlay.setEnabled(false);
        }
    }

    /**
     * Called when the Next button is pressed.
     */
    private void handleNextButtonPressed() {
        m_nextRequestID = m_mediaPlayerServiceManager.next();
        if (m_nextRequestID != MediaPlayerServiceConstants.INVALID_REQUEST_ID) {
            m_btNext.setEnabled(false);
        }
    }

    /**
     * Called when the Prev button is pressed.
     */
    private void handlePrevButtonPressed() {
        m_prevRequestID = m_mediaPlayerServiceManager.prev();
        if (m_prevRequestID != MediaPlayerServiceConstants.INVALID_REQUEST_ID) {
            m_btPrev.setEnabled(false);
        }
    }

    /**
     * Called when the has dragged the seek/progress bar.
     *
     * @param progress The point to where the seek bar was dragged (in seconds)
     */
    private void handleSeek(int progress) {
        m_seekRequestID = m_mediaPlayerServiceManager.seek(progress);
        if (m_seekRequestID != MediaPlayerServiceConstants.INVALID_REQUEST_ID) {
            updateSeekBar(progress, m_mediaItemLength);
            m_sbSeek.setEnabled(false);
        }
    }

    /**
     * Helper method that updates the state of the UI.
     */
    private synchronized void updateUI() {
        m_btConnect.setText(!m_mediaPlayerServiceManager.isConnected()
                        ? R.string.connect : R.string.disconnect);
        m_btConnect.setEnabled(!m_mediaPlayerServiceManager.isAutoManagementEnabled());
        m_cbAutoConnect.setChecked(m_mediaPlayerServiceManager.isAutoManagementEnabled());
        m_cbAutoConnect.setEnabled(!m_mediaPlayerServiceManager.isConnected());
        m_spAvailableServices.setEnabled(!m_mediaPlayerServiceManager.isAutoManagementEnabled() && m_activeService.isEmpty());
        m_btPlay.setText(getString(m_isPlaying
                        ? R.string.pause : R.string.play));
        m_tvTitle.setText(m_title.length() > MAX_STRING_LENGTH ? m_title.substring(0, MAX_STRING_LENGTH) + " ..." : m_title);
        m_tvArtist.setText(m_artist.length() > MAX_STRING_LENGTH ? m_title.substring(0, MAX_STRING_LENGTH) + " ..." : m_artist);
        m_tvAlbum.setText(m_album.length() > MAX_STRING_LENGTH ? m_album.substring(0, MAX_STRING_LENGTH) + " ..." : m_album);
        m_tvMediaStatus.setText(m_mediaStatus);
        m_tvAudioStatus.setText(m_audioStatus);
        m_tvVideoStatus.setText(m_videoStatus);
        m_ivArtwork.setImageBitmap(m_artwork);
        updateSeekBar(m_playbackPosition, m_mediaItemLength);
        enableControlButtons(!m_mediaPlayerServiceManager.getCurrentService().isEmpty());
    }

    /**
     * Helper method that enables or disables the media control buttons: Prev, Next, Play and the
     * Seek Bar.
     *
     * @param enable true to enable, false to disable
     */
    private void enableControlButtons(boolean enable) {
        m_btPlay.setEnabled(enable && m_playPauseRequestID == MediaPlayerServiceConstants.INVALID_REQUEST_ID);
        m_btPrev.setEnabled(enable && m_prevRequestID == MediaPlayerServiceConstants.INVALID_REQUEST_ID);
        m_btNext.setEnabled(enable && m_nextRequestID == MediaPlayerServiceConstants.INVALID_REQUEST_ID);
        m_sbSeek.setEnabled(enable && m_mediaItemLength > 0
                && m_seekRequestID == MediaPlayerServiceConstants.INVALID_REQUEST_ID);
    }

    /**
     * Clears all information about the current media item.
     */
    private void reset() {
        m_seekRequestID = -1;
        m_playPauseRequestID = -1;
        m_nextRequestID = -1;
        m_prevRequestID = -1;
        m_mediaItemLength = 0;
        m_playbackPosition = 0;
        m_title = "";
        m_artist = "";
        m_album = "";
        m_mediaStatus = "";
        m_audioStatus = "";
        m_videoStatus = "";
        m_artwork = null;
        m_isPlaying = false;
        m_isProgressBarDragged = false;
    }

    /**
     * Helper method that update the seek bar and the elapsed time and the total time labels with the
     * current progress.
     *
     * @param playbackPosition Current playback position
     * @param length Current media item length
     */
    void updateSeekBar(int playbackPosition, int length) {
        if (!m_isProgressBarDragged && m_seekRequestID == MediaPlayerServiceConstants.INVALID_REQUEST_ID) {
            m_sbSeek.setProgress(playbackPosition);
            m_sbSeek.setMax(length);
            m_sbSeek.setEnabled(false);
            m_tvPlaybackPosition.setText(asTimeString(playbackPosition));
            m_tvMediaItemLength.setText(asTimeString(length));
        }

        if (m_seekRequestID != MediaPlayerServiceConstants.INVALID_REQUEST_ID) {
            if (Math.abs(m_sbSeek.getProgress() - playbackPosition) < 3) {
                m_seekRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
                m_sbSeek.setEnabled(length > 0);
            }
        }
    }

    /**
     * Converts seconds into a String with the format MM:ss.
     *
     * @param seconds Number of seconds
     * @return Formatted string
     */
    String asTimeString(long seconds) {
        if (seconds > 0) {
            long minutes = TimeUnit.MINUTES.convert(seconds, TimeUnit.SECONDS);
            seconds = seconds - TimeUnit.SECONDS.convert(minutes, TimeUnit.MINUTES);
            return String.format(Locale.US, "%02d:%02d", minutes, seconds);
        } else {
            return "";
        }
    }

    @Override
    public synchronized void onStop() {
        super.onStop();
        MCSLogger.log(MCSLogger.eDebug, TAG, "onDetach");
        m_mediaPlayerServiceManager.setMediaNotificationHandler(null);
    }

    @Override
    public synchronized void onDestroy() {
        super.onDestroy();
        m_mediaPlayerServiceManager.terminate();
    }

    @Override
    public void onMediaServiceAvailable(final String serviceName) {
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (MediaPlayerFragment.this) {
                    for (int i = 0; i < m_spAvailableServicesAdapter.getCount(); ++i) {
                        String item = m_spAvailableServicesAdapter.getItem(i);
                        if (item != null && item.equals(serviceName)) {
                            return;
                        }
                    }

                    m_spAvailableServicesAdapter.add(serviceName);
                    m_spAvailableServicesAdapter.notifyDataSetChanged();
                }
            }
        });
    }

    @Override
    public void onMediaServiceNotAvailable(final String serviceName) {
        m_handler.post(new Runnable() {
           @Override
           public void run() {
               synchronized (MediaPlayerFragment.this) {
                   m_spAvailableServicesAdapter.remove(serviceName);
                   if (m_activeService.equals(serviceName)) {
                       reset();
                       updateUI();
                   }
               }
           }
       });
    }

    @Override
    public void onCurrentServiceChanged(final String serviceName) {
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                m_activeService = serviceName;
                if (m_mediaPlayerServiceManager.isAutoManagementEnabled()) {
                    synchronized (MediaPlayerFragment.this) {
                        for (int i = 0; i < m_spAvailableServices.getCount(); ++i) {
                            if (m_spAvailableServices.getItemAtPosition(i).equals(serviceName)) {
                                m_spAvailableServices.setSelection(i);
                                break;
                            }
                        }
                    }
                }
                updateUI();
                m_mediaPlayerServiceManager.requestCurrentMediaItemInfo();
            }
        });
    }

    @Override
    public void onMediaItemChanged(final String title, final String artist, final String album) {
        m_title = title;
        m_artist = artist;
        m_album = album;
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }

    @Override
    public void onAlbumArtReceived(final Bitmap albumArt) {
        m_artwork = albumArt;
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                m_ivArtwork.setImageBitmap(albumArt);
            }
        });
    }

    @Override
    public void onMediaStatusChanged(String mediaStatus, String audioStatus, String videoStatus, int playbackPosition, int mediaItemLength) {
        m_mediaStatus = mediaStatus;
        m_audioStatus = audioStatus;
        m_videoStatus = videoStatus;
        m_playbackPosition = playbackPosition;
        m_mediaItemLength = mediaItemLength;
        m_isPlaying = m_mediaStatus.equals(MediaPlayerServiceConstants.STATUS_PLAYING);
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }

    @Override
    public void onRequestFailed(int requestID, EServiceErrorCode code, JSONObject response) {
        if (m_nextRequestID == requestID) {
            m_nextRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
        }
        if (m_prevRequestID == requestID) {
            m_prevRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
        }
        if (m_seekRequestID == requestID) {
            m_seekRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
        }
        if (m_playPauseRequestID == requestID) {
            m_playPauseRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
        }
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }

    @Override
    public void onResponseReceived(int requestID, JSONObject response) {
        if (m_nextRequestID == requestID) {
            m_nextRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
        }
        if (m_prevRequestID == requestID) {
            m_prevRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
        }
        if (m_seekRequestID == requestID) {
            m_seekRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
        }
        if (m_playPauseRequestID == requestID) {
            m_playPauseRequestID = MediaPlayerServiceConstants.INVALID_REQUEST_ID;
        }

        m_handler.post(new Runnable() {
            @Override
            public void run() {
                updateUI();
            }
        });
    }
}
