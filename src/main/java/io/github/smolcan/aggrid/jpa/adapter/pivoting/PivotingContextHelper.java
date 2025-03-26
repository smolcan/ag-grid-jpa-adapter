package io.github.smolcan.aggrid.jpa.adapter.pivoting;

import io.github.smolcan.aggrid.jpa.adapter.exceptions.OnPivotMaxColumnsExceededException;
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
    private final Integer pivotMaxGeneratedColumns;

    public PivotingContextHelper(Class<E> entityClass, EntityManager entityManager, CriteriaBuilder cb, Root<E> root, ServerSideGetRowsRequest request, String serverSidePivotResultFieldSeparator, Integer pivotMaxGeneratedColumns) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        this.cb = cb;
        this.root = root;
        this.request = request;
        this.serverSidePivotResultFieldSeparator = serverSidePivotResultFieldSeparator;
        this.pivotMaxGeneratedColumns = pivotMaxGeneratedColumns;
    }

    /**
     * Creates pivoting context object to hold all the info about pivoting
     * @throws OnPivotMaxColumnsExceededException when number of columns to be generated from pivot values is bigger than limit  
     */
    public PivotingContext createPivotingContext() throws OnPivotMaxColumnsExceededException {
        
        PivotingContext pivotingContext = new PivotingContext();
        if (!this.request.isPivotMode() || this.request.getPivotCols().isEmpty()) {
            // no pivoting
            pivotingContext.setPivoting(false);
        } else {
            pivotingContext.setPivoting(true);

            // check if number of generated columns did not exceed the limit
            if (this.pivotMaxGeneratedColumns != null) {
                long numberOfPivotColumns = this.countPivotColumnsToBeGenerated();
                if (numberOfPivotColumns > this.pivotMaxGeneratedColumns) {
                    throw new OnPivotMaxColumnsExceededException(this.pivotMaxGeneratedColumns, numberOfPivotColumns);
                }
            }

            // distinct values for pivoting
            Map<String, List<Object>> pivotValues = this.getPivotValues();
            // pair pivot columns with values
            List<Set<Pair<String, Object>>> pivotPairs = this.createPivotPairs(pivotValues);
            // cartesian product of pivot pairs
            List<List<Pair<String, Object>>> cartesianProduct = cartesianProduct(pivotPairs);
            // for each column name its expression
            Map<String, Expression<?>> columnNamesToExpression = this.createPivotingExpressions(cartesianProduct);
            // expressions with selections
            List<Selection<?>> pivotingSelections = columnNamesToExpression.entrySet().stream()
                    .map(entry -> entry.getValue().alias(entry.getKey()))
                    .collect(Collectors.toList());
            // result fields are column names
            List<String> pivotingResultFields = new ArrayList<>(columnNamesToExpression.keySet());
            
            pivotingContext.setPivotValues(pivotValues);
            pivotingContext.setPivotPairs(pivotPairs);
            pivotingContext.setCartesianProduct(cartesianProduct);
            pivotingContext.setColumnNamesToExpression(columnNamesToExpression);
            pivotingContext.setPivotingSelections(pivotingSelections);
            pivotingContext.setPivotingResultFields(pivotingResultFields);
        }

        return pivotingContext;
    }

    /**
     * Extracts original column name from pivoted name <br/>
     * For example: Piv1_Piv2_Piv3_originalCol -> originalCol
     * @param pivotedName                           pivoted column name
     * @param serverSidePivotResultFieldSeparator   separator
     * @return                                      original name of the column
     */
    public static String originalColNameFromPivoted(String pivotedName, String serverSidePivotResultFieldSeparator) {
        Objects.requireNonNull(pivotedName);
        return pivotedName.substring(pivotedName.lastIndexOf(serverSidePivotResultFieldSeparator) + 1);
    }
    
    private Map<String, Expression<?>> createPivotingExpressions(List<List<Pair<String, Object>>> cartesianProduct) {
        Map<String, Expression<?>> pivotingExpressions = new LinkedHashMap<>();
        
        cartesianProduct.forEach(pairs -> {
            
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

            this.request.getValueCols()
                    .forEach(columnVO -> {

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

                        String columnName = alias + this.serverSidePivotResultFieldSeparator + columnVO.getField();
                        pivotingExpressions.put(columnName, aggregatedField);
                    });
        });
        
        return pivotingExpressions;
    }

    /**
     * Calculates the product of the distinct counts of all pivot columns in a single query using the Criteria API.
     * <p>
     * This method dynamically constructs a query that computes the product of distinct counts for all fields specified 
     * as pivot columns in the current request. It uses subqueries to calculate the distinct count for each field and 
     * combines them into a single product expression.
     * </p>
     *
     * @return The product of distinct counts for all pivot columns.
     *         Returns 0 if pivot mode is disabled or no pivot columns are defined in the request.
     */
    private long countPivotColumnsToBeGenerated() {
        if (!this.request.isPivotMode() || this.request.getPivotCols().isEmpty()) {
            return 0;
        }

        CriteriaQuery<Long> mainQuery = this.cb.createQuery(Long.class);

        Expression<Long> productExpression = this.cb.literal(1L);
        for (ColumnVO pivotCol : this.request.getPivotCols()) {
            // Subquery for count(distinct <field>)
            Subquery<Long> subquery = mainQuery.subquery(Long.class);
            Root<E> subRoot = subquery.from(this.entityClass);
            subquery.select(this.cb.countDistinct(subRoot.get(pivotCol.getField())));

            productExpression = this.cb.prod(productExpression, subquery.getSelection());
        }

        mainQuery.select(productExpression);

        return this.entityManager.createQuery(mainQuery).getSingleResult();
    }

    /**
     * For each pivoting column fetch distinct values
     * @return map where key is column name and value is distinct column values
     */
    private Map<String, List<Object>> getPivotValues() {
        Map<String, List<Object>> pivotValues = new LinkedHashMap<>(this.request.getPivotCols().size());
        for (ColumnVO column : this.request.getPivotCols()) {
            String field = column.getField();

            CriteriaQuery<Object> query = this.cb.createQuery(Object.class);
            Root<E> root = query.from(this.entityClass);

            // select
            query.multiselect(root.get(field)).distinct(true);
            query.orderBy(this.cb.asc(root.get(field)));

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
        List<Set<Pair<String, Object>>> pivotPairs = new ArrayList<>(pivotValues.size());
        for (var entry : pivotValues.entrySet()) {
            String column = entry.getKey();
            List<Object> values = entry.getValue();

            Set<Pair<String, Object>> pairs = new LinkedHashSet<>(values.size());
            for (Object value : values) {
                pairs.add(Pair.of(column, value));
            }

            // Add the set of pairs to the list
            pivotPairs.add(pairs);
        }

        return pivotPairs;
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
