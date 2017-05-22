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
package com.wolandsoft.sss.pcr.security;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

import com.wolandsoft.sss.pcr.core.PrefProperties;

/**
 * Base64 wrapped AES cipher
 *
 * @author Alexander Shulgin
 */

public class TextCipher extends AESCipher {

    private static final String PREF_KEY = "key";

    public TextCipher() throws GeneralSecurityException {
	super(getAesKey());
	if (isGenerated()) {
	    PrefProperties prefs = PrefProperties.getInstance();
	    prefs.put(PREF_KEY, Base64.getEncoder().encodeToString(getKey()));
	    prefs.commit();
	}
    }

    public String cipher(String text) {
	try {
	    byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
	    byte[] textCiphered = cipher(textBytes);
	    return Base64.getEncoder().encodeToString(textCiphered);
	} catch (GeneralSecurityException e) {
	    throw new RuntimeException(e.getMessage(), e);
	}
    }

    public String decipher(String secret) {
	try {
	    byte[] textCiphered = Base64.getDecoder().decode(secret);
	    byte[] textBytes = decipher(textCiphered);
	    return new String(textBytes, StandardCharsets.UTF_8);
	} catch (GeneralSecurityException e) {
	    throw new RuntimeException(e.getMessage(), e);
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
