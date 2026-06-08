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
public class MultiFilterParams implements IFilterParams {
    
    @NonNull
    private List<IFilter<?, ?>> filters;
    
    public static class Builder {
        private List<IFilter<?, ?>> filters = new ArrayList<>();

        @Tolerate
        public Builder filters(@NonNull IFilter<?, ?>... filters) {
            this.filters = Arrays.asList(filters);
            return this;
        }
    }
}
