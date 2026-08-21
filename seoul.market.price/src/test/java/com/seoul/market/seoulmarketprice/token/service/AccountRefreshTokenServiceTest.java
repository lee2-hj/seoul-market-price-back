package com.seoul.market.seoulmarketprice.token.service;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccountRefreshTokenServiceTest {

    private final TokenHashService tokenHashService = new TokenHashService();
    private final RefreshTokenService memberTokenService =
            new RefreshTokenService(tokenHashService);
    private final AdminRefreshTokenService adminTokenService =
            new AdminRefreshTokenService(tokenHashService);

    @Test
    void memberLoginOverwritesPreviousRefreshToken() {
        Member member = Member.createLocalMember(
                "user", "password", "name", null, null, null,
                "010-1234-5678", null, (byte) 1, (byte) 1, (byte) 1,
                null, null, null, null
        );

        memberTokenService.save(member, "first-token");
        memberTokenService.save(member, "second-token");

        assertThat(member.getRefreshTokenHash())
                .isEqualTo(tokenHashService.hash("second-token"));
        assertThatThrownBy(() -> memberTokenService.validate(member, "first-token"))
                .isInstanceOf(IllegalArgumentException.class);
        memberTokenService.validate(member, "second-token");
    }

    @Test
    void adminLogoutClearsStoredRefreshToken() {
        Admin admin = BeanUtils.instantiateClass(Admin.class);

        adminTokenService.save(admin, "admin-token");
        adminTokenService.revoke(admin, "admin-token");

        assertThat(admin.getRefreshTokenHash()).isNull();
    }
}
