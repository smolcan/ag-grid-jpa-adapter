package com.aggrid.jpa.adapter.request.filter.advanced;

import com.aggrid.jpa.adapter.request.filter.advanced.enums.ScalarAdvancedFilterModelType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class NumberAdvancedFilterModel implements ColumnAdvancedFilterModel {
    private final String filterType = "number";
    private String colId;
    private ScalarAdvancedFilterModelType type;
    private BigDecimal filter;

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        return null;
    }
}
