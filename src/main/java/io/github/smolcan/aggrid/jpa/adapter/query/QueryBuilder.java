package io.github.smolcan.aggrid.jpa.adapter.query;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.InvalidRequestException;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.OnPivotMaxColumnsExceededException;
import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.JoinOperator;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.JoinAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column.*;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.DateFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.NumberFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.simple.params.TextFilterParams;
import io.github.smolcan.aggrid.jpa.adapter.query.metadata.*;
import io.github.smolcan.aggrid.jpa.adapter.request.ColumnVO;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortModelItem;
import io.github.smolcan.aggrid.jpa.adapter.request.SortType;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.AdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.query.metadata.PivotingContext;
import io.github.smolcan.aggrid.jpa.adapter.utils.Pair;
import io.github.smolcan.aggrid.jpa.adapter.utils.TypeValueSynchronizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static io.github.smolcan.aggrid.jpa.adapter.utils.CartesianProduct.cartesianProduct;

public class QueryBuilder<E> {
    private static final DateTimeFormatter DATE_FORMATTER_FOR_DATE_ADVANCED_FILTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String AUTO_GROUP_COLUMN_NAME = "ag-Grid-AutoColumn";
    
    private final Class<E> entityClass;
    private final EntityManager entityManager;
    private final String serverSidePivotResultFieldSeparator;
    private final boolean enableAdvancedFilter;
    private final Integer pivotMaxGeneratedColumns;
    private final boolean paginateChildRows;
    private final boolean groupAggFiltering;
    private final boolean suppressAggFilteredOnly;
    private final Map<String, ColDef> colDefs;
    
    public static <E> Builder<E> builder(Class<E> entityClass, EntityManager entityManager) {
        return new Builder<>(entityClass, entityManager);
    }
    
    private QueryBuilder(Builder<E> builder) {
        this.entityClass = builder.entityClass;
        this.entityManager = builder.entityManager;
        this.serverSidePivotResultFieldSeparator = builder.serverSidePivotResultFieldSeparator;
        this.enableAdvancedFilter = builder.enableAdvancedFilter;
        this.pivotMaxGeneratedColumns = builder.pivotMaxGeneratedColumns;
        this.paginateChildRows = builder.paginateChildRows;
        this.groupAggFiltering = builder.groupAggFiltering;
        this.suppressAggFilteredOnly = builder.groupAggFiltering || builder.suppressAggFilteredOnly;
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
     * @throws OnPivotMaxColumnsExceededException - when number of pivot columns exceeded limit
     */
    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) throws OnPivotMaxColumnsExceededException {
        this.validateRequest(request);
        
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<E> root = query.from(this.entityClass);
        
        // record all the context we put into query
        QueryContext queryContext = new QueryContext();
        // if pivoting, load all information needed for pivoting into pivoting context
        queryContext.setPivotingContext(this.createPivotingContext(cb, root, request));
        
        this.select(cb, root, request, queryContext);
        this.where(cb, root, request, queryContext);
        this.groupBy(cb, root, request, queryContext);
        this.having(cb, request, queryContext);
        this.orderBy(cb, request, queryContext);
        this.limitOffset(request, queryContext);
        
        List<Tuple> data = this.apply(query, queryContext);
        LoadSuccessParams loadSuccessParams = new LoadSuccessParams();
        loadSuccessParams.setRowData(tupleToMap(data));
        loadSuccessParams.setPivotResultFields(queryContext.getPivotingContext().getPivotingResultFields());
        return loadSuccessParams;
    }

    /**
     * Counts the number of rows or groups that match the criteria specified in the request.
     * <p>
     * This method determines the total count of rows or groups based on the filtering and grouping
     * specifications in the provided {@link ServerSideGetRowsRequest}. The behavior differs depending
     * on whether the request contains row grouping:
     * <ul>
     *   <li>If no grouping is applied, it counts the total number of entity rows that match the filter criteria.</li>
     *   <li>If grouping is applied, it counts the distinct values of the root group column that would be displayed
     *       at the current grouping level, taking into account all filters and having conditions.</li>
     * </ul>
     * <p>
     *     
     * This count can be used for server-side pagination calculations or to determine the total
     * number of rows/groups available when displaying partial results.
     *
     * @param request The {@link ServerSideGetRowsRequest} containing filtering, grouping, and other criteria information.
     * @return The count of rows or groups that match the criteria in the request.
     * @throws OnPivotMaxColumnsExceededException If the number of pivot columns to be generated exceeds the configured limit.
     * @see #getRows(ServerSideGetRowsRequest) For retrieving the actual row data.
     */
    public long countRows(ServerSideGetRowsRequest request) throws OnPivotMaxColumnsExceededException {
        this.validateRequest(request);

        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<E> root = query.from(this.entityClass);
        
        // record all the context we put into query
        QueryContext queryContext = new QueryContext();
        
        // we count groups when there is grouping
        boolean hasGroupCols = !request.getRowGroupCols().isEmpty();
        boolean countingGroups = hasGroupCols;
        if (hasGroupCols && this.paginateChildRows) {
            // if paginateChildRows is turned on and all groups are expanded, we count records inside group (not counting groups)
            boolean allGroupsExpanded = request.getRowGroupCols().size() == request.getGroupKeys().size();
            if (allGroupsExpanded) {
                countingGroups = false;
            }
        }
        
        if (countingGroups) {
            // select the group col that we are counting
            int countingGroupColIndex = this.paginateChildRows
                    // when paginating child rows, we count the first unexpanded group (next after last group key)
                    ? request.getGroupKeys().size()
                    // otherwise, we count root group
                    : 0;
            String countingGroupCol = request.getRowGroupCols().get(countingGroupColIndex).getId();

            // subquery will only select the group column 
            Subquery<?> subquery = query.subquery(root.get(countingGroupCol).getJavaType());
            Root<E> subqueryRoot = subquery.from(this.entityClass);
            
            queryContext.setPivotingContext(this.createPivotingContext(cb, subqueryRoot, request));
            this.select(cb, subqueryRoot, request, queryContext);
            this.where(cb, subqueryRoot, request, queryContext);
            this.groupBy(cb, subqueryRoot, request, queryContext);
            this.having(cb, request, queryContext);
            
            // select the group column in subquery
            subquery.select(subqueryRoot.get(countingGroupCol));
            // where
            if (!queryContext.getWherePredicates().isEmpty()) {
                Predicate[] predicates = queryContext.getWherePredicates().stream().map(WherePredicateMetadata::getPredicate).toArray(Predicate[]::new);
                subquery.where(predicates);
            }
            // group by
            if (!queryContext.getGrouping().isEmpty()) {
                subquery.groupBy(queryContext.getGrouping().values().stream().map(GroupingMetadata::getGropingExpression).collect(Collectors.toList()));
            }
            // having
            if (!queryContext.getHaving().isEmpty()) {
                Predicate[] having = queryContext.getHaving().stream().map(HavingMetadata::getPredicate).toArray(Predicate[]::new);
                subquery.having(having);
            }
            
            // in parent query, count distinct values of column group that are returned in subquery
            query.select(cb.countDistinct(root.get(countingGroupCol)));
            query.where(cb.in(root.get(countingGroupCol)).value(subquery));
            
            return this.entityManager.createQuery(query).getSingleResult();
        } else {
            // no groups, count rows
            this.select(cb, root, request, queryContext);
            this.where(cb, root, request, queryContext);
            
            query.select(cb.count(root));
            if (!queryContext.getWherePredicates().isEmpty()) {
                Predicate[] predicates = queryContext.getWherePredicates().stream().map(WherePredicateMetadata::getPredicate).toArray(Predicate[]::new);
                query.where(predicates);
            }
            
            return this.entityManager.createQuery(query).getSingleResult();
        }
    }

    /**
     * Applies the given {@link CriteriaQuery} by setting its select, where, group by,
     * having, and order by clauses based on the provided {@link QueryContext}, then executes it.
     *
     * @param query        the criteria query to configure
     * @param queryContext the context containing metadata for selections, filters, grouping, etc.
     * @return a list of results returned by the executed query
     */
    protected List<Tuple> apply(CriteriaQuery<Tuple> query, QueryContext queryContext) {
        // select
        query.multiselect(queryContext.getSelections().values().stream().map(SelectionMetadata::getSelection).collect(Collectors.toList()));
        // where
        if (!queryContext.getWherePredicates().isEmpty()) {
            Predicate[] predicates = queryContext.getWherePredicates().stream().map(WherePredicateMetadata::getPredicate).toArray(Predicate[]::new);
            query.where(predicates);
        }
        // group by
        if (!queryContext.getGrouping().isEmpty()) {
            query.groupBy(queryContext.getGrouping().values().stream().map(GroupingMetadata::getGropingExpression).collect(Collectors.toList()));
        }
        // having
        if (!queryContext.getHaving().isEmpty()) {
            Predicate[] having = queryContext.getHaving().stream().map(HavingMetadata::getPredicate).toArray(Predicate[]::new);
            query.having(having);
        }
        // order by
        if (!queryContext.getOrders().isEmpty()) {
            query.orderBy(queryContext.getOrders().stream().map(OrderMetadata::getOrder).collect(Collectors.toList()));
        }

        TypedQuery<Tuple> typedQuery = this.entityManager.createQuery(query);
        typedQuery.setFirstResult(queryContext.getFirstResult());
        typedQuery.setMaxResults(queryContext.getMaxResults());
        
        return typedQuery.getResultList();
    }

    /**
     * Populates the {@link QueryContext} with appropriate selections based on the request type.
     * <p>
     * Handles both flat data and grouped/pivoted data by determining the structure of the
     * {@link ServerSideGetRowsRequest}. If grouping or pivoting is active, selects group and
     * aggregate expressions accordingly; otherwise, selects all fields directly from the root entity.
     *
     * @param cb           the {@link CriteriaBuilder} used to construct aggregate expressions
     * @param root         the root entity in the criteria query
     * @param request      the AG Grid server-side row request containing grouping and aggregation info
     * @param queryContext the query context to populate with selection metadata
     */
    protected void select(CriteriaBuilder cb, Root<E> root, ServerSideGetRowsRequest request, QueryContext queryContext) {
        // select
        List<SelectionMetadata> selections;
        
        // we know data are still grouped if request contains more group columns than group keys
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (!isGrouping) {
            // SELECT * from root if not grouping
            selections = this.colDefs.values()
                    .stream()
                    .map(colDef -> {
                        Path<?> field = root.get(colDef.getField());
                        Selection<?> selection = field.alias(colDef.getField());
                        
                        return SelectionMetadata.builder(selection)
                                .selection(selection)
                                .build();
                    })
                    .collect(Collectors.toList());
        } else {
            selections = new ArrayList<>();
            // group columns
            for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
                ColumnVO groupCol = request.getRowGroupCols().get(i);
                Selection<?> groupSelection = root.get(groupCol.getField()).alias(groupCol.getField());
                
                SelectionMetadata groupSelectionMetadata = SelectionMetadata
                        .builder(groupSelection)
                        .isGroupingSelection(true)
                        .build();
                selections.add(groupSelectionMetadata);
            }

            if (queryContext.getPivotingContext().isPivoting()) {
                // pivoting
                List<SelectionMetadata> pivotingSelections = queryContext.getPivotingContext().getPivotingSelections()
                        .stream()
                        .map(s -> SelectionMetadata
                                .builder(s)
                                .isPivotingSelection(true)
                                .isAggregationSelection(true)
                                .build()
                        )
                        .collect(Collectors.toList());
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
                    Selection<?> aggregatedFieldSelection = aggregatedField.alias(columnVO.getField());
                    
                    selections.add(
                            SelectionMetadata
                                    .builder(aggregatedFieldSelection)
                                    .isAggregationSelection(true)
                                    .build()
                    );
                }
            }
        }
        
        // set selections to query context
        Map<String, SelectionMetadata> selectionsWithAliases = selections
                .stream()
                .collect(Collectors.toMap(s -> s.getSelection().getAlias(), selection -> selection));
        
        queryContext.setSelections(selectionsWithAliases);
    }

    /**
     * Constructs and adds {@code WHERE} predicates to the {@link QueryContext} based on the grouping keys
     * and filter model in the {@link ServerSideGetRowsRequest}.
     * <p>
     * For each group column with a corresponding group key, a predicate is created to match the key.
     * Additional filter predicates are generated from the filter model and added as well.
     *
     * @param cb           the {@link CriteriaBuilder} used to create predicates
     * @param root         the root entity in the criteria query
     * @param request      the AG Grid server-side row request containing group keys and filters
     * @param queryContext the query context to populate with predicate metadata
     */
    protected void where(CriteriaBuilder cb, Root<E> root, ServerSideGetRowsRequest request, QueryContext queryContext) {
        List<WherePredicateMetadata> wherePredicateMetadata = new ArrayList<>();

        // must add where statement for every group column that also has a key (was expanded)
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size(); i++) {
            String groupKey = request.getGroupKeys().get(i);
            String groupCol = request.getRowGroupCols().get(i).getField();

            // try to synchronize col and key to same data type to prevent errors
            // for example, group key is date as string, but field is date, need to parse to date and then compare
            TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(root.get(groupCol), groupKey);
            Predicate groupPredicate = cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());
            
            // wrap in predicate info object
            WherePredicateMetadata groupPredicateInfo = WherePredicateMetadata
                    .builder(groupPredicate)
                    .isGroupPredicate(true)
                    .groupKey(synchronizedValueType.getSynchronizedValue())
                    .groupCol(groupCol)
                    .build();
            wherePredicateMetadata.add(groupPredicateInfo);
        }
        
        // filter where
        if (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) {
            WherePredicateMetadata filterPredicate = this.filterToWherePredicate(cb, root, request, queryContext);
            wherePredicateMetadata.add(filterPredicate);
        }
        
        queryContext.setWherePredicates(wherePredicateMetadata);
    }


    /**
     * Adds {@code GROUP BY} expressions to the {@link QueryContext} if the request indicates active grouping.
     * <p>
     * Determines whether grouping is needed based on the number of group columns vs. group keys,
     * and collects grouping metadata for the next unresolved group column.
     *
     * @param cb           the {@link CriteriaBuilder}, not used here but provided for consistency
     * @param root         the root entity in the criteria query
     * @param request      the AG Grid server-side row request containing group column info
     * @param queryContext the query context to populate with grouping metadata
     */
    protected void groupBy(CriteriaBuilder cb, Root<E> root, ServerSideGetRowsRequest request, QueryContext queryContext) {
        // we know data are still grouped if request contains more group columns than group keys
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (isGrouping) {
            Map<String, GroupingMetadata> groupingInfo = new HashMap<>();
            for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
                String groupCol = request.getRowGroupCols().get(i).getField();
                GroupingMetadata groupingMetadata = GroupingMetadata
                        .builder(root.get(groupCol))
                        .column(groupCol)
                        .build();
                
                groupingInfo.put(groupCol, groupingMetadata);
            }
            
            queryContext.setGrouping(groupingInfo);
        }
    }


    /**
     * Builds {@code ORDER BY} expressions based on the sorting model in the {@link ServerSideGetRowsRequest}
     * and the current query mode (pivoting, grouping, or flat).
     * <p>
     * Depending on the mode, it generates appropriate sort orders for grouped columns,
     * aggregated fields, or direct selections, and stores them in the {@link QueryContext}.
     *
     * @param cb           the {@link CriteriaBuilder} used to construct order expressions
     * @param request      the AG Grid server-side row request containing sort model data
     * @param queryContext the query context to populate with order metadata
     */
    protected void orderBy(CriteriaBuilder cb, ServerSideGetRowsRequest request, QueryContext queryContext) {
        
        List<Order> orders = new ArrayList<>();
        
        // grid is in pivot mode
        if (queryContext.getPivotingContext().isPivoting()) {
            // when pivoting, only groups or aggregated values can be sorted
            List<Order> pivotingGroupsOrders = request.getSortModel()
                    .stream()
                    .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))
                    .filter(model -> queryContext.getGrouping().containsKey(model.getColId()))
                    .limit(request.getGroupKeys().size() + 1)
                    .map(sortModel -> {
                        Expression<?> groupingExpression = queryContext.getGrouping().get(sortModel.getColId()).getGropingExpression();
                        if (sortModel.getSort() == SortType.asc) {
                            return cb.asc(groupingExpression);
                        } else if (sortModel.getSort() == SortType.desc) {
                            return cb.desc(groupingExpression);
                        } else {
                            throw new RuntimeException();
                        }
                    })
                    .collect(Collectors.toList());
            
            List<Order> pivotingAggregationOrders = request.getSortModel()
                    .stream()
                    .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))
                    .filter(model -> queryContext.getSelections().containsKey(model.getColId()))
                    .filter(model -> queryContext.getSelections().get(model.getColId()).isPivotingSelection())
                    .map(sortModel -> {
                        Expression<?> pivotingAggregationExpression = (Expression<?>) queryContext.getSelections().get(sortModel.getColId()).getSelection();
                        if (sortModel.getSort() == SortType.asc) {
                            return cb.asc(pivotingAggregationExpression);
                        } else if (sortModel.getSort() == SortType.desc) {
                            return cb.desc(pivotingAggregationExpression);
                        } else {
                            throw new RuntimeException();
                        }
                    })
                    .collect(Collectors.toList());
            
            orders.addAll(pivotingGroupsOrders);
            orders.addAll(pivotingAggregationOrders);
        } else {
            // we know data are still grouped if request contains more group columns than group keys
            boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
            if (isGrouping) {
                // grid is in grouping mode

                // ordering by grouped columns
                List<Order> groupOrders = request.getSortModel().stream()
                        .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))    // ignore auto-generated column
                        .filter(model -> queryContext.getGrouping().containsKey(model.getColId()))
                        .map(sortModel -> {
                            Expression<?> gropingExpression = queryContext.getGrouping().get(sortModel.getColId()).getGropingExpression();
                            if (sortModel.getSort() == SortType.asc) {
                                return cb.asc(gropingExpression);
                            } else if (sortModel.getSort() == SortType.desc) {
                                return cb.desc(gropingExpression);
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .limit(request.getGroupKeys().size() + 1)
                        .collect(Collectors.toList());

                // ordering by aggregated columns
                List<Order> aggregationOrders = request.getSortModel().stream()
                        .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))
                        // in aggregation cols
                        .filter(model -> queryContext.getSelections().containsKey(model.getColId()))
                        .filter(model -> queryContext.getSelections().get(model.getColId()).isAggregationSelection())
                        .map(model -> {
                            Expression<?> aggregatedField = (Expression<?>) queryContext.getSelections().get(model.getColId()).getSelection();
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

                orders.addAll(groupOrders);
                orders.addAll(aggregationOrders);

            } else {
                // grid is in basic mode :)
                List<Order> columnOrders = request.getSortModel().stream()
                        .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))
                        .filter(model -> this.colDefs.containsKey(model.getColId()))
                        .filter(model -> queryContext.getSelections().containsKey(model.getColId()))
                        .map(sortModel -> {
                            Expression<?> field = (Expression<?>) queryContext.getSelections().get(sortModel.getColId()).getSelection();
                            if (sortModel.getSort() == SortType.asc) {
                                return cb.asc(field);
                            } else if (sortModel.getSort() == SortType.desc) {
                                return cb.desc(field);
                            } else {
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

                orders.addAll(columnOrders);
            }
        }

        queryContext.setOrders(
                orders.stream()
                        .map(o -> OrderMetadata.builder(o).colId(o.getExpression().getAlias()).build())
                        .collect(Collectors.toList())
        );
    }

    /**
     * Constructs {@code HAVING} predicates from the filter model for fields that are aggregated selections.
     * <p>
     *
     * @param cb           the {@link CriteriaBuilder} used to construct predicates
     * @param request      the AG Grid server-side row request containing filter model data
     * @param queryContext the query context to populate with {@code HAVING} clause metadata
     * @throws IllegalArgumentException if a referenced column is not found in {@code colDefs}
     */
    @SuppressWarnings("unchecked")
    protected void having(CriteriaBuilder cb, ServerSideGetRowsRequest request, QueryContext queryContext) {
        if (request.getFilterModel() == null) {
            return;
        }
        // we know data are still grouped if request contains more group columns than group keys
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (!isGrouping) {
            // when not grouping, can't have 'having' clause
            return;
        }
        
        if (queryContext.getPivotingContext().isPivoting()) {
            // todo: pivoting filtering later, need to investigate more
            if (request.getRowGroupCols().size() != request.getGroupKeys().size() + 1) {
                // only apply filtering when we are on the last group
                return;
            }
            
            List<HavingMetadata> pivotingHavingMetadata = request.getFilterModel().entrySet()
                    .stream()
                    .filter(entry -> queryContext.getSelections().containsKey(entry.getKey()))
                    .filter(entry -> queryContext.getSelections().get(entry.getKey()).isPivotingSelection())
                    .map(entry -> {
                        String selectionAlias = entry.getKey();
                        SelectionMetadata selectionMetadata = queryContext.getSelections().get(selectionAlias);
                        String columnName = selectionAlias.substring(selectionAlias.lastIndexOf(this.serverSidePivotResultFieldSeparator) + 1);;

                        IFilter<?, ?> filter = Optional.ofNullable(this.colDefs.get(columnName)).map(ColDef::getFilter)
                                .orElseThrow(() -> new IllegalArgumentException("Column " + columnName + " not found in col defs"));
                        Map<String, Object> filterMap = (Map<String, Object>) entry.getValue();

                        Predicate predicate = filter.toPredicate(cb, (Expression<?>) selectionMetadata.getSelection(), filterMap);

                        return HavingMetadata.builder(predicate)
                                .isPivoting(true)
                                .build();
                    })
                    .collect(Collectors.toList());
            
            queryContext.setHaving(pivotingHavingMetadata);
        } else {
            // not pivoting
            // 'groupAggFiltering' must be turned on, and we filter only the first group (group keys is empty)
            if (this.groupAggFiltering && request.getGroupKeys().isEmpty()) {
                List<HavingMetadata> havingMetadata = request.getFilterModel().entrySet()
                        .stream()
                        .filter(entry -> queryContext.getSelections().containsKey(entry.getKey()))
                        .filter(entry -> queryContext.getSelections().get(entry.getKey()).isAggregationSelection())
                        .map(entry -> {
                            String columnName = entry.getKey();
                            SelectionMetadata selectionMetadata = queryContext.getSelections().get(columnName);

                            IFilter<?, ?> filter = Optional.ofNullable(this.colDefs.get(columnName)).map(ColDef::getFilter)
                                    .orElseThrow(() -> new IllegalArgumentException("Column " + columnName + " not found in col defs"));
                            Map<String, Object> filterMap = (Map<String, Object>) entry.getValue();

                            Predicate predicate = filter.toPredicate(cb, (Expression<?>) selectionMetadata.getSelection(), filterMap);
                            return HavingMetadata.builder(predicate).build();
                        })
                        .collect(Collectors.toList());
                
                queryContext.setHaving(havingMetadata);
            }
        }
    }

    /**
     * Sets pagination parameters in the {@link QueryContext} based on the request's row range.
     * <p>
     * Configures the starting index and the maximum number of results to fetch from the data source.
     *
     * @param request      the AG Grid server-side row request containing {@code startRow} and {@code endRow}
     * @param queryContext the query context to populate with pagination (offset and limit)
     */
    protected void limitOffset(ServerSideGetRowsRequest request, QueryContext queryContext) {
        queryContext.setFirstResult(request.getStartRow());
        queryContext.setMaxResults(request.getEndRow() - request.getStartRow() + 1);
    }

    /**
     * Converts a list of JPA {@link Tuple} objects to a list of maps keyed by their aliases.
     *
     * @param tuples the list of JPA tuples to convert
     * @return a list of maps where each map represents a tuple with alias-value pairs
     */
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

    /**
     * Converts a filter model into a {@link Predicate} wrapped in {@link WherePredicateMetadata}.
     * <p>
     * Supports both column-based and advanced filters. Filters on aggregation fields are excluded
     * and should be processed separately as HAVING clauses.
     *
     * @param cb            the {@link CriteriaBuilder} used to construct predicates
     * @param root          the query root entity
     * @param request       the request received from the client (AG Grid)
     * @param queryContext  the current query context holding metadata like selections
     * @return the constructed {@link WherePredicateMetadata} representing the filter predicate
     * @throws IllegalArgumentException if a filter references a non-existent or non-filterable column
     */
    @SuppressWarnings("unchecked")
    private WherePredicateMetadata filterToWherePredicate(CriteriaBuilder cb, Root<E> root, ServerSideGetRowsRequest request, QueryContext queryContext) {
        Map<String, Object> filterModel = request.getFilterModel();
        
        if (!this.isColumnFilter(filterModel)) {
            // advanced filter
            AdvancedFilterModel advancedFilterModel = this.recognizeAdvancedFilter(filterModel);
            Predicate predicate = advancedFilterModel.toPredicate(cb, root);
            
            return WherePredicateMetadata
                    .builder(predicate)
                    .isFilterPredicate(true)
                    .isAdvancedFilterPredicate(true)
                    .advancedFilterModel(advancedFilterModel)
                    .build();
        } else {
            // we know data are still grouped if request contains more group columns than group keys
            boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
            
            List<Predicate> predicates = new ArrayList<>(filterModel.size());
            for (var entry : filterModel.entrySet()) {
                String columnName = entry.getKey();
                Map<String, Object> filterMap = (Map<String, Object>) entry.getValue();
                
                if (queryContext.getPivotingContext().isPivoting() && queryContext.getPivotingContext().getPivotingResultFields().contains(columnName)) {
                    // filter on pivot-generated column, will be resolved in 'having' clause, skip
                    continue;
                }
                
                // find col def
                ColDef colDef = Optional.ofNullable(this.colDefs.get(columnName))
                        .orElseThrow(() -> new IllegalArgumentException("Column " + columnName + " not found in col defs"));
                // filter of given column
                IFilter<?, ?> filter = colDef.getFilter();
                if (filter == null) {
                    throw new IllegalArgumentException("Column " + columnName + " is not filterable field!");
                }
                
                // allowing filters to also apply against the aggregated values
                if (this.groupAggFiltering) {
                    // when filter is applied on column that is aggregated, we do not apply
                    // predicate in 'where' clause, but in 'having' clause (we are filtering on aggregated value)
                    boolean isAggregationColumn = request.getValueCols().stream().anyMatch(vc -> vc.getId().equals(columnName));
                    if (isAggregationColumn) {
                        continue;
                    }
                }
                // When using Filters and Aggregations together, the aggregated values reflect only the rows which have passed the filter. 
                // This can be changed to instead ignore applied filters by using the 'suppressAggFilteredOnly' grid option.
                if (isGrouping && this.suppressAggFilteredOnly) {
                    // if this filter was applied above the column that is not group column, ignore it
                    boolean isGroupFilter = request.getRowGroupCols().stream().anyMatch(c -> c.getId().equals(columnName));
                    if (!isGroupFilter) {
                        continue;
                    }
                }
                
                // predicate from filter
                Predicate predicate = filter.toPredicate(cb, root.get(columnName), filterMap);
                predicates.add(predicate);
            }

            Predicate predicate = cb.and(predicates.toArray(new Predicate[0]));
            
            return WherePredicateMetadata
                    .builder(predicate)
                    .isFilterPredicate(true)
                    .isColumnFilterPredicate(true)
                    .build();
        }
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
            if (!this.colDefs.containsKey(colId)) {
                throw new IllegalArgumentException("Can not filter on column not defined in col defs!");
            }
            // assert the filter has enabled filtering
            if (this.colDefs.get(colId).getFilter() == null) {
                throw new IllegalArgumentException("Can not filter on column which has filtering turned-off");
            }
            
            switch (filterType) {
                case "text": case "object": {
                    TextAdvancedFilterModel textAdvancedFilterModel = new TextAdvancedFilterModel(colId);
                    textAdvancedFilterModel.setType(TextAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    textAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).orElse(null));
                    Optional.ofNullable(this.colDefs.get(colId).getFilter())
                            .map(IFilter::getFilterParams)
                            .filter(fp -> fp instanceof TextFilterParams)
                            .map(fp -> (TextFilterParams) fp)
                            .ifPresent(textAdvancedFilterModel::setFilterParams);
                    return textAdvancedFilterModel;
                }
                case "date": case "dateString": {
                    DateAdvancedFilterModel dateAdvancedFilterModel = new DateAdvancedFilterModel(colId);
                    dateAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    dateAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(f -> LocalDate.parse(f, DATE_FORMATTER_FOR_DATE_ADVANCED_FILTER)).orElse(null));
                    Optional.ofNullable(this.colDefs.get(colId).getFilter())
                            .map(IFilter::getFilterParams)
                            .filter(fp -> fp instanceof DateFilterParams)
                            .map(fp -> (DateFilterParams) fp)
                            .ifPresent(dateAdvancedFilterModel::setFilterParams);
                    return dateAdvancedFilterModel;
                }
                case "number": {
                    NumberAdvancedFilterModel numberAdvancedFilterModel = new NumberAdvancedFilterModel(colId);
                    numberAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    numberAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(BigDecimal::new).orElse(null));
                    Optional.ofNullable(this.colDefs.get(colId).getFilter())
                            .map(IFilter::getFilterParams)
                            .filter(fp -> fp instanceof NumberFilterParams)
                            .map(fp -> (NumberFilterParams) fp)
                            .ifPresent(numberAdvancedFilterModel::setFilterParams);
                    return numberAdvancedFilterModel;
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

    protected void validateRequest(ServerSideGetRowsRequest request) {
        List<InvalidRequestException.ValidationError> errors = new ArrayList<>();

        // validate groups cols
        if (request.getRowGroupCols() != null && !request.getRowGroupCols().isEmpty()) {
            List<ColumnVO> rowGroupColsNotInColDefs = request.getRowGroupCols().stream()
                    .filter(c -> !this.colDefs.containsKey(c.getField()))
                    .collect(Collectors.toList());
            if (!rowGroupColsNotInColDefs.isEmpty()) {
                String invalidFields = rowGroupColsNotInColDefs.stream()
                        .map(ColumnVO::getField)
                        .collect(Collectors.joining(", "));
                errors.add(new InvalidRequestException.ValidationError(
                        "rowGroupCols",
                        "These row group columns are not found in column definitions: " + invalidFields,
                        rowGroupColsNotInColDefs
                ));
            }

            List<ColumnVO> rowGroupColsDisabledGrouping = request.getRowGroupCols()
                    .stream()
                    .filter(c -> this.colDefs.containsKey(c.getField()))
                    .filter(c -> !this.colDefs.get(c.getField()).isEnableRowGroup())
                    .collect(Collectors.toList());
            if (!rowGroupColsDisabledGrouping.isEmpty()) {
                String disabledFields = rowGroupColsDisabledGrouping.stream()
                        .map(ColumnVO::getField)
                        .collect(Collectors.joining(", "));
                errors.add(new InvalidRequestException.ValidationError(
                        "rowGroupCols",
                        "These row group columns do not have grouping enabled: " + disabledFields,
                        rowGroupColsDisabledGrouping
                ));
            }
        }

        // validate value cols
        if (request.getValueCols() != null && !request.getValueCols().isEmpty()) {
            List<ColumnVO> valueColsNotInColDefs = request.getValueCols().stream()
                    .filter(c -> !this.colDefs.containsKey(c.getField()))
                    .collect(Collectors.toList());
            if (!valueColsNotInColDefs.isEmpty()) {
                String invalidFields = valueColsNotInColDefs.stream()
                        .map(ColumnVO::getField)
                        .collect(Collectors.joining(", "));
                errors.add(new InvalidRequestException.ValidationError(
                        "valueCols",
                        "These value columns are not found in column definitions: " + invalidFields,
                        valueColsNotInColDefs
                ));
            }

            // turned off aggregations
            List<ColumnVO> valueColsTurnedOffAggregations = request.getValueCols()
                    .stream()
                    .filter(valueCol -> this.colDefs.containsKey(valueCol.getField()))
                    .filter(valueCol -> !this.colDefs.get(valueCol.getField()).isEnableValue())
                    .collect(Collectors.toList());
            if (!valueColsTurnedOffAggregations.isEmpty()) {
                String disabledFields = valueColsTurnedOffAggregations.stream()
                        .map(ColumnVO::getField)
                        .collect(Collectors.joining(", "));
                errors.add(new InvalidRequestException.ValidationError(
                        "valueCols",
                        "These value columns have aggregations disabled. Set enableValue=true in column definitions: " + disabledFields,
                        valueColsTurnedOffAggregations
                ));
            }

            // validate agg functions
            List<ColumnVO> valueColsNotAllowedAggregations = request.getValueCols()
                    .stream()
                    .filter(valueCol -> this.colDefs.containsKey(valueCol.getField()))
                    .filter(valueCol -> this.colDefs.get(valueCol.getField()).isEnableValue())
                    .filter(valueCol -> !this.colDefs.get(valueCol.getField()).getAllowedAggFuncs().contains(valueCol.getAggFunc()))
                    .collect(Collectors.toList());
            if (!valueColsNotAllowedAggregations.isEmpty()) {
                for (ColumnVO col : valueColsNotAllowedAggregations) {
                    errors.add(new InvalidRequestException.ValidationError(
                            "valueCols",
                            String.format("Column '%s' does not allow aggregation function '%s'. Allowed functions: %s",
                                    col.getField(),
                                    col.getAggFunc(),
                                    this.colDefs.get(col.getField()).getAllowedAggFuncs()),
                            col
                    ));
                }
            }
        }

        // validate pivot cols
        if (request.getPivotCols() != null && !request.getPivotCols().isEmpty()) {
            List<ColumnVO> pivotColsNotInColDefs = request.getPivotCols()
                    .stream()
                    .filter(c -> !this.colDefs.containsKey(c.getField()))
                    .collect(Collectors.toList());
            if (!pivotColsNotInColDefs.isEmpty()) {
                String invalidFields = pivotColsNotInColDefs.stream()
                        .map(ColumnVO::getField)
                        .collect(Collectors.joining(", "));
                errors.add(new InvalidRequestException.ValidationError(
                        "pivotCols",
                        "These pivot columns are not found in column definitions: " + invalidFields,
                        pivotColsNotInColDefs
                ));
            }

            List<ColumnVO> pivotColsDisabledPivoting = request.getPivotCols()
                    .stream()
                    .filter(c -> this.colDefs.containsKey(c.getField()))
                    .filter(c -> !this.colDefs.get(c.getField()).isEnablePivot())
                    .collect(Collectors.toList());
            if (!pivotColsDisabledPivoting.isEmpty()) {
                String disabledFields = pivotColsDisabledPivoting.stream()
                        .map(ColumnVO::getField)
                        .collect(Collectors.joining(", "));
                errors.add(new InvalidRequestException.ValidationError(
                        "pivotCols",
                        "These pivot columns do not have pivoting enabled: " + disabledFields,
                        pivotColsDisabledPivoting
                ));
            }
        }

        // validate sort cols
        if (request.getSortModel() != null && !request.getSortModel().isEmpty()) {
            List<SortModelItem> sortModelItemsNotInColDefs = request.getSortModel()
                    .stream()
                    .filter(c -> !AUTO_GROUP_COLUMN_NAME.equals(c.getColId()))
                    .filter(c -> {
                        // check col defs
                        boolean isInColDefs = this.colDefs.containsKey(c.getColId());
                        if (!isInColDefs && request.isPivotMode()) {
                            // check pivoted cols
                            String pivotedColumnOriginalName = this.originalColNameFromPivoted(c.getColId());
                            isInColDefs = this.colDefs.containsKey(pivotedColumnOriginalName);
                        }
                        return !isInColDefs;
                    }).collect(Collectors.toList());
            if (!sortModelItemsNotInColDefs.isEmpty()) {
                String invalidFields = sortModelItemsNotInColDefs.stream()
                        .map(SortModelItem::getColId)
                        .collect(Collectors.joining(", "));
                errors.add(new InvalidRequestException.ValidationError(
                        "sortModel",
                        "These sort columns are not found in column definitions: " + invalidFields,
                        sortModelItemsNotInColDefs
                ));
            }

            Set<String> notSortableColDefs = this.colDefs.keySet().stream()
                    .filter(field -> !this.colDefs.get(field).isSortable())
                    .collect(Collectors.toSet());
            List<SortModelItem> sortModelItemsIllegalSort = request.getSortModel()
                    .stream()
                    .filter(c -> !AUTO_GROUP_COLUMN_NAME.equals(c.getColId()))
                    .filter(sm -> {
                        boolean isNotSortable = notSortableColDefs.contains(sm.getColId());
                        if (!isNotSortable && request.isPivotMode()) {
                            // check pivoted cols
                            String pivotedColumnOriginalName = this.originalColNameFromPivoted(sm.getColId());
                            isNotSortable = notSortableColDefs.contains(pivotedColumnOriginalName);
                        }
                        return isNotSortable;
                    }).collect(Collectors.toList());
            if (!sortModelItemsIllegalSort.isEmpty()) {
                String unsortableFields = sortModelItemsIllegalSort.stream()
                        .map(SortModelItem::getColId)
                        .collect(Collectors.joining(", "));
                errors.add(new InvalidRequestException.ValidationError(
                        "sortModel",
                        "These columns cannot be sorted (sorting disabled in column definitions): " + unsortableFields,
                        sortModelItemsIllegalSort
                ));
            }
        }

        if (!errors.isEmpty()) {
            throw new InvalidRequestException(errors);
        }
    }

    /**
     * Creates pivoting context object to hold all the info about pivoting
     * @param cb    criteria builder
     * @param root  root
     * @param request   request             
     * @throws OnPivotMaxColumnsExceededException when number of columns to be generated from pivot values is bigger than limit  
     * @return pivoting context
     */
    public PivotingContext createPivotingContext(CriteriaBuilder cb, Root<E> root, ServerSideGetRowsRequest request) throws OnPivotMaxColumnsExceededException {

        PivotingContext pivotingContext = new PivotingContext();
        if (!request.isPivotMode() || request.getPivotCols().isEmpty()) {
            // no pivoting
            pivotingContext.setPivoting(false);
        } else {
            pivotingContext.setPivoting(true);

            // check if number of generated columns did not exceed the limit
            if (this.pivotMaxGeneratedColumns != null) {
                long numberOfPivotColumns = this.countPivotColumnsToBeGenerated(cb, request);
                if (numberOfPivotColumns > this.pivotMaxGeneratedColumns) {
                    throw new OnPivotMaxColumnsExceededException(this.pivotMaxGeneratedColumns, numberOfPivotColumns);
                }
            }

            // distinct values for pivoting
            Map<String, List<Object>> pivotValues = this.getPivotValues(cb ,request);
            // pair pivot columns with values
            List<Set<Pair<String, Object>>> pivotPairs = this.createPivotPairs(pivotValues);
            // cartesian product of pivot pairs
            List<List<Pair<String, Object>>> cartesianProduct = cartesianProduct(pivotPairs);
            // for each column name its expression
            Map<String, Expression<?>> columnNamesToExpression = this.createPivotingExpressions(cb, root, request, cartesianProduct);
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
     * For each pivoting column fetch distinct values
     * @return map where key is column name and value is distinct column values
     */
    private Map<String, List<Object>> getPivotValues(CriteriaBuilder cb, ServerSideGetRowsRequest request) {
        Map<String, List<Object>> pivotValues = new LinkedHashMap<>(request.getPivotCols().size());
        for (ColumnVO column : request.getPivotCols()) {
            String field = column.getField();

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

    /**
     * Extracts original column name from pivoted name <br>
     * For example: Piv1_Piv2_Piv3_originalCol will be originalCol
     * @param pivotedName                           pivoted column name
     * @return                                      original name of the column
     */
    private String originalColNameFromPivoted(String pivotedName) {
        return pivotedName.substring(pivotedName.lastIndexOf(this.serverSidePivotResultFieldSeparator) + 1);
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
    private long countPivotColumnsToBeGenerated(CriteriaBuilder cb, ServerSideGetRowsRequest request) {
        if (!request.isPivotMode() || request.getPivotCols().isEmpty()) {
            return 0;
        }

        CriteriaQuery<Long> mainQuery = cb.createQuery(Long.class);

        Expression<Long> productExpression = cb.literal(1L);
        for (ColumnVO pivotCol : request.getPivotCols()) {
            // Subquery for count(distinct <field>)
            Subquery<Long> subquery = mainQuery.subquery(Long.class);
            Root<E> subRoot = subquery.from(this.entityClass);
            subquery.select(cb.countDistinct(subRoot.get(pivotCol.getField())));

            productExpression = cb.prod(productExpression, subquery.getSelection());
        }

        mainQuery.select(productExpression);

        return this.entityManager.createQuery(mainQuery).getSingleResult();
    }

    private Map<String, Expression<?>> createPivotingExpressions(CriteriaBuilder cb, Root<E> root, ServerSideGetRowsRequest request, List<List<Pair<String, Object>>> cartesianProduct) {
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

            request.getValueCols()
                    .forEach(columnVO -> {

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

                        // wrap case expression onto aggregation
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

                        String columnName = alias + this.serverSidePivotResultFieldSeparator + columnVO.getField();
                        pivotingExpressions.put(columnName, aggregatedField);
                    });
        });

        return pivotingExpressions;
    }
    
    public static class Builder<E> {
        private static final String DEFAULT_SERVER_SIDE_PIVOT_RESULT_FIELD_SEPARATOR = "_";
        
        private final Class<E> entityClass;
        private final EntityManager entityManager;

        private String serverSidePivotResultFieldSeparator = DEFAULT_SERVER_SIDE_PIVOT_RESULT_FIELD_SEPARATOR;
        private Integer pivotMaxGeneratedColumns;
        private boolean enableAdvancedFilter;
        private boolean paginateChildRows;
        private boolean groupAggFiltering;
        private boolean suppressAggFilteredOnly;
        
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
        
        public Builder<E> paginateChildRows(boolean paginateChildRows) {
            this.paginateChildRows = paginateChildRows;
            return this;
        }
        
        public Builder<E> suppressAggFilteredOnly(boolean suppressAggFilteredOnly) {
            this.suppressAggFilteredOnly = suppressAggFilteredOnly;
            return this;
        }
        
        public Builder<E> groupAggFiltering(boolean groupAggFiltering) {
            this.groupAggFiltering = groupAggFiltering;
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
