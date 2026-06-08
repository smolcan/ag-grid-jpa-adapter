package io.github.smolcan.aggrid.jpa.adapter.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.request.AggregationFunction;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Tolerate;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Column definition, tries to be same as in frontend
 */
@Getter
@Builder(builderClassName = "Builder")
public class ColDef {
    
    @NonNull
    private final String field;
    // Set false to disable sorting which is enabled by default.
    private final boolean sortable;
    // Set to `true` if you want to be able to row group by this column
    private final boolean enableRowGroup;
    // Set to `true` if you want to be able to aggregate by this column
    private final boolean enableValue;
    private final boolean enablePivot;
    private final Set<String> allowedAggFuncs;
    private final IFilter<?, ?> filter;

    public static class Builder {
        
        // default builder values
        private boolean sortable = true;
        private IFilter<?, ?> filter = new AgTextColumnFilter();
        
        // overloaded builder methods
        @Tolerate
        public Builder filter(boolean filter) {
            if (filter) {
                this.filter = new AgTextColumnFilter();
            } else {
                this.filter = null;
            }
            return this;
        }
        
        @Tolerate
        public Builder allowedAggFuncs(@NonNull AggregationFunction ...functions) {
            this.allowedAggFuncs = Arrays.stream(functions).map(Enum::name).collect(Collectors.toSet());
            return this;
        }
        
        @Tolerate
        public Builder allowedAggFuncs(@NonNull String ...functions) {
            this.allowedAggFuncs = new HashSet<>(Arrays.asList(functions));
            return this;
        }
    }
}
