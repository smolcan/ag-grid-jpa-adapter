package com.aggrid.jpa.adapter.request.filter.simple;

import com.aggrid.jpa.adapter.request.filter.simple.enums.SimpleFilterModelType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter @Setter
public class NumberFilterModel implements SimpleFilterModel {
    private final String filterType = "number";
    private SimpleFilterModelType type;
    private BigDecimal filter;
    private BigDecimal filterTo;

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String colId) {
        return null;
    }
}
