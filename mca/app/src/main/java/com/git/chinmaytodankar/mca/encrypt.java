package com.git.chinmaytodankar.mca;

import java.math.BigInteger;
import java.security.MessageDigest;

public class encrypt {
    public static String encryptPass(String pass) throws Exception {
        byte[] Input = pass.getBytes();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(Input);
        BigInteger encryptedPass = null;

        encryptedPass = new BigInteger(1,md5.digest());

        return encryptedPass.toString(16);
    }
}
