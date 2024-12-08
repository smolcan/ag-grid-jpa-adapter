package com.aggrid.jpa.adapter;

import com.aggrid.jpa.adapter.request.ColumnVO;
import com.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import com.aggrid.jpa.adapter.request.SortModel;
import com.aggrid.jpa.adapter.request.filter.NumberColumnFilter;
import com.aggrid.jpa.adapter.response.LoadSuccessParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

public class QueryBuilder<E> {
    
    protected final Class<E> entityClass;
    protected final EntityManager entityManager;
    
    public QueryBuilder(Class<E> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }
    
    public LoadSuccessParams<E> getRows(ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();

        CriteriaQuery<E> query = cb.createQuery(this.entityClass);
        Root<E> root = query.from(this.entityClass);
        
        this.select(cb, query, root, request);
        this.where(cb, query, root, request);
        this.groupBy(cb, query, root, request);
        this.orderBy(cb, query, root, request);

        TypedQuery<E> typedQuery = this.entityManager.createQuery(query);
        this.limitOffset(typedQuery, request);

        List<E> data = typedQuery.getResultList();
        
        LoadSuccessParams<E> loadSuccessParams = new LoadSuccessParams<>();
        loadSuccessParams.setRowData(data);
        return loadSuccessParams;
    }
    
    
    private void select(CriteriaBuilder cb, CriteriaQuery<E> query, Root<E> root, ServerSideGetRowsRequest request) {
        // select
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (!isGrouping) {
            // SELECT * if not grouping
            query.select(root);
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
                Expression<? extends Number> aggregatedField;
                switch (columnVO.getAggFunc()) {
                    case "avg" -> {
                        Expression<Double> avgResult = cb.avg(root.get(columnVO.getField()));

                        Class<?> fieldNumberType = root.get(columnVO.getField()).getJavaType();
                        if (fieldNumberType == Double.class) {
                            aggregatedField = avgResult;
                        } else if (fieldNumberType == Long.class) {
                            aggregatedField = cb.toLong(avgResult);
                        } else if (fieldNumberType == Integer.class) {
                            aggregatedField = cb.toInteger(avgResult);
                        } else if (fieldNumberType == Float.class) {
                            aggregatedField = cb.toFloat(avgResult);
                        } else if (fieldNumberType == BigDecimal.class) {
                            aggregatedField = cb.toBigDecimal(avgResult);
                        } else if (fieldNumberType == BigInteger.class) {
                            aggregatedField = cb.toBigInteger(avgResult);
                        } else {
                            throw new IllegalStateException();
                        }
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
                        Expression<Long> countExpr = cb.count(root.get(columnVO.getField()));

                        Class<?> fieldNumberType = root.get(columnVO.getField()).getJavaType();
                        if (fieldNumberType == Double.class) {
                            aggregatedField = cb.toDouble(countExpr);
                        } else if (fieldNumberType == Long.class) {
                            aggregatedField = countExpr;
                        } else if (fieldNumberType == Integer.class) {
                            aggregatedField = cb.toInteger(countExpr);
                        } else if (fieldNumberType == Float.class) {
                            aggregatedField = cb.toFloat(countExpr);
                        } else if (fieldNumberType == BigDecimal.class) {
                            aggregatedField = cb.toBigDecimal(countExpr);
                        } else if (fieldNumberType == BigInteger.class) {
                            aggregatedField = cb.toBigInteger(countExpr);
                        } else {
                            throw new IllegalStateException();
                        }
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
    
    
    private void where(CriteriaBuilder cb, CriteriaQuery<E> query, Root<E> root, ServerSideGetRowsRequest request) {
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
                switch (key) {
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
            }

            // todo: another types of filter
        });
        query.where(predicates.toArray(new Predicate[0]));
    }
    
    private void groupBy(CriteriaBuilder cb, CriteriaQuery<E> query, Root<E> root, ServerSideGetRowsRequest request) {
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
    
    private void orderBy(CriteriaBuilder cb, CriteriaQuery<E> query, Root<E> root, ServerSideGetRowsRequest request) {
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        // order by
        if (!request.getSortModel().isEmpty()) {

            List<Order> orders = new ArrayList<>(request.getSortModel().size());
            for (int i = 0; i < request.getSortModel().size(); i++) {
                SortModel sortModel = request.getSortModel().get(i);
                if (isGrouping && request.getRowGroupCols().stream().noneMatch(c -> c.getField().equals(sortModel.getColId()))) {
                    continue;
                }

                if (sortModel.getSort().equalsIgnoreCase("asc")) {
                    orders.add(cb.asc(root.get(sortModel.getColId())));
                } else if (sortModel.getSort().equalsIgnoreCase("desc")) {
                    orders.add(cb.desc(root.get(sortModel.getColId())));
                } else {
                    throw new IllegalArgumentException("Unsupported sort type: " + sortModel.getSort());
                }
            }

            query.orderBy(orders);
        }
    }
    
    private void limitOffset(TypedQuery<E> typedQuery, ServerSideGetRowsRequest request) {
        typedQuery.setFirstResult(request.getStartRow());
        typedQuery.setMaxResults(request.getEndRow() - request.getStartRow() + 1);
    }
    
}
