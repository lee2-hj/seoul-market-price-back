package com.seoul.market.seoulmarketprice.auth.repository;

import com.seoul.market.seoulmarketprice.auth.entity.Admin;
import java.util.Optional;

public interface AdminRepositoryCustom {
    Optional<Admin> findActiveByAdminId(String adminId);
    boolean existsActiveById(Long id);
    Optional<Admin> findActiveByIdForTokenUpdate(Long id);
}
