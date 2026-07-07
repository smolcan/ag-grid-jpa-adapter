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
    
    /**
     * @param filters the filters combined by this multi filter (all typed to the column type {@code T}).
     * @return the filters combined by this multi filter.
     */
    @NonNull
    private List<IFilter<T, ?, ?>> filters;

    public static class Builder<T> {
        private List<IFilter<T, ?, ?>> filters = new ArrayList<>();

        @SafeVarargs
        @Tolerate
        @NonNull
        public final Builder<T> filters(@NonNull IFilter<T, ?, ?>... filters) {
            this.filters = Arrays.asList(filters);
            return this;
        }
    }
}
