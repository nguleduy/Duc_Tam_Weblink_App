/****************************************************************************
 *
 * @file WebLinkFragment_SurfaceView.java
 * @brief
 *
 * Contains the WebLinkFragment_SurfaceView class.
 *
 * @author Abalta Technologies, Inc.
 * @date Aug, 2019
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

import android.graphics.Point;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.abaltatech.weblinkclient.WLClientDisplay;
import com.testabalta.R;

/**
 * Extension of the {@link WebLinkFragment} that uses SurfaceView as the target view for the video
 * decoder.
 */
public class WebLinkFragment_SurfaceView extends WebLinkFragment {

    private static final String TAG = "WebLinkFragment_SurfaceView";
    private SurfaceView m_videoView;

    protected WLClientDisplay m_defaultClientDisplay; ///< The current client display

    /**
     * Should be called by the parent activity to set the default {@link WLClientDisplay}
     * @param defaultDisplay
     */
    public void setDefaultDisplay(WLClientDisplay defaultDisplay) {
        m_defaultClientDisplay = defaultDisplay;
    }

    /**
     * See {@link WebLinkFragment#getLayoutResourceId()}
     * @return
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.weblink_fragment_surfaceview;
    }

    /**
     * Prepares the SurfaceView
     * @param fragmentView - the parent fragment view
     */
    @Override
    protected void prepareVideoView(View fragmentView) {
        m_videoView = (SurfaceView) fragmentView.findViewById(R.id.video_view);

        SurfaceHolder holder = m_videoView.getHolder();
        holder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d(TAG, "surfaceCreated!");
                Point renderSize = m_wlClient.getRenderSize();
                m_scaleX = m_videoView.getWidth() > 0 ? renderSize.x / (float) m_videoView.getWidth() : 1.0f;
                m_scaleY = m_videoView.getHeight() > 0 ? renderSize.y / (float) m_videoView.getHeight() : 1.0f;
                m_defaultClientDisplay.onEncodeSurfaceReady(holder.getSurface());
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                Point renderSize = m_wlClient.getRenderSize();
                m_scaleX = m_videoView.getWidth() > 0 ? renderSize.x / (float) m_videoView.getWidth() : 1.0f;
                m_scaleY = m_videoView.getHeight() > 0 ? renderSize.y / (float) m_videoView.getHeight() : 1.0f;
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                Log.d(TAG, "surfaceDestroyed!");
                //Perform the pausing when the texture is destroyed
                //this represents when the texture is no longer valid
                //to the system so we will have to recreate it next start() cycle.
                m_defaultClientDisplay.onEncodeSurfaceDestroyed();
            }
        });

/*
///<DIAG
        //optional:
 		//if diag is enabled on the weblink client, use this listener to complete the
 		// diagnostic pipeline.
		if (m_wlClient.diagIsEnabled()) {
		   m_videoView.setOnFrameRenderedListener(new VideoView.OnFrameRenderedListener() {
		       @Override
		       public void onFrameRendered(int frameIndex) {
		           m_wlClient.diagFlushFrame(frameIndex, SystemClock.uptimeMillis());
		       }
		   });
		}
///<DIAG
*/
    }

    /**
     * See {@link WebLinkFragment#getVideoViewDimensions()}
     * @return
     */
    @Override
    protected Point getVideoViewDimensions() {
        if (m_videoView != null) {
            return new Point(m_videoView.getWidth(), m_videoView.getHeight());
        } else {
            return new Point(0, 0);
        }
    }

    /**
     * See {@link WebLinkFragment#onFragmentStarted()}
     */
    @Override
    protected void onFragmentStarted() {
        // Do nothing for now
    }

    /**
     * See {@link WebLinkFragment#onFragmentStopped()}
     */
    @Override
    protected void onFragmentStopped() {
        // Do nothing for now
    }
}
