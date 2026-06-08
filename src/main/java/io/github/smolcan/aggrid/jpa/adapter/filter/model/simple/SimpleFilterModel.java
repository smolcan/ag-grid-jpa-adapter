package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Setter(onMethod_ = {@NonNull})
@Getter
@AllArgsConstructor(access = PROTECTED)
@NoArgsConstructor(access = PROTECTED)
public abstract class SimpleFilterModel extends ProvidedFilterModel {
    protected SimpleFilterModelType type;

    protected SimpleFilterModel(@NonNull String filterType) {
        super(filterType);
    }
    
    protected SimpleFilterModel(@NonNull String filterType, @NonNull SimpleFilterModelType type) {
        super(filterType);
        this.type = type;
    }

}
