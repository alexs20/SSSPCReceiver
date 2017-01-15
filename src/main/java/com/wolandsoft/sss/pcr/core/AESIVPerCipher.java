package com.wolandsoft.sss.pcr.core;

import java.security.GeneralSecurityException;
import java.util.Base64;

public class AESIVPerCipher extends AESIVCipher {
    private static final String PREF_KEY = "key";

    public AESIVPerCipher() throws GeneralSecurityException {
	super(getAesKey());
	if (isGenerated()) {
	    PrefProperties prefs = PrefProperties.getInstance();
	    prefs.put(PREF_KEY, Base64.getEncoder().encodeToString(getKey()));
	    prefs.commit();
	}
    }

    private static byte[] getAesKey() {
	PrefProperties prefs = PrefProperties.getInstance();
	String aesKeyB64 = prefs.getProperty(PREF_KEY);
	if (aesKeyB64 != null) {
	    return Base64.getDecoder().decode(aesKeyB64);
	}
	return null;
    }
}
