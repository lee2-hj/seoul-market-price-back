package com.seoul.market.seoulmarketprice.board.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 게시글 유형과 DB의 TINYINT 값을 상호 변환한다. */
@Converter(autoApply = false)
public class PostTypeConverter implements AttributeConverter<PostType, Integer> {

    /** 게시글 유형을 DB 저장 코드로 변환한다. */
    @Override
    public Integer convertToDatabaseColumn(PostType attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    /** DB 저장 코드를 게시글 유형으로 변환한다. */
    @Override
    public PostType convertToEntityAttribute(Integer dbData) {
        return PostType.fromValue(dbData);
    }
}
