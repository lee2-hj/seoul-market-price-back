package com.seoul.market.seoulmarketprice.auth.crypto;

import jakarta.persistence.AttributeConverter;

abstract class EncryptedStringConverter implements AttributeConverter<String, String> {
    private final String field;
    protected EncryptedStringConverter(String field) { this.field = field; }
    @Override public String convertToDatabaseColumn(String value) { return MemberDataCrypto.encrypt(field, value); }
    @Override public String convertToEntityAttribute(String value) { return MemberDataCrypto.decrypt(field, value); }
}
