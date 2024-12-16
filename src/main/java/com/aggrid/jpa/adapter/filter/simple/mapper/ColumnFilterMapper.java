package com.aggrid.jpa.adapter.filter.simple.mapper;

import com.aggrid.jpa.adapter.filter.JoinOperator;
import com.aggrid.jpa.adapter.filter.simple.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class ColumnFilterMapper {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    public static boolean isColumnFilter(Map<String, Object> filterModel) {
        return filterModel.values().stream().allMatch(v -> v instanceof Map);
    }

    @SuppressWarnings("unchecked")
    public static ColumnFilter fromMap(Map<String, Object> filter) {
        String filterType = filter.get("filterType").toString();
        boolean isCombinedFilter = filter.containsKey("conditions");
        ColumnFilter columnFilter;
        switch (filterType) {
            case "text": {
                if (isCombinedFilter) {
                    CombinedSimpleModel<TextFilter> combinedTextFilter = new CombinedSimpleModel<>();
                    combinedTextFilter.setFilterType("text");
                    combinedTextFilter.setOperator(JoinOperator.valueOf(filter.get("operator").toString()));
                    combinedTextFilter.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(ColumnFilterMapper::parseTextFilter).collect(Collectors.toList()));
                    columnFilter = combinedTextFilter;
                } else {
                    columnFilter = parseTextFilter(filter);
                }
                break;
            }
            case "date": {
                if (isCombinedFilter) {
                    CombinedSimpleModel<DateFilter> combinedTextFilter = new CombinedSimpleModel<>();
                    combinedTextFilter.setFilterType("date");
                    combinedTextFilter.setOperator(JoinOperator.valueOf(filter.get("operator").toString()));
                    combinedTextFilter.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(ColumnFilterMapper::parseDateFilter).collect(Collectors.toList()));
                    columnFilter = combinedTextFilter;
                } else {
                    columnFilter = parseDateFilter(filter);
                }
                break;
            }
            case "number": {
                if (isCombinedFilter) {
                    CombinedSimpleModel<NumberFilter> combinedNumberFilter = new CombinedSimpleModel<>();
                    combinedNumberFilter.setFilterType("number");
                    combinedNumberFilter.setOperator(JoinOperator.valueOf(filter.get("operator").toString()));
                    combinedNumberFilter.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(ColumnFilterMapper::parseNumberFilter).collect(Collectors.toList()));
                    columnFilter = combinedNumberFilter;
                } else {
                    columnFilter = parseNumberFilter(filter);
                }
                break;
            }
            case "set": {
                columnFilter = parseSetFilter(filter);
                break;
            }
            case "multi": {
                columnFilter = parseMultiFilter(filter);
                break;
            }
            default: {
                columnFilter = null;
            }
        }
        
        return columnFilter;
    }

    @SuppressWarnings("unchecked")
    private static MultiFilter parseMultiFilter(Map<String, Object> filter) {
        MultiFilter multiFilter = new MultiFilter();
        if (filter.containsKey("filterModels") && filter.get("filterModels") != null) {
            multiFilter.setFilterModels(((List<Map<String, Object>>) filter.get("filterModels"))
                    .stream()
                    .filter(Objects::nonNull)
                    .map(ColumnFilterMapper::fromMap)
                    .collect(Collectors.toList())
            );
        }
        return multiFilter;
    }

    private static TextFilter parseTextFilter(Map<String, Object> filter) {
        TextFilter textFilter = new TextFilter();
        textFilter.setType(SimpleFilterModelType.valueOf(filter.get("type").toString()));
        textFilter.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).orElse(null));
        textFilter.setFilterTo(Optional.ofNullable(filter.get("filterTo")).map(Object::toString).orElse(null));
        return textFilter;
    }

    @SuppressWarnings("unchecked")
    private static SetFilter parseSetFilter(Map<String, Object> filter) {
        SetFilter setFilter = new SetFilter();
        setFilter.setValues((List<String>) filter.get("values"));
        return setFilter;
    }

    private static NumberFilter parseNumberFilter(Map<String, Object> filter) {
        NumberFilter numberFilter = new NumberFilter();
        numberFilter.setType(SimpleFilterModelType.valueOf(filter.get("type").toString()));
        numberFilter.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(BigDecimal::new).orElse(null));
        numberFilter.setFilterTo(Optional.ofNullable(filter.get("filterTo")).map(Object::toString).map(BigDecimal::new).orElse(null));
        return numberFilter;
    }

    private static DateFilter parseDateFilter(Map<String, Object> filter) {

        DateFilter dateFilter = new DateFilter();
        dateFilter.setType(SimpleFilterModelType.valueOf(filter.get("type").toString()));
        dateFilter.setDateFrom(Optional.ofNullable(filter.get("dateFrom")).map(Object::toString).map(d -> LocalDateTime.parse(d, DATE_TIME_FORMATTER)).orElse(null));
        dateFilter.setDateTo(Optional.ofNullable(filter.get("dateTo")).map(Object::toString).map(d -> LocalDateTime.parse(d, DATE_TIME_FORMATTER)).orElse(null));

        return dateFilter;
    }
    
}
