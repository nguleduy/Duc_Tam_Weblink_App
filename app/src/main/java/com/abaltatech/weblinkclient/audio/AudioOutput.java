/****************************************************************************
 *
 * @file AudioOutput.java
 * @brief
 *
 * Contains the AudioOutput class.
 *
 * @author Abalta Technologies, Inc.
 * @date Feb, 2014
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
package com.abaltatech.weblinkclient.audio;

import android.media.AudioManager;
import android.media.AudioTrack;

import com.abaltatech.weblink.core.DataBuffer;
import com.abaltatech.weblinkclient.audiodecoding.IAudioOutput;

/**
 * Concrete implementation of the IAudioOutput interface.
 */
public class AudioOutput implements IAudioOutput {
    private static final String      TAG  = "AudioDecoder";

    private AudioTrack m_audioTrack;
    private int m_minBufSize;

    @Override
    public boolean startAudio(int sampleRate, int bitsPerChannel, int channelCount) {
        m_minBufSize  = AudioTrack.getMinBufferSize(sampleRate,
                channelCount == 2 ? android.media.AudioFormat.CHANNEL_OUT_STEREO : android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT);

        m_minBufSize = Math.max(m_minBufSize, bitsPerChannel / 8 * channelCount * 1024);

        m_audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRate,
                channelCount == 2 ? android.media.AudioFormat.CHANNEL_OUT_STEREO : android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT, m_minBufSize, AudioTrack.MODE_STREAM);
        m_audioTrack.play();
        return true;
    }

    @Override
    public boolean stopAudio() {
        if (m_audioTrack != null) {
            m_audioTrack.pause();
            m_audioTrack.flush();
            m_audioTrack.play();
            m_audioTrack.release();
            m_audioTrack = null;
        }

        return true;
    }

    @Override
    public boolean outputAudio(DataBuffer audioData) {
        if (m_audioTrack != null) {
            DataBuffer data = audioData.copy();
        }

        return false;
    }

    @Override
    public int getBufferedLength() {
        return m_minBufSize;
    }
};
