package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberCheckRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.request.member.MemberIdCheckRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberCheckResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.member.MemberIdCheckResponse;
import com.seoul.market.seoulmarketprice.member.exception.DuplicateMemberException;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import com.seoul.market.seoulmarketprice.phoneverification.dto.request.PhoneVerificationConfirmRequest;
import com.seoul.market.seoulmarketprice.phoneverification.dto.response.PhoneVerificationConfirmResponse;
import com.seoul.market.seoulmarketprice.phoneverification.service.PhoneVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;

/**
 * 회원 관리 비즈니스 로직을 실제로 구현하는 서비스.
 *
 * <p>
 * 아이디 중복 확인, 비밀번호 암호화, 일반 회원 저장을 처리한다.
 * 조회 메서드는 읽기 전용 트랜잭션을 사용하고,
 * 회원 생성 메서드에서만 쓰기 트랜잭션을 사용한다.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private static final Duration MAX_VERIFICATION_AGE = Duration.ofMinutes(5);
    private static final Duration FUTURE_CLOCK_TOLERANCE = Duration.ofMinutes(1);

    /**
     * 회원 중복 확인과 저장을 담당하는 Repository.
     *
     * <p>
     * 인증 패키지의 MemberRepository와 Bean 이름이 충돌하지 않도록
     * 회원 관리 전용 이름을 사용한다.
     * </p>
     */
    private final MemberManagementRepository memberManagementRepository;

    /**
     * 회원 비밀번호를 BCrypt 해시로 변환한다.
     */
    private final PasswordEncoder passwordEncoder;

    private final PhoneVerificationService phoneVerificationService;

    public MemberResponse getMember(Long memberId) {
        Member member = memberManagementRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("회원을 찾을 수 없습니다."));

        return MemberResponse.from(member);
    }

    /**
     * 아이디와 비밀번호를 사용하는 일반 회원을 생성한다.
     *
     * <ol>
     *     <li>사용자 아이디의 중복 여부를 확인한다.</li>
     *     <li>평문 비밀번호를 BCrypt 방식으로 암호화한다.</li>
     *     <li>LOCAL 유형의 Member 엔티티를 생성한다.</li>
     *     <li>회원 정보를 DB에 저장한다.</li>
     *     <li>결과 메시지를 반환한다.</li>
     * </ol>
     *
     * @param request 회원가입 요청 정보
     * @return 처리 결과 상태 메시지를 담은 회원가입 응답
     */
    @Transactional
    public MemberCreateResponse createMember(MemberCreateRequest request) {

        PhoneVerificationConfirmResponse verification = phoneVerificationService.confirm(
                new PhoneVerificationConfirmRequest(request.identityVerificationId())
        );
        validateRecentVerification(verification.verifiedAt());

        if (!request.name().trim().equals(verification.name().trim())
                || !MemberIdFindService.normalizePhone(request.phone()).equals(
                        MemberIdFindService.normalizePhone(verification.phoneNumber())
                )) {
            throw new IllegalArgumentException("회원가입 정보와 본인인증 정보가 일치하지 않습니다.");
        }

        // 유저 아이디 중복 체크
        if (memberManagementRepository.existsByUserId(request.userId())) {
            throw new DuplicateMemberException();
        }

        //전화번호 중복체크
        if(memberManagementRepository.existsByPhone(request.phone())){
            throw new DuplicateMemberException("이미 사용 중인 전화번호입니다.");
        }

        if (memberManagementRepository.existsByCi(verification.ci())) {
            throw new DuplicateMemberException("이미 가입된 본인인증 정보입니다.");
        }

        Member member = request.toEntity(
                passwordEncoder.encode(request.password()),
                verification.ci()
        );
        memberManagementRepository.save(member);

        String msg = "회원가입이 완료되었습니다.";
        return new MemberCreateResponse(msg);
    }

    private void validateRecentVerification(String verifiedAtValue) {
        try {
            Instant verifiedAt = Instant.parse(verifiedAtValue);
            Instant now = Instant.now();
            if (verifiedAt.isBefore(now.minus(MAX_VERIFICATION_AGE))
                    || verifiedAt.isAfter(now.plus(FUTURE_CLOCK_TOLERANCE))) {
                throw new IllegalArgumentException("본인인증 유효시간이 만료되었습니다. 다시 인증해 주세요.");
            }
        } catch (NullPointerException | DateTimeException exception) {
            throw new IllegalArgumentException("본인인증 완료 시각을 확인할 수 없습니다. 다시 인증해 주세요.");
        }
    }

    //회원 아이디 중복 체크(회원가입 시 아이디 중복 체크 용도)
    public MemberIdCheckResponse checkMemberId(MemberIdCheckRequest request) {
        // 유저 아이디 중복 체크
        boolean isDuplicated = memberManagementRepository.existsByUserId(request.userId());

        boolean isAvailable = !isDuplicated;

        return new MemberIdCheckResponse(isAvailable);
    }

    //일반 회원 가입 시 이미 등록 된 회원인지 체크
    public MemberCheckResponse checkMember(MemberCheckRequest request) {

        boolean check = memberManagementRepository.existsByNameAndPhone(request.name(), request.phone());

        boolean isDuple = check;

        return new MemberCheckResponse(isDuple);
    }
}
