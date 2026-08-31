package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced;

import io.github.smolcan.aggrid.jpa.adapter.column.ColumnSource;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

@Setter
@Getter
public abstract class ColumnAdvancedFilterModel<E, T> extends AdvancedFilterModel<E> {
    @NonNull
    private ColumnSource<? super E, T> columnField;
    
    protected ColumnAdvancedFilterModel(@NonNull String filterType, @NonNull ColumnSource<? super E, T> columnField) {
        super(filterType);
        this.columnField = columnField;
    }

}
