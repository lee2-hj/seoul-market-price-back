package com.seoul.market.seoulmarketprice.member.repository;

import com.seoul.market.seoulmarketprice.auth.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

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
    @Query("SELECT (COUNT(m) > 0) FROM Member m WHERE m.userId = :userId AND m.deleted_at IS NULL")
    boolean existsActiveByUserId(@Param("userId") String userId);

    @Query("SELECT (COUNT(m) > 0) FROM Member m WHERE m.phone = :phone AND m.deleted_at IS NULL")
    boolean existsActiveByPhone(@Param("phone") String phoneNumber);

    @Query("SELECT (COUNT(m) > 0) FROM Member m WHERE m.ci = :ci AND m.deleted_at IS NULL")
    boolean existsActiveByCi(@Param("ci") String ci);

    @Query("SELECT (COUNT(m) > 0) FROM Member m WHERE m.ci = :ci")
    boolean existsAnyByCi(@Param("ci") String ci);

    boolean existsByNameAndPhone(String name, String phone);

    /**
     * PASS에서 확인한 이름과 전화번호로 탈퇴하지 않은 일반 회원을 조회한다.
     * 기존 전화번호 데이터에 포함된 하이픈과 공백은 조회 시 제거한다.
     */
    @Query(value = """
            SELECT *
            FROM tb_user
            WHERE TRIM(name) = :name
              AND REPLACE(REPLACE(phone, '-', ''), ' ', '') = :phone
              AND user_type = 0
              AND deleted_at IS NULL
            """, nativeQuery = true)
    List<Member> findActiveLocalMembersByVerifiedIdentity(
            @Param("name") String name,
            @Param("phone") String phone
    );

    /** CI 확인 및 최초 연결을 원자적으로 처리하기 위해 회원 행을 잠가 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM Member m
            WHERE m.userId = :userId
              AND m.userType = com.seoul.market.seoulmarketprice.auth.entity.UserType.LOCAL
              AND m.deleted_at IS NULL
            """)
    Optional<Member> findActiveLocalByUserIdForCiRegistration(
            @Param("userId") String userId
    );

    /** 재설정 완료 시 동시 요청을 직렬화하기 위해 회원 행을 잠가 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT m
            FROM Member m
            WHERE m.id = :memberId
              AND m.deleted_at IS NULL
            """)
    Optional<Member> findActiveByIdForPasswordReset(
            @Param("memberId") Long memberId
    );

    /** 현재 회원 정보 조회에 사용할 활성 회원을 PK로 조회한다. */
    @Query("SELECT m FROM Member m WHERE m.id = :memberId AND m.deleted_at IS NULL")
    Optional<Member> findActiveById(@Param("memberId") Long memberId);

    /** 중복 탈퇴 요청을 직렬화하도록 회원 행을 쓰기 잠금으로 조회한다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM Member m WHERE m.id = :memberId AND m.deleted_at IS NULL")
    Optional<Member> findActiveByIdForWithdrawal(@Param("memberId") Long memberId);
}
