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
package com.wolandsoft.sss.pcr.ui;

import java.awt.CheckboxMenuItem;
import java.awt.Image;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.image.BufferedImage;

import javax.imageio.ImageIO;

/**
 * System tray icon and it's menu.
 */
public class TrayIconUI {
    private TrayIcon mTrayIcon;
    private TrayIconListener mListener;

    public TrayIconUI(TrayIconListener listener) {
	mListener = listener;
	// Check the SystemTray support
	if (!SystemTray.isSupported()) {
	    throw new RuntimeException(EStrings.msg_system_tray_is_not_supported.toString());
	}

	try {
	    final PopupMenu popup = new PopupMenu();
	    BufferedImage trayIconImage = ImageIO.read(getClass().getResource("images/tray.png"));
	    int trayIconWidth = new TrayIcon(trayIconImage).getSize().width;
	    mTrayIcon = new TrayIcon(trayIconImage.getScaledInstance(trayIconWidth, -1, Image.SCALE_SMOOTH));
	    mTrayIcon.setImageAutoSize(true);
	    mTrayIcon.setToolTip(EStrings.lbl_app_name.toString());
	    final SystemTray tray = SystemTray.getSystemTray();

	    // Create a popup menu components
	    MenuItem showKeyItem = new MenuItem(EStrings.lbl_show_pair_key.toString());
	    MenuItem exitItem = new MenuItem(EStrings.lbl_exit.toString());
	    CheckboxMenuItem pauseItem = new CheckboxMenuItem(EStrings.lbl_pause.toString());

	    // Add components to popup menu
	    popup.add(pauseItem);
	    popup.add(showKeyItem);
	    popup.addSeparator();
	    popup.add(exitItem);

	    mTrayIcon.setPopupMenu(popup);
	    tray.add(mTrayIcon);

	    showKeyItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    mListener.onShowKey();
		}
	    });

	    pauseItem.addItemListener(new ItemListener() {
		public void itemStateChanged(ItemEvent e) {
		    mListener.onPause(e.getStateChange() == ItemEvent.SELECTED);
		}
	    });

	    exitItem.addActionListener(new ActionListener() {
		public void actionPerformed(ActionEvent e) {
		    tray.remove(mTrayIcon);
		    mListener.onExit();
		}
	    });

	} catch (Exception e) {
	    throw new RuntimeException(e.getMessage(), e);
	}
    }

    public void showNotification(String title, String info) {
	mTrayIcon.displayMessage(title, info, TrayIcon.MessageType.INFO);
    }

    public interface TrayIconListener {
	void onShowKey();
	void onPause(boolean state);
	void onExit();
    }
}
