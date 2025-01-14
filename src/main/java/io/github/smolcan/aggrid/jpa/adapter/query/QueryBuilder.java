package io.github.smolcan.aggrid.jpa.adapter.query;

import io.github.smolcan.aggrid.jpa.adapter.exceptions.OnPivotMaxColumnsExceededException;
import io.github.smolcan.aggrid.jpa.adapter.filter.JoinOperator;
import io.github.smolcan.aggrid.jpa.adapter.filter.advanced.JoinAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.advanced.column.*;
import io.github.smolcan.aggrid.jpa.adapter.filter.simple.*;
import io.github.smolcan.aggrid.jpa.adapter.request.ColumnVO;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortType;
import io.github.smolcan.aggrid.jpa.adapter.filter.advanced.AdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.pivoting.PivotingContext;
import io.github.smolcan.aggrid.jpa.adapter.utils.TypeValueSynchronizer;
import io.github.smolcan.aggrid.jpa.adapter.pivoting.PivotingContextHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;

public class QueryBuilder<E> {
    private static final DateTimeFormatter DATE_TIME_FORMATTER_FOR_DATE_FILTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER_FOR_DATE_ADVANCED_FILTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String AUTO_GROUP_COLUMN_NAME = "ag-Grid-AutoColumn";
    private static final String DEFAULT_SERVER_SIDE_PIVOT_RESULT_FIELD_SEPARATOR = "_";
    
    private final Class<E> entityClass;
    private final EntityManager entityManager;
    private final String serverSidePivotResultFieldSeparator;
    private final boolean groupAggFiltering;
    private final Integer pivotMaxGeneratedColumns;

    // List of custom filter recognizers for user-defined filters in AG Grid.
    // 
    // When a user creates a custom filter, they must register its recognizer here.
    // Each recognizer is a function that:
    // 1. Checks if the provided map represents its custom filter.
    // 2. If recognized, converts the map into implementation of the ColumnFilter class and returns it.
    // 3. Returns null if the map is not recognized.
    //
    // The system first attempts to recognize the filter as a default column filter.
    // If not found, it iterates through all custom recognizers and applies the first one that returns a non-null result.
    // IMPORTANT: do not throw any exception from your recognizer, just return mapped filter or null
    private final List<Function<Map<String, Object>, ColumnFilter>> customColumnFilterRecognizers;
    
    public static <E> Builder<E> builder(Class<E> entityClass, EntityManager entityManager) {
        return new Builder<>(entityClass, entityManager);
    }
    
    private QueryBuilder(
            Class<E> entityClass, 
            EntityManager entityManager,
            String serverSidePivotResultFieldSeparator,
            boolean groupAggFiltering,
            Integer pivotMaxGeneratedColumns,
            List<Function<Map<String, Object>, ColumnFilter>> customColumnFilterRecognizers
    ) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        this.serverSidePivotResultFieldSeparator = serverSidePivotResultFieldSeparator;
        this.groupAggFiltering = groupAggFiltering;
        this.pivotMaxGeneratedColumns = pivotMaxGeneratedColumns;
        this.customColumnFilterRecognizers = customColumnFilterRecognizers;
    }


    /**
     * Processes a server-side request to fetch rows and returns the result wrapped in a {@link LoadSuccessParams} object.
     * <p>
     * This method builds a dynamic query using the JPA Criteria API based on the provided {@link ServerSideGetRowsRequest}.
     * It performs the following steps:
     * <ul>
     *   <li>Builds a {@link CriteriaQuery} for the target entity class.</li>
     *   <li>Applies the SELECT, WHERE, GROUP BY, and ORDER BY clauses as specified in the request.</li>
     *   <li>Applies pagination (limit and offset) for server-side row retrieval.</li>
     *   <li>Executes the query and maps the resulting {@link Tuple} data to a row data structure.</li>
     * </ul>
     *
     * @param request The {@link ServerSideGetRowsRequest} containing filtering, sorting, grouping, and pagination information.
     *                This request defines the criteria for fetching rows.
     * @return A {@link LoadSuccessParams} object containing the retrieved row data mapped to a format suitable for AG Grid.
     */
    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) throws OnPivotMaxColumnsExceededException {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();

        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<E> root = query.from(this.entityClass);

        // if pivoting, load all information needed for pivoting into pivoting context
        PivotingContext pivotingContext = new PivotingContextHelper<>(this.entityClass, this.entityManager, cb, root, request, this.serverSidePivotResultFieldSeparator, this.pivotMaxGeneratedColumns).createPivotingContext();
        
        this.select(cb, query, root, request, pivotingContext);
        this.where(cb, query, root, request, pivotingContext);
        this.groupBy(cb, query, root, request);
        this.having(cb, query, root, request, pivotingContext);
        this.orderBy(cb, query, root, request, pivotingContext);

        TypedQuery<Tuple> typedQuery = this.entityManager.createQuery(query);
        this.limitOffset(typedQuery, request);

        List<Tuple> data = typedQuery.getResultList();
        
        LoadSuccessParams loadSuccessParams = new LoadSuccessParams();
        loadSuccessParams.setRowData(tupleToMap(data));
        loadSuccessParams.setPivotResultFields(pivotingContext.getPivotingResultFields());
        return loadSuccessParams;
    }
    
    protected void select(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request, PivotingContext pivotingContext) {
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
        
        if (pivotingContext.isPivoting()) {
            // pivoting
            List<Selection<?>> pivotingSelections = pivotingContext.getPivotingSelections();
            selections.addAll(pivotingSelections);
        } else {
            // aggregated columns
            for (ColumnVO columnVO : request.getValueCols()) {
                Expression<?> aggregatedField;
                switch (columnVO.getAggFunc()) {
                    case avg: {
                        aggregatedField = cb.avg(root.get(columnVO.getField()));
                        break;
                    }
                    case sum: {
                        aggregatedField = cb.sum(root.get(columnVO.getField()));
                        break;
                    }
                    case min: {
                        aggregatedField = cb.least((Expression) root.get(columnVO.getField()));
                        break;
                    }
                    case max: {
                        aggregatedField = cb.greatest((Expression) root.get(columnVO.getField()));
                        break;
                    }
                    case count: {
                        aggregatedField = cb.count(root.get(columnVO.getField()));
                        break;
                    }
                    default: {
                        throw new IllegalArgumentException("unsupported aggregation function: " + columnVO.getAggFunc());
                    }
                }
                selections.add(aggregatedField.alias(columnVO.getField()));
            }
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
    
    
    protected void where(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request, PivotingContext pivotingContext) {
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
        if (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) {
            Predicate filterPredicate = this.filterToWherePredicate(cb, root, request.getFilterModel(), pivotingContext);
            predicates.add(filterPredicate);
        }
        
        query.where(predicates.toArray(new Predicate[0]));
    }
    
    protected void groupBy(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
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
    
    protected void orderBy(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request, PivotingContext pivotingContext) {
        // we know data are still grouped if request contains more group columns than group keys
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        int limit = isGrouping ? request.getGroupKeys().size() + 1 : MAX_VALUE;
        
        // if not grouping, ordering can be done on all fields
        // otherwise, only by grouped fields
        List<Order> orderByCols = request.getSortModel().stream()
                .filter(model -> !isGrouping || request.getRowGroupCols().stream().anyMatch(rgc -> rgc.getField().equals(model.getColId())))
                .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))    // ignore auto-generated column
                .filter(model -> !pivotingContext.isPivoting() || pivotingContext.getColumnNamesToExpression().containsKey(model.getColId()))  // ignore pivoting sorting
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
                .collect(Collectors.toList());
        
        // ordering can be also done on aggregated fields
        if (isGrouping && !request.getValueCols().isEmpty()) {
            List<Order> orderByAggregatedCols = request.getSortModel().stream()
                    // not in grouped columns
                    .filter(model -> request.getRowGroupCols().stream().noneMatch(rgc -> rgc.getField().equals(model.getColId())))
                    // in aggregation columns
                    .filter(model -> request.getValueCols().stream().anyMatch(aggCol -> aggCol.getField().equals(model.getColId())))
                    // ignore auto-generated column
                    .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))
                    // ignore pivoting sorting
                    .filter(model -> !pivotingContext.isPivoting() || pivotingContext.getColumnNamesToExpression().containsKey(model.getColId()))
                    .map(model -> {
                        ColumnVO aggregatedColumn = request.getValueCols().stream().filter(aggCol -> aggCol.getField().equals(model.getColId())).findFirst().orElseThrow();
                        Expression<? extends Number> aggregatedField;
                        switch (aggregatedColumn.getAggFunc()) {
                            case avg: {
                                aggregatedField = cb.avg(root.get(model.getColId()));
                                break;
                            }
                            case sum: {
                                aggregatedField = cb.sum(root.get(model.getColId()));
                                break;
                            }
                            case min: {
                                aggregatedField = cb.least((Expression) root.get(model.getColId()));
                                break;
                            }
                            case max: {
                                aggregatedField = cb.greatest((Expression) root.get(model.getColId()));
                                break;
                            }
                            case count: {
                                aggregatedField = cb.count(root.get(model.getColId()));
                                break;
                            }
                            default: {
                                throw new IllegalArgumentException("unsupported aggregation function: " + aggregatedColumn.getAggFunc());
                            }
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
                    .collect(Collectors.toList());
            
            orderByCols = Stream.concat(orderByCols.stream(), orderByAggregatedCols.stream()).collect(Collectors.toList());
        }
        
        // ordering of pivoting cols
        if (pivotingContext.isPivoting()) {
            List<Order> orderByPivotingCols = request.getSortModel()
                    .stream()
                    .filter(model -> pivotingContext.getColumnNamesToExpression().containsKey(model.getColId()))
                    .map(model -> {
                        Expression<?> pivotingColExpression = pivotingContext.getColumnNamesToExpression().get(model.getColId());
                        if (model.getSort() == SortType.asc) {
                            return cb.asc(pivotingColExpression);
                        } else if (model.getSort() == SortType.desc) {
                            return cb.desc(pivotingColExpression);
                        } else {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            orderByCols = Stream.concat(orderByCols.stream(), orderByPivotingCols.stream()).collect(Collectors.toList());
        }
        
        query.orderBy(orderByCols);
    }

    @SuppressWarnings("unchecked")
    protected void having(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request, PivotingContext pivotingContext) {
        List<Predicate> havingPredicates = new ArrayList<>();
        
        if (pivotingContext.isPivoting() && this.isColumnFilter(request.getFilterModel())) {
            // pivoting filters
            request.getFilterModel().entrySet()
                    .stream()
                    .filter(entry -> pivotingContext.getColumnNamesToExpression().containsKey(entry.getKey()))
                    .forEach(entry -> {
                        String columnName = entry.getKey();
                        Expression<?> columnExpression = pivotingContext.getColumnNamesToExpression().get(columnName);
                        
                        Map<String, Object> filterMap = (Map<String, Object>) entry.getValue();

                        ColumnFilter columnFilter = this.recognizeColumnFilter(filterMap);
                        havingPredicates.add(columnFilter.toPredicate(cb, columnExpression));
                    });
        }
        
        if (!havingPredicates.isEmpty()) {
            query.having(havingPredicates.toArray(new Predicate[0]));
        }
    }
    
    protected void limitOffset(TypedQuery<Tuple> typedQuery, ServerSideGetRowsRequest request) {
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
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private Predicate filterToWherePredicate(CriteriaBuilder cb, Root<E> root, Map<String, Object> filterModel, PivotingContext pivotingContext) {
        
        Predicate predicate;
        if (this.isColumnFilter(filterModel)) {
            // column filter
            // columnName: filter
            List<Predicate> predicates = filterModel.entrySet()
                    .stream()
                    // filter out pivot filtering (should be in having clause since it's filtering of aggregated fields)
                    .filter(entry -> !pivotingContext.isPivoting() || !pivotingContext.getColumnNamesToExpression().containsKey(entry.getKey()))
                    .map(entry -> {
                        String columnName = entry.getKey();
                        Map<String, Object> filterMap = (Map<String, Object>) entry.getValue();

                        ColumnFilter columnFilter = this.recognizeColumnFilter(filterMap);
                        return columnFilter.toPredicate(cb, root, columnName);
                    })
                    .collect(Collectors.toList());
            
            predicate = cb.and(predicates.toArray(new Predicate[0]));
        } else {
            // advanced filter
            AdvancedFilterModel advancedFilterModel = this.recognizeAdvancedFilter(filterModel);
            predicate = advancedFilterModel.toPredicate(cb, root);
        }
        
        return predicate;
    }

    /**
     * Determines if the received map structure is column filter
     * if so, should have this structure
     * columnName: {filterModel}
     */
    private boolean isColumnFilter(Map<String, Object> filterModel) {
        if (filterModel == null) {
            return false;
        }
        return filterModel.values().stream().allMatch(v -> v instanceof Map);
    }


    /**
     * Recognizes and converts a given filter map into a {@link ColumnFilter} implementation.
     * <p>
     * This method processes the input map to identify and create either:
     * <ul>
     *   <li>A default AG Grid-provided column filter (e.g., text, date, number, set, or multi-filter).</li>
     *   <li>A user-defined custom column filter registered through custom recognizers.</li>
     * </ul>
     * <p>
     * The method first checks for AG Grid default filters using the "filterType" field. If the filter is
     * recognized as a combined filter (contains "conditions"), it processes it as a combined model.
     * If the filter type is unrecognized, the method iterates through all custom filter recognizers
     * and applies the first one that successfully processes the filter.
     *
     * @param filter A map representing the filter to be recognized. Must not be {@code null}.
     *               The map should contain the "filterType" key for default AG Grid filters.
     * @return A {@link ColumnFilter} implementation corresponding to the input filter.
     * @throws IllegalArgumentException If the filter type cannot be recognized as either a default
     *                                  or custom filter type.
     */
    @SuppressWarnings("unchecked")
    private ColumnFilter recognizeColumnFilter(Map<String, Object> filter) {
        Objects.requireNonNull(filter);
        ColumnFilter columnFilter = null;
        
        if (filter.containsKey("filterType") && filter.get("filterType") != null) {
            // try to recognize default provided ag-grid column filters
            // all default provided ag-grid column filters have "filterType" field
            String filterType = filter.get("filterType").toString();
            boolean isCombinedFilter = filter.containsKey("conditions");
            switch (filterType) {
                case "text": {
                    if (isCombinedFilter) {
                        CombinedSimpleModel<TextFilter> combinedTextFilter = new CombinedSimpleModel<>();
                        combinedTextFilter.setFilterType("text");
                        combinedTextFilter.setOperator(JoinOperator.valueOf(filter.get("operator").toString()));
                        combinedTextFilter.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(this::parseTextFilter).collect(Collectors.toList()));
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
                        combinedTextFilter.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(this::parseDateFilter).collect(Collectors.toList()));
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
                        combinedNumberFilter.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(this::parseNumberFilter).collect(Collectors.toList()));
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
            }
        }
        
        if (columnFilter == null) {
            // not recognized in default provided ag-grid column filters, try to find in custom recognizers
            columnFilter = this.customColumnFilterRecognizers
                    .stream()
                    .map(recognizerFunction -> recognizerFunction.apply(filter))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Not recognized filter type for " + filter + " either in custom or default types"));
        }
        
        return columnFilter;
    }

    @SuppressWarnings("unchecked")
    private MultiFilter parseMultiFilter(Map<String, Object> filter) {
        MultiFilter multiFilter = new MultiFilter();
        if (filter.containsKey("filterModels") && filter.get("filterModels") != null) {
            multiFilter.setFilterModels(((List<Map<String, Object>>) filter.get("filterModels"))
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::recognizeColumnFilter)
                    .collect(Collectors.toList())
            );
        }
        return multiFilter;
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

        DateFilter dateFilter = new DateFilter();
        dateFilter.setType(SimpleFilterModelType.valueOf(filter.get("type").toString()));
        dateFilter.setDateFrom(Optional.ofNullable(filter.get("dateFrom")).map(Object::toString).map(d -> LocalDateTime.parse(d, DATE_TIME_FORMATTER_FOR_DATE_FILTER)).orElse(null));
        dateFilter.setDateTo(Optional.ofNullable(filter.get("dateTo")).map(Object::toString).map(d -> LocalDateTime.parse(d, DATE_TIME_FORMATTER_FOR_DATE_FILTER)).orElse(null));

        return dateFilter;
    }

    /**
     * Recognizes and converts the given filter map into an appropriate {@link AdvancedFilterModel} implementation.
     * <p>
     * This method identifies the filter type based on the "filterType" key in the provided map and creates the corresponding
     * {@link AdvancedFilterModel} subclass (such as {@link TextAdvancedFilterModel}, {@link DateAdvancedFilterModel}, 
     * {@link NumberAdvancedFilterModel}, etc.). If the filter type is "join", it recursively processes the conditions as 
     * a {@link JoinAdvancedFilterModel}.
     * <p>
     * The method supports various filter types, including:
     * <ul>
     *   <li>{@code text}: Text-based filter</li>
     *   <li>{@code date}: Date-based filter (with {@link LocalDate} conversion)</li>
     *   <li>{@code number}: Number-based filter (with {@link BigDecimal} conversion)</li>
     *   <li>{@code boolean}: Boolean-based filter</li>
     *   <li>{@code object}: Generic object filter</li>
     *   <li>{@code join}: Composite filter that combines multiple conditions (recursive)</li>
     * </ul>
     *
     * @param filter A map representing the filter to be recognized. Must contain a "filterType" key.
     * @return An appropriate {@link AdvancedFilterModel} subclass based on the "filterType" and other filter parameters.
     * @throws UnsupportedOperationException If the filter type is not supported.
     * @throws NullPointerException If the input {@code filter} map is {@code null}.
     */
    @SuppressWarnings("unchecked")
    private AdvancedFilterModel recognizeAdvancedFilter(Map<String, Object> filter) {
        Objects.requireNonNull(filter);
        String filterType = filter.get("filterType").toString();
        if (filterType.equals("join")) {
            // join
            JoinAdvancedFilterModel joinAdvancedFilterModel = new JoinAdvancedFilterModel();
            joinAdvancedFilterModel.setType(JoinOperator.valueOf(filter.get("type").toString()));
            joinAdvancedFilterModel.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(this::recognizeAdvancedFilter).collect(Collectors.toList()));

            return joinAdvancedFilterModel;
        } else {
            // column
            String colId = filter.get("colId").toString();
            switch (filterType) {
                case "text": {
                    TextAdvancedFilterModel textAdvancedFilterModel = new TextAdvancedFilterModel(colId);
                    textAdvancedFilterModel.setType(TextAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    textAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).orElse(null));
                    return textAdvancedFilterModel;
                }
                case "date": {
                    DateAdvancedFilterModel dateAdvancedFilterModel = new DateAdvancedFilterModel(colId);
                    dateAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    dateAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(f -> LocalDate.parse(f, DATE_FORMATTER_FOR_DATE_ADVANCED_FILTER)).orElse(null));
                    return dateAdvancedFilterModel;
                }
                case "dateString": {
                    DateStringAdvancedFilterModel dateAdvancedFilterModel = new DateStringAdvancedFilterModel(colId);
                    dateAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    dateAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(f -> LocalDate.parse(f, DATE_FORMATTER_FOR_DATE_ADVANCED_FILTER)).orElse(null));
                    return dateAdvancedFilterModel;
                }
                case "number": {
                    NumberAdvancedFilterModel numberAdvancedFilterModel = new NumberAdvancedFilterModel(colId);
                    numberAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    numberAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(BigDecimal::new).orElse(null));
                    return numberAdvancedFilterModel;
                }
                case "object": {
                    ObjectAdvancedFilterModel objectAdvancedFilterModel = new ObjectAdvancedFilterModel(colId);
                    objectAdvancedFilterModel.setType(TextAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    objectAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).orElse(null));
                    return objectAdvancedFilterModel;
                }
                case "boolean": {
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
                default: throw new UnsupportedOperationException("Unsupported advanced filter type: " + filterType);
            }
        }
    }

    
    public static class Builder<E> {
        private final Class<E> entityClass;
        private final EntityManager entityManager;

        private String serverSidePivotResultFieldSeparator = DEFAULT_SERVER_SIDE_PIVOT_RESULT_FIELD_SEPARATOR;
        private boolean groupAggFiltering;
        private Integer pivotMaxGeneratedColumns;

        private final List<Function<Map<String, Object>, ColumnFilter>> customColumnFilterRecognizers = new ArrayList<>();

        private Builder(Class<E> entityClass, EntityManager entityManager) {
            this.entityClass = entityClass;
            this.entityManager = entityManager;
        }

        public Builder<E> serverSidePivotResultFieldSeparator(String separator) {
            if (separator == null || separator.isEmpty()) {
                throw new IllegalArgumentException("Server side pivot result field separator cannot be null or empty");
            }
            this.serverSidePivotResultFieldSeparator = separator;
            return this;
        }

        public Builder<E> groupAggFiltering(boolean groupAggFiltering) {
            this.groupAggFiltering = groupAggFiltering;
            return this;
        }

        public Builder<E> pivotMaxGeneratedColumns(Integer pivotMaxGeneratedColumns) {
            if (pivotMaxGeneratedColumns != null && pivotMaxGeneratedColumns <= 0) {
                throw new IllegalArgumentException("pivot max generated columns must be greater than zero");
            }
            this.pivotMaxGeneratedColumns = pivotMaxGeneratedColumns;
            return this;
        }

        /**
         * Registers a single custom column filter recognizer.
         * <p>
         * A recognizer is a function that inspects a filter represented as a map and determines whether
         * it matches a custom filter implementation. If recognized, the function should return a concrete
         * implementation of {@link ColumnFilter}. Otherwise, it should return {@code null}.
         *
         * @param recognizerFunction A function that attempts to recognize and convert a map into a {@link ColumnFilter}.
         *                           Must not be {@code null}.
         * @return The current {@link QueryBuilder} instance for method chaining.
         * @throws NullPointerException If the {@code recognizerFunction} is {@code null}.
         */
        public Builder<E> addCustomColumnFilterRecognizer(Function<Map<String, Object>, ColumnFilter> recognizerFunction) {
            this.customColumnFilterRecognizers.add(Objects.requireNonNull(recognizerFunction));
            return this;
        }

        /**
         * Registers multiple custom column filter recognizers as varargs.
         * <p>
         * Each recognizer function inspects a filter represented as a map and determines whether
         * it matches a custom filter implementation. If recognized, the function should return a concrete
         * implementation of {@link ColumnFilter}. Otherwise, it should return {@code null}.
         *
         * @param recognizerFunctions Varargs of functions that attempt to recognize and convert maps into {@link ColumnFilter} implementations.
         *                            Must not be {@code null}.
         * @return The current {@link QueryBuilder} instance for method chaining.
         * @throws NullPointerException If the {@code recognizerFunctions} array or any of its elements is {@code null}.
         */
        public Builder<E> addCustomColumnFilterRecognizers(Function<Map<String, Object>, ColumnFilter>... recognizerFunctions) {
            this.customColumnFilterRecognizers.addAll(Objects.requireNonNull(Arrays.asList(recognizerFunctions)));
            return this;
        }

        /**
         * Registers a list of custom column filter recognizers.
         * <p>
         * Each recognizer function inspects a filter represented as a map and determines whether
         * it matches a custom filter implementation. If recognized, the function should return a concrete
         * implementation of {@link ColumnFilter}. Otherwise, it should return {@code null}.
         *
         * @param recognizerFunctions A list of functions that attempt to recognize and convert maps into {@link ColumnFilter} implementations.
         *                            Must not be {@code null}.
         * @return The current {@link QueryBuilder} instance for method chaining.
         * @throws NullPointerException If the {@code recognizerFunctions} list or any of its elements is {@code null}.
         */
        public Builder<E> addCustomColumnFilterRecognizers(List<Function<Map<String, Object>, ColumnFilter>> recognizerFunctions) {
            this.customColumnFilterRecognizers.addAll(Objects.requireNonNull(recognizerFunctions));
            return this;
        }

        public QueryBuilder<E> build() {
            return new QueryBuilder<>(
                    this.entityClass,
                    this.entityManager,
                    this.serverSidePivotResultFieldSeparator,
                    this.groupAggFiltering,
                    this.pivotMaxGeneratedColumns,
                    this.customColumnFilterRecognizers
            );
        }
    }
}
