package com.seoul.market.seoulmarketprice.menus.repository.customImpl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seoul.market.seoulmarketprice.menus.entity.MenuCategoryEntity;
import static com.seoul.market.seoulmarketprice.menus.entity.QMenuCategoryEntity.menuCategoryEntity;
import com.seoul.market.seoulmarketprice.menus.repository.custom.MenuCategoryRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;


@RequiredArgsConstructor
public class MenuCategoryRepositoryCustomImpl implements MenuCategoryRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MenuCategoryEntity> findCodeOrName(String menuCode, String menuName, long offset, int size) {
        BooleanBuilder builder = new BooleanBuilder();

        if(StringUtils.hasText(menuCode)){
            builder.or(menuCategoryEntity.menuCode.like("%" +menuCode+"%"));
        }

        if(StringUtils.hasText(menuName)){
            builder.or(menuCategoryEntity.menuName.like("%" +menuName+"%"));
        }

        return queryFactory
                .selectFrom(menuCategoryEntity)
                .where(builder)
                .offset(offset)
                .limit(size)
                .fetch();
    }

    @Override
    public long countMenuCodeOrMenuName(String menuCode, String menuName) {
        BooleanBuilder builder = new BooleanBuilder();

        if(StringUtils.hasText(menuCode)){
            builder.or(menuCategoryEntity.menuCode.like("%" +menuCode+"%"));
        }

        if(StringUtils.hasText(menuName)){
            builder.or(menuCategoryEntity.menuName.like("%" +menuName+"%"));
        }

        Long total = queryFactory
                .select(menuCategoryEntity.count())
                .from(menuCategoryEntity)
                .where(builder)
                .fetchOne();
        return total == null ? 0L : total;
    }
}
