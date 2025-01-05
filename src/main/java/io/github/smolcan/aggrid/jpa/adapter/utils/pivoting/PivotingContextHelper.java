package io.github.smolcan.aggrid.jpa.adapter.utils.pivoting;

import io.github.smolcan.aggrid.jpa.adapter.request.ColumnVO;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
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

    /**
     * Creates pivoting context object to hold all the info about pivoting
     */
    public PivotingContext createPivotingContext() {
        
        PivotingContext pivotingContext = new PivotingContext();
        if (!this.request.isPivotMode() || this.request.getPivotCols().isEmpty()) {
            // no pivoting
            pivotingContext.setPivoting(false);
        } else {
            pivotingContext.setPivoting(true);
            pivotingContext.setPivotValues(this.getPivotValues());
            pivotingContext.setPivotPairs(this.createPivotPairs(pivotingContext.getPivotValues()));
            pivotingContext.setCartesianProduct(cartesianProduct(pivotingContext.getPivotPairs()));
            pivotingContext.setPivotingSelections(this.createPivotingSelections(pivotingContext.getCartesianProduct()));
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

                    return this.request.getValueCols()
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

    /**
     * Creates pivot pairs from pivot values <br/>
     * For example, for input: <br/>
     * <code>
     *     {
     *         book: [Book1, Book2],
     *         product: [Product1, Product2]
     *     }
     * </code> <br/>
     * Output will be: <br/>
     * <code>
     *     [
     *       [(book, Book1), (book, Book2)], 
     *       [(product, Product1), (product, Product2)]
     *     ]
     * </code>
     * 
     * @param pivotValues   pivot values
     * @return              pivot pairs
     */
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


    /**
     * Creates Selections from cartesian product of pivot pairs
     * @param cartesianProduct  cartesian product of pivot pairs
     * @return                  list of selections
     */
    private List<Selection<?>> createPivotingSelections(List<List<Pair<String, Object>>> cartesianProduct) {
        // each pivot column with pair with pivot value
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


                    return this.request.getValueCols()
                            .stream()
                            .map(columnVO -> {

                                Path<?> field = this.root.get(columnVO.getField());

                                CriteriaBuilder.Case<?> caseExpression = null;
                                for (Pair<String, Object> pair : pairs) {
                                    if (caseExpression == null) {
                                        caseExpression = this.cb.selectCase()
                                                .when(this.cb.equal(this.root.get(pair.getKey()), pair.getValue()), field);
                                    } else {
                                        caseExpression = this.cb.selectCase()
                                                .when(this.cb.equal(this.root.get(pair.getKey()), pair.getValue()), caseExpression);
                                    }
                                }
                                Objects.requireNonNull(caseExpression);

                                // wrap case expression onto aggregation
                                Expression<?> aggregatedField;
                                switch (columnVO.getAggFunc()) {
                                    case avg: {
                                        aggregatedField = this.cb.avg((Expression<? extends Number>) caseExpression);
                                        break;
                                    }
                                    case sum: {
                                        aggregatedField = this.cb.sum((Expression<? extends Number>) caseExpression);
                                        break;
                                    }
                                    case min: {
                                        aggregatedField = this.cb.least((Expression) caseExpression);
                                        break;
                                    }
                                    case max: {
                                        aggregatedField = this.cb.greatest((Expression) caseExpression);
                                        break;
                                    }
                                    case count: {
                                        aggregatedField = this.cb.count(caseExpression);
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


    private static <T> List<List<T>> cartesianProduct(List<Set<T>> sets) {
        return _cartesianProduct(0, sets);
    }

    private static <T> List<List<T>> _cartesianProduct(int index, List<Set<T>> sets) {
        List<List<T>> result = new ArrayList<>();
        if (index == sets.size()) {
            result.add(new ArrayList<>());
        } else {
            for (T element : sets.get(index)) {
                for (List<T> product : _cartesianProduct(index + 1, sets)) {
                    List<T> newProduct = new ArrayList<>(product);
                    newProduct.add(0, element); // Maintain order
                    result.add(newProduct);
                }
            }
        }
        return result;
    }
    
    
}
