package com.seoul.market.seoulmarketprice.auth.crypto;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.member-data.migrate-plaintext", havingValue = "true")
public class MemberDataMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(MemberDataMigrationRunner.class);
    private static final String ENCRYPTED_PREFIX = "enc:v1:%";
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        expandLegacyGeneratedColumns();
        migrateAdmins();
        List<MemberRow> rows = jdbcTemplate.query("""
                SELECT id, user_id, name, phone, ci
                FROM tb_user
                WHERE user_id NOT LIKE ?
                   OR name NOT LIKE ?
                   OR (phone IS NOT NULL AND phone <> '' AND phone NOT LIKE ?)
                   OR (ci IS NOT NULL AND ci <> '' AND ci NOT LIKE ?)
                FOR UPDATE
                """, (rs, rowNum) -> new MemberRow(
                rs.getLong("id"), rs.getString("user_id"), rs.getString("name"),
                rs.getString("phone"), rs.getString("ci")
        ), ENCRYPTED_PREFIX, ENCRYPTED_PREFIX, ENCRYPTED_PREFIX, ENCRYPTED_PREFIX);

        for (MemberRow row : rows) {
            jdbcTemplate.update("""
                    UPDATE tb_user
                    SET user_id = ?, user_id_hash = ?,
                        name = ?, name_hash = ?,
                        phone = ?, phone_hash = ?,
                        ci = ?, ci_hash = ?
                    WHERE id = ?
                    """,
                    encryptIfPlain("userId", row.userId()), hashPlainOrEncrypted("userId", row.userId()),
                    encryptIfPlain("name", row.name()), hashPlainOrEncrypted("name", row.name()),
                    encryptIfPlain("phone", row.phone()), hashPlainOrEncrypted("phone", row.phone()),
                    encryptIfPlain("ci", row.ci()), hashPlainOrEncrypted("ci", row.ci()), row.id());
        }
        log.info("tb_user 개인정보 평문 마이그레이션 완료: {}건", rows.size());
    }

    private void migrateAdmins() {
        List<AdminRow> rows = jdbcTemplate.query("""
                SELECT id, user_id, name, phone FROM tb_member
                WHERE user_id NOT LIKE ? OR name NOT LIKE ?
                   OR (phone IS NOT NULL AND phone <> '' AND phone NOT LIKE ?)
                FOR UPDATE
                """, (rs, rowNum) -> new AdminRow(rs.getLong("id"), rs.getString("user_id"),
                rs.getString("name"), rs.getString("phone")), ENCRYPTED_PREFIX, ENCRYPTED_PREFIX, ENCRYPTED_PREFIX);
        for (AdminRow row : rows) {
            jdbcTemplate.update("""
                    UPDATE tb_member SET user_id = ?, user_id_hash = ?, name = ?, name_hash = ?, phone = ?, phone_hash = ? WHERE id = ?
                    """, encryptIfPlain("userId", row.userId()), hashPlainOrEncrypted("userId", row.userId()),
                    encryptIfPlain("name", row.name()), hashPlainOrEncrypted("name", row.name()),
                    encryptIfPlain("phone", row.phone()), hashPlainOrEncrypted("phone", row.phone()), row.id());
        }
        log.info("tb_member 관리자 개인정보 마이그레이션 완료: {}건", rows.size());
    }

    private void expandLegacyGeneratedColumns() {
        jdbcTemplate.execute("""
                ALTER TABLE tb_user
                MODIFY COLUMN active_user_id VARCHAR(512)
                    GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN user_id ELSE NULL END) STORED,
                MODIFY COLUMN active_ci VARCHAR(512)
                    GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN ci ELSE NULL END) STORED
                """);
        jdbcTemplate.execute("""
                ALTER TABLE tb_member
                MODIFY COLUMN user_id VARCHAR(512) NOT NULL,
                MODIFY COLUMN name VARCHAR(512) NOT NULL,
                MODIFY COLUMN phone VARCHAR(512)
                """);
    }

    private String encryptIfPlain(String field, String value) {
        return value == null || value.isEmpty() || value.startsWith("enc:v1:")
                ? value : MemberDataCrypto.encrypt(field, value);
    }

    private String hashPlainOrEncrypted(String field, String value) {
        if (value == null || value.isBlank()) return null;
        return MemberDataCrypto.searchHash(field, MemberDataCrypto.decrypt(field, value));
    }

    private record MemberRow(Long id, String userId, String name, String phone, String ci) {}
    private record AdminRow(Long id, String userId, String name, String phone) {}
}
