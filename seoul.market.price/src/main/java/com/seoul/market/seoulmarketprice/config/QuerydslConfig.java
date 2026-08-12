package com.seoul.market.seoulmarketprice.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** QueryDSL JPA 쿼리 작성에 사용하는 공통 객체를 구성한다. */
@Configuration
public class QuerydslConfig {

    @PersistenceContext
    private EntityManager entityManager;

    /** 현재 영속성 컨텍스트를 사용하는 QueryDSL 쿼리 팩토리를 등록한다. */
    @Bean
    public JPAQueryFactory jpaQueryFactory() {
        return new JPAQueryFactory(entityManager);
    }
}
