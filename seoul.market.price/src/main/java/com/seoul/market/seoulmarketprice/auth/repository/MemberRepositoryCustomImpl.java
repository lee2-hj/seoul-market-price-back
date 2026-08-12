package com.seoul.market.seoulmarketprice.auth.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.entity.QMember;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {
    private static final QMember member = QMember.member;
    private final JPAQueryFactory queryFactory;

    public Optional<Member> findActiveByUserId(String userId) {
        return Optional.ofNullable(queryFactory.selectFrom(member)
                .where(member.userId.eq(userId), member.deleted_at.isNull()).fetchOne());
    }
    public boolean existsActiveById(Long id) {
        return queryFactory.selectOne().from(member)
                .where(member.id.eq(id), member.deleted_at.isNull()).fetchFirst() != null;
    }
    public Optional<Member> findActiveById(Long id) {
        return Optional.ofNullable(queryFactory.selectFrom(member)
                .where(member.id.eq(id), member.deleted_at.isNull()).fetchOne());
    }
}
