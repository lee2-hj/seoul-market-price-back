package com.seoul.market.seoulmarketprice.auth.crypto;
import jakarta.persistence.Converter;
@Converter public class CiEncryptionConverter extends EncryptedStringConverter { public CiEncryptionConverter() { super("ci"); } }
