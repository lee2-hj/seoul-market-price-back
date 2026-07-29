package com.seoul.market.seoulmarketprice.token.service;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * 토큰 원문을 SHA-256 해시값으로 변환하는 클래스이다.
 *
 * <p>
 * Refresh Token 원문을 DB에 그대로 저장하지 않고,
 * SHA-256으로 변환한 해시값만 저장하기 위해 사용한다.
 * </p>
 *
 * <p>
 * 해시는 단방향 변환이므로,
 * 해시값을 이용해 원래 Refresh Token을 복원할 수 없다.
 * </p>
 */
@Component
public class TokenHashService {

    /**
     * 전달받은 토큰을 SHA-256 해시값으로 변환한다.
     *
     * <p>
     * 변환된 값은 64자리의 16진수 문자열이 된다.
     * 따라서 tb_refresh_token.token_hash 컬럼의 길이를
     * 64자로 설정했다.
     * </p>
     *
     * @param token 해시로 변환할 Refresh Token 원문
     * @return SHA-256으로 변환된 64자리 문자열
     */
    public String hash(String token) {

        /*
         * null 또는 빈 문자열이 전달되면
         * 정상적인 토큰으로 처리할 수 없으므로 예외를 발생시킨다.
         */
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException(
                    "해시로 변환할 토큰이 비어 있습니다."
            );
        }

        try {
            /*
             * SHA-256 해시 알고리즘을 사용하는 MessageDigest 객체를 생성한다.
             */
            MessageDigest messageDigest =
                    MessageDigest.getInstance("SHA-256");

            /*
             * 문자열 토큰을 UTF-8 바이트 배열로 변환한 뒤
             * SHA-256 해시값을 계산한다.
             */
            byte[] hashBytes = messageDigest.digest(
                    token.getBytes(StandardCharsets.UTF_8)
            );

            /*
             * 해시 결과는 byte 배열이므로
             * DB에 저장하기 쉬운 16진수 문자열로 변환한다.
             */
            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException exception) {

            /*
             * SHA-256은 일반적인 Java 실행 환경에서 반드시 지원된다.
             * 따라서 이 예외가 발생하면 애플리케이션 설정 문제로 간주한다.
             */
            throw new IllegalStateException(
                    "SHA-256 해시 알고리즘을 사용할 수 없습니다.",
                    exception
            );
        }
    }
}