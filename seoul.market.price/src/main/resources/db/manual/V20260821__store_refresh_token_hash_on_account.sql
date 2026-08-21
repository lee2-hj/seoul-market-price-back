-- 계정마다 마지막으로 발급한 Refresh Token 해시 하나만 보관한다.
ALTER TABLE tb_user
    ADD COLUMN refresh_token_hash VARCHAR(64) NULL;

ALTER TABLE tb_member
    ADD COLUMN refresh_token_hash VARCHAR(64) NULL;

-- 기존 다중 기기 Refresh Token 저장소는 더 이상 사용하지 않는다.
DROP TABLE IF EXISTS tb_refresh_token;
DROP TABLE IF EXISTS tb_admin_refresh_token;
