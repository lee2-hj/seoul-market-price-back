package com.seoul.market.seoulmarketprice.auth.repository;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 관리자 정보를 조회하는 Repository이다.
 *
 * <p>
 * {@link Admin} 엔티티와 {@code tb_admin} 테이블을 연결한다.
 * 관리자 로그인 과정에서 관리자 아이디를 기준으로 계정을 조회할 때 사용한다.
 * </p>
 *
 * <p>
 * {@link JpaRepository}를 상속하므로
 * 저장, 조회, 수정, 삭제와 같은 기본 CRUD 기능을
 * 별도로 구현하지 않아도 사용할 수 있다.
 * </p>
 */
public interface AdminRepository extends JpaRepository<Admin, Long> {

    /**
     * 관리자 로그인 아이디로 관리자 정보를 조회한다.
     *
     * <p>
     * Spring Data JPA가 메서드 이름을 해석하여
     * {@code tb_admin.admin_id} 컬럼을 기준으로 조회 쿼리를 생성한다.
     * </p>
     *
     * <pre>
     * SELECT *
     * FROM tb_admin
     * WHERE admin_id = ?
     * </pre>
     *
     * <p>
     * 조회 결과가 없을 수도 있으므로
     * {@link Optional}로 반환한다.
     * </p>
     *
     * @param adminId 조회할 관리자 로그인 아이디
     * @return 조회된 관리자 정보
     */
    Optional<Admin> findByAdminId(String adminId);
}