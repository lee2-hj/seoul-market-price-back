package com.seoul.market.seoulmarketprice.auth.crypto;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

public final class MemberDataCrypto {
    private static final String PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static volatile String configuredKey;

    private MemberDataCrypto() {}

    static void configureKey(String encryptionKey) {
        configuredKey = encryptionKey;
    }

    public static String encrypt(String field, String value) {
        if (value == null || value.isEmpty() || value.startsWith(PREFIX)) return value;
        try {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128, iv));
            cipher.updateAAD(field.getBytes(StandardCharsets.UTF_8));
            byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("회원 개인정보를 암호화할 수 없습니다.", exception);
        }
    }

    public static String decrypt(String field, String value) {
        if (value == null || !value.startsWith(PREFIX)) return value;
        try {
            byte[] payload = Base64.getDecoder().decode(value.substring(PREFIX.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(128, Arrays.copyOf(payload, IV_LENGTH)));
            cipher.updateAAD(field.getBytes(StandardCharsets.UTF_8));
            return new String(cipher.doFinal(Arrays.copyOfRange(payload, IV_LENGTH, payload.length)), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("회원 개인정보를 복호화할 수 없습니다.", exception);
        }
    }

    public static String searchHash(String field, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key().getEncoded(), "HmacSHA256"));
            byte[] result = mac.doFinal((field + ':' + normalize(field, value)).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(result);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("회원 개인정보 검색값을 생성할 수 없습니다.", exception);
        }
    }

    private static String normalize(String field, String value) {
        if ("phone".equals(field)) {
            String digits = value.replaceAll("[^0-9]", "");
            return digits.startsWith("82") ? "0" + digits.substring(2) : digits;
        }
        return "name".equals(field) ? value.trim() : value;
    }

    private static SecretKeySpec key() {
        String encoded = configuredKey;
        if (encoded == null || encoded.isBlank()) throw new IllegalStateException("app.member-data.encryption-key 설정값이 필요합니다.");
        try {
            byte[] bytes = Base64.getDecoder().decode(encoded);
            if (bytes.length != 32) throw new IllegalStateException("app.member-data.encryption-key는 Base64로 인코딩한 32바이트 키여야 합니다.");
            return new SecretKeySpec(bytes, "AES");
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("app.member-data.encryption-key는 Base64 형식이어야 합니다.", exception);
        }
    }
}
