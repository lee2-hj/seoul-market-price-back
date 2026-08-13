package com.seoul.market.seoulmarketprice.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "tb_sgg_master")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SggMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sgg_cd", nullable = false, unique = true, length = 10)
    private String sggCode;

    @Column(name = "sgg_nm", nullable = false, length = 50)
    private String sggName;

    @Column(name = "center_lat", nullable = false, precision = 10, scale = 7)
    private BigDecimal centerLatitude;

    @Column(name = "center_lng", nullable = false, precision = 10, scale = 7)
    private BigDecimal centerLongitude;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
