package com.seoul.market.seoulmarketprice.auth.crypto;
import jakarta.persistence.Converter;
@Converter public class PhoneEncryptionConverter extends EncryptedStringConverter { public PhoneEncryptionConverter() { super("phone"); } }
