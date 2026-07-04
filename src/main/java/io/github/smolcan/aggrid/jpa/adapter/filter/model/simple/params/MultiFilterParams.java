package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params;

import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Tolerate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Setter
@Getter
@Builder(builderClassName = "Builder")
public class MultiFilterParams<T> implements IFilterParams {
    
    @NonNull
    private List<IFilter<T, ?, ?>> filters;

    public static class Builder<T> {
        private List<IFilter<T, ?, ?>> filters = new ArrayList<>();

        @Tolerate
        public Builder<T> filters(@NonNull IFilter<T, ?, ?>... filters) {
            this.filters = Arrays.asList(filters);
            return this;
        }
    }
}
