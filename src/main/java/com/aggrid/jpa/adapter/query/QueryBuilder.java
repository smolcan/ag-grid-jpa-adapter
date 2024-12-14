package com.aggrid.jpa.adapter.query;

import com.aggrid.jpa.adapter.filter.simple.mapper.ColumnFilterMapper;
import com.aggrid.jpa.adapter.filter.simple.model.ColumnFilter;
import com.aggrid.jpa.adapter.request.ColumnVO;
import com.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import com.aggrid.jpa.adapter.request.SortType;
import com.aggrid.jpa.adapter.filter.advanced.mapper.AdvancedFilterMapper;
import com.aggrid.jpa.adapter.filter.advanced.model.AdvancedFilterModel;
import com.aggrid.jpa.adapter.response.LoadSuccessParams;
import com.aggrid.jpa.adapter.utils.TypeValueSynchronizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.*;
import java.util.stream.Stream;

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
            Expression<? extends Number> aggregatedField;
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
            
            // try to synchronize col and key to same data type to prevent errors
            // for example, group key is date as string, but field is date, need to parse to date and then compare
            TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(root.get(groupCol), groupKey);
            Predicate groupPredicate = cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());
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
        
        // if grouping, ordering can be done on all fields
        // otherwise, only by grouped fields
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
        
        // ordering can be also done on aggregated fields
        if (isGrouping && !request.getValueCols().isEmpty()) {
            List<Order> orderByAggregatedCols = request.getSortModel().stream()
                    // not in grouped columns
                    .filter(model -> request.getRowGroupCols().stream().noneMatch(rgc -> rgc.getField().equals(model.getColId())))
                    // in aggregation columns
                    .filter(model -> request.getValueCols().stream().anyMatch(aggCol -> aggCol.getField().equals(model.getColId())))
                    .map(model -> {
                        ColumnVO aggregatedColumn = request.getValueCols().stream().filter(aggCol -> aggCol.getField().equals(model.getColId())).findFirst().orElseThrow();
                        Expression<? extends Number> aggregatedField;
                        switch (aggregatedColumn.getAggFunc()) {
                            case avg -> aggregatedField = cb.avg(root.get(model.getColId()));
                            case sum -> aggregatedField = cb.sum(root.get(model.getColId()));
                            case min -> aggregatedField = cb.min(root.get(model.getColId()));
                            case max -> aggregatedField = cb.max(root.get(model.getColId()));
                            case count -> aggregatedField = cb.count(root.get(model.getColId()));
                            default -> throw new IllegalArgumentException("unsupported aggregation function: " + aggregatedColumn.getAggFunc());
                        }
                        
                        if (model.getSort() == SortType.asc) {
                            return cb.asc(aggregatedField);
                        } else if (model.getSort() == SortType.desc) {
                            return cb.desc(aggregatedField);
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .toList();
            
            orderByCols = Stream.concat(orderByCols.stream(), orderByAggregatedCols.stream()).toList();
        }
        
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
        if (ColumnFilterMapper.isColumnFilter(filterModel)) {
            // simple filter
            // columnName: filter
            List<Predicate> predicates = filterModel.entrySet()
                    .stream()
                    .map(entry -> {
                        String columnName = entry.getKey();
                        Map<String, Object> filter = (Map<String, Object>) entry.getValue();

                        ColumnFilter columnFilter = ColumnFilterMapper.fromMap(filter);
                        return columnFilter.toPredicate(cb, root, columnName);
                    })
                    .toList();
            
            predicate = cb.and(predicates.toArray(new Predicate[0]));
        } else {
            // advanced filter
            AdvancedFilterModel advancedFilterModel = AdvancedFilterMapper.fromMap(filterModel);
            predicate = advancedFilterModel.toPredicate(cb, root);
        }
        
        return predicate;
    }
}
