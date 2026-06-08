package io.github.smolcan.aggrid.jpa.adapter.filter.model.simple;

import io.github.smolcan.aggrid.jpa.adapter.filter.model.IFilterModel;
import lombok.*;

import static lombok.AccessLevel.PROTECTED;

@Setter
@Getter
@AllArgsConstructor(access = PROTECTED)
@NoArgsConstructor(access = PROTECTED)
public abstract class ProvidedFilterModel implements IFilterModel {
    @NonNull
    private String filterType;

}
