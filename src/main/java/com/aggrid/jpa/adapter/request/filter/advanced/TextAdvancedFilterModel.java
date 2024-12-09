package com.aggrid.jpa.adapter.request.filter.advanced;

import com.aggrid.jpa.adapter.request.filter.advanced.enums.TextAdvancedFilterModelType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TextAdvancedFilterModel implements ColumnAdvancedFilterModel {
    private final String filterType = "text";
    private String colId;
    private TextAdvancedFilterModelType type;
    private String filter;

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root) {
        return null;
    }
}
