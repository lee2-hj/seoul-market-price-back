-- tb_dong_master의 행정동 코드는 상위 자치구 코드로 시작한다는 전제의 1회성 마이그레이션이다.
ALTER TABLE tb_dong_master
    ADD COLUMN sgg_id BIGINT NULL;

UPDATE tb_dong_master d
JOIN tb_sgg_master s
  ON LEFT(d.dong_cd, CHAR_LENGTH(s.sgg_cd)) = s.sgg_cd
SET d.sgg_id = s.id
WHERE d.sgg_id IS NULL;

-- 아래 조회 결과가 없을 때만 NOT NULL 및 FK를 적용한다.
SELECT id, dong_cd, dong_nm
FROM tb_dong_master
WHERE sgg_id IS NULL;

ALTER TABLE tb_dong_master
    MODIFY COLUMN sgg_id BIGINT NOT NULL,
    ADD INDEX idx_dong_master_sgg_id (sgg_id),
    ADD CONSTRAINT fk_dong_master_sgg
        FOREIGN KEY (sgg_id) REFERENCES tb_sgg_master(id);
