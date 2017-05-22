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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * AES cipher
 *
 * @author Alexander Shulgin
 */
public class AESCipher {
    private static final String AES = "AES";
    private static final String CIPHER_MODE = "AES/GCM/NoPadding";
    private static final int KEY_SIZE = 128;
    private SecretKey mAesKey;
    private boolean mIsGenerated;

    /**
     * Initialize.
     *
     * @param aesKeyBytes
     *            an aes key.
     */
    public AESCipher(byte[] aesKeyBytes) throws GeneralSecurityException {
	if (aesKeyBytes != null) {
	    mAesKey = new SecretKeySpec(aesKeyBytes, AES);
	    mIsGenerated = false;
	} else {
	    KeyGenerator keygen = KeyGenerator.getInstance(AES);
	    keygen.init(KEY_SIZE);
	    mAesKey = keygen.generateKey();
	    mIsGenerated = true;
	}
    }

    /**
     * Cipher value using public key stored in keystore.
     *
     * @param payload
     *            value to encrypt.
     * @return encrypted value
     * @throws GeneralSecurityException
     */
    public byte[] cipher(byte[] payload) throws GeneralSecurityException {
	Cipher cipher = Cipher.getInstance(CIPHER_MODE);
	cipher.init(Cipher.ENCRYPT_MODE, mAesKey);
	byte[] encrypted = cipher.doFinal(payload);
	byte[] iv = cipher.getIV();
	int tLen = (encrypted.length - payload.length) * 8;

	try {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(baos);
	    dos.writeInt(tLen);
	    dos.writeInt(iv.length);
	    dos.write(iv);
	    dos.writeInt(encrypted.length);
	    dos.write(encrypted);
	    dos.close();
	    return baos.toByteArray();
	} catch (IOException e) {
	    throw new GeneralSecurityException(e.getMessage(), e);
	}
    }

    /**
     * Decipher value using private key stored in keystore.
     *
     * @param payload
     *            encrypted value
     * @return decrypted vale
     * @throws GeneralSecurityException
     */
    public byte[] decipher(byte[] payload) throws GeneralSecurityException {
	int tLen;
	byte[] iv;
	byte[] encrypted;
	try {
	    ByteArrayInputStream bais = new ByteArrayInputStream(payload);
	    DataInputStream dis = new DataInputStream(bais);
	    tLen = dis.readInt();
	    int size = dis.readInt();
	    iv = new byte[size];
	    dis.readFully(iv);
	    size = dis.readInt();
	    encrypted = new byte[size];
	    dis.read(encrypted);
	} catch (IOException e) {
	    throw new GeneralSecurityException(e.getMessage(), e);
	}
	GCMParameterSpec ivSpec = new GCMParameterSpec(tLen, iv);
	Cipher cipher = Cipher.getInstance(CIPHER_MODE, "SunJCE");
	cipher.init(Cipher.DECRYPT_MODE, mAesKey, ivSpec);
	return cipher.doFinal(encrypted);
    }
    
    public byte[] getKey() {
	return mAesKey.getEncoded();
    }
    
    public boolean isGenerated() {
	return mIsGenerated;
    }
}
