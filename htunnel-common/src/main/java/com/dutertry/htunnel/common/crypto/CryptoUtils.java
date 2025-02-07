/*
 * htunnel - A simple HTTP tunnel 
 * https://github.com/nicolas-dutertry/htunnel
 * 
 * Written by Nicolas Dutertry.
 * 
 * This file is provided under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *    http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 */
package com.dutertry.htunnel.common.crypto;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.jcajce.provider.digest.MD5;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;

public class CryptoUtils {
    private static final String AES_KEY_ALGO = "AES";
    private static final String AES_ENCRYPT_ALGO = "AES/CBC/PKCS5Padding";
    private static final int IV_LENGTH_BYTE = 16;
    private static final String RSA_CRYPT_ALG = "RSA/ECB/PKCS1Padding";
    
    public static SecretKey generateAESKey() throws NoSuchAlgorithmException {
        KeyGenerator keyGen = KeyGenerator.getInstance(AES_KEY_ALGO);
        keyGen.init(256);
        return keyGen.generateKey();
    }
    
    public static String encodeAESKey(SecretKey key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }
    
    public static SecretKey decodeAESKey(String encodedKey) {
        return new SecretKeySpec(Base64.getDecoder().decode(encodedKey), AES_KEY_ALGO);
    }
    
    public static byte[] encryptAES(byte[] pText, SecretKey secret) throws GeneralSecurityException  {
        Cipher cipher = Cipher.getInstance(AES_ENCRYPT_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        byte[] iv = cipher.getIV();
        byte[] encryptedText = cipher.doFinal(pText);
        
        ByteBuffer bb = ByteBuffer.allocate(iv.length + encryptedText.length);
        bb.put(iv);
        bb.put(encryptedText);
        return bb.array();
    }
    
    public static byte[] decryptAES(byte[] cText, SecretKey secret) throws GeneralSecurityException {
        ByteBuffer bb = ByteBuffer.wrap(cText);

        byte[] iv = new byte[IV_LENGTH_BYTE];
        bb.get(iv);

        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);
        
        Cipher cipher = Cipher.getInstance(AES_ENCRYPT_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(iv));
        return cipher.doFinal(cipherText);
    }
    
    public static PrivateKey readRSAPrivateKey(String keyPath) throws IOException {
        PrivateKeyInfo privateKeyInfo;
        try(FileReader reader = new FileReader(keyPath);
                PEMParser pemParser = new PEMParser(reader)) {
            Object obj = pemParser.readObject();
            if (obj instanceof PEMKeyPair) {
                privateKeyInfo = ((PEMKeyPair) obj).getPrivateKeyInfo();
            } else {
                privateKeyInfo = PrivateKeyInfo.getInstance(pemParser.readObject());
            }
        }
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        return converter.getPrivateKey(privateKeyInfo);
    }
    
    public static PublicKey readRSAPublicKey(String keyPath) throws IOException {
        SubjectPublicKeyInfo subjectPublicKeyInfo;
        try(FileReader reader = new FileReader(keyPath);
                PEMParser pemParser = new PEMParser(reader)) {
            subjectPublicKeyInfo = SubjectPublicKeyInfo.getInstance(pemParser.readObject());
        }
        JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
        return converter.getPublicKey(subjectPublicKeyInfo);
    }
    
    public static byte[] decryptRSA(byte[] crypted, PublicKey publicKey) throws GeneralSecurityException {
        byte[] decoded = Base64.getDecoder().decode(crypted);
        Cipher cipher = Cipher.getInstance(RSA_CRYPT_ALG);
        cipher.init(Cipher.DECRYPT_MODE, publicKey);
        return cipher.doFinal(decoded);
    }
    
    public static byte[] encryptRSA(byte[] decrypted, PrivateKey privateKey) throws GeneralSecurityException {
        Cipher cipher = Cipher.getInstance(RSA_CRYPT_ALG);
        cipher.init(Cipher.ENCRYPT_MODE, privateKey);
        byte[] crypted = cipher.doFinal(decrypted);
        return Base64.getEncoder().encode(crypted);
    }

    public static String md5Digest(Path file) throws IOException {
        MD5.Digest digest = new MD5.Digest();
        byte[] buff = new byte[8192];
        int len;
        try (InputStream inputStream = Files.newInputStream(file)) {
            while ((len = inputStream.read(buff)) != -1) {
                digest.update(buff, 0, len);
            }
            return Base64.getEncoder().encodeToString(digest.digest());
        }
    }
}
