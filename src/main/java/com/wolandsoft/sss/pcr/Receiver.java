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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.EnumMap;
import java.util.Map;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.swing.JDialog;
import javax.swing.JOptionPane;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.wolandsoft.sss.pcr.core.Keystore;
import com.wolandsoft.sss.pcr.core.MulticastServer;
import com.wolandsoft.sss.pcr.core.MulticastServer.MulticastServerDataListener;
import com.wolandsoft.sss.pcr.ui.EStrings;
import com.wolandsoft.sss.pcr.ui.QRCodePanel;
import com.wolandsoft.sss.pcr.ui.TrayIconUI;
import com.wolandsoft.sss.pcr.ui.TrayIconUI.TrayIconListener;

public class Receiver implements TrayIconListener, MulticastServerDataListener {

    public static void main(String[] args) throws InterruptedException {
	Receiver receiver = new Receiver();
	receiver.run();
    }

    private static final int CMD_PING = 0;
    private static final int CMD_DATA = 1;

    private TrayIconUI mTrayIcon;
    private Keystore mKeystore;
    private MulticastServer mServer;
    private JDialog mQRCodeDialog;
    private boolean mPause = false;

    public Receiver() {
	mKeystore = new Keystore();
	mTrayIcon = new TrayIconUI(this);
	mServer = new MulticastServer(this);
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
	    // store port
	    dos.writeInt(mServer.getPort());
	    // store key
	    byte[] key = mKeystore.getKey();
	    dos.writeInt(key.length);
	    dos.write(key, 0, key.length);
	    dos.close();
	    byte[] payload = baos.toByteArray();
	    Checksum checksum = new CRC32();
	    checksum.update(payload, 0, payload.length);
	    long checksumValue = checksum.getValue();

	    baos = new ByteArrayOutputStream();
	    dos = new DataOutputStream(baos);
	    // store payload
	    dos.writeInt(payload.length);
	    dos.write(payload, 0, payload.length);
	    // store crc
	    dos.writeLong(checksumValue);
	    dos.close();
	    byte[] data = baos.toByteArray();
	    // to base64
	    String strToEncode = Base64.getEncoder().encodeToString(data);

	    Dimension Size = Toolkit.getDefaultToolkit().getScreenSize();
	    int size = (int) Math.min(Size.getWidth() / 2, Size.getHeight() / 2);
	    Map<EncodeHintType, Object> hintMap = new EnumMap<EncodeHintType, Object>(EncodeHintType.class);
	    hintMap.put(EncodeHintType.CHARACTER_SET, "UTF-8");

	    // Now with zxing version 3.2.1 you could change border size (white
	    // border size to just 1)
	    hintMap.put(EncodeHintType.MARGIN, 1); /* default = 4 */
	    hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.L);

	    QRCodeWriter qrCodeWriter = new QRCodeWriter();
	    BitMatrix byteMatrix = qrCodeWriter.encode(strToEncode, BarcodeFormat.QR_CODE, size, size, hintMap);
	    int CrunchifyWidth = byteMatrix.getWidth();
	    BufferedImage image = new BufferedImage(CrunchifyWidth, CrunchifyWidth, BufferedImage.TYPE_INT_RGB);
	    image.createGraphics();

	    Graphics2D graphics = (Graphics2D) image.getGraphics();
	    graphics.setColor(Color.WHITE);
	    graphics.fillRect(0, 0, CrunchifyWidth, CrunchifyWidth);
	    graphics.setColor(Color.BLACK);

	    for (int i = 0; i < CrunchifyWidth; i++) {
		for (int j = 0; j < CrunchifyWidth; j++) {
		    if (byteMatrix.get(i, j)) {
			graphics.fillRect(i, j, 1, 1);
		    }
		}
	    }

	    QRCodePanel imgPanel = new QRCodePanel(image);
	    mQRCodeDialog = new JDialog((Frame) null, EStrings.lbl_app_name.toString(), true);
	    mQRCodeDialog.setResizable(false);
	    mQRCodeDialog.getContentPane().add(imgPanel);
	    imgPanel.setPreferredSize(new Dimension(size, size));
	    mQRCodeDialog.pack();
	    mQRCodeDialog.setLocation(new Double((Size.getWidth() / 2) - (mQRCodeDialog.getWidth() / 2)).intValue(),
		    new Double((Size.getHeight() / 2) - (mQRCodeDialog.getHeight() / 2)).intValue());
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
	    byte[] plain = mKeystore.decipher(data);
	    // first byte = command
	    // rest = payload
	    switch (plain[0]) {
	    case CMD_PING:
		hideQRCode();
		mTrayIcon.showNotification(EStrings.lbl_app_name.toString(), EStrings.msg_pair_completed.toString());
		break;
	    case CMD_DATA:
	    default:
		try {
		    ByteArrayInputStream bais = new ByteArrayInputStream(plain, 1, plain.length - 1);
		    DataInputStream dis = new DataInputStream(bais);
		    int size = dis.readInt();
		    byte[] strData = new byte[size];
		    dis.readFully(strData);
		    String title = new String(strData, "UTF-8");
		    size = dis.readInt();
		    strData = new byte[size];
		    dis.readFully(strData);
		    String str = new String(strData, "UTF-8");
		    StringSelection stringSelection = new StringSelection(str);
		    Clipboard clpbrd = Toolkit.getDefaultToolkit().getSystemClipboard();
		    clpbrd.setContents(stringSelection, null);
		    mTrayIcon.showNotification(EStrings.lbl_app_name.toString(),
			    String.format(EStrings.msg_data_copied.toString(), title));
		    break;
		} catch (Exception ignore) {
		}
	    }
	}
    }
}
