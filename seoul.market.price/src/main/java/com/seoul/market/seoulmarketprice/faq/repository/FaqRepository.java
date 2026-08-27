package com.seoul.market.seoulmarketprice.faq.repository;

import com.seoul.market.seoulmarketprice.faq.entity.Faq;
import org.springframework.data.jpa.repository.JpaRepository;

/** FAQ의 저장과 기본 CRUD를 담당한다. */
public interface FaqRepository extends JpaRepository<Faq, Long> {
}
