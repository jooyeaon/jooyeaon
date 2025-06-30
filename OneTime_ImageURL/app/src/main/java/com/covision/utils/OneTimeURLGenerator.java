package com.covision.utils;

import com.eatthepath.otp.TimeBasedOneTimePasswordGenerator;
import com.google.api.client.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import javax.crypto.spec.SecretKeySpec;

public class OneTimeURLGenerator {
    //Encryption CheckSum
    final String ENC_CHECKSUM = "COTU";	//Covision OneTimeURL

    //TOTP ALGORITHM
    final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
    //TOTP Password Length
    final int TOTP_PASSWORD_LEN = 6;
    //TOTP Common date format
    final String HMAC_SHA256_KEY_DATE_FORMAT = "yyyyMMdd";
    //TOTP Common Key(Today by HMAC_SHA256_KEY_DATE_FORMAT)
    String HMAC_SHA256_KEY;
    //TOTP Duration Second(default=1Hour)
    int TOTP_DURATION_SEC = 60*60;

    public OneTimeURLGenerator() {
        setHMAC_SHA256_KEY();
    }

    /* TOPT 시간 조정시 사용합니다.
     * OTP 키 지속시간이 줄어 들수로 Mismatch가 빈번하게 일어날 수 있습니다. */
    public OneTimeURLGenerator(int TOTP_DURATION_SEC) {
        this.TOTP_DURATION_SEC = TOTP_DURATION_SEC;
        setHMAC_SHA256_KEY();
    }

    public String encode(String plainText) throws Exception {
        String dec = ENC_CHECKSUM+plainText;
        String enc = XOREncrypt(dec, createTOTP());
        return Base64.encodeBase64URLSafeString(enc.getBytes());
    }

    public String decode(String encryptionText) throws Exception {
        String enc = new String(Base64.decodeBase64(encryptionText));
        String dec = XORDecrypt(enc, createTOTP());
        if(!dec.startsWith(ENC_CHECKSUM)) {
            throw new Exception("Checksum mismatch = " + dec.substring(0, ENC_CHECKSUM.length()));
        }
        return  dec.substring(ENC_CHECKSUM.length());
    }

    private void setHMAC_SHA256_KEY() {
        SimpleDateFormat sdf = new SimpleDateFormat(HMAC_SHA256_KEY_DATE_FORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String todayUTC = ENC_CHECKSUM + "UTC" + sdf.format(new Date());
        this.HMAC_SHA256_KEY = new String(Base64.encodeBase64URLSafeString(todayUTC.getBytes()));
    }

    private String createTOTP() throws Exception {
        TimeBasedOneTimePasswordGenerator totp = null;
        Instant timestamp = null;
        Key key = null;

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            Instant epochSeconds = Instant.now();
            totp = new TimeBasedOneTimePasswordGenerator(Duration.ofSeconds(TOTP_DURATION_SEC), TOTP_PASSWORD_LEN, HMAC_SHA256_ALGORITHM);

            timestamp = Instant.ofEpochSecond(epochSeconds.getEpochSecond());
            key = new SecretKeySpec(HMAC_SHA256_KEY.getBytes(StandardCharsets.US_ASCII), HMAC_SHA256_ALGORITHM);
        }

        return String.format("%06d", totp.generateOneTimePassword(key, timestamp));
    }

    private String XOREncrypt(String input, String key) {
        byte[] bytes = input.getBytes();
        byte[] keyBytes = key.getBytes();
        byte[] encryptedBytes = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            encryptedBytes[i] = (byte) (bytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return new String(encryptedBytes);
    }

    private static String XORDecrypt(String encrypted, String key) {
        byte[] encryptedBytes = encrypted.getBytes();
        byte[] keyBytes = key.getBytes();
        byte[] decryptedBytes = new byte[encryptedBytes.length];
        for (int i = 0; i < encryptedBytes.length; i++) {
            decryptedBytes[i] = (byte) (encryptedBytes[i] ^ keyBytes[i % keyBytes.length]);
        }
        return new String(decryptedBytes);
    }
}
