package com.examly.springapp.util;
import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.*;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
@Service public class FileEncryptionService {
    private static final int IV=12;
    private static final SecureRandom RANDOM=new SecureRandom();
    private final byte[]key;
    public FileEncryptionService() {
        String secret=System.getenv().getOrDefault("AES_SECRET","change-me-to-a-32-byte-aes-key!!");
        byte[]raw=secret.getBytes(StandardCharsets.UTF_8);
        if(raw.length<32)throw new IllegalStateException("AES_SECRET must contain at least 32 bytes");
        key=Arrays.copyOf(raw,32);
    }
    public byte[] encrypt(byte[]plain) {
        try {
            byte[]iv=new byte[IV];
            RANDOM.nextBytes(iv);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));
            byte[]enc=c.doFinal(plain);
            byte[]all=new byte[iv.length+enc.length];
            System.arraycopy(iv,0,all,0,iv.length);
            System.arraycopy(enc,0,all,iv.length,enc.length);
            return all;
        }catch(Exception e) {
            throw new IllegalStateException("Unable to encrypt file",e);
        }
    }
    public byte[] decrypt(byte[]ciphertext) {
        try {
            byte[]iv=Arrays.copyOfRange(ciphertext,0,IV);
            byte[]enc=Arrays.copyOfRange(ciphertext,IV,ciphertext.length);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(key,"AES"),new GCMParameterSpec(128,iv));
            return c.doFinal(enc);
        }catch(Exception e) {
            throw new IllegalStateException("Unable to decrypt file",e);
        }
    }
}
