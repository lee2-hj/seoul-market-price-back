package com.seoul.market.seoulmarketprice.auth.service;

import com.seoul.market.seoulmarketprice.auth.dto.request.AdminLoginRequest;
import com.seoul.market.seoulmarketprice.auth.dto.response.AdminLoginResponse;
import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.entity.Role;
import com.seoul.market.seoulmarketprice.auth.repository.AdminRepository;
import com.seoul.market.seoulmarketprice.security.jwt.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 인증 기능을 구현하는 서비스이다.
 *
 * <p>
 * 관리자 로그인 요청을 받아 아이디와 비밀번호를 검증하고,
 * 인증에 성공하면 관리자용 Access Token을 발급한다.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class AdminAuthServiceImpl implements AdminAuthService {

    /**
     * 관리자 정보를 조회하는 Repository.
     */
    private final AdminRepository adminRepository;

    /**
     * BCrypt 비밀번호 검증을 수행한다.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * JWT 생성 및 검증을 담당한다.
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 생성자 주입.
     */
    public AdminAuthServiceImpl(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 관리자 로그인을 처리한다.
     *
     * @param request 관리자 로그인 요청
     * @return Access Token과 관리자 정보
     */
    @Override
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {

        // 관리자 아이디로 계정을 조회한다.
        Admin admin = adminRepository
                .findByAdminId(request.adminId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "관리자 아이디 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        // 비밀번호가 존재하는지 확인한다.
        if (!admin.hasPassword()) {
            throw new IllegalStateException(
                    "관리자 비밀번호 정보가 존재하지 않습니다."
            );
        }

        // 입력한 비밀번호와 DB의 BCrypt 비밀번호를 비교한다.
        boolean passwordMatches =
                passwordEncoder.matches(
                        request.password(),
                        admin.getPassword()
                );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "관리자 아이디 또는 비밀번호가 올바르지 않습니다."
            );
        }

        // 관리자용 Access Token을 생성한다.
        String accessToken =
                jwtTokenProvider.createAccessToken(
                        admin.getId(),
                        admin.getAdminId(),
                        Role.ADMIN
                );

        // 로그인 결과를 반환한다.
        return new AdminLoginResponse(
                accessToken,
                admin.getAdminId(),
                admin.getName()
        );
    }
}