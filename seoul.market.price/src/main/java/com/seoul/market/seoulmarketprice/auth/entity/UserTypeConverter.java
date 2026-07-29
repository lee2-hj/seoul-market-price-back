package com.seoul.market.seoulmarketprice.auth.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * UserType(Enum)과 DB의 숫자(TINYINT)를 서로 변환하는 클래스이다.
 *
 * <p>
 * Java에서는 UserType.LOCAL, UserType.SOCIAL을 사용하고,
 * DB에는 0 또는 1을 저장하기 위해 사용된다.
 * </p>
 *
 * <pre>
 * Java                 DB
 * ----------------------------
 * LOCAL      ↔          0
 * SOCIAL     ↔          1
 * </pre>
 *
 * JPA가 저장하거나 조회할 때 자동으로 호출된다.
 */
@Converter(autoApply = false)
public class UserTypeConverter
        implements AttributeConverter<UserType, Integer> {

    /**
     * Entity → DB
     *
     * UserType을 DB에 저장할 숫자로 변환한다.
     *
     * ex)
     * LOCAL  -> 0
     * SOCIAL -> 1
     */
    @Override
    public Integer convertToDatabaseColumn(UserType attribute) {

        if (attribute == null) {
            return null;
        }

        return attribute.getCode();
    }

    /**
     * DB → Entity
     *
     * DB의 숫자를 UserType으로 변환한다.
     *
     * ex)
     * 0 -> LOCAL
     * 1 -> SOCIAL
     */
    @Override
    public UserType convertToEntityAttribute(Integer dbData) {

        if (dbData == null) {
            return null;
        }

        return UserType.fromCode(dbData);
    }
}