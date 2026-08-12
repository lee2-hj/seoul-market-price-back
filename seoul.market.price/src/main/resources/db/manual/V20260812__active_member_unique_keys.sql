-- 탈퇴 회원 행을 보존하면서 동일 user_id/CI로 새 회원을 INSERT하기 위한 MySQL 8 마이그레이션.
-- 2026-08-12 원격 DB에 적용한 실제 인덱스 이름을 기록한다.
-- 당시 user_id에는 UNIQUE 인덱스가 없었고 CI에만 UNIQUE 인덱스가 있었다.

ALTER TABLE tb_user
    DROP INDEX UKf44lpmh62tg0qoect7fms9aa6;

ALTER TABLE tb_user
    ADD COLUMN active_user_id VARCHAR(50)
        GENERATED ALWAYS AS (
            CASE WHEN deleted_at IS NULL THEN user_id ELSE NULL END
        ) STORED,
    ADD COLUMN active_ci VARCHAR(255)
        GENERATED ALWAYS AS (
            CASE WHEN deleted_at IS NULL THEN ci ELSE NULL END
        ) STORED,
    ADD UNIQUE INDEX uk_tb_user_active_user_id (active_user_id),
    ADD UNIQUE INDEX uk_tb_user_active_ci (active_ci);

-- 검증 쿼리
SHOW INDEX FROM tb_user;

SELECT ci, COUNT(*) AS active_count
FROM tb_user
WHERE deleted_at IS NULL AND ci IS NOT NULL
GROUP BY ci
HAVING COUNT(*) > 1;

SELECT user_id, COUNT(*) AS active_count
FROM tb_user
WHERE deleted_at IS NULL
GROUP BY user_id
HAVING COUNT(*) > 1;
