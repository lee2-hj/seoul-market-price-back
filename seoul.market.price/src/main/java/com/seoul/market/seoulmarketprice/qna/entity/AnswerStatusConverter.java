package com.seoul.market.seoulmarketprice.qna.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/** 답변 상태 열거형과 DB TINYINT 값을 상호 변환한다. */
@Converter
public class AnswerStatusConverter implements AttributeConverter<AnswerStatus, Integer> {
    /** 답변 상태를 데이터베이스 숫자 코드로 변환한다. */
    @Override
    public Integer convertToDatabaseColumn(AnswerStatus attribute) {
        return attribute == null ? null : attribute.getValue();
    }

    /** 데이터베이스 숫자 코드를 답변 상태로 변환한다. */
    @Override
    public AnswerStatus convertToEntityAttribute(Integer dbData) {
        return AnswerStatus.fromValue(dbData);
    }
}
