package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import static lombok.AccessLevel.PROTECTED;

@Setter
@Getter
@AllArgsConstructor(access = PROTECTED)
public abstract class AdvancedFilterModel<E> {
    @NonNull
    private String filterType;

    public abstract Predicate toPredicate(CriteriaBuilder cb, Root<E> root);
}
