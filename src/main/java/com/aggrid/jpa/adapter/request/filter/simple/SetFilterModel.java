package com.aggrid.jpa.adapter.request.filter.simple;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter
public class SetFilterModel implements SimpleFilterModel {
    private final String filterType = "set";
    private List<String> values = new ArrayList<>();

    @Override
    public Predicate toPredicate(CriteriaBuilder cb, Root<?> root, String colId) {
        return null;
    }
}
