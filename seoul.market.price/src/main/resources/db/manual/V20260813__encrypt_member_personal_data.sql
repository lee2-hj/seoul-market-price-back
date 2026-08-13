-- 애플리케이션 배포 전에 실행한다. 기존 행의 개인정보 값은 변경하지 않는다.
-- 신규/수정 행은 애플리케이션에서 AES-256-GCM 암호문과 HMAC 검색값을 저장한다.

ALTER TABLE tb_user
    MODIFY COLUMN user_id VARCHAR(512) NOT NULL,
    MODIFY COLUMN name VARCHAR(512) NOT NULL,
    MODIFY COLUMN phone VARCHAR(512) NULL,
    MODIFY COLUMN ci VARCHAR(512) NULL,
    MODIFY COLUMN active_user_id VARCHAR(512)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN user_id ELSE NULL END) STORED,
    MODIFY COLUMN active_ci VARCHAR(512)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN ci ELSE NULL END) STORED,
    ADD COLUMN user_id_hash VARCHAR(43) NULL,
    ADD COLUMN name_hash VARCHAR(43) NULL,
    ADD COLUMN phone_hash VARCHAR(43) NULL,
    ADD COLUMN ci_hash VARCHAR(43) NULL;

CREATE INDEX idx_tb_user_user_id_hash ON tb_user (user_id_hash);
CREATE INDEX idx_tb_user_name_phone_hash ON tb_user (name_hash, phone_hash);
CREATE INDEX idx_tb_user_phone_hash ON tb_user (phone_hash);
CREATE INDEX idx_tb_user_ci_hash ON tb_user (ci_hash);

-- 탈퇴 회원을 보존하면서 활성 회원만 유일하도록 생성 컬럼을 사용한다.
ALTER TABLE tb_user
    ADD COLUMN active_user_id_hash VARCHAR(43)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN user_id_hash ELSE NULL END) STORED,
    ADD COLUMN active_ci_hash VARCHAR(43)
        GENERATED ALWAYS AS (CASE WHEN deleted_at IS NULL THEN ci_hash ELSE NULL END) STORED,
    ADD UNIQUE INDEX uk_tb_user_active_user_id_hash (active_user_id_hash),
    ADD UNIQUE INDEX uk_tb_user_active_ci_hash (active_ci_hash);
