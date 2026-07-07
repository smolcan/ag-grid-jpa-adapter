package io.github.smolcan.aggrid.jpa.adapter.column;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.request.AggregationFunction;
import jakarta.persistence.metamodel.SingularAttribute;
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
public class ColDef<P, T> {
    
    @NonNull
    private final FieldPath<P, T> field;
    // Set false to disable sorting which is enabled by default.
    private final boolean sortable;
    // Set to `true` if you want to be able to row group by this column
    private final boolean enableRowGroup;
    // Set to `true` if you want to be able to aggregate by this column
    private final boolean enableValue;
    private final boolean enablePivot;
    private final Set<String> allowedAggFuncs;
    private final IFilter<T, ?, ?> filter;
    
    @NonNull
    public static <P, T> Builder<P, T> builder(@NonNull SingularAttribute<P, T> field) {
        return new Builder<P, T>().field(FieldPath.of(field));
    }

    @NonNull
    public static <P, T> Builder<P, T> builder(@NonNull FieldPath<P, T> field) {
        return new Builder<P, T>().field(field);
    }

    public @NonNull String getFieldName() {
        return field.getName();
    }

    public static class Builder<P, T> {
        
        // default builder values
        private FieldPath<P, T> field;
        private boolean sortable = true;
        private Set<String> allowedAggFuncs;
        
        private Builder<P, T> field(@NonNull FieldPath<P, T> field) {
            this.field = field;
            return this;
        }
        
        @Tolerate
        @NonNull
        public Builder<P, T> allowedAggFuncs(@NonNull AggregationFunction ...functions) {
            this.allowedAggFuncs = Arrays.stream(functions).map(Enum::name).collect(Collectors.toSet());
            return this;
        }

        @Tolerate
        @NonNull
        public Builder<P, T> allowedAggFuncs(@NonNull String ...functions) {
            this.allowedAggFuncs = new HashSet<>(Arrays.asList(functions));
            return this;
        }
    }
}
