package com.aggrid.jpa.adapter.query;

import com.aggrid.jpa.adapter.request.ColumnVO;
import com.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import com.aggrid.jpa.adapter.request.SortType;
import com.aggrid.jpa.adapter.request.filter.simple.*;
import com.aggrid.jpa.adapter.response.LoadSuccessParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Integer.MAX_VALUE;

public class QueryBuilder<E> {
    
    private final Class<E> entityClass;
    private final EntityManager entityManager;
    
    public QueryBuilder(Class<E> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        
    }
    
    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();

        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<E> root = query.from(this.entityClass);
        
        this.select(cb, query, root, request);
        this.where(cb, query, root, request);
        this.groupBy(cb, query, root, request);
        this.orderBy(cb, query, root, request);

        TypedQuery<Tuple> typedQuery = this.entityManager.createQuery(query);
        this.limitOffset(typedQuery, request);

        List<Tuple> data = typedQuery.getResultList();
        
        LoadSuccessParams loadSuccessParams = new LoadSuccessParams();
        loadSuccessParams.setRowData(tupleToMap(data));
        return loadSuccessParams;
    }
    
    
    private void select(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
        // select
        // we know data are still grouped if request contains more group columns than group keys
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (!isGrouping) {
            // SELECT * from root if not grouping
            this.selectAll(query, root);
            return;
        }

        List<Selection<?>> selections = new ArrayList<>();
        // group columns
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
            ColumnVO groupCol = request.getRowGroupCols().get(i);
            selections.add(root.get(groupCol.getField()).alias(groupCol.getField()));
        }
        // aggregated columns
        for (ColumnVO columnVO : request.getValueCols()) {
            Expression<?> aggregatedField;
            switch (columnVO.getAggFunc()) {
                case avg -> aggregatedField = cb.avg(root.get(columnVO.getField()));
                case sum -> aggregatedField = cb.sum(root.get(columnVO.getField()));
                case min -> aggregatedField = cb.min(root.get(columnVO.getField()));
                case max -> aggregatedField = cb.max(root.get(columnVO.getField()));
                case count -> aggregatedField = cb.count(root.get(columnVO.getField()));
                default -> throw new IllegalArgumentException("unsupported aggregation function: " + columnVO.getAggFunc());
            }
            selections.add(aggregatedField.alias(columnVO.getField()));
        }

        query.multiselect(selections);
    }
    
    private void selectAll(CriteriaQuery<Tuple> query, Root<E> root) {
        List<Selection<?>> selections = new ArrayList<>();
        
        // fetch all fields from given entity
        Metamodel metamodel = this.entityManager.getMetamodel();
        EntityType<E> entityType = metamodel.entity(this.entityClass);
        for (SingularAttribute<? super E, ?> attribute : entityType.getDeclaredSingularAttributes()) {
            Path<?> field = root.get(attribute.getName());
            selections.add(field.alias(attribute.getName()));
        }

        query.multiselect(selections);
    }
    
    
    private void where(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
        // where
        List<Predicate> predicates = new ArrayList<>();
        
        // must add where statement for every group column that also has a key (was expanded)
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size(); i++) {
            String groupKey = request.getGroupKeys().get(i);
            String groupCol = request.getRowGroupCols().get(i).getField();

            Predicate groupPredicate = cb.equal(root.get(groupCol), groupKey);
            predicates.add(groupPredicate);
        }
        
        // filter where
        if (request.getFilterModel() != null) {
            Predicate filterPredicate = this.filterToPredicate(cb, root, request.getFilterModel());
            predicates.add(filterPredicate);
        }
        
        query.where(predicates.toArray(new Predicate[0]));
    }
    
    private void groupBy(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
        // we know data are still grouped if request contains more group columns than group keys
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (isGrouping) {
            List<Expression<?>> groupByExpressions = new ArrayList<>();
            for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
                String groupCol = request.getRowGroupCols().get(i).getField();
                groupByExpressions.add(root.get(groupCol));
            }
            query.groupBy(groupByExpressions);
        }
    }
    
    private void orderBy(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
        // we know data are still grouped if request contains more group columns than group keys
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        int limit = isGrouping ? request.getGroupKeys().size() + 1 : MAX_VALUE;
        
        List<Order> orderByCols = request.getSortModel().stream()
                .filter(model -> !isGrouping || request.getRowGroupCols().stream().anyMatch(rgc -> rgc.getField().equals(model.getColId())))
                .map(sortModel -> {
                    if (sortModel.getSort() == SortType.asc) {
                        return cb.asc(root.get(sortModel.getColId()));
                    } else if (sortModel.getSort() == SortType.desc) {
                        return cb.desc(root.get(sortModel.getColId()));
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
        
        query.orderBy(orderByCols);
    }
    
    private void limitOffset(TypedQuery<Tuple> typedQuery, ServerSideGetRowsRequest request) {
        typedQuery.setFirstResult(request.getStartRow());
        typedQuery.setMaxResults(request.getEndRow() - request.getStartRow() + 1);
    }
    
    private static List<Map<String, Object>> tupleToMap(List<Tuple> tuples) {
        return tuples.stream()
                .map(tuple -> {
                    Map<String, Object> map = new HashMap<>();
                    tuple.getElements().forEach(element -> {
                        String alias = element.getAlias();
                        if (alias != null) {
                            map.put(alias, tuple.get(alias));
                        }
                    });
                    return map;
                })
                .toList();
    }

    @SuppressWarnings("unchecked")
    private Predicate filterToPredicate(CriteriaBuilder cb, Root<E> root, Map<String, Object> filterModel) {
        
        Predicate predicate;
        if (filterModel.values().stream().allMatch(v -> v instanceof Map)) {
            // simple filter
            // columnName: filter
            List<Predicate> predicates = new ArrayList<>();
            filterModel.forEach((columnName, o) -> {
                Map<String, Object> filter = (Map<String, Object>) o;
                
                String filterType = filter.get("filterType").toString();
                boolean isCombinedFilter = filter.containsKey("conditions");
                ColumnFilter columnFilter;
                switch (filterType) {
                    case "text" -> {
                        if (isCombinedFilter) {
                            CombinedSimpleModel<TextFilter> combinedTextFilter = new CombinedSimpleModel<>();
                            combinedTextFilter.setFilterType("text");
                            combinedTextFilter.setOperator(JoinOperator.valueOf(filter.get("operator").toString()));
                            combinedTextFilter.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(this::parseTextFilter).toList());
                            columnFilter = combinedTextFilter;
                        } else {
                            columnFilter = this.parseTextFilter(filter);
                        }
                    }
                    case "date" -> {
                        if (isCombinedFilter) {
                            CombinedSimpleModel<DateFilter> combinedTextFilter = new CombinedSimpleModel<>();
                            combinedTextFilter.setFilterType("date");
                            combinedTextFilter.setOperator(JoinOperator.valueOf(filter.get("operator").toString()));
                            combinedTextFilter.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(this::parseDateFilter).toList());
                            columnFilter = combinedTextFilter;
                        } else {
                            columnFilter = this.parseDateFilter(filter);
                        }
                    }
                    case "number" -> {
                        if (isCombinedFilter) {
                            CombinedSimpleModel<NumberFilter> combinedNumberFilter = new CombinedSimpleModel<>();
                            combinedNumberFilter.setFilterType("number");
                            combinedNumberFilter.setOperator(JoinOperator.valueOf(filter.get("operator").toString()));
                            combinedNumberFilter.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(this::parseNumberFilter).toList());
                            columnFilter = combinedNumberFilter;
                        } else {
                            columnFilter = this.parseNumberFilter(filter);
                        }
                    }
                    case "set" -> columnFilter = parseSetFilter(filter);
                    default -> throw new IllegalArgumentException("unsupported filter type: " + filterType);
                }
                
                predicates.add(columnFilter.toPredicate(cb, root, columnName));
            });
            
            predicate = cb.and(predicates.toArray(new Predicate[0]));
        } else {
            // advanced filter
            
            predicate = cb.and();
        }
        
        return predicate;
    }
    
    
    private TextFilter parseTextFilter(Map<String, Object> filter) {
        TextFilter textFilter = new TextFilter();
        textFilter.setType(SimpleFilterModelType.valueOf(filter.get("type").toString()));
        textFilter.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).orElse(null));
        textFilter.setFilterTo(Optional.ofNullable(filter.get("filterTo")).map(Object::toString).orElse(null));
        return textFilter;
    }

    @SuppressWarnings("unchecked")
    private SetFilter parseSetFilter(Map<String, Object> filter) {
        SetFilter setFilter = new SetFilter();
        setFilter.setValues((List<String>) filter.get("values"));
        return setFilter;
    }
    
    private NumberFilter parseNumberFilter(Map<String, Object> filter) {
        NumberFilter numberFilter = new NumberFilter();
        numberFilter.setType(SimpleFilterModelType.valueOf(filter.get("type").toString()));
        numberFilter.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(BigDecimal::new).orElse(null));
        numberFilter.setFilterTo(Optional.ofNullable(filter.get("filterTo")).map(Object::toString).map(BigDecimal::new).orElse(null));
        return numberFilter;
    }
    
    private DateFilter parseDateFilter(Map<String, Object> filter) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        
        DateFilter dateFilter = new DateFilter();
        dateFilter.setType(SimpleFilterModelType.valueOf(filter.get("type").toString()));
        dateFilter.setDateFrom(Optional.ofNullable(filter.get("dateFrom")).map(Object::toString).map(d -> LocalDateTime.parse(d, formatter)).orElse(null));
        dateFilter.setDateTo(Optional.ofNullable(filter.get("dateTo")).map(Object::toString).map(d -> LocalDateTime.parse(d, formatter)).orElse(null));

        return dateFilter;
    }
}
