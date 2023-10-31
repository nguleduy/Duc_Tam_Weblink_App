/****************************************************************************
 *
 * @file WebLinkFragment.java
 * @brief
 *
 * Contains the WebLinkFragment class.
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

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.fragment.app.Fragment;

import com.abaltatech.mcs.connectionmanager.EConnectionResult;
import com.abaltatech.mcs.connectionmanager.PeerDevice;
import com.abaltatech.mcs.logger.MCSLogger;
import com.abaltatech.weblink.core.WLTypes;
import com.abaltatech.weblink.core.commandhandling.BrowserCommand;
import com.abaltatech.weblink.core.commandhandling.Command;
import com.abaltatech.weblink.core.commandhandling.HideKeyboardCommand;
import com.abaltatech.weblink.core.commandhandling.KeyboardCommand;
import com.abaltatech.weblink.core.commandhandling.ShowKeyboardCommand;
import com.abaltatech.weblinkclient.IClientNotification;
import com.abaltatech.weblinkclient.WebLinkClientCore;
import com.abaltatech.weblinkclient.commandhandling.TouchCommand;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoder_H264;
import com.abaltatech.weblinkclient.framedecoding.FrameDecoder_H264_Custom;
import com.abaltatech.weblinkclient.framedecoding.IFrameDecoder;
import com.abaltatech.wlappservices.WLServicesHTTPProxy;
import com.testabalta.R;

import java.util.HashSet;
import java.util.Set;

/**
 * WebLinkFragment is the main screen that displays the video stream received from the WebLink server
 * on the connected phone. This fragment intercepts touches to sends them the WebLink server.
 * <p>
 * This is a base abstract class that has the main functionality. There are different derived
 * classes {@link WebLinkFragment_TextureView} and {@link WebLinkFragment_SurfaceView} that use
 * different types of video view. They override the abstract methods and implement the different
 * behaviour based on the view type.
 */
public abstract class WebLinkFragment
        extends
        Fragment
        implements
        IClientNotification,
        View.OnKeyListener,
        View.OnTouchListener,
        IPingHandler {

    private static final String TAG = WebLinkFragment.class.getSimpleName();

    private static final boolean _D = false;

    Handler m_handler = new Handler();

    // Store a reference to the WebLinkClientCore for easy access
    protected WebLinkClientCore m_wlClient = App.instance().getWLClientCore();

    // Stores the active Audio channels
    Set<Integer> m_activeAudioChannels = new HashSet<Integer>();

    // Wait indicator image to display when switching between applications
    private ProgressBar m_waitIndicator;

    // TextView that displays debug/diagnostic statistics
    private TextView m_stats;

    // Command Bar that contains the WebLink Navigation Buttons: Forward, Back, Home
    private View m_cmdBar;

    // Command Bar buttons container
    private View m_cmdBarContents;

    // Sample background image to be displayed while loading
    private LinearLayout m_loadingLayout;

    // Touch scaling
    protected float m_scaleX;
    protected float m_scaleY;

    private boolean m_cmdBarVisible;
    private boolean m_showCmdBar;
    private boolean m_showStats;
    private boolean m_startProxy;

    /**
     * See {@link Fragment#onCreateView(LayoutInflater, ViewGroup, Bundle)}
     *
     * @param inflater
     * @param container
     * @param savedInstanceState
     * @return
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(getLayoutResourceId(), container, false);

        //receive all touch events on the base view.
        view.setOnTouchListener(this);

        // setup the UI
        m_waitIndicator = (ProgressBar) view.findViewById(R.id.wait_indicator);
        m_stats = (TextView) view.findViewById(R.id.text_stats);
        m_cmdBar = view.findViewById(R.id.cmd_bar);
        m_cmdBarContents = view.findViewById(R.id.cmd_bar_buttons);
        m_loadingLayout = (LinearLayout) view.findViewById(R.id.loading_screen);
        m_cmdBarVisible = true;
        updateUI();

        prepareVideoView(view);

        setLoadingScreenState(true);
        showCmdBar(m_showCmdBar);
        showStats(m_showStats);

        //the bar is default shown, do a animated hide after 500ms.
        m_cmdBar.postDelayed(new Runnable() {
            public void run() {
                showCmdBar(false);
            }
        }, 500);

        ((ImageButton) view.findViewById(R.id.hide_button))
                .setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View v) {
                        showCmdBar(!m_cmdBarVisible);
                    }
                });

        ((ImageButton) view.findViewById(R.id.minimize_button))
                .setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View v) {
                        showCmdBar(false);
                    }
                });

        ((ImageButton) view.findViewById(R.id.close_button))
                .setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View v) {
                        m_wlClient.disconnect();
                    }
                });

        ((ImageButton) view.findViewById(R.id.back_button))
                .setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View v) {
                        m_wlClient.sendCommand(new BrowserCommand(BrowserCommand.ACT_BACK, null));
                    }
                });

        ((ImageButton) view.findViewById(R.id.home_button))
                .setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View v) {
                        m_wlClient.sendCommand(new BrowserCommand(BrowserCommand.ACT_HOME, null));
                    }
                });

        ((ImageButton) view.findViewById(R.id.forward_button))
                .setOnClickListener(new Button.OnClickListener() {
                    public void onClick(View v) {
                        m_wlClient.sendCommand(new BrowserCommand(BrowserCommand.ACT_FORWARD, null));
                    }
                });

        // When this button is toggled off, this means that the handbrake is оn.
        // When the handbrake is none there are no Driver Distraction restrictions, hence we send
        // UIRESTRICTION_LEVEL_NONE. When the button is toggled on, this means that the handbrake is
        // off and there is a Major Driver Distraction Restriction in effect, hence we send
        // UIRESTRICTION_LEVEL_MAJOR to the Host Application.
        ((ImageView) view.findViewById(R.id.handbrake_button))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        v.setActivated(!v.isActivated());

                        String level = (v.isActivated() ?
                                BrowserCommand.UIRESTRICTION_LEVEL_MAJOR :
                                BrowserCommand.UIRESTRICTION_LEVEL_NONE);
                        m_wlClient.sendCommand(new BrowserCommand(BrowserCommand.ACT_UI_RESTRICTION_LEVEL, level));
                    }
                });

        ((ImageView) view.findViewById(R.id.prev))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (m_wlClient != null && m_wlClient.isConnected()) {
                            m_wlClient.sendCommand(new KeyboardCommand((short) WLTypes.VK_MEDIA_PREV_TRACK, KeyboardCommand.ACT_KEY_DOWN));
                            m_wlClient.sendCommand(new KeyboardCommand((short) WLTypes.VK_MEDIA_PREV_TRACK, KeyboardCommand.ACT_KEY_UP));
                        } else {
                            MCSLogger.log(MCSLogger.eWarning, "Client is not connected!");
                        }
                    }
                });

        ((ImageView) view.findViewById(R.id.play))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (m_wlClient != null && m_wlClient.isConnected()) {
                            m_wlClient.sendCommand(new KeyboardCommand((short) WLTypes.VK_MEDIA_PLAY_PAUSE, KeyboardCommand.ACT_KEY_DOWN));
                            m_wlClient.sendCommand(new KeyboardCommand((short) WLTypes.VK_MEDIA_PLAY_PAUSE, KeyboardCommand.ACT_KEY_UP));
                        } else {
                            MCSLogger.log(MCSLogger.eWarning, "Client is not connected!");
                        }
                    }
                });

        ((ImageView) view.findViewById(R.id.next))
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (m_wlClient != null && m_wlClient.isConnected()) {
                            m_wlClient.sendCommand(new KeyboardCommand((short) WLTypes.VK_MEDIA_NEXT_TRACK, KeyboardCommand.ACT_KEY_DOWN));
                            m_wlClient.sendCommand(new KeyboardCommand((short) WLTypes.VK_MEDIA_NEXT_TRACK, KeyboardCommand.ACT_KEY_UP));
                        } else {
                            MCSLogger.log(MCSLogger.eWarning, "Client is not connected!");
                        }
                    }
                });

        return view;
    }

    private void updateUI() {
        Activity activity = getActivity();
        if (activity instanceof HomeActivity) {
            HomeActivity homeActivity = (HomeActivity) activity;

            PreferenceHelper sharedPrefs = homeActivity.getSharedPreferences();
            m_showStats = sharedPrefs.areStatisticsEnabled();
            m_showCmdBar = sharedPrefs.shouldShowCmdBar();
            m_startProxy = sharedPrefs.shouldAutoStartProxy();
        }
        m_cmdBarVisible = m_cmdBar != null && m_cmdBar.getVisibility() == View.VISIBLE;

        showCmdBar(m_showCmdBar);
        showStats(m_showStats);
    }

    /**
     * Called by the base class for the derived class to prepare the video view and store it internally.
     *
     * @param fragmentView - the parent fragment view
     */
    protected abstract void prepareVideoView(View fragmentView);

    /**
     * Returns the resource id for the fragment layout to use.
     *
     * @return
     */
    protected abstract int getLayoutResourceId();

    /**
     * Returns the dimensions of the video view.
     *
     * @return
     */
    protected abstract Point getVideoViewDimensions();

    /**
     * Called when the fragment has been started.
     */
    protected abstract void onFragmentStarted();

    /**
     * Called when the fragment has been stopped.
     */
    protected abstract void onFragmentStopped();

    /**
     * See {@link Fragment#onStart()}
     */
    @Override
    public void onStart() {
        if (m_wlClient.isConnected()) {
            if (m_wlClient.isVideoDecodingPaused()) {
                boolean res = m_wlClient.resumeVideoEncoding();
                Log.d(TAG, "resumed video encoding :" + res);
            }

            // Display the loading screen
            setLoadingScreenState(true);

            m_waitIndicator.setVisibility(m_wlClient.isWaitIndicatorShown()
                    ? View.VISIBLE : View.GONE);

            //listen for connection state changes
//            m_wlClient.registerConnectionListener(this);

            //attach to receive client notifications
            App.instance().getWLClient().setClientListener(this);
            App.instance().getWLClient().setPingHandler(this);


            onFragmentStarted();

            //Check if any scaling is needed to sync between the render size and the client.
            Point videoViewSize = getVideoViewDimensions();
            Point renderSize = m_wlClient.getRenderSize();
            m_scaleX = videoViewSize.x > 0 ? renderSize.x / (float) videoViewSize.x : 1.0f;
            m_scaleY = videoViewSize.y > 0 ? renderSize.y / (float) videoViewSize.y : 1.0f;

            if (m_showStats) {
                m_handler.postDelayed(m_updateStats, 0);
            }
            //optional: when using the WL services proxy,
            if (m_startProxy) {
                if (!WLServicesHTTPProxy.getInstance().startWLServicesClient()) {
                    MCSLogger.log(MCSLogger.eWarning, TAG, "Could not start WL Services Client HTTP Proxy");
                }
            }

            m_handler.post(new Runnable() {
                @Override
                public void run() {
                    m_waitIndicator.setVisibility(View.INVISIBLE);
                }
            });
        } else {
            onConnectionClosed(null);
        }
        super.onStart();
    }

    /**
     * See {@link Fragment#onStop()}
     */
    @Override
    public void onStop() {
        //must stop to prevent surface crashes
        m_handler.removeCallbacks(m_updateStats);

        //unregister from client notifications.
        App.instance().getWLClient().setClientListener(null);

        //force hide keyboard (not automatic on all platforms)
        HomeActivity act = (HomeActivity) getActivity();
        act.hideKeyboard();

        setLoadingScreenState(false);

        onFragmentStopped();

        super.onStop();
    }


    /**
     * Send the back command to the client core.
     *
     * @return
     */
    public boolean onBackPressed() {
        m_wlClient.sendCommand(new BrowserCommand(BrowserCommand.ACT_BACK, null));
        return true;
    }

    /**
     * Send the home command to the client core.
     *
     * @return
     */
    public boolean onHomePressed() {
        boolean ret = m_wlClient.canGoHome();
        m_wlClient.sendCommand(new BrowserCommand(BrowserCommand.ACT_HOME, null));
        return ret;
    }

    //--------

    @Override
    public void onServerListUpdated(ServerInfo[] servers) {
        //already connected
    }

    /**
     * Called when connection to the server has been established
     */
    @Override
    public void onConnectionEstablished(PeerDevice peerDevice) {
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onConnectionEstablished()");
        // do nothing !
        if (m_startProxy) {
            if (!WLServicesHTTPProxy.getInstance().startWLServicesClient()) {
                MCSLogger.log(MCSLogger.eWarning, TAG, "Could not start WL Services Client HTTP Proxy");
            }
        }

        // No restrictions by default. Here a check for the handbrake should be made like the
        // OnClickListener of the handbrake_button.
        m_wlClient.sendCommand(new BrowserCommand(BrowserCommand.ACT_UI_RESTRICTION_LEVEL, BrowserCommand.UIRESTRICTION_LEVEL_NONE));
    }

    /**
     * Called when failed to connect to the server
     */
    @Override
    public void onConnectionFailed(PeerDevice peerDevice, EConnectionResult result) {
        MCSLogger.log(MCSLogger.eWarning, TAG, "onConnectionFailed, device: %s, reason: %s", peerDevice.getName(), result.name());
    }

    /**
     * Called when connection to the server has been closed
     */
    @Override
    public void onConnectionClosed(PeerDevice peerDevice) {
        WLServicesHTTPProxy.getInstance().stopWLServicesClient();
        // the connection is gone, close this fragment and return to non-connected screen.
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                HomeActivity activity = (HomeActivity) getActivity();
                activity.stopWebLink();
            }
        });

        m_activeAudioChannels.clear();

        m_handler.post(new Runnable() {
            @Override
            public void run() {
                m_waitIndicator.setVisibility(View.INVISIBLE);
            }
        });
    }

    /**
     * Called when the current application has changed
     *
     * @param appID the current application ID
     */
    @Override
    public void onApplicationChanged(int appID) {
        MCSLogger.log(TAG, "onApplicationChanged: appID" + appID);
    }

    /**
     * Called when new frame has been decoded and rendered to the frame buffer
     */
    @Override
    public void onFrameRendered() {
        // Hide the loading screen
        setLoadingScreenState(false);
    }

    /**
     * Called to check if the client is able to receive new frame
     * <p>
     * return boolean - true to receive the frame, false to ignore it
     */
    @Override
    public boolean canProcessFrame() {
        return true;
    }

    //---

    /**
     * Called when a "Show Keyboard" command has been received
     *
     * @param type the requested keyboard type
     */
    @Override
    public void onShowKeyboard(short type) {
        final short kbdType = type;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                HomeActivity act = (HomeActivity) getActivity();
                act.showKeyboard(WebLinkFragment.this);
                m_wlClient.sendCommand(new ShowKeyboardCommand(kbdType));
            }
        });
    }

    /**
     * Called when a "Hide Keyboard" command has been received
     */
    @Override
    public void onHideKeyboard() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                HomeActivity act = (HomeActivity) getActivity();
                act.hideKeyboard();

                m_wlClient.sendCommand(new HideKeyboardCommand());
            }
        });
    }

    /**
     * Called when a "Wait Indicator" command has been received
     *
     * @param showWaitIndicator true to show the wait indicator, false to hide it
     */
    @Override
    public void onWaitIndicator(boolean showWaitIndicator) {
        //do not show the spinner.
        final boolean show = showWaitIndicator;
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                m_waitIndicator.setVisibility(show
                        ? View.VISIBLE : View.GONE);
            }
        });
    }

    /**
     * Called to update the image for the application with the given ID
     *
     * @param appID the application ID
     * @param image the application Image
     */
    @Override
    public void onAppImageChanged(int appID, Bitmap image) {
        Log.v(TAG, "onAppImageChanged()");
    }

    /**
     * Called when the server connection has been lost
     */
    @Override
    public void onConnectionLost() {
        MCSLogger.log(MCSLogger.ELogType.eDebug, TAG, "onConnectionLost()");
    }

    /**
     * Called when the server connection has been resumed
     */
    @Override
    public void onConnectionResumed() {
    }

    /**
     * See {@link IClientNotification#onCommandReceived(Command)}
     *
     * @param command the command received
     * @return
     */
    @Override
    public boolean onCommandReceived(Command command) {
        return true;
    }

    /**
     * Handle touch events on the view.
     */
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (!m_wlClient.sendCommand(new TouchCommand(event, m_scaleX, m_scaleY))) {
            MCSLogger.log(MCSLogger.eWarning, TAG, "Failed to send touch event!");
        }
        return true;
    }


    /**
     * Handle the key events sent to this view.
     *
     * @param keyCode
     * @param event
     * @return
     */
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        boolean result = false;
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            m_wlClient.sendCommand(new BrowserCommand(BrowserCommand.ACT_BACK, null));
            result = true;
        } else {
            //Pass the keys to weblink client core.
            //First pass through extraction to get the underlying key code.
            int key = extractVirtualKeyCode(event);
            if (key != 0) {
                m_wlClient.sendCommand(new KeyboardCommand((short) key, KeyboardCommand.ACT_KEY_DOWN));
            }
            result = true;
        }
        return result;
    }

    /**
     * Process the android Key multiple event.
     *
     * @param keyCode
     * @param event
     * @return
     */
    public boolean onKeyMultiple(int keyCode, KeyEvent event) {
        boolean result = false;
        if (keyCode == 0) {
            String characters = event.getCharacters();
            int len = characters.codePointCount(0, characters.length());
            //NOTE: for now only send 1 character keys.  There are some generic IME events
            //that will emit the dummy buffer (del fix) characters
            if (len == 1) {
                int code = characters.codePointAt(0);
                Log.d(TAG, "onKeyMultiple sending: " + code);
                if (code != 0) {
                    m_wlClient.sendCommand(new KeyboardCommand((short) code, KeyboardCommand.ACT_KEY_DOWN));
                }
            } else {
                Log.d(TAG, "onKeyMultiple not handling event len=" + len);
            }

        }
        result = true;

        return result;
    }

    /**
     * Process the android key up event.
     *
     * @param keyCode
     * @param event
     * @return
     */
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        boolean result = false;
        if (keyCode != KeyEvent.KEYCODE_BACK && keyCode != KeyEvent.KEYCODE_MENU) {
            int key = extractVirtualKeyCode(event);
            if (key != 0) {
                m_wlClient.sendCommand(new KeyboardCommand((short) key, KeyboardCommand.ACT_KEY_UP));
            }
            if (keyCode == KeyEvent.KEYCODE_ENTER) {


                HomeActivity act = (HomeActivity) getActivity();
                act.hideKeyboard();
            }
            result = true;
        } else if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (event.isLongPress()) {
                m_wlClient.sendCommand(new BrowserCommand(BrowserCommand.ACT_HOME, null));
            }
            result = true;
        } else {
            int key = extractVirtualKeyCode(event);
            if (key != 0) {
                m_wlClient.sendCommand(new KeyboardCommand((short) key, KeyboardCommand.ACT_KEY_UP));
            }
            result = true;
        }
        return result;
    }

    /**
     * Extract the key code from the andriod key event.
     *
     * @param event
     * @return
     */
    private int extractVirtualKeyCode(KeyEvent event) {
        int keyCode = event.getKeyCode();

        keyCode = event.isShiftPressed() ?
                KeyMap.KEY_MAPPINGS_SHIFT.get(keyCode) :
                KeyMap.KEY_MAPPINGS_NORMAL.get(keyCode);
        //fall back to the unicode value
        if (keyCode == 0) {
            keyCode = event.getUnicodeChar();
        }
        return keyCode;
    }

    /**
     * Helper method that shows / hides the command bar.
     *
     * @param state
     */
    private void showCmdBar(boolean state) {
        if (m_cmdBar != null) {
            m_cmdBar.animate()
                    .translationY(state ? 0 : m_cmdBarContents.getMeasuredHeight())
                    .setDuration(300);
        }
        m_cmdBarVisible = state;
    }

    /**
     * Shows or hides the Loading Screen.
     * @param shown {@code true} to show, {@code false} to hide
     */
    private void setLoadingScreenState(final boolean shown) {
        m_handler.post(new Runnable() {
            @Override
            public void run() {
                if (m_loadingLayout != null) {
                    m_loadingLayout.setVisibility(shown ? View.VISIBLE : View.GONE);
                }
            }
        });
    }

    /**
     * Helper method that shows / hides the statistics view.
     *
     * @param state
     */
    private void showStats(boolean state) {
        if (m_stats != null) {
            m_stats.setVisibility(state ? View.VISIBLE : View.INVISIBLE);
        }
        m_cmdBarVisible = state;
    }

    /**
     * Diagnostic stats.
     */
    final Runnable m_updateStats = new Runnable() {
        @Override
        public void run() {
            if (m_showStats) {
                int frameRate = m_wlClient.getFrameRate();
                int dataRate = m_wlClient.getDataRate() / 1024;

                long frameCount = -1;
                long keyFrameCount = -1;
                IFrameDecoder decoder = m_wlClient.getFrameDecoder();
                if (decoder != null) {
                    if (decoder instanceof FrameDecoder_H264) {
                        frameCount = ((FrameDecoder_H264) decoder).getFrameInputCount();
                        keyFrameCount = ((FrameDecoder_H264) decoder).getKeyFrameInputCount();
                    }
                    if (decoder instanceof FrameDecoder_H264_Custom) {
                        frameCount = ((FrameDecoder_H264_Custom) decoder).getFrameInputCount();
                        keyFrameCount = ((FrameDecoder_H264_Custom) decoder).getKeyFrameInputCount();
                    }
                }

                int count = 0;
                StringBuilder sb = new StringBuilder();
                if (frameRate != -1) {
                    count++;
                    sb.append(String.format("Frame rate: %dfps", frameRate));
                }
                if (dataRate != -1) {
                    if (count > 0) {
                        sb.append("\n");
                        count--;
                    }
                    sb.append(String.format("Data rate: %dKB/s", dataRate));
                    count++;
                }
                if (frameCount != -1) {
                    if (count > 0) {
                        sb.append("\n");
                        count--;
                    }
                    sb.append(String.format("Frame #: %d", frameCount));
                    count++;
                }
                if (keyFrameCount != -1) {
                    if (count > 0) {
                        sb.append("\n");
                        count--;
                    }
                    sb.append(String.format("KeyFrame #: %d", keyFrameCount));
                    count++;
                }
                if (!m_activeAudioChannels.isEmpty()) {
                    sb.append("\n");
                    for (int activeAudioChannel : m_activeAudioChannels) {
                        sb.append(String.format("Active Audio channel #: %d\n", activeAudioChannel));
                    }
                }
                m_stats.setText(sb.toString());

                m_handler.postDelayed(this, 1000);
            }
        }
    };

    /**
     * Handle key events sent to this View.
     */
    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_UP) {
            return onKeyUp(keyCode, event);
        } else if (event.getAction() == KeyEvent.ACTION_DOWN) {
            return onKeyDown(keyCode, event);
        } else if (event.getAction() == KeyEvent.ACTION_MULTIPLE) {
            return onKeyMultiple(keyCode, event);
        }
        return false;
    }

    /**
     * Example handling the ping timeout.
     * <p>
     * The client disconnects.
     */
    @Override
    public void onPingResponseTimeout() {
        m_wlClient.disconnect();
    }

    /**
     * See {@link IPingHandler#onPingResponseReceived(boolean)}
     *
     * @param isSenderInactive the host app's reported activity status.
     */
    @Override
    public void onPingResponseReceived(boolean isSenderInactive) {

    }

    @Override
    public void onAudioChannelStarted(final int channelID) {
        m_activeAudioChannels.add(channelID);
    }

    @Override
    public void onAudioChannelStopped(final int channelID) {
        m_activeAudioChannels.remove(channelID);
    }

}
