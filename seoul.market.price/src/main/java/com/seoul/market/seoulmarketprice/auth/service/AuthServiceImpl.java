package com.seoul.market.seoulmarketprice.auth.service;

import com.seoul.market.seoulmarketprice.auth.dto.request.LoginRequest;
import com.seoul.market.seoulmarketprice.auth.dto.response.LoginResponse;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.repository.MemberRepository;
import com.seoul.market.seoulmarketprice.security.jwt.JwtTokenProvider;
import com.seoul.market.seoulmarketprice.token.domain.RefreshToken;
import com.seoul.market.seoulmarketprice.token.service.RefreshTokenService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.seoul.market.seoulmarketprice.auth.entity.Role;
/**
 * 인증 관련 비즈니스 로직을 실제로 구현하는 서비스이다.
 *
 * <p>
 * 일반 로그인과 Refresh Token Rotation을 처리한다.
 * </p>
 */
@Service
@Transactional(readOnly = true)
public class AuthServiceImpl implements AuthService {

    /**
     * 회원 조회를 담당한다.
     */
    private final MemberRepository memberRepository;

    /**
     * 평문 비밀번호와 BCrypt 비밀번호를 비교한다.
     */
    private final PasswordEncoder passwordEncoder;

    /**
     * Access Token과 Refresh Token을 생성하고 검증한다.
     */
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * Refresh Token의 저장, 조회, 폐기를 담당한다.
     */
    private final RefreshTokenService refreshTokenService;

    /**
     * 생성자 주입을 사용한다.
     */
    public AuthServiceImpl(
            MemberRepository memberRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenProvider jwtTokenProvider,
            RefreshTokenService refreshTokenService
    ) {
        this.memberRepository = memberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * 일반 로그인을 처리한다.
     *
     * <ol>
     *     <li>로그인 아이디로 회원을 조회한다.</li>
     *     <li>일반 로그인 회원인지 확인한다.</li>
     *     <li>비밀번호를 검증한다.</li>
     *     <li>Access Token과 Refresh Token을 발급한다.</li>
     *     <li>Refresh Token의 해시값을 DB에 저장한다.</li>
     * </ol>
     *
     * @param request 로그인 요청 정보
     * @return 로그인 응답과 Refresh Token 원문
     */
    @Override
    @Transactional
    public LoginResult login(LoginRequest request) {

        /*
         * 로그인 아이디로 회원을 조회한다.
         *
         * 회원 존재 여부와 비밀번호 오류를 같은 메시지로 처리하면
         * 공격자가 특정 아이디의 가입 여부를 알아내기 어려워진다.
         */
        Member member = memberRepository
                .findByUserId(request.userId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "아이디 또는 비밀번호가 올바르지 않습니다."
                ));

        /*
         * 소셜 로그인 회원은 일반 비밀번호 로그인을 할 수 없다.
         */
        if (!member.isLocalUser()) {
            throw new IllegalArgumentException(
                    "소셜 로그인으로 가입한 회원입니다."
            );
        }

        /*
         * 일반 로그인 회원인데 비밀번호가 없는 비정상 상태를 검사한다.
         */
        if (!member.hasPassword()) {
            throw new IllegalStateException(
                    "회원 비밀번호 정보가 존재하지 않습니다."
            );
        }

        /*
         * React가 보낸 평문 비밀번호와
         * DB의 BCrypt 비밀번호를 비교한다.
         */
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                member.getPassword()
        );

        if (!passwordMatches) {
            throw new IllegalArgumentException(
                    "아이디 또는 비밀번호가 올바르지 않습니다."
            );
        }

        /*
         * 인증 API 요청에 사용할 Access Token을 생성한다.
         */
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getId(),
                member.getUserId(),
                member.getName(),
                Role.USER
        );

        /*
         * Access Token 재발급에 사용할 Refresh Token을 생성한다.
         */
        String refreshToken = jwtTokenProvider.createRefreshToken(
                member.getId()
        );

        /*
         * Refresh Token 원문은 저장하지 않고
         * RefreshTokenService가 해시값으로 변환해 DB에 저장한다.
         */
        refreshTokenService.save(
                member,
                refreshToken
        );

        /*
         * 응답 본문에는 Access Token과 사용자 정보만 담는다.
         */
        LoginResponse loginResponse = new LoginResponse(
                accessToken,
                member.getId(),
                member.getUserId(),
                member.getName()
        );

        /*
         * Refresh Token은 Controller에서 HttpOnly 쿠키로 전달한다.
         */
        return new LoginResult(
                loginResponse,
                refreshToken
        );
    }

    /**
     * Refresh Token Rotation 방식으로 토큰을 재발급한다.
     *
     * <ol>
     *     <li>Refresh Token의 JWT 서명과 만료 여부를 검증한다.</li>
     *     <li>DB에서 해시값이 일치하는 토큰을 조회한다.</li>
     *     <li>JWT의 회원 번호와 DB 토큰의 회원 번호를 비교한다.</li>
     *     <li>기존 Refresh Token을 폐기한다.</li>
     *     <li>새 Access Token과 Refresh Token을 생성한다.</li>
     *     <li>새 Refresh Token의 해시값을 DB에 저장한다.</li>
     * </ol>
     *
     * @param rawRefreshToken 쿠키에서 전달받은 기존 Refresh Token
     * @return 새 Access Token과 새 Refresh Token
     */
    @Override
    @Transactional
    public TokenReissueResult reissue(String rawRefreshToken) {

        /*
         * Refresh Token 자체의 서명과 만료 시간을 검증한다.
         */
        if (!jwtTokenProvider.validateToken(rawRefreshToken)) {
            throw new IllegalArgumentException(
                    "유효하지 않은 Refresh Token입니다."
            );
        }

        /*
         * 쿠키의 토큰을 해시로 변환해 DB에서 조회하고,
         * 폐기되었거나 만료된 토큰인지 확인한다.
         */
        RefreshToken savedRefreshToken =
                refreshTokenService.getUsableToken(rawRefreshToken);

        /*
         * JWT subject에 저장된 회원 PK를 꺼낸다.
         */
        Long tokenMemberId =
                jwtTokenProvider.getMemberId(rawRefreshToken);

        /*
         * DB에 저장된 Refresh Token의 회원을 가져온다.
         */
        Member member = savedRefreshToken.getMember();

        /*
         * JWT 안의 회원 번호와 DB 토큰 소유자의 회원 번호가 다르면
         * 위조되었거나 잘못 연결된 토큰으로 판단한다.
         */
        if (!member.getId().equals(tokenMemberId)) {
            throw new IllegalArgumentException(
                    "Refresh Token의 사용자 정보가 올바르지 않습니다."
            );
        }

        /*
         * 기존 Refresh Token을 폐기한다.
         *
         * 재발급 이후 기존 토큰을 다시 사용할 수 없게 만드는 것이
         * Refresh Token Rotation의 핵심이다.
         */
        savedRefreshToken.revoke();

        /*
         * 새로운 Access Token을 생성한다.
         */
        String newAccessToken =
                jwtTokenProvider.createAccessToken(
                        member.getId(),
                        member.getUserId(),
                        member.getName(),
                        Role.USER
                );

        /*
         * 새로운 Refresh Token을 생성한다.
         */
        String newRefreshToken =
                jwtTokenProvider.createRefreshToken(
                        member.getId()
                );

        /*
         * 새 Refresh Token의 해시값을 DB에 저장한다.
         */
        refreshTokenService.save(
                member,
                newRefreshToken
        );

        /*
         * 새 Access Token은 응답 본문으로,
         * 새 Refresh Token은 HttpOnly 쿠키로 전달할 수 있도록 반환한다.
         */
        return new TokenReissueResult(
                newAccessToken,
                newRefreshToken
        );
    }

    /**
     * 현재 기기의 로그아웃을 처리한다.
     */
    @Override
    @Transactional
    public void logout(String rawRefreshToken) {

        // 쿠키가 없으면 이미 로그아웃된 상태로 처리한다.
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }

        // 유효하지 않은 토큰도 로그아웃된 상태로 처리한다.
        if (!jwtTokenProvider.validateToken(rawRefreshToken)) {
            return;
        }

        // DB에 저장된 Refresh Token을 폐기한다.
        refreshTokenService.revoke(rawRefreshToken);
    }

}