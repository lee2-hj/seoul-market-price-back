package com.seoul.market.seoulmarketprice.member.repository;

import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminListResponse;
import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminUpdateResponse;
import com.seoul.market.seoulmarketprice.auth.entity.Role;
import com.seoul.market.seoulmarketprice.auth.crypto.MemberDataCrypto;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

/** 관리자 계정의 생성, 조회, 수정 및 소프트 삭제 SQL을 담당한다. */
@Repository
public class AdminCreationRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminCreationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 관리자 로그인 아이디의 중복 여부를 확인한다. */
    public boolean existsByAdminId(String adminId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_member WHERE user_id_hash = ?",
                Long.class,
                MemberDataCrypto.searchHash("userId", adminId)
        );
        return count != null && count > 0;
    }

    /** 고유번호에 해당하는 활성 관리자가 존재하는지 확인한다. */
    public boolean existsActiveById(Long id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_member WHERE id = ? AND deleted_at IS NULL",
                Long.class,
                id
        );
        return count != null && count > 0;
    }

    /** 삭제되지 않은 관리자 목록을 최신 생성 순으로 조회한다. */
    public List<AdminListResponse> findAll(int size, long offset) {
        return jdbcTemplate.query(
                """
                SELECT id, user_id, name, phone, email, role, created_at, updated_at
                FROM tb_member
                WHERE deleted_at IS NULL
                ORDER BY created_at DESC
                LIMIT ? OFFSET ?
                """,
                (resultSet, rowNum) -> new AdminListResponse(
                        resultSet.getLong("id"),
                        MemberDataCrypto.decrypt("userId", resultSet.getString("user_id")),
                        MemberDataCrypto.decrypt("name", resultSet.getString("name")),
                        MemberDataCrypto.decrypt("phone", resultSet.getString("phone")),
                        resultSet.getString("email"),
                        Role.valueOf(resultSet.getString("role")),
                        resultSet.getTimestamp("created_at") == null
                                ? null : resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getTimestamp("updated_at") == null
                                ? null : resultSet.getTimestamp("updated_at").toLocalDateTime()
                ),
                size,
                offset
        );
    }

    /** 삭제되지 않은 전체 관리자 수를 조회한다. */
    public long countAll() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_member WHERE deleted_at IS NULL",
                Long.class
        );
        return count == null ? 0L : count;
    }

    /** 비밀번호가 암호화된 새 관리자 계정을 저장하고 생성된 고유번호를 반환한다. */
    public Long save(String adminId, String password, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tb_member (user_id, user_id_hash, password, name, name_hash) VALUES (?, ?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, MemberDataCrypto.encrypt("userId", adminId));
            statement.setString(2, MemberDataCrypto.searchHash("userId", adminId));
            statement.setString(3, password);
            statement.setString(4, MemberDataCrypto.encrypt("name", name));
            statement.setString(5, MemberDataCrypto.searchHash("name", name));
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }

    /** 전달된 항목만 변경하고 수정된 관리자 정보를 반환한다. */
    public Optional<AdminUpdateResponse> update(
            Long id, String password, String name, String phone, String email, Role role
    ) {
        int updatedRows = jdbcTemplate.update(
                """
                UPDATE tb_member
                SET password = COALESCE(?, password), name = COALESCE(?, name),
                    phone = COALESCE(?, phone), name_hash = COALESCE(?, name_hash), phone_hash = COALESCE(?, phone_hash),
                    email = COALESCE(?, email), role = COALESCE(?, role),
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND deleted_at IS NULL
                """,
                password, name == null ? null : MemberDataCrypto.encrypt("name", name),
                phone == null ? null : MemberDataCrypto.encrypt("phone", phone),
                name == null ? null : MemberDataCrypto.searchHash("name", name),
                phone == null ? null : MemberDataCrypto.searchHash("phone", phone),
                email, role == null ? null : role.name(), id
        );
        if (updatedRows == 0) {
            return Optional.empty();
        }
        return jdbcTemplate.query(
                "SELECT id, user_id, name, phone, email, role, updated_at FROM tb_member WHERE id = ? AND deleted_at IS NULL",
                resultSet -> resultSet.next()
                        ? Optional.of(new AdminUpdateResponse(
                                resultSet.getLong("id"),
                                resultSet.getString("user_id"),
                                resultSet.getString("name"),
                                resultSet.getString("phone"),
                                resultSet.getString("email"),
                                Role.valueOf(resultSet.getString("role")),
                                resultSet.getTimestamp("updated_at").toLocalDateTime()
                        )) : Optional.empty(),
                id
        );
    }

    public Optional<AdminUpdateResponse> update(Long id, String password, String name, String phone, String email) {
        return update(id, password, name, phone, email, null);
    }

    /** 관리자의 삭제 시각을 기록하여 계정을 소프트 삭제한다. */
    public int softDelete(Long id) {
        return jdbcTemplate.update(
                "UPDATE tb_member SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND deleted_at IS NULL",
                id
        );
    }
}
