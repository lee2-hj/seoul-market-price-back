-- 기존 자치구명 값을 tb_sgg_master.id 문자열로 변환한다.
-- 이미 ID가 저장된 행과 NULL 행은 그대로 유지한다.
UPDATE tb_user u
JOIN tb_sgg_master s ON TRIM(u.my_gu) = s.sgg_nm
SET u.my_gu = CAST(s.id AS CHAR)
WHERE u.my_gu IS NOT NULL
  AND u.my_gu <> '';

-- 변환되지 않은 값이 없어야 FK를 안전하게 추가할 수 있다.
SELECT u.id, u.my_gu
FROM tb_user u
LEFT JOIN tb_sgg_master s ON u.my_gu = CAST(s.id AS CHAR)
WHERE u.my_gu IS NOT NULL
  AND u.my_gu <> ''
  AND s.id IS NULL;
