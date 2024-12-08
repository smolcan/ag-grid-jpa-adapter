package com.aggrid.jpa.adapter;

import com.aggrid.jpa.adapter.request.ColumnVO;
import com.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import com.aggrid.jpa.adapter.request.filter.NumberColumnFilter;
import com.aggrid.jpa.adapter.request.filter.TextColumnFilter;
import com.aggrid.jpa.adapter.response.LoadSuccessParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

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
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (!isGrouping) {
            // SELECT * from root if not grouping
            this.selectAll(cb, query, root);
            return;
        }

        List<Selection<?>> selections = new ArrayList<>();
        // group columns
        for (int i = 0; i < request.getRowGroupCols().size(); i++) {
            ColumnVO groupCol = request.getRowGroupCols().get(i);
            selections.add(root.get(groupCol.getField()).alias(groupCol.getField()));
        }
        // aggregated columns
        for (int i = 0; i < request.getValueCols().size(); i++) {
            ColumnVO columnVO = request.getValueCols().get(i);

            if (columnVO.getAggFunc() == null) {
                selections.add(root.get(columnVO.getField()).alias(columnVO.getField()));
            } else {
                // aggregation function on field, must be number type
                Expression<?> aggregatedField;
                switch (columnVO.getAggFunc()) {
                    case "avg" -> {
                       aggregatedField = cb.avg(root.get(columnVO.getField()));
                    }
                    case "sum" -> {
                        aggregatedField = cb.sum(root.get(columnVO.getField()));
                    }
                    case "min" -> {
                        aggregatedField = cb.min(root.get(columnVO.getField()));
                    }
                    case "max" -> {
                        aggregatedField = cb.max(root.get(columnVO.getField()));
                    }
                    case "count" -> {
                        aggregatedField = cb.count(root.get(columnVO.getField()));
                    }
                    default -> {
                        throw new IllegalArgumentException("unsupported aggregation function: " + columnVO.getAggFunc());
                    }
                }
                selections.add(aggregatedField.alias(columnVO.getField()));
            }
        }

        query.multiselect(selections);
    }
    
    private void selectAll(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root) {
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
        // grouping where
        for (int i = 0; i < request.getGroupKeys().size() && i < request.getRowGroupCols().size(); i++) {
            String groupKey = request.getGroupKeys().get(i);
            String groupCol = request.getRowGroupCols().get(i).getField();

            Predicate predicate = cb.equal(root.get(groupCol), groupKey);
            predicates.add(predicate);
        }
        // filter where
        request.getFilterModel().forEach((key, filter) -> {
            if (filter instanceof NumberColumnFilter numberColumnFilter) {

                Path<? extends Number> field = root.get(key);
                Predicate numberFilterPredicate = null;
                switch (numberColumnFilter.getType()) {
                    case "inRange" -> {
                        Integer lower = numberColumnFilter.getFilter();
                        Integer upper = numberColumnFilter.getFilterTo();

                        Predicate lowerPredicate = cb.ge(field, lower);
                        Predicate upperPredicate = cb.le(field, upper);
                        numberFilterPredicate = cb.and(lowerPredicate, upperPredicate);
                    }
                    case "equals" -> {
                        numberFilterPredicate = cb.equal(field, numberColumnFilter.getFilter());
                    }
                    case "notEqual" -> {
                        numberFilterPredicate = cb.notEqual(field, numberColumnFilter.getFilter());
                    }
                    case "lessThan" -> {
                        numberFilterPredicate = cb.lt(field, numberColumnFilter.getFilter());
                    }
                    case "lessThanOrEqual" -> {
                        numberFilterPredicate = cb.le(field, numberColumnFilter.getFilter());
                    }
                    case "greaterThan" -> {
                        numberFilterPredicate = cb.gt(field, numberColumnFilter.getFilter());
                    }
                    case "greaterThanOrEqual" -> {
                        numberFilterPredicate = cb.ge(field, numberColumnFilter.getFilter());
                    }
                }

                if (numberFilterPredicate != null) {
                    predicates.add(numberFilterPredicate);
                }
            } else if (filter instanceof TextColumnFilter textColumnFilter) {
                Path<String> field = root.get(key);
                Predicate textFilterPredicate = null;
                switch (textColumnFilter.getType()) {
                    case "contains" -> {
                        textFilterPredicate = cb.like(field, "%" + textColumnFilter.getFilter() + "%");
                    }
                    case "notContains" -> {
                        textFilterPredicate = cb.notLike(field, "%" + textColumnFilter.getFilter() + "%");
                    }
                    case "equals" -> {
                        textFilterPredicate = cb.equal(field, textColumnFilter.getFilter());
                    }
                    case "notEquals" -> {
                        textFilterPredicate = cb.notEqual(field, textColumnFilter.getFilter());
                    }
                    case "beginsWith" -> {
                        textFilterPredicate = cb.like(field, textColumnFilter.getFilter() + "%");
                    }
                    case "endsWith" -> {
                        textFilterPredicate = cb.like(field, "%" + textColumnFilter.getFilter());
                    }
                    case "blank" -> {
                        textFilterPredicate = cb.or(cb.isNull(field), cb.equal(field, ""));
                    }
                    case "notBlank" -> {
                        textFilterPredicate = cb.and(cb.isNotNull(field), cb.notEqual(field, ""));
                    }
                }
                
                if (textFilterPredicate != null) {
                    predicates.add(textFilterPredicate);
                }
            }

            // todo: another types of filter
        });
        query.where(predicates.toArray(new Predicate[0]));
    }
    
    private void groupBy(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (isGrouping) {
            List<Expression<?>> groupByExpressions = new ArrayList<>();
            for (int i = 0; i < request.getRowGroupCols().size(); i++) {
                String groupCol = request.getRowGroupCols().get(i).getField();
                groupByExpressions.add(root.get(groupCol));
            }
            query.groupBy(groupByExpressions);
        }
    }
    
    private void orderBy(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        int limit = isGrouping ? request.getGroupKeys().size() + 1 : MAX_VALUE;
        
        List<Order> orderByCols = request.getSortModel().stream()
                .filter(model -> {
                    if (isGrouping) {
                        return
                                request.getRowGroupCols().stream().anyMatch(c -> c.getField().equals(model.getColId()))
                                || request.getValueCols().stream().anyMatch(c -> c.getField().equals(model.getColId()));
                    } else {
                        try {
                            root.get(model.getColId());
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                })
                .filter(Objects::nonNull)
                .map(sortModel -> {
                    Path<?> field = root.get(sortModel.getColId());
                    if (sortModel.getSort().equalsIgnoreCase("asc")) {
                        return cb.asc(root.get(sortModel.getColId()));
                    } else if (sortModel.getSort().equalsIgnoreCase("desc")) {
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
    
}
