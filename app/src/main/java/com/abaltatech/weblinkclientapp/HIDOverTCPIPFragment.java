/****************************************************************************
 *
 * @file HIDOverTCPIPFragment.java
 * @brief
 *
 * Implementation of the HIDOverTCPIPFragment class.
 *
 * @author Abalta Technologies, Inc.
 * @date March/2019
 *
 * @cond Copyright
 *
 * COPYRIGHT 2019 ABALTA TECHNOLOGIES ALL RIGHTS RESERVED.<br>
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

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.abaltatech.weblinkclient.hid.TCPIPHIDUtils;
import com.testabalta.R;

import java.util.Locale;

/**
 * Example that demonstrates how to use the {@link TCPIPHIDUtils} class to forward HID
 * commands to an HID TCP/IP Server.
 *
 * This is done in cases where the IVU is unable to send USB data to a device via USB cable. In this
 * case a HID TCP/IP Server is used. The Server just forwards the HID commands to the phone via
 * a USB cable.
 *
 * This is intended for debugging! DO NOT USE IN PRODUCTION!
 */
public class HIDOverTCPIPFragment extends Fragment implements TCPIPHIDUtils.ITCPIPHIDUtilNotification {

    private static final String TAG = HIDOverTCPIPFragment.class.getSimpleName();

    // Width of the debug touch area
    private int m_width = TCPIPHIDUtils.getInstance().getWidth();
    // Height of the debug touch area
    private int m_height = TCPIPHIDUtils.getInstance().getHeight();
    // Orientation of the debug touch area
    private TCPIPHIDUtils.ETouchDeviceOrientation m_orientation = TCPIPHIDUtils.ETouchDeviceOrientation.Landscape;

    // Address of the HID TCP/IP Server
    private String m_address = "10.40.41.103";
    // Port of the HID TCP/IP Server
    private int m_port = 60010;

    private EditText m_etDebugTouchAreaHeight;
    private EditText m_etDebugTouchAreaWidth;
    private EditText m_etAddress;
    private EditText m_etPort;
    private Button m_btConnect;
    private View m_debugTouchArea;
    private Button m_btOrientation;

    // Used to prevent multiple Toasts from spamming the UI for a long time.
    private Toast m_toast;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.tcpiphid_fragment,
                container, false);

        m_etDebugTouchAreaWidth = (EditText)view.findViewById(R.id.et_debug_touch_area_width);
        m_etDebugTouchAreaWidth.setText(String.valueOf(TCPIPHIDUtils.getInstance().getWidth()));
        m_etDebugTouchAreaWidth.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String t = s.toString();
                try {
                    int width = Integer.parseInt(t);
                    TCPIPHIDUtils.getInstance().setWidth(width);
                } catch (Exception ex) {
                    showToast("Invalid width entered!", Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        m_etDebugTouchAreaHeight = (EditText)view.findViewById(R.id.et_debug_touch_area_height);
        m_etDebugTouchAreaHeight.setText(String.valueOf(TCPIPHIDUtils.getInstance().getHeight()));
        m_etDebugTouchAreaHeight.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String t = s.toString();
                try {
                    int height = Integer.parseInt(t);
                    TCPIPHIDUtils.getInstance().setHeight(height);
                } catch (Exception ex) {
                    showToast("Invalid height entered!", Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        m_etAddress = (EditText) view.findViewById(R.id.et_address);
        m_etAddress.setText(TCPIPHIDUtils.getInstance().getAddress());
        m_etAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                TCPIPHIDUtils.getInstance().setAddress(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        m_etPort = (EditText) view.findViewById(R.id.et_port);
        m_etPort.setText(String.valueOf(TCPIPHIDUtils.getInstance().getPort()));
        m_etPort.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    TCPIPHIDUtils.getInstance().setPort(Integer.parseInt(s.toString()));
                } catch (NumberFormatException e) {
                    showToast("Invalid port entered!", Toast.LENGTH_SHORT);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        m_btConnect = (Button) view.findViewById(R.id.bt_connect);
        m_btConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TCPIPHIDUtils.getInstance().isConnected()) {
                    TCPIPHIDUtils.getInstance().disconnect();
                } else {
                    TCPIPHIDUtils.getInstance().connect();
                    v.setEnabled(false);
                }
            }
        });

        m_debugTouchArea = view.findViewById(R.id.debug_touch_area);
        m_debugTouchArea.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int x = (int)event.getX();
                int y = (int)event.getY();

                boolean isPressed = false;

                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        isPressed = true;
                        break;

                }

                TCPIPHIDUtils.getInstance().sendSingleTouchEvent(isPressed, x, y);
                return true;
            }
        });

        m_btOrientation = (Button) view.findViewById(R.id.bt_rotation);
        m_btOrientation.setText(String.format(Locale.US, getString(R.string.orientation),
                TCPIPHIDUtils.getInstance().getOrientation().toString()));
        m_btOrientation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int rotation = TCPIPHIDUtils.getInstance().getOrientation().ordinal();
                rotation = (rotation + 1) % (TCPIPHIDUtils.ETouchDeviceOrientation.ReverseLandscape.ordinal() + 1);
                TCPIPHIDUtils.getInstance().setOrientation(TCPIPHIDUtils.ETouchDeviceOrientation.values()[rotation]);
                m_btOrientation.setText(String.format(Locale.US, getString(R.string.orientation),
                        TCPIPHIDUtils.getInstance().getOrientation().toString()));
            }
        });

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        TCPIPHIDUtils.getInstance().registerDeviceListener(this);
        updateUI();
    }

    @Override
    public void onStop() {
        super.onStop();
        TCPIPHIDUtils.getInstance().unregisterDeviceListener(this);
    }

    private void updateUI() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                m_btConnect.setEnabled(true);
                m_btConnect.setText(TCPIPHIDUtils.getInstance().isConnected()
                        ? getString(R.string.disconnect) : getString(R.string.connect));
                m_btOrientation.setText(String.valueOf(TCPIPHIDUtils.getInstance().getOrientation().ordinal()));
                m_etDebugTouchAreaWidth.setText(String.valueOf(TCPIPHIDUtils.getInstance().getWidth()));
                m_etDebugTouchAreaHeight.setText(String.valueOf(TCPIPHIDUtils.getInstance().getHeight()));
                m_etAddress.setText(TCPIPHIDUtils.getInstance().getAddress());
                m_etPort.setText(String.valueOf(TCPIPHIDUtils.getInstance().getPort()));
            }
        });
    }

    @Override
    public void onConnectionWithServerEstablished() {
        updateUI();
    }

    @Override
    public void onConnectionWithServerFailed() {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getActivity(), getString(R.string.failed_to_connect), Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onConnectionWithServerClosed() {
        updateUI();
    }


    /**
     * Used to prevent multiple Toasts from spamming the UI for a long time.
     *
     * @param text Text to display
     * @param duration Toast duration
     */
    @SuppressLint("ShowToast")
    private void showToast(CharSequence text, int duration) {
        if (m_toast == null) {
            m_toast = Toast.makeText(getActivity(), text, duration);
        } else {
            m_toast.setText(text);
        }
        m_toast.show();
    }
}
