package com.seoul.market.seoulmarketprice.member.repository;

import com.seoul.market.seoulmarketprice.member.dto.response.admin.AdminListResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * auth 패키지의 관리자 Entity를 변경하지 않고 관리자 생성 SQL을 처리한다.
 */
@Repository
public class AdminCreationRepository {

    private final JdbcTemplate jdbcTemplate;

    public AdminCreationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean existsByAdminId(String adminId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM tb_admin WHERE admin_id = ?",
                Long.class,
                adminId
        );
        return count != null && count > 0;
    }

    public List<AdminListResponse> findAll() {
        return jdbcTemplate.query(
                """
                SELECT admin_id, name, phone, email, created_at, updated_at
                FROM tb_admin
                ORDER BY created_at DESC
                """,
                (resultSet, rowNum) -> new AdminListResponse(
                        resultSet.getString("admin_id"),
                        resultSet.getString("name"),
                        resultSet.getString("phone"),
                        resultSet.getString("email"),
                        resultSet.getTimestamp("created_at") == null
                                ? null
                                : resultSet.getTimestamp("created_at").toLocalDateTime(),
                        resultSet.getTimestamp("updated_at") == null
                                ? null
                                : resultSet.getTimestamp("updated_at").toLocalDateTime()
                )
        );
    }

    public Long save(String adminId, String password, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO tb_admin (admin_id, password, name) VALUES (?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setString(1, adminId);
            statement.setString(2, password);
            statement.setString(3, name);
            return statement;
        }, keyHolder);

        Number key = keyHolder.getKey();
        return key == null ? null : key.longValue();
    }
}
