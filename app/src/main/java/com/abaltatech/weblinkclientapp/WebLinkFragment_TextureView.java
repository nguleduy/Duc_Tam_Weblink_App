/****************************************************************************
 *
 * @file WebLinkFragment_TextureView.java
 * @brief
 *
 * Contains the WebLinkFragment_TextureView class.
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
import android.graphics.SurfaceTexture;
import android.util.Log;
import android.view.TextureView;
import android.view.View;

import com.testabalta.R;


/**
 * Extension of the {@link WebLinkFragment} that uses TextureView as the target view for the video
 * decoder.
 */
public class WebLinkFragment_TextureView extends WebLinkFragment {

    private static final String TAG = "WebLinkFragment_TextureView";
    private TextureView m_videoView;

    /**
     * See {@link WebLinkFragment#getLayoutResourceId()}
     * @return
     */
    @Override
    protected int getLayoutResourceId() {
        return R.layout.weblink_fragment_textureview;
    }

    /**
     * Prepares the TextureView
     * @param fragmentView - the parent fragment view
     */
    @Override
    protected void prepareVideoView(View fragmentView) {
        m_videoView = (TextureView) fragmentView.findViewById(R.id.video_view);

        m_videoView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {

            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surface, int w, int h) {
                Point renderSize = m_wlClient.getRenderSize();
                m_scaleX = w > 0 ? renderSize.x / (float)w : 1.0f;
                m_scaleY = h > 0 ? renderSize.y / (float)h : 1.0f;

            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int w, int h) {
                Point renderSize = m_wlClient.getRenderSize();
                m_scaleX = w > 0 ? renderSize.x / (float)w : 1.0f;
                m_scaleY = h > 0 ? renderSize.y / (float)h : 1.0f;
            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
                Log.d(TAG,"onSurfaceTextureDestroyed !");
                if (App.instance().getWLClientCore().isConnected()) {
                    App.instance().getWLClientCore().pauseVideoEncoding();
                }
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surface) {
                //Note: sometimes can cause problems since there is no delta between start-end draw
                HomeActivity activity = (HomeActivity)getActivity();
                if (activity != null) {
                    if (m_wlClient != null) {
                        m_wlClient.beginDrawFrame();
                        //texture was already drawn!
                        m_wlClient.endDrawFrame();
                    }
                }
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
        //due to the weird stop->start transition when turning screen on / off on
        //some systems, only attach if the view isn't setup already.
        if(m_videoView.getSurfaceTexture() == null) {
            m_videoView.setSurfaceTexture(m_wlClient.getSurface().getSurfaceTexture());
        }
    }

    /**
     * See {@link WebLinkFragment#onFragmentStopped()}
     */
    @Override
    protected void onFragmentStopped() {
        // Do nothing for now
    }
}
