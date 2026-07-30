package com.seoul.market.seoulmarketprice.member.repository;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 회원가입과 회원 관리에서 사용하는 JPA Repository.
 *
 * <p>
 * 기존 인증 기능의 Repository는 그대로 유지하고,
 * 이 Repository는 회원 생성과 아이디 중복 확인을 담당한다.
 * 두 Repository 모두 {@link Member} 엔티티와 {@code tb_user} 테이블을 사용한다.
 * </p>
 */
public interface MemberManagementRepository extends JpaRepository<Member, Long> {

    /**
     * 동일한 사용자 아이디가 존재하는지 확인한다.
     *
     * <p>
     * Spring Data JPA가 메서드 이름을 해석하여
     * {@code tb_user.user_id} 존재 여부를 조회한다.
     * </p>
     *
     * <p>
     * {@code existsBy} 접두사는 실행 시 항상 boolean 결과를 반환하므로
     * 리턴 타입도 boolean이어야 한다. 과거 {@code Optional<Member>}로
     * 선언되어 있었는데, boolean 결과가 {@code Optional.ofNullable}로
     * 감싸지면서 false도 값이 존재하는 것으로 처리되어
     * {@code isPresent()}가 항상 true를 반환하는 버그가 있었다.
     * </p>
     *
     * @param userId 확인할 사용자 아이디
     * @return 동일한 아이디가 존재하면 true
     */
    boolean existsByUserId(String userId);
}
