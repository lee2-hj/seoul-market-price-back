package com.seoul.market.seoulmarketprice.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 암호화 관련 설정 클래스.
 *
 * <p>
 * 회원 비밀번호를 평문으로 비교하지 않고,
 * BCrypt 방식으로 암호화된 DB 비밀번호와 비교하기 위해 사용한다.
 * </p>
 *
 * <p>
 * 회원가입 기능은 현재 구현하지 않지만,
 * 로그인 시 입력받은 평문 비밀번호와
 * DB에 저장된 암호화 비밀번호를 비교할 때 필요하다.
 * </p>
 */
@Configuration
public class PasswordConfig {

    /**
     * Spring이 관리하는 PasswordEncoder 객체를 등록한다.
     *
     * <p>
     * 다른 클래스에서는 직접 BCryptPasswordEncoder를 생성하지 않고,
     * 생성자 주입을 통해 PasswordEncoder를 사용한다.
     * </p>
     *
     * @return BCrypt 방식의 비밀번호 인코더
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}