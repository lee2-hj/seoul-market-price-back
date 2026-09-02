package com.seoul.market.seoulmarketprice.menus.repository.customImpl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.seoul.market.seoulmarketprice.menus.entity.MenuEntity;
import com.seoul.market.seoulmarketprice.menus.repository.custom.MenuRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.seoul.market.seoulmarketprice.menus.entity.QMenuEntity.menuEntity;

@RequiredArgsConstructor
public class MenuRepositoryCustomImpl implements MenuRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<MenuEntity> findmenuCategoryCodeOrmenuCodeOrmenuName(String menuCategoryCode, String menuCode, String menuName, long offset, int size) {
        BooleanBuilder builder = new BooleanBuilder();

        if(StringUtils.hasText(menuCategoryCode)){
            builder.or(menuEntity.category.menuCode.eq(menuCategoryCode));
        }

        if(StringUtils.hasText(menuCode)){
            builder.or(menuEntity.menuCode.like("%" +menuCode+"%"));
        }

        if(StringUtils.hasText(menuName)){
            builder.or(menuEntity.menuName.like("%" +menuName+"%"));
        }

        return queryFactory
                .selectFrom(menuEntity)
                .where(builder)
                .offset(offset)
                .limit(size)
                .fetch();

    }

    @Override
    public long countmenuCategoryCodeOrmenuCodeOrmenuName(String menuCategoryCode, String menuCode, String menuName) {
        BooleanBuilder builder = new BooleanBuilder();

        if(StringUtils.hasText(menuCategoryCode)){
            builder.or(menuEntity.category.menuCode.eq(menuCategoryCode));
        }

        if(StringUtils.hasText(menuCode)){
            builder.or(menuEntity.menuCode.like("%" +menuCode+"%"));
        }

        if(StringUtils.hasText(menuName)){
            builder.or(menuEntity.menuName.like("%" +menuName+"%"));
        }

        Long total = queryFactory
                .select(menuEntity.count())
                .from(menuEntity)
                .where(builder)
                .fetchOne();
        return total == null ? 0L : total;
    }
}
