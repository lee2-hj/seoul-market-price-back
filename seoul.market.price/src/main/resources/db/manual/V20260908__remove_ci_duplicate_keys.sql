-- CI is retained only for PASS identity verification and is not a duplicate key.
DROP INDEX uk_tb_user_active_ci_hash ON tb_user;
DROP INDEX uk_tb_user_active_ci ON tb_user;

ALTER TABLE tb_user
    DROP COLUMN active_ci_hash,
    DROP COLUMN active_ci,
    DROP COLUMN ci_hash;
