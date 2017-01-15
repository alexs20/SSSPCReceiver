package com.wolandsoft.sss.pcr.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;

public class AESIVCipher extends AESCipher {

    public AESIVCipher(byte[] aesKeyBytes) throws GeneralSecurityException {
	super(aesKeyBytes);
    }

    public byte[] cipher(byte[] data) throws GeneralSecurityException {
	byte[] iv = generateIV();
	byte[] chipered = cipher(iv, data);
	try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos)) {
	    dos.writeInt(iv.length);
	    dos.write(iv);
	    dos.writeInt(chipered.length);
	    dos.write(chipered);
	    dos.close();
	    return baos.toByteArray();
	} catch (IOException e) {
	    throw new GeneralSecurityException(e.getMessage(), e);
	}
    }

    public byte[] decipher(byte[] data) throws GeneralSecurityException {
	try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
		DataInputStream dis = new DataInputStream(bais)) {
	    int size = dis.readInt();
	    byte[] iv = new byte[size];
	    dis.readFully(iv);
	    size = dis.readInt();
	    byte[] chipered = new byte[size];
	    dis.readFully(chipered);
	    return decipher(iv, chipered);
	} catch (IOException e) {
	    throw new GeneralSecurityException(e.getMessage(), e);
	}
    }
}
