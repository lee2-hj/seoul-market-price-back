package com.seoul.market.seoulmarketprice.auth.service;

import com.seoul.market.seoulmarketprice.auth.dto.request.AdminLoginRequest;
import com.seoul.market.seoulmarketprice.auth.dto.response.AdminLoginResponse;
import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.entity.Role;
import com.seoul.market.seoulmarketprice.auth.repository.AdminRepository;
import com.seoul.market.seoulmarketprice.security.jwt.JwtTokenProvider;
import com.seoul.market.seoulmarketprice.token.service.AdminRefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 인증 기능을 구현하는 서비스이다.
 *
 * <p>

 * 관리자 로그인, Refresh Token Rotation,
 * 로그아웃 기능을 처리한다.
 * </p>
 *
 * <p>
 * 일반 회원 인증과 완전히 분리된 관리자 전용
 * Refresh Token 저장소를 사용한다.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class AdminAuthServiceImpl
        implements AdminAuthService {

    /**
     * 관리자 정보를 조회한다.
     */
    private final AdminRepository adminRepository;

    /**
     * BCrypt 비밀번호 검증을 수행한다.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * JWT 생성, 검증 및 정보 추출을 담당한다.
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 관리자 Refresh Token의 저장과 폐기를 담당한다.
     */
    private final AdminRefreshTokenService
            adminRefreshTokenService;

    /**
     * 생성자 주입을 사용한다.
     *
     * @param adminRepository         관리자 Repository
     * @param passwordEncoder         비밀번호 검증 객체
     * @param jwtTokenProvider        JWT 처리 객체
     * @param adminRefreshTokenService 관리자 토큰 서비스
     */
    public AdminAuthServiceImpl(
            AdminRepository adminRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            AdminRefreshTokenService adminRefreshTokenService
    ) {
        this.adminRepository = adminRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.adminRefreshTokenService =
                adminRefreshTokenService;
    }

    /**
     * 관리자 로그인을 처리한다.
     *
     * <ol>
     *     <li>삭제되지 않은 관리자 계정을 조회한다.</li>
     *     <li>비밀번호를 검증한다.</li>
     *     <li>관리자 Access Token을 생성한다.</li>
     *     <li>관리자 Refresh Token을 생성한다.</li>
     *     <li>Refresh Token의 해시값만 DB에 저장한다.</li>
     * </ol>
     *
     * @param request 관리자 로그인 요청
     * @return 관리자 로그인 응답과 Refresh Token 원문
     */
    @Override
    @Transactional
    public AdminLoginResult login(
            AdminLoginRequest request
    ) {
        /*
         * 삭제되지 않은 관리자만 조회한다.
         * 존재하지 않거나 삭제된 관리자에게 동일한 오류를 반환한다.
         */
        Admin admin = adminRepository
                .findActiveByAdminId(request.adminId())
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "관리자 아이디 또는 비밀번호가 올바르지 않습니다."
                        )
                );

        // 관리자 비밀번호가 존재하는지 확인한다.
        if (!admin.hasPassword()) {
            throw new IllegalStateException(
                    "관리자 비밀번호 정보가 존재하지 않습니다."
            );
        }

        // 평문 비밀번호와 BCrypt 암호문을 비교한다.
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

        /*
         * 관리자 API 인증에 사용할 Access Token을 생성한다.
         * DB에 저장된 실제 권한(admin.getRole())을 사용해야
         * MASTER 관리자가 ADMIN으로 강등되어 인증되는 것을 막는다.
         * 하드코딩된 Role.ADMIN을 사용하면 MASTER 계정도 항상
         * ADMIN 권한 토큰을 발급받아, MASTER 전용 기능(다른 관리자의
         * 활성 메뉴 관리 등)에서 서비스 레벨 IDOR 검증에 막혀버린다.
         */
        String accessToken =
                jwtTokenProvider.createAccessToken(
                        admin.getId(),
                        admin.getAdminId(),
                        admin.getRole()
                );

        // Access Token 재발급에 사용할 Refresh Token을 생성한다.
        String refreshToken =
                jwtTokenProvider.createRefreshToken(
                        admin.getId(),
                        Role.ADMIN
                );

        /*
         * Refresh Token 원문은 저장하지 않고,
         * SHA-256 해시값만 관리자 토큰 테이블에 저장한다.
         */
        adminRefreshTokenService.save(
                admin,
                refreshToken
        );

        // 클라이언트 응답에 사용할 관리자 정보를 생성한다.
        AdminLoginResponse response =
                new AdminLoginResponse(
                        accessToken,
                        admin.getAdminId(),
                        admin.getName()
                );

        /*
         * 응답 Body와 Refresh Token 쿠키를 만들 수 있도록
         * Controller에 내부 로그인 결과를 전달한다.
         */
        return new AdminLoginResult(
                response,
                refreshToken
        );
    }

    /**
     * 관리자 Refresh Token Rotation을 처리한다.
     *
     * <ol>
     *     <li>JWT의 서명과 만료 여부를 검증한다.</li>
     *     <li>Refresh Token인지 확인한다.</li>
     *     <li>ADMIN 권한의 토큰인지 확인한다.</li>
     *     <li>DB에서 해시값이 일치하는 토큰을 조회한다.</li>
     *     <li>JWT의 관리자 PK와 DB의 관리자 PK를 비교한다.</li>
     *     <li>삭제되지 않은 관리자인지 확인한다.</li>
     *     <li>기존 Refresh Token을 폐기한다.</li>
     *     <li>새 Access/Refresh Token을 발급한다.</li>
     * </ol>
     *
     * @param rawRefreshToken 기존 관리자 Refresh Token
     * @return 새 Access Token과 새 Refresh Token
     */
    @Override
    @Transactional
    public TokenReissueResult reissue(
            String rawRefreshToken
    ) {
        // JWT의 서명과 만료 여부를 검증한다.
        if (!jwtTokenProvider.validateToken(rawRefreshToken)) {
            throw new IllegalArgumentException(
                    "유효하지 않은 관리자 Refresh Token입니다."
            );
        }

        // Access Token을 재발급 API에 사용할 수 없도록 한다.
        if (!jwtTokenProvider.isRefreshToken(rawRefreshToken)) {
            throw new IllegalArgumentException(
                    "관리자 Refresh Token이 아닙니다."
            );
        }

        // 일반 회원의 Refresh Token 사용을 차단한다.
        if (
                jwtTokenProvider.getRole(rawRefreshToken)
                        != Role.ADMIN
        ) {
            throw new IllegalArgumentException(
                    "관리자 권한의 Refresh Token이 아닙니다."
            );
        }

        // JWT subject에서 관리자 PK를 가져온다.
        Long tokenAdminId =
                jwtTokenProvider
                        .getMemberId(rawRefreshToken);

        Admin admin = adminRepository.findActiveByIdForTokenUpdate(tokenAdminId)
                .orElseThrow(() -> new IllegalStateException("사용할 수 없는 관리자 계정입니다."));
        adminRefreshTokenService.validate(admin, rawRefreshToken);

        // 새로운 관리자 Access Token을 생성한다. DB의 실제 권한을 반영한다(로그인과 동일한 이유).
        String newAccessToken =
                jwtTokenProvider.createAccessToken(
                        admin.getId(),
                        admin.getAdminId(),
                        admin.getRole()
                );

        // 새로운 관리자 Refresh Token을 생성한다.
        String newRefreshToken =
                jwtTokenProvider.createRefreshToken(
                        admin.getId(),
                        Role.ADMIN
                );

        // 새 Refresh Token의 SHA-256 해시값을 저장한다.
        adminRefreshTokenService.save(
                admin,
                newRefreshToken
        );

        return new TokenReissueResult(
                newAccessToken,
                newRefreshToken
        );
    }

    /**
     * 현재 기기의 관리자 로그아웃을 처리한다.
     *
     * @param rawRefreshToken 관리자 Refresh Token 원문
     */
    @Override
    @Transactional
    public void logout(String rawRefreshToken) {
        // 쿠키가 없으면 이미 로그아웃된 것으로 처리한다.
        if (
                rawRefreshToken == null
                        || rawRefreshToken.isBlank()
        ) {
            return;
        }

        // 유효하지 않거나 만료된 JWT도 로그아웃 상태로 처리한다.
        if (!jwtTokenProvider.validateToken(rawRefreshToken)) {
            return;
        }

        // Refresh Token이 아니면 관리자 토큰을 폐기하지 않는다.
        if (!jwtTokenProvider.isRefreshToken(rawRefreshToken)) {
            return;
        }

        // 일반 회원의 Refresh Token이면 처리하지 않는다.
        if (
                jwtTokenProvider.getRole(rawRefreshToken)
                        != Role.ADMIN
        ) {
            return;
        }

        Long adminId = jwtTokenProvider.getMemberId(rawRefreshToken);
        adminRepository.findActiveByIdForTokenUpdate(adminId)
                .ifPresent(admin -> {
                    try {
                        adminRefreshTokenService.revoke(admin, rawRefreshToken);
                    } catch (IllegalArgumentException ignored) {
                        // 다른 로그인으로 이미 교체된 토큰이면 로그아웃 상태로 처리한다.
                    }
                });
    }
}
