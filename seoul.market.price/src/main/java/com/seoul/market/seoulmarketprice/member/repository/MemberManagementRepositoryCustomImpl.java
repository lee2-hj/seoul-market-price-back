package com.seoul.market.seoulmarketprice.member.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seoul.market.seoulmarketprice.auth.entity.Member;
import com.seoul.market.seoulmarketprice.auth.entity.QMember;
import com.seoul.market.seoulmarketprice.auth.entity.UserType;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import java.util.Optional;

@RequiredArgsConstructor
public class MemberManagementRepositoryCustomImpl implements MemberManagementRepositoryCustom {
    private static final QMember member = QMember.member;
    private final JPAQueryFactory queryFactory;

    public boolean existsActiveByUserId(String userId) { return exists(member.userId.eq(userId).and(active())); }
    public boolean existsActiveByPhone(String phone) { return exists(member.phone.eq(phone).and(active())); }
    public boolean existsActiveByCi(String ci) { return exists(member.ci.eq(ci).and(active())); }
    public boolean existsAnyByCi(String ci) { return exists(member.ci.eq(ci)); }
    public boolean existsActiveByNameAndPhone(String name, String phone) {
        return exists(member.name.eq(name).and(member.phone.eq(phone)).and(active()));
    }
    public Optional<Member> findActiveLocalByUserIdForCiRegistration(String userId) {
        return locked(member.userId.eq(userId).and(member.userType.eq(UserType.LOCAL)).and(active()));
    }
    public Optional<Member> findActiveByIdForPasswordReset(Long id) { return locked(member.id.eq(id).and(active())); }
    public Optional<Member> findActiveById(Long id) {
        return Optional.ofNullable(queryFactory.selectFrom(member).where(member.id.eq(id), active()).fetchOne());
    }
    public Optional<Member> findActiveByIdForWithdrawal(Long id) { return locked(member.id.eq(id).and(active())); }

    private boolean exists(BooleanExpression condition) {
        return queryFactory.selectOne().from(member).where(condition).fetchFirst() != null;
    }
    private Optional<Member> locked(BooleanExpression condition) {
        return Optional.ofNullable(queryFactory.selectFrom(member).where(condition)
                .setLockMode(LockModeType.PESSIMISTIC_WRITE).fetchOne());
    }
    private BooleanExpression active() { return member.deleted_at.isNull(); }
}
