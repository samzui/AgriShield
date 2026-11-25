package com.examly.springapp.util;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
@Converter(autoApply=false)
public class Aes256Converter implements AttributeConverter<String,String> {
    private static final int IV_LENGTH=12;
    private static final SecureRandom RANDOM=new SecureRandom();
    private static final byte[] KEY=loadKey();
    private static byte[] loadKey() {
        String secret=System.getenv().getOrDefault("AES_SECRET","change-me-to-a-32-byte-aes-key!!");
        byte[] raw=secret.getBytes(StandardCharsets.UTF_8);
        if(raw.length<32)throw new IllegalStateException("AES_SECRET must contain at least 32 bytes");
        return java.util.Arrays.copyOf(raw,32);
    }
    @Override public String convertToDatabaseColumn(String value) {
        if(value==null)return null;
        try {
            byte[]iv=new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(KEY,"AES"),new GCMParameterSpec(128,iv));
            byte[]enc=c.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[]all=new byte[iv.length+enc.length];
            System.arraycopy(iv,0,all,0,iv.length);
            System.arraycopy(enc,0,all,iv.length,enc.length);
            return Base64.getEncoder().encodeToString(all);
        }catch(Exception e) {
            throw new IllegalStateException("Unable to encrypt sensitive value",e);
        }
    }
    @Override public String convertToEntityAttribute(String value) {
        if(value==null)return null;
        try {
            byte[]all=Base64.getDecoder().decode(value);
            byte[]iv=java.util.Arrays.copyOfRange(all,0,IV_LENGTH);
            byte[]enc=java.util.Arrays.copyOfRange(all,IV_LENGTH,all.length);
            Cipher c=Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE,new SecretKeySpec(KEY,"AES"),new GCMParameterSpec(128,iv));
            return new String(c.doFinal(enc),StandardCharsets.UTF_8);
        }catch(Exception e) {
            throw new IllegalStateException("Unable to decrypt sensitive value",e);
        }
    }
}
