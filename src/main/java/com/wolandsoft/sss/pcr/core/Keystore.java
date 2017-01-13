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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;
import java.security.Security;
import java.util.Base64;
import java.util.NoSuchElementException;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class Keystore {
    private SecretKey mKey;
    private Cipher mCipher;

    public Keystore() {
	try {
	    mCipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
	} catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
	    throw new RuntimeException(e.getMessage(), e);
	}
	File storagePath = new File(System.getProperty("user.home"), ".sss");
	storagePath.mkdir();
	File filePath = new File(storagePath, "key");
	try (Scanner scanner = new Scanner(filePath)) {
	    String content = scanner.useDelimiter("\\Z").next();
	    // recreate key
	    byte[] aesKeyBuff = Base64.getDecoder().decode(content);
	    mKey = new SecretKeySpec(aesKeyBuff, "AES");
	} catch (FileNotFoundException | NoSuchElementException ignore) {

	} finally {
	    if (mKey == null) {
		filePath.delete();
		try (FileWriter fw = new FileWriter(filePath)) {
		    // Generate key
		    KeyGenerator keygen = KeyGenerator.getInstance("AES");
		    mKey = keygen.generateKey();
		    String aesB64 = Base64.getEncoder().encodeToString(mKey.getEncoded());
		    fw.write(aesB64);
		} catch (IOException | NoSuchAlgorithmException e) {
		    throw new RuntimeException(e.getMessage(), e);
		}
	    }
	}
	try {
	    mCipher.init(Cipher.DECRYPT_MODE, mKey);
	} catch (InvalidKeyException e) {
	    throw new RuntimeException(e.getMessage(), e);
	}

    }

    public String decipher(byte[] data) {
	try {
	    byte[] decipheredBuff = mCipher.doFinal(data);
	    return new String(decipheredBuff);
	} catch (IllegalBlockSizeException | BadPaddingException e) {
	    throw new RuntimeException(e.getMessage(), e);
	}
    }

    public String getKeyBase64() {
	return Base64.getEncoder().encodeToString(mKey.getEncoded());
    }
}
