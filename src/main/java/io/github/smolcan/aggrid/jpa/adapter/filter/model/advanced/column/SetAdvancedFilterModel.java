package io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column;

import io.github.smolcan.aggrid.jpa.adapter.column.ColumnSource;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.ColumnAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.SetFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.AgSetColumnFilter;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Setter(onMethod_ = {@NonNull})
public class SetAdvancedFilterModel<E, T> extends ColumnAdvancedFilterModel<E, T> {

    private SetAdvancedFilterModelType type;
    private List<String> values = new ArrayList<>();
    private AgSetColumnFilter<T> columnFilter;

    public SetAdvancedFilterModel(@NonNull ColumnSource<? super E, T> columnField, @NonNull AgSetColumnFilter<T> columnFilter) {
        super("set", columnField);
        this.columnFilter = columnFilter;
    }

    @Override
    @NonNull
    public Predicate toPredicate(@NonNull CriteriaBuilder cb, @NonNull Root<? extends E> root) {
        Expression<T> path = this.getColumnField().getExpression(cb, root);

        SetFilterModel setFilterModel = new SetFilterModel();
        setFilterModel.setValues(this.values);
        Predicate anyOfPredicate = this.columnFilter.toPredicate(cb, path, setFilterModel);

        Predicate predicate;
        switch (this.type) {
            case isAnyOf: {
                predicate = anyOfPredicate;
                break;
            }
            case isNoneOf: {
                predicate = cb.not(anyOfPredicate);
                if (this.values.stream().noneMatch(Objects::isNull)) {
                    predicate = cb.or(predicate, cb.isNull(path));
                }
                break;
            }
            default: {
                throw new IllegalStateException("Unexpected value: " + this.type);
            }
        }

        return predicate;
    }
}
