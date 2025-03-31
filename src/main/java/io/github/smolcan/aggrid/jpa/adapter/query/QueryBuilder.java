package io.github.smolcan.aggrid.jpa.adapter.query;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.OnPivotMaxColumnsExceededException;
import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.JoinOperator;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.JoinAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column.*;
import io.github.smolcan.aggrid.jpa.adapter.request.ColumnVO;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortModelItem;
import io.github.smolcan.aggrid.jpa.adapter.request.SortType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.AdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.pivoting.PivotingContext;
import io.github.smolcan.aggrid.jpa.adapter.utils.TypeValueSynchronizer;
import io.github.smolcan.aggrid.jpa.adapter.pivoting.PivotingContextHelper;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Integer.MAX_VALUE;

public class QueryBuilder<E> {
    private static final DateTimeFormatter DATE_FORMATTER_FOR_DATE_ADVANCED_FILTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String AUTO_GROUP_COLUMN_NAME = "ag-Grid-AutoColumn";
    
    private final Class<E> entityClass;
    private final EntityManager entityManager;
    private final String serverSidePivotResultFieldSeparator;
    private final boolean groupAggFiltering;
    private final boolean enableAdvancedFilter;
    private final Integer pivotMaxGeneratedColumns;
    private final Map<String, ColDef> colDefs;
    
    public static <E> Builder<E> builder(Class<E> entityClass, EntityManager entityManager) {
        return new Builder<>(entityClass, entityManager);
    }
    
    private QueryBuilder(Builder<E> builder) {
        this.entityClass = builder.entityClass;
        this.entityManager = builder.entityManager;
        this.serverSidePivotResultFieldSeparator = builder.serverSidePivotResultFieldSeparator;
        this.groupAggFiltering = builder.groupAggFiltering;
        this.enableAdvancedFilter = builder.enableAdvancedFilter;
        this.pivotMaxGeneratedColumns = builder.pivotMaxGeneratedColumns;
        this.colDefs = builder.colDefs;
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
        this.validateRequest(request);
        
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
    
    protected void validateRequest(ServerSideGetRowsRequest request) {
        StringBuilder errors = new StringBuilder();
        
        // validate groups cols
        if (request.getRowGroupCols() != null && !request.getRowGroupCols().isEmpty()) {
            List<ColumnVO> rowGroupColsNotInColDefs = request.getRowGroupCols().stream().filter(c -> !this.colDefs.containsKey(c.getField())).collect(Collectors.toList());
            if (!rowGroupColsNotInColDefs.isEmpty()) {
                errors.append(String.format("These row group cols not found in col defs: %s\n", rowGroupColsNotInColDefs.stream().map(ColumnVO::getField).collect(Collectors.joining(", "))));
            }
        }
        // validate value cols
        if (request.getValueCols() != null && !request.getValueCols().isEmpty()) {
            List<ColumnVO> valueColsNotInColDefs = request.getValueCols().stream().filter(c -> !this.colDefs.containsKey(c.getField())).collect(Collectors.toList());
            if (!valueColsNotInColDefs.isEmpty()) {
                errors.append(String.format("These value cols not found in col defs: %s\n", valueColsNotInColDefs.stream().map(ColumnVO::getField).collect(Collectors.joining(", "))));
            }
            // validate agg functions
            List<ColumnVO> valueColsNotAllowedAggregations = request.getValueCols().stream().filter(valueCol -> {
                ColDef colDef = this.colDefs.get(valueCol.getField());
                return !colDef.getAllowedAggFuncs().contains(valueCol.getAggFunc());
            }).collect(Collectors.toList());
            if (!valueColsNotAllowedAggregations.isEmpty()) {
                errors.append(String.format("These row value cols do not have allowed received aggregation: %s\n", valueColsNotAllowedAggregations.stream().map(ColumnVO::getField).collect(Collectors.joining(", "))));
            }
        }
        // validate pivot cols
        if (request.getPivotCols() != null && !request.getPivotCols().isEmpty()) {
            List<ColumnVO> pivotColsNotInColDefs = request.getPivotCols().stream().filter(c -> !this.colDefs.containsKey(c.getField())).collect(Collectors.toList());
            if (!pivotColsNotInColDefs.isEmpty()) {
                errors.append(String.format("These pivot cols not found in col defs: %s\n", pivotColsNotInColDefs.stream().map(ColumnVO::getField).collect(Collectors.joining(", "))));
            }
        }
        // sort cols
        if (request.getSortModel() != null && !request.getSortModel().isEmpty()) {
            List<SortModelItem> sortModelItemsNotInColDefs = request.getSortModel()
                    .stream()
                    .filter(c -> {
                        // check col defs
                        boolean isInColDefs = this.colDefs.containsKey(c.getColId());
                        if (!isInColDefs && request.isPivotMode()) {
                            // check pivoted cols
                            String pivotedColumnOriginalName = PivotingContextHelper.originalColNameFromPivoted(c.getColId(), this.serverSidePivotResultFieldSeparator);
                            isInColDefs = this.colDefs.containsKey(pivotedColumnOriginalName);
                        }
                        return !isInColDefs;
                    }).collect(Collectors.toList());
            if (!sortModelItemsNotInColDefs.isEmpty()) {
                errors.append(String.format("These sort model cols not found in col defs: %s\n", sortModelItemsNotInColDefs.stream().map(SortModelItem::getColId).collect(Collectors.joining(", "))));
            }
            
            Set<String> notSortableColDefs = this.colDefs.keySet().stream().filter(field -> !this.colDefs.get(field).isSortable()).collect(Collectors.toSet());
            List<SortModelItem> sortModelItemsIllegalSort = request.getSortModel()
                    .stream()
                    .filter(sm -> {
                        boolean isNotSortable = notSortableColDefs.contains(sm.getColId());
                        if (!isNotSortable && request.isPivotMode()) {
                            // check pivoted cols
                            String pivotedColumnOriginalName = PivotingContextHelper.originalColNameFromPivoted(sm.getColId(), this.serverSidePivotResultFieldSeparator);
                            isNotSortable = notSortableColDefs.contains(pivotedColumnOriginalName);
                        }
                        return isNotSortable;
                    }).collect(Collectors.toList());
            if (!sortModelItemsIllegalSort.isEmpty()) {
                errors.append(String.format("These sort model cols can not be sorted by (disabled in col defs): %s\n", sortModelItemsIllegalSort.stream().map(SortModelItem::getColId).collect(Collectors.joining(", "))));
            }
        }
        
        if (errors.length() > 0) {
            throw new IllegalArgumentException(errors.toString());
        }
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
        List<Selection<?>> selections = this.colDefs.values()
                .stream()
                .map(colDef -> {
                    Path<?> field = root.get(colDef.getField());
                    return field.alias(colDef.getField());
                })
                .collect(Collectors.toList());
        
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
                        String pivotingColumnName = entry.getKey();
                        Expression<?> pivotingColumnExpression = pivotingContext.getColumnNamesToExpression().get(pivotingColumnName);


                        String columnName = pivotingColumnName.substring(pivotingColumnName.lastIndexOf(this.serverSidePivotResultFieldSeparator) + 1);
                        IFilter<?, ?> filter = Optional.ofNullable(this.colDefs.get(columnName)).map(ColDef::getFilter).orElseThrow(() -> new IllegalArgumentException("Column " + columnName + " not found in col defs"));
                        Map<String, Object> filterMap = (Map<String, Object>) entry.getValue();

                        havingPredicates.add(filter.toPredicate(cb, pivotingColumnExpression, filterMap));
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
        if (!this.isColumnFilter(filterModel)) {
            // advanced filter
            AdvancedFilterModel advancedFilterModel = this.recognizeAdvancedFilter(filterModel);
            predicate = advancedFilterModel.toPredicate(cb, root);
        } else {
            // column filter
            // columnName: filter
            List<Predicate> predicates = filterModel.entrySet()
                    .stream()
                    // filter out pivot filtering (should be in having clause since it's filtering of aggregated fields)
                    .filter(entry -> !pivotingContext.isPivoting() || !pivotingContext.getColumnNamesToExpression().containsKey(entry.getKey()))
                    .map(entry -> {
                        String columnName = entry.getKey();
                        Map<String, Object> filterMap = (Map<String, Object>) entry.getValue();
                        
                        ColDef colDef = Optional.ofNullable(this.colDefs.get(columnName)).orElseThrow(() -> new IllegalArgumentException("Column " + columnName + " not found in col defs"));
                        IFilter<?, ?> filter = colDef.getFilter();
                        if (filter == null) {
                            throw new IllegalArgumentException("Column " + columnName + " is not filterable field!");
                        }
                        
                        return filter.toPredicate(cb, root.get(columnName), filterMap);
                    })
                    .collect(Collectors.toList());

            predicate = cb.and(predicates.toArray(new Predicate[0]));
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
        if (!this.enableAdvancedFilter) {
            throw new IllegalArgumentException("Can not perform advanced filtering, enableAdvancedFilter is set to false!");
        }
        
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
        private static final String DEFAULT_SERVER_SIDE_PIVOT_RESULT_FIELD_SEPARATOR = "_";
        
        private final Class<E> entityClass;
        private final EntityManager entityManager;

        private String serverSidePivotResultFieldSeparator = DEFAULT_SERVER_SIDE_PIVOT_RESULT_FIELD_SEPARATOR;
        private boolean groupAggFiltering;
        private Integer pivotMaxGeneratedColumns;
        private boolean enableAdvancedFilter;
        
        private Map<String, ColDef> colDefs;


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
        
        public Builder<E> colDefs(ColDef ...colDefs) {
            this.colDefs = new HashMap<>(colDefs.length);
            for (ColDef colDef : colDefs) {
                this.colDefs.put(colDef.getField(), colDef);
            }
            return this;
        }
        
        public Builder<E> colDefs(Collection<ColDef> colDefs) {
            this.colDefs = new HashMap<>(colDefs.size());
            for (ColDef colDef : colDefs) {
                this.colDefs.put(colDef.getField(), colDef);
            }
            return this;
        }
        
        public Builder<E> enableAdvancedFilter(boolean enableAdvancedFilter) {
            this.enableAdvancedFilter = enableAdvancedFilter;
            return this;
        }

        public QueryBuilder<E> build() {
            if (this.colDefs == null || this.colDefs.isEmpty()) {
                throw new IllegalArgumentException("colDefs cannot be null or empty");
            }
            
            return new QueryBuilder<>(this);
        }
    }
}
