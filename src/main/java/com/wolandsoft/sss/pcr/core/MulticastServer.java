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
package com.wolandsoft.sss.pcr.core;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.util.Scanner;

public class MulticastServer extends Thread {
    private static final String ALL_HOSTS_MC_ADDRESS = "224.0.0.1";
    private static final int PACKET_SIZE = 8192;
    private int mPort;
    private MulticastServerDataListener mListener;
    private MulticastSocket mSocket;
    private InetAddress mAddress;

    public MulticastServer(MulticastServerDataListener listener) {
	File storagePath = new File(System.getProperty("user.home"), ".sss");
	storagePath.mkdir();
	File filePath = new File(storagePath, "port");
	if (filePath.exists()) {
	    try (Scanner scanner = new Scanner(filePath)) {
		String content = scanner.useDelimiter("\\Z").next();
		// recreate port
		mPort = Integer.valueOf(content);
	    } catch (FileNotFoundException ignore) {

	    } finally {
		if (mPort == 0) {
		    filePath.delete();
		    try (FileWriter fw = new FileWriter(filePath); ServerSocket serverSocket = new ServerSocket(0)) {
			mPort = serverSocket.getLocalPort();
			fw.write(String.valueOf(mPort));
		    } catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		    }
		}
	    }
	}
	try {
	    mAddress = InetAddress.getByName(ALL_HOSTS_MC_ADDRESS);
	    mSocket = new MulticastSocket(mPort);
	    mSocket.joinGroup(mAddress);
	} catch (IOException e) {
	    throw new RuntimeException(e.getMessage(), e);
	}
	mListener = listener;
    }

    public int getPort() {
	return mPort;
    }

    public void run() {
	try {
	    byte[] buf = new byte[PACKET_SIZE];
	    DatagramPacket packet = new DatagramPacket(buf, buf.length);
	    // Receive packet and get the Data
	    while (true) {
		mSocket.receive(packet);
		byte[] data = new byte[packet.getLength()];
		System.arraycopy(packet.getData(), packet.getOffset(), data, 0, data.length);
		mListener.onDataReceived(data);
	    }
	} catch (IOException ignore) {

	}
    }

    public void finish() {
	if (mSocket != null) {
	    try {
		mSocket.leaveGroup(mAddress);
		mSocket.close();
	    } catch (IOException ignore) {

	    }
	}
    }

    public interface MulticastServerDataListener {
	void onDataReceived(byte[] data);
    }
}
