package com.seoul.market.seoulmarketprice.auth.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import com.seoul.market.seoulmarketprice.auth.entity.QAdmin;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@RequiredArgsConstructor
public class AdminRepositoryCustomImpl implements AdminRepositoryCustom {
    private static final QAdmin admin = QAdmin.admin;
    private final JPAQueryFactory queryFactory;

    public Optional<Admin> findActiveByAdminId(String adminId) {
        return Optional.ofNullable(queryFactory.selectFrom(admin)
                .where(admin.adminId.eq(adminId), admin.deleted_at.isNull()).fetchOne());
    }
    public boolean existsActiveById(Long id) {
        return queryFactory.selectOne().from(admin)
                .where(admin.id.eq(id), admin.deleted_at.isNull()).fetchFirst() != null;
    }
}
