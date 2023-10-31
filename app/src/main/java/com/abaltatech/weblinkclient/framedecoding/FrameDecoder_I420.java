/****************************************************************************
 *
 * @file FrameDecoder_I420.java
 * @brief
 *
 * Contains the FrameDecoder_I420 class.
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
package com.abaltatech.weblinkclient.framedecoding;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.Surface;

import com.abaltatech.weblink.core.DataBuffer;
import com.abaltatech.weblink.core.WLTypes;

/**
 * Decodes video stream using I420+LZ4
 */
public class FrameDecoder_I420 implements IFrameDecoder {
    
    private static final String TAG = "FrameDecoder_I420";


    private Surface m_surface;
    private Bitmap  m_frameBuffer;
    private int     m_frameIndex;
    private IFrameDecoderNotification m_notification;
    
    static {
        System.loadLibrary("i420decoder");
    }

    @Override
    synchronized public boolean startDecoding(
      IFrameDecoderNotification notification,
      int          width, 
      int          height, 
      VideoSurface surface
      )
    {
        boolean result = false;
        
        if (surface != null && width > 0 && height > 0) {
            m_frameIndex   = 0;   
            m_surface      = surface.getSurface();
            surface.getSurfaceTexture().setDefaultBufferSize(width, height);
            m_notification = notification;
            notification.onDecodingStarted();
            result = true;
        }
        notification.onDecodingStartFailed();
        return result;
    }

    @Override
    synchronized public void stopDecoding() {
        m_surface          = null; 
        if (m_frameBuffer != null) {
            m_frameBuffer.recycle();
            m_frameBuffer = null;
            m_notification.onDecodingStopped();
        }
        reset();
    }
    
    @Override
    public void reset() {
        m_frameIndex = 0;   
    }
    
    @Override
    public boolean decodeImage(DataBuffer frameBits) {
        Bitmap frame = decodeFrame(m_frameBuffer, frameBits.getData(), frameBits.getPos(), frameBits.getSize());
        if (frame != null) {
            m_frameBuffer = frame;
            m_frameIndex++;
            Canvas canvas = m_surface.lockCanvas(null);
            if (canvas != null) {
                canvas.drawBitmap(m_frameBuffer, 
                        new Rect(0, 0, m_frameBuffer.getWidth() - 1, m_frameBuffer.getHeight() - 1), 
                        new Rect(0, 0, canvas.getWidth() - 1, canvas.getHeight() - 1), null);
                m_surface.unlockCanvasAndPost(canvas);
            }
        } else {
            m_notification.onDecodingError();
        }
        return true;
    }

    @Override
    public int getType() {
        return WLTypes.FRAME_ENCODING_I420;    
    }
    
    @Override
    synchronized public Bitmap getScreenshot() {
        Bitmap result = null;
        if (m_frameIndex > 0) {
            return m_frameBuffer;
        }
        return result;
    }
    
    @Override
    public boolean canSkipFrames() {
        return true;
    }
    
    @Override
    public boolean isVideoOutGenerated() {
        return m_frameIndex > 0;
    }
    
    @Override
    public void onFrameSkipped(DataBuffer frameBits) {
    }
    
    @Override
    public DataBuffer getConfigFrameBits(){
        return null; // I420 has no in-stream configuration data.
    }
    
    public static boolean isSupported() {
        return true;
    }
    
    private static native Bitmap decodeFrame(
       Bitmap   frameBuffer, 
       byte[]   encFrame, 
       int      startPos, 
       int      size
       );
};
