package com.seoul.market.seoulmarketprice.board.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 게시글 유형과 DB의 TINYINT 값을 상호 변환한다. */
@Converter(autoApply = false)
public class PostTypeConverter implements AttributeConverter<PostType, Integer> {

    @Override
    public Integer convertToDatabaseColumn(PostType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    @Override
    public PostType convertToEntityAttribute(Integer dbData) {
        return PostType.fromValue(dbData);
    }
}
