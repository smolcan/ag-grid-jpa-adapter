package com.aggrid.jpa.adapter.request.filter.simple;

import com.aggrid.jpa.adapter.request.filter.simple.enums.SimpleFilterModelType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter @Setter
public class DateFilterModel implements SimpleFilterModel {
    private final String filterType = "date";
    private SimpleFilterModelType type;
    private LocalDateTime dateFrom;
    private LocalDateTime dateTo;

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String colId) {
        return null;
    }
}
