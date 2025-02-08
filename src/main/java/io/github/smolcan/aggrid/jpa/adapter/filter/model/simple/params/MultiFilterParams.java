package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiFilterParams implements IFilterParams {
    
    private List<IFilter<?, ?>> filters = new ArrayList<>();

    private MultiFilterParams(Builder builder) {
        this.filters = builder.filters;
    }

    public List<IFilter<?, ?>> getFilters() {
        return filters;
    }

    public void setFilters(List<IFilter<?, ?>> filters) {
        this.filters = filters;
    }

    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private List<IFilter<?, ?>> filters = new ArrayList<>();

        public Builder filters(List<IFilter<?, ?>> filters) {
            this.filters = new ArrayList<>(filters); // Ensures immutability
            return this;
        }

        public Builder filters(IFilter<?, ?>... filters) {
            this.filters = Arrays.asList(filters);
            return this;
        }

        public MultiFilterParams build() {
            return new MultiFilterParams(this);
        }
    }
}
