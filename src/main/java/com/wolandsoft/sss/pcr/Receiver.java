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
package com.wolandsoft.sss.pcr;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;
import java.util.Collections;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import org.json.JSONObject;

import com.google.zxing.WriterException;
import com.wolandsoft.sss.pcr.security.TextCipher;
import com.wolandsoft.sss.pcr.server.ServerDataListener;
import com.wolandsoft.sss.pcr.server.TcpServer;
import com.wolandsoft.sss.pcr.ui.EStrings;
import com.wolandsoft.sss.pcr.ui.QRCodeDialog;
import com.wolandsoft.sss.pcr.ui.TrayIconUI;
import com.wolandsoft.sss.pcr.ui.TrayIconUI.TrayIconListener;

public class Receiver implements TrayIconListener, ServerDataListener {
    private static final int PROTOCOL_VER = 1;
    private static final Logger logger = Logger.getLogger(Receiver.class.getName());

    public static void main(String[] args) throws InterruptedException {
	Receiver receiver = new Receiver();
	receiver.run();
    }

    private static final String ACTION_PING = "com.wolandsoft.sss.ACTION_PING";
    private static final String ACTION_PAYLOAD = "com.wolandsoft.sss.ACTION_PAYLOAD";
    private static final String KEY_ACTION = "action";
    private static final String KEY_DATA = "data";
    private static final String KEY_TITLE = "title";

    private TrayIconUI mTrayIcon;
    private TextCipher mKeystore;
    private TcpServer mServer;
    private JDialog mQRCodeDialog;
    private boolean mPause = false;

    public Receiver() {
	try {
	    mKeystore = new TextCipher();
	} catch (GeneralSecurityException e) {
	    throw new RuntimeException(e.getMessage(), e);
	}
	mTrayIcon = new TrayIconUI(this);
	mServer = new TcpServer(this);
    }

    public void run() throws InterruptedException {
	mServer.start();
	mServer.join();
    }

    private void hideQRCode() {
	if (mQRCodeDialog != null) {
	    mQRCodeDialog.setVisible(false);
	    mQRCodeDialog.dispose();
	    mQRCodeDialog = null;
	}
    }

    @Override
    public void onShowKey() {
	hideQRCode();
        
	try {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(baos);
	    // store IP
	    byte[] host = mServer.getIP();
	    dos.writeInt(host.length);
	    dos.write(host, 0, host.length);
	    // store port
	    dos.writeInt(mServer.getPort());
	    //store host
	    host = mServer.getHost().getBytes(StandardCharsets.UTF_8);
	    dos.writeInt(host.length);
	    dos.write(host, 0, host.length);
	    // store key
	    byte[] key = mKeystore.getKey();
	    dos.writeInt(key.length);
	    dos.write(key, 0, key.length);
	    dos.flush();
	    byte[] payload = baos.toByteArray();
	    Checksum checksum = new CRC32();
	    checksum.update(payload, 0, payload.length);
	    long checksumValue = checksum.getValue();
	    baos.reset();
	    dos.writeInt(PROTOCOL_VER);
	    dos.writeLong(checksumValue);
	    dos.writeInt(payload.length);
	    dos.write(payload);
	    dos.close();
	    byte[] data = baos.toByteArray();
	    // to base64
	    String strToEncode = Base64.getEncoder().encodeToString(data);
	    mQRCodeDialog = new QRCodeDialog(strToEncode);
	    mQRCodeDialog.setVisible(true);
	} catch (WriterException | IOException e) {
	    JOptionPane.showMessageDialog(null, e.getMessage(), EStrings.lbl_error.toString(),
		    JOptionPane.ERROR_MESSAGE);
	}

    }

    @Override
    public void onPause(boolean state) {
	mPause = state;
    }

    @Override
    public void onExit() {
	mServer.finish();
	System.exit(0);
    }

    @Override
    public void onDataReceived(byte[] data) {
	if (!mPause) {
	    try {
		byte[] plain = mKeystore.decipher(data);
		String json = new String(plain, StandardCharsets.UTF_8);
		JSONObject jsonObj = new JSONObject(json);
		String action = jsonObj.getString(KEY_ACTION);
		if (ACTION_PING.equals(action)) {
		    hideQRCode();
		    mTrayIcon.showNotification(EStrings.lbl_app_name.toString(),
			    EStrings.msg_pair_completed.toString());
		} else if (ACTION_PAYLOAD.equals(action)) {
		    String title = jsonObj.getString(KEY_TITLE);
		    String payload = jsonObj.getString(KEY_DATA);
		    StringSelection stringSelection = new StringSelection(payload);
		    Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		    clpbrd.setContents(stringSelection, null);
		    mTrayIcon.showNotification(EStrings.lbl_app_name.toString(),
			    String.format(EStrings.msg_data_copied.toString(), title));
		}
	    } catch (GeneralSecurityException e) {
		logger.log(Level.WARNING, e.getMessage(), e);
	    }
	}
    }
}
