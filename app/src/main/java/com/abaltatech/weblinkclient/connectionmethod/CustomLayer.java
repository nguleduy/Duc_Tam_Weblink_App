/****************************************************************************
 *
 * @file CustomLayer.java
 * @brief 
 *
 * Implementation of data transferring through USB bulk transfers
 *
 * @author Abalta Technologies, Inc.
 * @date October, 2014
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

package com.abaltatech.weblinkclient.connectionmethod;

import android.annotation.SuppressLint;
import android.util.Log;

import com.abaltatech.mcs.common.IMCSDataStats;
import com.abaltatech.mcs.common.MCSDataLayerBase;
import com.abaltatech.mcs.logger.MCSLogger;

/**
 *  Custom layer is an example implementation of the IMCSDataLayer and MCSDataLayerBase.
 */
public class CustomLayer extends MCSDataLayerBase {

    private static final String TAG = "CustomLayer";

    private static final boolean DEBUG = false;

    private static long s_errorCounter = 0;

    private ReadThread          m_readThread        = null;
    private byte                m_readBuffer[]      = null;
    private int				    m_bytesAvailable 	= 0;

	private Object m_connection; //connection object.

    /**
     *
     * @param address address for the usb device
     * @return success or failure of the operation
     */
    public boolean connect(CustomAddress address){
		if(address != null) {

			//if the connection is successful
			m_connection = new Object();

			m_readBuffer = new byte[16 * 1024];
			m_readThread = new ReadThread();
			m_readThread.start();
			return true;

		} else {
			Log.e(TAG,"conenct error");
			return false;
		}
    }

	/**
	 * isReady is asked to determine if the layer is ready to communicate.
	 * @return
     */
    @Override
    public boolean isReady(){
        return m_connection != null;
    }

	/**
	 * Public read function, used by entities to copy data to their buffer.
	 * @param buffer byte array to fill with the data received by IMCSDataLayer
	 * @param size maximum number of bytes to read
     * @return number of bytes actually read.
     */
    @Override
    public int readData(byte[] buffer, int size) {
    	int bytesRead = 0;
		IMCSDataStats stats = getDataStats();

    	try
		{
			synchronized(this)
			{
				if (m_readBuffer != null && m_bytesAvailable > 0 && size > 0) {
					bytesRead = Math.min(m_bytesAvailable, size);

					System.arraycopy(m_readBuffer, 0, buffer, 0, bytesRead);

					if (m_bytesAvailable > bytesRead) {
						System.arraycopy(m_readBuffer, bytesRead, m_readBuffer, 0, m_bytesAvailable - bytesRead);
					}

					m_bytesAvailable -= bytesRead;
					if (DEBUG) {
						MCSLogger.log(TAG, bytesRead + " bytes read");
					}

					if (stats != null) {
						stats.onDataReceived(bytesRead);
					}
				}
			}
		} catch (Exception e) {
			MCSLogger.log( TAG + " EXCEPTION", e.toString() );
			closeConnection();
		}

    	return bytesRead;
    }

	/**
	 * The internal writeData function, this is called when data is needed to be sent.
	 * @param buffer buffer to write.
	 * @param size number of bytes from the buffer to write.
     */
    @Override
    protected void writeDataInternal(byte[] buffer, int size) {
    	int bytesWritten = 0;
        IMCSDataStats stats = getDataStats();

        try {
//			bytesWritten = write_api_here(buffer,size);

			if (bytesWritten > 0) {
				if (DEBUG) {
					MCSLogger.log(TAG, size + " bytes sent");
				}

				if (stats != null) {
					stats.onDataSent(size);
				}
			} else {
				Log.w(TAG, "write(" + size + ") error! count=" + s_errorCounter++);
			}
        }
		catch (Exception ex) {
			Log.e(TAG, "writeDataInternal caught " + ex.getMessage() + " , closing connection.");
			closeConnection();
		}
    }

	/**
	 * Clean up the state for disconnection.
	 */
    @Override
    public void closeConnection() {
		Log.d(TAG, "closing UsbLayer");
		synchronized (CustomLayer.this) {
			if (m_readThread != null) {
				m_readThread.interrupt();
				m_readThread = null;
			}

			if (m_connection != null) {
				m_connection = null;
			}

			m_readBuffer = null;
		}
    }

	/**
     * Provided thread for running a loop on reding.
	 */
    private class ReadThread extends Thread {
        private volatile boolean m_stopped = false;

        private int m_exampleReadEndpoint = -1;

		ReadThread() {
			setName("UsbLayerReadThread");
		}

        @Override
        public void interrupt() {
            m_stopped = true;
            super.interrupt();
        }

        @SuppressLint("NewApi") @Override
        public void run() {
			m_bytesAvailable = 0;
            try {
				MCSLogger.log(TAG,"UsbLayerReadThread START");
				while (!m_stopped) {
					int bytesRead =0;
					int bytesRemaining;
					int bytesAvailable;
					synchronized (CustomLayer.this) {
						bytesRemaining = m_readBuffer.length - m_bytesAvailable;
						bytesAvailable = m_bytesAvailable;
					}

					if (bytesAvailable == 0) {
						bytesRead = readAPIWithStartIndex(m_exampleReadEndpoint, m_readBuffer, m_bytesAvailable, bytesRemaining, 0);

						if (bytesRead > 0) {
							synchronized (CustomLayer.this) {
								m_bytesAvailable += bytesRead;
							}

							notifyForData();
						} else if (bytesRead < 0) {
							Log.d(TAG, "read error =" + bytesRead);
							closeConnection();
							break;
						}
					} else {
						//we still have data, wait a bit and try to notify!
						Thread.sleep(10);
						notifyForData();
					}
				}
            }
            catch (Exception e) {
                Log.e(TAG,"CustomLayerReadThread encountered exception "+e.getMessage());
            } finally {
				closeConnection();
				//send the notify connection closed from the thread.
				notifyForConnectionClosed();
				Log.w(TAG,"CustomLayerReadThread finished");
			}
        }
    }

    /**
     * Replace this with your platform API for reading data to a buffer.
     */
    static int readAPIWithStartIndex(int endpoint, byte[] buffer, int startIndex, int count, int timeout) {
        return 0; //stub
    }
}
