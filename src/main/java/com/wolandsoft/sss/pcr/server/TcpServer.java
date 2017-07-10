/*
    Copyright 2017 Alexander Shulgin

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

        http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
*/
package com.wolandsoft.sss.pcr.server;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;

/**
 * Tcp data receiver.
 */
public class TcpServer extends Thread {
    private static final String PORT = "port";
    private static final Logger logger = Logger.getLogger(TcpServer.class.getName());
    private ServerDataListener mListener;
    private ServerSocket mSocket;
    private InetAddress mLocalAddress;
    private int mPort;

    public TcpServer(ServerDataListener listener) {
	Preferences prefs = Preferences.userNodeForPackage(TcpServer.class);
	mPort = prefs.getInt(PORT, 0);
	try {
	    mLocalAddress = InetAddress.getLocalHost();
	    mSocket = new ServerSocket(mPort);
	    if (mPort == 0) {
		mPort = mSocket.getLocalPort();
		prefs.putInt(PORT, mPort);
	    }
	} catch (IOException e) {
	    logger.log(Level.SEVERE, e.getMessage(), e);
	    throw new RuntimeException(e.getMessage(), e);
	}
	mListener = listener;
    }

    public void run() {
	ByteArrayOutputStream baOutStream = new ByteArrayOutputStream();
	while (mSocket != null) {
	    try (Socket inSocket = mSocket.accept(); InputStream inStream = inSocket.getInputStream()) {
		baOutStream.reset();
		int bt;
		while ((bt = inStream.read()) != -1) {
		    baOutStream.write(bt);
		}
	    } catch (IOException e) {
		logger.log(Level.WARNING, e.getMessage(), e);
	    }
	    byte[] data = baOutStream.toByteArray();
	    if(data.length > 0) {
		mListener.onDataReceived(data);
	    }
	}
    }

    public void finish() {
	if (mSocket != null) {
	    try {
		mSocket.close();
	    } catch (IOException ignore) {

	    } finally {
		mSocket = null;
	    }
	}
    }

    public int getPort() {
	return mPort;
    }

    public String getHost() {
	return mLocalAddress.getHostName();
    }
}
