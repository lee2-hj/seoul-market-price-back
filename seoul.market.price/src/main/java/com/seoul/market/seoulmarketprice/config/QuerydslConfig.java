package com.seoul.market.seoulmarketprice.config;

import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** QueryDSL JPA 쿼리 작성에 사용하는 공통 객체를 구성한다. */
@Configuration
public class QuerydslConfig {

    /** 현재 영속성 컨텍스트를 사용하는 QueryDSL 쿼리 팩토리를 등록한다. */
    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager entityManager) {
        return new JPAQueryFactory(entityManager);
    }
}
