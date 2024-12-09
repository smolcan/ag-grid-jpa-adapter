package com.aggrid.jpa.adapter.request.filter.simple;


import com.aggrid.jpa.adapter.request.filter.simple.enums.SimpleFilterModelType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TextFilterModel implements SimpleFilterModel {
    private final String filterType = "text";
    private SimpleFilterModelType type;
    private String filter;
    private String filterTo;

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String colId) {
        return null;
    }
}
