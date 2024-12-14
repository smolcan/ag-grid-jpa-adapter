package com.aggrid.jpa.adapter.filter.advanced.mapper;

import com.aggrid.jpa.adapter.filter.JoinOperator;
import com.aggrid.jpa.adapter.filter.advanced.model.AdvancedFilterModel;
import com.aggrid.jpa.adapter.filter.advanced.model.ColumnAdvancedFilterModel;
import com.aggrid.jpa.adapter.filter.advanced.model.JoinAdvancedFilterModel;
import com.aggrid.jpa.adapter.filter.advanced.model.column.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class AdvancedFilterMapper {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @SuppressWarnings("unchecked")
    public static AdvancedFilterModel fromMap(Map<String, Object> filter) {
        String filterType = filter.get("filterType").toString();
        if (filterType.equals("join")) {
            // join
            JoinAdvancedFilterModel joinAdvancedFilterModel = new JoinAdvancedFilterModel();
            joinAdvancedFilterModel.setType(JoinOperator.valueOf(filter.get("type").toString()));
            joinAdvancedFilterModel.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(AdvancedFilterMapper::fromMap).toList());

            return joinAdvancedFilterModel;
        } else {
            // column
            return parseColumnAdvancedFilter(filter);
        }
    }

    private static ColumnAdvancedFilterModel parseColumnAdvancedFilter(Map<String, Object> filter) {

        String colId = filter.get("colId").toString();
        String filterType = filter.get("filterType").toString();

        switch (filterType) {
            case "text" -> {
                TextAdvancedFilterModel textAdvancedFilterModel = new TextAdvancedFilterModel(colId);
                textAdvancedFilterModel.setType(TextAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                textAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).orElse(null));
                return textAdvancedFilterModel;
            }
            case "date" -> {
                DateAdvancedFilterModel dateAdvancedFilterModel = new DateAdvancedFilterModel(colId);
                dateAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                dateAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(f -> LocalDate.parse(f, DATE_FORMATTER)).orElse(null));
                return dateAdvancedFilterModel;
            }
            case "dateString" -> {
                DateStringAdvancedFilterModel dateAdvancedFilterModel = new DateStringAdvancedFilterModel(colId);
                dateAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                dateAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(f -> LocalDate.parse(f, DATE_FORMATTER)).orElse(null));
                return dateAdvancedFilterModel;
            }
            case "number" -> {
                NumberAdvancedFilterModel numberAdvancedFilterModel = new NumberAdvancedFilterModel(colId);
                numberAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                numberAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(BigDecimal::new).orElse(null));
                return numberAdvancedFilterModel;
            }
            case "object" -> {
                ObjectAdvancedFilterModel objectAdvancedFilterModel = new ObjectAdvancedFilterModel(colId);
                objectAdvancedFilterModel.setType(TextAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                objectAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).orElse(null));
                return objectAdvancedFilterModel;
            }
            case "boolean" -> {
                BooleanAdvancedFilterModel booleanAdvancedFilterModel = new BooleanAdvancedFilterModel(colId);
                booleanAdvancedFilterModel.setType(Optional.ofNullable(filter.get("type")).map(Object::toString).map(v -> {
                    if (v.equalsIgnoreCase("true")) {
                        return BooleanAdvancedFilterModelType.TRUE;
                    } else if (v.equalsIgnoreCase("false")) {
                        return BooleanAdvancedFilterModelType.FALSE;
                    } else {
                        return BooleanAdvancedFilterModelType.valueOf(v);
                    }
                }).orElseThrow());
                return booleanAdvancedFilterModel;
            }
            default -> throw new UnsupportedOperationException("Unsupported filter type: " + filterType);
        }

    }
    
    
}
