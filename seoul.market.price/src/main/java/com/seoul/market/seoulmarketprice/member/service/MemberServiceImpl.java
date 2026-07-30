package com.seoul.market.seoulmarketprice.member.service;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.member.dto.request.MemberCreateRequest;
import com.seoul.market.seoulmarketprice.member.dto.response.MemberCreateResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.UserIdCheckResponse;
import com.seoul.market.seoulmarketprice.member.exception.DuplicateMemberException;
import com.seoul.market.seoulmarketprice.member.repository.MemberManagementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class MemberServiceImpl implements MemberService {

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

    /**
     * 전달받은 아이디가 DB에 존재하는지 확인한다.
     *
     * @param userId 확인할 사용자 아이디
     * @return 아이디와 사용 가능 여부
     */
    @Override
    public UserIdCheckResponse checkUserId(String userId) {
        // 동일한 아이디가 존재하지 않을 때만 사용할 수 있다.
        boolean available = !memberManagementRepository.existsByUserId(userId);
        return new UserIdCheckResponse(userId, available);
    }

    /**
     * 아이디와 비밀번호를 사용하는 일반 회원을 생성한다.
     *
     * <ol>
     *     <li>사용자 아이디의 중복 여부를 확인한다.</li>
     *     <li>평문 비밀번호를 BCrypt 방식으로 암호화한다.</li>
     *     <li>LOCAL 유형의 Member 엔티티를 생성한다.</li>
     *     <li>회원 정보를 DB에 저장한다.</li>
     *     <li>생성된 회원의 기본 정보를 반환한다.</li>
     * </ol>
     *
     * @param request 회원가입 요청 정보
     * @return 생성된 회원의 기본 정보
     * @throws DuplicateMemberException 이미 사용 중인 아이디인 경우
     */
    @Override
    @Transactional
    public MemberCreateResponse createMember(MemberCreateRequest request) {
        // 불필요한 비밀번호 암호화와 INSERT 전에 중복을 먼저 확인한다.
        if (memberManagementRepository.existsByUserId(request.userId())) {
            throw new DuplicateMemberException();
        }

        /*
         * 비밀번호 원문은 DB에 저장하지 않는다.
         * PasswordEncoder가 생성한 BCrypt 해시값만 Member에 전달한다.
         */
        Member member = Member.createLocalMember(
                request.userId(),
                passwordEncoder.encode(request.password()),
                request.name(),
                request.zipcode(),
                request.address(),
                request.addressDetail(),
                request.phone(),
                request.email()
        );

        try {
            /*
             * 즉시 INSERT를 실행하여 DB의 UNIQUE 제약 위반도
             * 현재 요청 안에서 확인한다.
             */
            Member savedMember = memberManagementRepository.saveAndFlush(member);

            // 비밀번호와 개인정보 전체를 노출하지 않고 기본 정보만 반환한다.
            return new MemberCreateResponse(
                    savedMember.getId(),
                    savedMember.getUserId(),
                    savedMember.getName()
            );
        } catch (DataIntegrityViolationException exception) {
            /*
             * 중복 확인 직후 다른 요청이 같은 아이디를 저장하는
             * 동시성 상황은 DB UNIQUE 제약으로 최종 방어한다.
             */
            throw new DuplicateMemberException();
        }
    }
}
