package io.github.smolcan.aggrid.jpa.adapter.utils.pivoting;

import io.github.smolcan.aggrid.jpa.adapter.request.ColumnVO;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.utils.CartesianProductHelper;
import io.github.smolcan.aggrid.jpa.adapter.utils.Pair;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class PivotingContextHelper<E> {

    private final Class<E> entityClass;
    private final EntityManager entityManager;
    private final CriteriaBuilder cb;
    private final Root<E> root;
    private final ServerSideGetRowsRequest request;
    private final String serverSidePivotResultFieldSeparator;

    public PivotingContextHelper(Class<E> entityClass, EntityManager entityManager, CriteriaBuilder cb, Root<E> root, ServerSideGetRowsRequest request, String serverSidePivotResultFieldSeparator) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        this.cb = cb;
        this.root = root;
        this.request = request;
        this.serverSidePivotResultFieldSeparator = serverSidePivotResultFieldSeparator;
    }

    public PivotingContext createPivotingContext() {
        
        PivotingContext pivotingContext = new PivotingContext();
        if (!this.request.isPivotMode() || this.request.getPivotCols().isEmpty()) {
            // no pivoting
            pivotingContext.setPivoting(false);
        } else {
            pivotingContext.setPivoting(true);
            pivotingContext.setPivotValues(this.getPivotValues());
            pivotingContext.setPivotPairs(this.createPivotPairs(pivotingContext.getPivotValues()));
            pivotingContext.setCartesianProduct(CartesianProductHelper.cartesianProduct(pivotingContext.getPivotPairs()));
            pivotingContext.setPivotingSelections(this.createPivotingSelections(pivotingContext.getPivotValues()));
            pivotingContext.setPivotingResultFields(this.createPivotResultFields(pivotingContext.getCartesianProduct()));
        }

        return pivotingContext;
    }

    private List<String> createPivotResultFields(List<List<Pair<String, Object>>> cartesianProduct) {
        return cartesianProduct.stream()
                .flatMap(pairs -> {

                    String alias = pairs.stream()
                            .map(Pair::getValue)
                            .map(value -> {
                                if (value instanceof LocalDate) {
                                    return ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE);
                                } else if (value instanceof LocalDateTime) {
                                    return ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE);
                                } else {
                                    return String.valueOf(value);
                                }
                            })
                            .collect(Collectors.joining(this.serverSidePivotResultFieldSeparator));

                    return request.getValueCols()
                            .stream()
                            .map(columnVO -> alias + this.serverSidePivotResultFieldSeparator + columnVO.getField());
                })
                .collect(Collectors.toList());
    }

    /**
     * For each pivoting column fetch distinct values
     * @return map where key is column name and value is distinct column values
     */
    private Map<String, List<Object>> getPivotValues() {
        if (!this.request.isPivotMode() || this.request.getPivotCols().isEmpty()) {
            // no pivoting
            return Collections.emptyMap();
        }

        Map<String, List<Object>> pivotValues = new LinkedHashMap<>();
        for (ColumnVO column : this.request.getPivotCols()) {
            String field = column.getField();

            CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
            CriteriaQuery<Object> query = cb.createQuery(Object.class);
            Root<E> root = query.from(this.entityClass);

            // select
            query.multiselect(root.get(field)).distinct(true);
            query.orderBy(cb.asc(root.get(field)));

            // result
            List<Object> result = this.entityManager.createQuery(query).getResultList();

            pivotValues.put(field, result);
        }

        return pivotValues;
    }

    private List<Set<Pair<String, Object>>> createPivotPairs(Map<String, List<Object>> pivotValues) {
        List<Set<Pair<String, Object>>> pivotPairs = new ArrayList<>();
        for (var entry : pivotValues.entrySet()) {
            String column = entry.getKey();
            List<Object> values = entry.getValue();

            Set<Pair<String, Object>> pairs = new LinkedHashSet<>();
            for (Object value : values) {
                pairs.add(Pair.of(column, value));
            }

            // Add the set of pairs to the list
            pivotPairs.add(pairs);
        }

        return pivotPairs;
    }


    private List<Selection<?>> createPivotingSelections(Map<String, List<Object>> pivotValues) {
        // each pivot column with pair with pivot value
        List<Set<Pair<String, Object>>> pivotPairs = this.createPivotPairs(pivotValues);
        List<List<Pair<String, Object>>> cartesianProduct = CartesianProductHelper.cartesianProduct(pivotPairs);

        return cartesianProduct
                .stream()
                .flatMap(pairs -> {

                    String alias = pairs.stream()
                            .map(Pair::getValue)
                            .map(value -> {
                                if (value instanceof LocalDate) {
                                    return ((LocalDate) value).format(DateTimeFormatter.ISO_LOCAL_DATE);
                                } else if (value instanceof LocalDateTime) {
                                    return ((LocalDateTime) value).format(DateTimeFormatter.ISO_LOCAL_DATE);
                                } else {
                                    return String.valueOf(value);
                                }
                            })
                            .collect(Collectors.joining(this.serverSidePivotResultFieldSeparator));


                    return request.getValueCols()
                            .stream()
                            .map(columnVO -> {

                                Path<?> field = root.get(columnVO.getField());

                                CriteriaBuilder.Case<?> caseExpression = null;
                                for (Pair<String, Object> pair : pairs) {
                                    if (caseExpression == null) {
                                        caseExpression = cb.selectCase()
                                                .when(cb.equal(root.get(pair.getKey()), pair.getValue()), field);
                                    } else {
                                        caseExpression = cb.selectCase()
                                                .when(cb.equal(root.get(pair.getKey()), pair.getValue()), caseExpression);
                                    }
                                }
                                Objects.requireNonNull(caseExpression);

                                Expression<?> aggregatedField;
                                switch (columnVO.getAggFunc()) {
                                    case avg: {
                                        aggregatedField = cb.avg((Expression<? extends Number>) caseExpression);
                                        break;
                                    }
                                    case sum: {
                                        aggregatedField = cb.sum((Expression<? extends Number>) caseExpression);
                                        break;
                                    }
                                    case min: {
                                        aggregatedField = cb.least((Expression) caseExpression);
                                        break;
                                    }
                                    case max: {
                                        aggregatedField = cb.greatest((Expression) caseExpression);
                                        break;
                                    }
                                    case count: {
                                        aggregatedField = cb.count(caseExpression);
                                        break;
                                    }
                                    default: {
                                        throw new IllegalArgumentException("unsupported aggregation function: " + columnVO.getAggFunc());
                                    }
                                }

                                return aggregatedField.alias(alias + this.serverSidePivotResultFieldSeparator + columnVO.getField());
                            });
                })
                .collect(Collectors.toList());
    }
    
    
}
