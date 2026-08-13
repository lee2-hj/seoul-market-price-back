package com.seoul.market.seoulmarketprice.auth.crypto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MemberDataCryptoConfiguration {
    public MemberDataCryptoConfiguration(
            @Value("${app.member-data.encryption-key}") String encryptionKey
    ) {
        MemberDataCrypto.configureKey(encryptionKey);
    }
}
