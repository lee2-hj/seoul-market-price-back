package com.seoul.market.seoulmarketprice.auth.crypto;
import jakarta.persistence.Converter;
@Converter public class UserIdEncryptionConverter extends EncryptedStringConverter { public UserIdEncryptionConverter() { super("userId"); } }
