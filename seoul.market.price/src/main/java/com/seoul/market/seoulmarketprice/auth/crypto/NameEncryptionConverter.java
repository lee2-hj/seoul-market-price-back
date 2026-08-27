package com.seoul.market.seoulmarketprice.auth.crypto;
import jakarta.persistence.Converter;
@Converter public class NameEncryptionConverter extends EncryptedStringConverter { public NameEncryptionConverter() { super("name"); } }
