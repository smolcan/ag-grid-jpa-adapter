package io.github.smolcan.aggrid.jpa.adapter.filter.advanced;


import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public abstract class AdvancedFilterModel {
    private String filterType;
    
    public AdvancedFilterModel(String filterType) {
        this.filterType = filterType;
    }

    public String getFilterType() {
        return filterType;
    }

    public void setFilterType(String filterType) {
        this.filterType = filterType;
    }
    
    public abstract Predicate toPredicate(CriteriaBuilder cb, Root<?> root);
}
