package io.github.smolcan.aggrid.jpa.adapter.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;

/**
 * Column definition, tries to be same as in frontend
 */
public class ColDef {
    
    private final String field;
    private final IFilter<?, ?> filter;

    private ColDef(Builder builder) {
        this.field = builder.field;
        this.filter = builder.filter;
    }

    public String getField() {
        return field;
    }

    public IFilter<?, ?> getFilter() {
        return filter;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String field;
        private IFilter<?, ?> filter = new AgTextColumnFilter();

        public Builder field(String field) {
            this.field = field;
            return this;
        }

        public Builder filter(IFilter<?, ?> filter) {
            this.filter = filter;
            return this;
        }

        public ColDef build() {
            if (this.field == null) {
                throw new IllegalArgumentException("field cannot be null");
            }
            
            return new ColDef(this);
        }
    }
}
