package com.wolandsoft.sss.pcr.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PrefProperties extends Properties {
    private static final long serialVersionUID = 4678973033864897460L;
    private static final String STORAGE_DIR = ".ssspcreceiver";
    private static final String STORAGE_FILE = ".properties";
    private static final Logger logger = Logger.getLogger(PrefProperties.class.getName());
    private File mStorageFile;
    private static PrefProperties _instance;

    public static PrefProperties getInstance() {
	if (_instance == null) {
	    _instance = new PrefProperties();
	}
	return _instance;
    }

    private PrefProperties() {
	super();
	File storagePath = new File(System.getProperty("user.home"), STORAGE_DIR);
	storagePath.mkdir();
	mStorageFile = new File(storagePath, STORAGE_FILE);
	if (mStorageFile.exists()) {
	    try (FileInputStream fis = new FileInputStream(mStorageFile)) {
		this.load(fis);
	    } catch (IOException e) {
		logger.log(Level.SEVERE, e.getMessage(), e);
	    }
	}
    }

    public void commit() {
	try (FileOutputStream fos = new FileOutputStream(mStorageFile)) {
	    this.store(fos, null);
	} catch (IOException e) {
	    logger.log(Level.SEVERE, e.getMessage(), e);
	}
    }
}
