package io.github.smolcan.aggrid.jpa.adapter.query;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.InvalidRequestException;
import io.github.smolcan.aggrid.jpa.adapter.exceptions.OnPivotMaxColumnsExceededException;
import io.github.smolcan.aggrid.jpa.adapter.filter.IFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.JoinOperator;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.JoinAdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.column.*;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgDateColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgNumberColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.filter.provided.simple.AgTextColumnFilter;
import io.github.smolcan.aggrid.jpa.adapter.query.metadata.*;
import io.github.smolcan.aggrid.jpa.adapter.request.*;
import io.github.smolcan.aggrid.jpa.adapter.filter.model.advanced.AdvancedFilterModel;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.query.metadata.PivotingContext;
import io.github.smolcan.aggrid.jpa.adapter.utils.Pair;
import io.github.smolcan.aggrid.jpa.adapter.utils.TriFunction;
import io.github.smolcan.aggrid.jpa.adapter.utils.TypeValueSynchronizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.Getter;
import lombok.NonNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.github.smolcan.aggrid.jpa.adapter.utils.Utils.cartesianProduct;

/**
 * A robust JPA adapter designed to seamlessly integrate AG Grid's Server-Side Row Model (SSRM)
 * with a database backend.
 * <p>
 * This class translates an AG Grid {@link ServerSideGetRowsRequest} into dynamic JPA {@link jakarta.persistence.criteria.CriteriaQuery}
 * structures. It handles the complexity of mapping grid operations directly to database queries, ensuring
 * efficient data retrieval for large datasets.
 * </p>
 *
 * <h2>Key Features</h2>
 * <ul>
 * <li><b>Dynamic Selection:</b> Maps AG Grid columns to JPA entity fields, supporting nested dot notation (e.g., "category.name").</li>
 * <li><b>Filtering:</b> Supports both Simple and Advanced filter models, including text, number, date, and boolean filters.</li>
 * <li><b>Sorting:</b> Applies single or multi-column sorting.</li>
 * <li><b>Row Grouping and Aggregation:</b> Dynamically groups data and calculates aggregates (sum, min, max, avg, count).</li>
 * <li><b>Pivoting:</b> Transforms rows into columns based on pivot configuration.</li>
 * <li><b>Tree Data:</b> Native support for hierarchical (self-referencing) data structures.</li>
 * <li><b>Master-Detail:</b> Supports fetching and mapping detail records for hierarchical grids.</li>
 * </ul>
 *
 *
 * @param <E> the type of the root JPA entity being queried
 * @author Samuel Molčan
 */
@SuppressWarnings({"unused", "java:S3776"})
public class QueryBuilder<E, D> {
    protected static final DateTimeFormatter DATE_FORMATTER_FOR_DATE_ADVANCED_FILTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    protected static final String AUTO_GROUP_COLUMN_NAME = "ag-Grid-AutoColumn";

    protected final Class<E> entityClass;
    protected final String primaryFieldName;
    protected final EntityManager entityManager;
    protected final String serverSidePivotResultFieldSeparator;
    protected final boolean enableAdvancedFilter;
    protected final Integer pivotMaxGeneratedColumns;
    protected final boolean paginateChildRows;
    protected final boolean groupAggFiltering;
    protected final boolean suppressAggFilteredOnly;
    protected final boolean isExternalFilterPresent;
    protected final Map<String, BiFunction<CriteriaBuilder, Expression<?>, Expression<?>>> aggFuncs;
    protected final TriFunction<CriteriaBuilder, Root<E>, Object, Predicate> doesExternalFilterPass;
    protected final boolean suppressFieldDotNotation;
    protected final boolean getChildCount;
    protected final String getChildCountFieldName;
    
    protected final boolean isQuickFilterPresent;
    protected final Function<String, List<String>> quickFilterParser;
    protected final TriFunction<CriteriaBuilder, Root<E>, List<String>, Predicate> quickFilterMatcher;
    protected final List<FieldPath<E, String>> quickFilterSearchInFields;
    protected final boolean quickFilterTrimInput;
    protected final boolean quickFilterCaseSensitive;
    protected final BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> quickFilterTextFormatter;

    protected final boolean treeData;
    protected final String isServerSideGroupFieldName;
    protected final String treeDataParentReferenceField;
    protected final String treeDataParentIdField;
    protected final String treeDataChildrenField;
    protected final String treeDataDataPathFieldName;
    protected final String treeDataDataPathSeparator;

    protected final boolean masterDetail;
    protected final boolean masterDetailLazy;
    protected final String masterDetailRowDataFieldName;
    protected final MasterDetailParams<E, D> masterDetailParams;
    protected final Function<Map<String, Object>, MasterDetailParams<E, D>> dynamicMasterDetailParams;
    protected final boolean grandTotalRow;


    protected final Map<String, ColDef<E, ?>> colDefs;
    
    @NonNull
    public static <E> Builder<E, Void> builder(@NonNull Class<E> entityClass, @NonNull EntityManager entityManager) {
        return new Builder<>(entityClass, entityManager);
    }
    
    @NonNull
    public static <E, D> Builder<E, D> builder(@NonNull Class<E> entityClass, @NonNull Class<D> detailClass, @NonNull EntityManager entityManager) {
        return new Builder<>(entityClass, detailClass, entityManager);
    }
    
    protected QueryBuilder(@NonNull Builder<E, D> builder) {
        this.entityClass = builder.entityClass;
        this.entityManager = builder.entityManager;
        this.primaryFieldName = builder.primaryFieldName;
        this.serverSidePivotResultFieldSeparator = builder.serverSidePivotResultFieldSeparator;
        this.enableAdvancedFilter = builder.enableAdvancedFilter;
        this.pivotMaxGeneratedColumns = builder.pivotMaxGeneratedColumns;
        this.paginateChildRows = builder.paginateChildRows;
        this.groupAggFiltering = builder.groupAggFiltering;
        this.suppressAggFilteredOnly = builder.groupAggFiltering || builder.suppressAggFilteredOnly;
        this.isExternalFilterPresent = builder.isExternalFilterPresent;
        this.aggFuncs = builder.aggFuncs;
        this.doesExternalFilterPass = builder.doesExternalFilterPass;
        this.suppressFieldDotNotation = builder.suppressFieldDotNotation;
        this.getChildCount = builder.getChildCount;
        this.getChildCountFieldName = builder.getChildCountFieldName;
        this.isQuickFilterPresent = builder.isQuickFilterPresent;
        this.quickFilterParser = builder.quickFilterParser;
        this.quickFilterMatcher = builder.quickFilterMatcher;
        this.quickFilterSearchInFields = builder.quickFilterSearchInFields;
        this.quickFilterTrimInput = builder.quickFilterTrimInput;
        this.quickFilterCaseSensitive = builder.quickFilterCaseSensitive;
        this.quickFilterTextFormatter = builder.quickFilterTextFormatter;
        this.treeData = builder.treeData;
        this.isServerSideGroupFieldName = builder.isServerSideGroupFieldName;
        this.treeDataParentReferenceField = builder.treeDataParentReferenceField;
        this.treeDataParentIdField = builder.treeDataParentIdField;
        this.treeDataChildrenField = builder.treeDataChildrenField;
        this.treeDataDataPathFieldName = builder.treeDataDataPathFieldName;
        this.treeDataDataPathSeparator = builder.treeDataDataPathSeparator;
        this.masterDetail = builder.masterDetail;
        this.masterDetailLazy = builder.masterDetailLazy;
        this.masterDetailRowDataFieldName = builder.masterDetailRowDataFieldName;
        this.masterDetailParams = builder.masterDetailParams;
        this.dynamicMasterDetailParams = builder.dynamicMasterDetailParams;
        this.grandTotalRow = builder.grandTotalRow;
        
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
    @NonNull
    public LoadSuccessParams getRows(@NonNull ServerSideGetRowsRequest request) {
        this.validateRequest(request);
        
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<E> root = query.from(this.entityClass);
        // record all the context we put into query
        QueryContext<E> queryContext = new QueryContext<>(cb, query, root);
        
        this.select(queryContext, request);
        this.where(queryContext, request);
        this.groupBy(queryContext, request);
        this.having(queryContext, request);
        this.orderBy(queryContext, request);
        this.limitOffset(queryContext, request);
        
        List<Tuple> data = this.apply(query, queryContext);
        List<Map<String, Object>> resData = this.tupleToMap(data);
        if (this.masterDetail && !this.masterDetailLazy) {
            this.attachDetailRowDataToMasters(resData);
        }
        
        LoadSuccessParams loadSuccessParams = new LoadSuccessParams();
        loadSuccessParams.setRowData(resData);
        loadSuccessParams.setPivotResultFields(queryContext.getPivotingContext().getPivotingResultFields());
        if (this.grandTotalRow && request.isNeedsGrandTotal()) {
            Map<String, Object> grandTotalData = this.getGrandTotalData(request);
            loadSuccessParams.setGrandTotalData(grandTotalData);
        }
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
    @SuppressWarnings("unchecked")
    public long countRows(@NonNull ServerSideGetRowsRequest request) throws OnPivotMaxColumnsExceededException {
        this.validateRequest(request);

        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<E> root = query.from(this.entityClass);
        // record all the context we put into query
        QueryContext<E> queryContext = new QueryContext<>(cb, query, root);
        
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
            ColDef<E, ?> countingGroupColDef = this.colDefs.get(countingGroupCol);

            // subquery will only select the group column 
            Subquery<?> subquery = query.subquery(countingGroupColDef.getField().getPath(root).getJavaType());
            Root<E> subqueryRoot = subquery.from(this.entityClass);
            QueryContext<E> subqueryContext = new QueryContext<>(cb, subquery, subqueryRoot);
            
            this.select(subqueryContext, request);
            this.where(subqueryContext, request);
            this.groupBy(subqueryContext, request);
            this.having(subqueryContext, request);
            
            // select the group column in subquery
            subquery.select((Expression) countingGroupColDef.getField().getPath(subqueryRoot));
            // where
            if (!subqueryContext.getWherePredicates().isEmpty()) {
                Predicate[] predicates = subqueryContext.getWherePredicates().stream().map(WherePredicateMetadata::getPredicate).toArray(Predicate[]::new);
                subquery.where(predicates);
            }
            // group by
            if (!subqueryContext.getGrouping().isEmpty()) {
                subquery.groupBy(subqueryContext.getGrouping().stream().map(GroupingMetadata::getGropingExpression).collect(Collectors.toList()));
            }
            // having
            if (!subqueryContext.getHaving().isEmpty()) {
                Predicate[] having = subqueryContext.getHaving().stream().map(HavingMetadata::getPredicate).toArray(Predicate[]::new);
                subquery.having(having);
            }
            
            // in parent query, count distinct values of column group that are returned in subquery
            query.select(cb.countDistinct(countingGroupColDef.getField().getPath(root)));
            query.where(cb.in(countingGroupColDef.getField().getPath(root)).value((Subquery) subquery));
            
            return this.entityManager.createQuery(query).getSingleResult();
        } else {
            // no groups, count rows
            this.select(queryContext, request);
            this.where(queryContext, request);
            
            query.select(cb.count(root));
            if (!queryContext.getWherePredicates().isEmpty()) {
                Predicate[] predicates = queryContext.getWherePredicates().stream().map(WherePredicateMetadata::getPredicate).toArray(Predicate[]::new);
                query.where(predicates);
            }
            
            return this.entityManager.createQuery(query).getSingleResult();
        }
    }
    
    /**
     * Computes aggregated values across all rows matching the request filters,
     * intended to populate the grid's grand total row.
     *
     * @param request the AG Grid server-side request containing value columns and filters.
     * @return a map of aggregated values keyed by field name.
     * @throws IllegalStateException if the grand total row is not enabled on this {@code QueryBuilder}.
     */
    @NonNull
    public Map<String, Object> getGrandTotalData(@NonNull ServerSideGetRowsRequest request) {
        if (!this.grandTotalRow) {
            throw new IllegalStateException("Grand total row is disabled, enable it to get grand total data");
        }
        
        this.validateRequest(request);
        if (request.getValueCols().isEmpty()) {
            return Collections.emptyMap();
        }
        
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<E> root = query.from(this.entityClass);
        // record all the context we put into query
        QueryContext<E> queryContext = new QueryContext<>(cb, query, root);

        // select value cols
        for (ColumnVO columnVO : request.getValueCols()) {
            Expression<?> path = this.colDefs.get(columnVO.getField()).getField().getPath(root);
            var aggregateFunction = this.aggFuncs.get(columnVO.getAggFunc());
            Expression<?> aggregatedField = aggregateFunction.apply(cb, path);
            queryContext.getSelections().add(
                    SelectionMetadata
                            .builder()
                            .alias(columnVO.getField())
                            .expression(aggregatedField)
                            .isAggregationSelection(true)
                            .build()
            );
        }
        // filter
        this.where(queryContext, request);
        // remove the ones that filter group keys
        if (!request.getGroupKeys().isEmpty()) {
            queryContext.setWherePredicates(
                    queryContext.getWherePredicates()
                            .stream()
                            .filter(p -> !p.isGroupPredicate())
                            .collect(Collectors.toList())
            );
        }
        
        
        // apply
        query.multiselect(queryContext.getSelections().stream().map(s -> s.getExpression().alias(s.getAlias())).collect(Collectors.toList()));
        if (!queryContext.getWherePredicates().isEmpty()) {
            Predicate[] predicates = queryContext.getWherePredicates().stream().map(WherePredicateMetadata::getPredicate).toArray(Predicate[]::new);
            query.where(predicates);
        }
        Tuple data = this.entityManager.createQuery(query).getSingleResult();
        
        return this.tupleToMap(List.of(data)).get(0);
    }

    /**
     * Retrieves the detail row data for a specific master row in Master-Detail mode.
     * <p>
     * This method executes a query to fetch child records associated with the provided
     * {@code masterRow}, applying any dynamic class resolution or column definitions if configured.
     *
     * @param masterRow the data of the parent row for which details are being requested
     * @return a list of maps representing the detail rows
     */
    @NonNull
    public List<Map<String, Object>> getDetailRowData(@NonNull Map<String, Object> masterRow) {
        if (!this.masterDetail) {
            throw new IllegalStateException("Please set masterDetail property to true to use detail row data");
        }
        
        // find params for detail grid
        MasterDetailParams<E, D> params = this.dynamicMasterDetailParams != null
                ? this.dynamicMasterDetailParams.apply(masterRow)   // dynamic
                : this.masterDetailParams;                          // static
        
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<D> root = query.from(params.getDetailClass());
        
        // select
        query.multiselect(
                params.getDetailColDefs().values().stream()
                .map(colDef -> params.getDetailColDefs().get(colDef.getFieldName()).getField().getPath(root).alias(colDef.getFieldName()))
                .collect(Collectors.toList())
        );

        // master predicate
        Predicate masterPredicate = this.createMasterRowPredicate(cb, root, masterRow, params);
        query.where(masterPredicate);

        // result
        TypedQuery<Tuple> typedQuery = this.entityManager.createQuery(query);
        List<Tuple> data = typedQuery.getResultList();
        return this.tupleToMap(data);
    }

    /**
     * Supplies a list of unique values for an AG Grid Set Filter for the specified field.
     * Fetches distinct values from the database and returns them sorted in ascending order.
     *
     * @param field the name of the field to retrieve unique values for.
     * @return a sorted list of distinct values present in the database.
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public <T> List<T> supplySetFilterValues(@NonNull FieldPath<E, T> field) {
        ColDef<E, T> colDef = (ColDef<E, T>) this.colDefs.get(field.getName());
        if (colDef == null) {
            throw new IllegalArgumentException(String.format("Column definition for field '%s' not found.", field));
        }
        if (colDef.getFilter() == null) {
            throw new IllegalStateException(String.format("Filter not enabled for field '%s'.", field));
        }

        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(field.getJavaType());
        Root<E> root = query.from(this.entityClass);
        Path<T> path = colDef.getField().getPath(root);
        
        // select
        query.select(path).distinct(true);
        // order by asc
        query.orderBy(cb.asc(path));
        
        return this.entityManager.createQuery(query).getResultList();
    }
    
    @NonNull
    @SuppressWarnings("unchecked")
    public List<Object> supplySetFilterValues(@NonNull String fieldName) {
        ColDef<E, ?> colDef = this.colDefs.get(fieldName);
        if (colDef == null) {
            throw new IllegalArgumentException(String.format("Column definition for field '%s' not found.", fieldName));
        }
        return (List<Object>) supplySetFilterValues(colDef.getField());
    }

    /**
     * Determines and sets the fields to be selected in the query.
     * Delegates the selection logic based on the active grid mode. 
     * Sets the selections into queryContext.
     * 
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     */
    protected void select(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        // select
        List<SelectionMetadata> selections;
        if (this.treeData) {
            // tree data
            selections = this.selectTreeData(queryContext, request);
        } else if (this.masterDetail) {
            // master-detail
            selections = this.selectMasterDetail(queryContext, request);
        } else if (request.isPivotMode() && !request.getPivotCols().isEmpty()) {
            // pivoting
            selections = this.selectPivoting(queryContext, request);
        } else if (!request.getRowGroupCols().isEmpty()) {
            // grouping
            selections = this.selectGrouping(queryContext, request);
        } else {
            // basic grid
            selections = this.selectBasic(queryContext, request);
        }

        queryContext.setSelections(selections);
    }

    /**
     * Constructs the filtering criteria (WHERE clause) for the query.
     * Delegates filtering criteria creation based on active grid mode.
     * Sets the filtering criteria into queryContext.
     * 
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid  
     */
    protected void where(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        List<WherePredicateMetadata> wherePredicates;
        if (this.treeData) {
            // tree data
            wherePredicates = this.whereTreeData(queryContext, request);
        } else if (this.masterDetail) {
            // master detail
            wherePredicates = this.whereMasterDetail(queryContext, request);
        } else if (request.isPivotMode() && !request.getPivotCols().isEmpty()) {
            // pivoting
            wherePredicates = this.wherePivoting(queryContext, request);
        } else if (!request.getRowGroupCols().isEmpty()) {
            // grouping
            wherePredicates = this.whereGrouping(queryContext, request);
        } else {
            // basic grid
            wherePredicates = this.whereBasic(queryContext, request);
        }

        queryContext.setWherePredicates(wherePredicates);
    }

    /**
     * Creates the grouping rules for the query.
     * Delegates the grouping rules creation based on active grid mode.
     * Sets the grouping rules into query context.
     * 
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     */
    protected void groupBy(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        List<GroupingMetadata> groupingMetadata;

        if (this.treeData) {
            // tree data
            groupingMetadata = this.groupByTreeData(queryContext, request);
        } else if (this.masterDetail) {
            // master-detail
            groupingMetadata = this.groupByMasterDetail(queryContext, request);
        } else if (request.isPivotMode() && !request.getPivotCols().isEmpty()) {
            // pivoting
            groupingMetadata = this.groupByPivoting(queryContext, request);
        } else if (!request.getRowGroupCols().isEmpty()) {
            // grouping
            groupingMetadata = this.groupByGrouping(queryContext, request);
        } else {
            // basic grid (does not have any grouping)
            groupingMetadata = List.of();
        }

        queryContext.setGrouping(groupingMetadata);
    }


    /**
     * Creates the sort order for the query results.
     * Delegates sort order creation based on active grid mode.
     * Sets the sorting rules into query context.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     */
    protected void orderBy(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        List<OrderMetadata> orders;
        if (this.treeData) {
            orders = this.orderByTreeData(queryContext, request);
        } else if (this.masterDetail) {
            orders = this.orderByMasterDetail(queryContext, request);
        } else if (request.isPivotMode() && !request.getPivotCols().isEmpty()) {
            orders = this.orderByPivoting(queryContext, request);
        } else if (!request.getRowGroupCols().isEmpty()) {
            orders = this.orderByGrouping(queryContext, request);
        } else {
            orders = this.orderByBasic(queryContext, request);
        }

        queryContext.setOrders(orders);
    }

    /**
     * Creates predicates for the HAVING clause.
     * Delegates predicate creation based on active query mode.
     * Sets the having predicates into query context.
     * 
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     */
    protected void having(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        List<HavingMetadata> havingPredicates;
        if (this.treeData) {
            havingPredicates = List.of();
        } else if (this.masterDetail) {
            havingPredicates = List.of();
        } else if (request.isPivotMode() && !request.getPivotCols().isEmpty()) {
            havingPredicates = this.havingPivoting(queryContext, request);
        } else if (!request.getRowGroupCols().isEmpty()) {
            havingPredicates = this.havingGrouping(queryContext, request);
        } else {
            havingPredicates = List.of();
        }
        queryContext.setHaving(havingPredicates);
    }

    /**
     * Efficiently fetches and attaches detail rows to the provided list of master rows.
     * <p>
     * This method avoids the "N+1 Select Problem" by executing a single query with an {@code IN} clause
     * to fetch all relevant details for the current page, then groups and assigns them in memory.
     * <p>
     * If dynamic configuration is active, it falls back to iterative fetching.
     *
     * @param masters the list of master row data maps to be populated
     */
    protected void attachDetailRowDataToMasters(@NonNull List<Map<String, Object>> masters) {
        if (masters.isEmpty()) {
            return;
        }

        // dynamic params or custom detail function, N+1
        if (this.dynamicMasterDetailParams != null || (this.masterDetailParams != null && this.masterDetailParams.createMasterRowPredicate != null)) {
            for (Map<String, Object> row : masters) {
                row.put(this.masterDetailRowDataFieldName, this.getDetailRowData(row));
            }
            return;
        }
        
        Objects.requireNonNull(this.masterDetailParams);
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<D> detailRoot = query.from(this.masterDetailParams.getDetailClass());
        
        List<Selection<?>> detailSelections = this.masterDetailParams.getDetailColDefs().values().stream()
                .map(colDef -> colDef.getField().getPath(detailRoot).alias(colDef.getFieldName()))
                .collect(Collectors.toList());
        // master rows map by primary field (as string)
        Map<String, Map<String, Object>> masterRowsGroupedByPrimaryField = masters.stream()
                .collect(Collectors.toMap(
                        v -> String.valueOf(v.get(this.primaryFieldName)),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        
        Set<Object> masterIds = masters.stream()
                .map(v -> v.get(this.primaryFieldName))
                .collect(Collectors.toSet());
        
        // helper selections for master id
        String masterPrimaryFieldAlias = "__fk_helper__";
        Path<?> masterPrimaryFieldPath;
        if (this.masterDetailParams.getDetailMasterReferenceField() != null) {
            masterPrimaryFieldPath = detailRoot.get(this.masterDetailParams.getDetailMasterReferenceField()).get(this.primaryFieldName);
        } else {
            masterPrimaryFieldPath = detailRoot.get(this.masterDetailParams.getDetailMasterIdField());
        }
        detailSelections.add(masterPrimaryFieldPath.alias(masterPrimaryFieldAlias));
        
        query.multiselect(detailSelections);
        query.where(masterPrimaryFieldPath.in(masterIds));

        List<Tuple> detailTuples = this.entityManager.createQuery(query).getResultList();
        Map<String, List<Map<String, Object>>> detailsGroupedByMaster = this.tupleToMap(detailTuples).stream()
                .collect(Collectors.groupingBy(v -> String.valueOf(v.get(masterPrimaryFieldAlias))));
        
        masterRowsGroupedByPrimaryField.forEach((masterIdStr, masterRow) -> {
            List<Map<String, Object>> detailRows = detailsGroupedByMaster.getOrDefault(masterIdStr, new ArrayList<>());
            
            detailRows.forEach(dr -> dr.remove(masterPrimaryFieldAlias));
            masterRow.put(this.masterDetailRowDataFieldName, detailRows);
        });
    }

    /**
     * Constructs the JPA predicate used to filter detail records based on the master row.
     * <p>
     * This method builds the {@code WHERE} clause that links the detail entity to the specific
     * parent record, using either a custom predicate function or standard foreign key matching.
     *
     * @param cb        the {@link CriteriaBuilder} used to construct the predicate
     * @param root      the query root of the detail entity
     * @param masterRow the data of the parent row containing the primary key
     * @param params    params for the detail grid             
     * @return the filtering {@link Predicate} used to select only relevant child records
     */
    protected Predicate createMasterRowPredicate(@NonNull CriteriaBuilder cb, @NonNull Root<D> root, @NonNull Map<String, Object> masterRow, @NonNull MasterDetailParams<E, D> params) {
        // add to wherePredicates predicate for parent
        Predicate masterRowPredicate;
        if (params.getCreateMasterRowPredicate() != null) {
            // must have provided predicate function
            masterRowPredicate = params.getCreateMasterRowPredicate().apply(cb, root, masterRow);
        } else {
            Object masterIdValue = masterRow.get(this.primaryFieldName);
            if (masterIdValue == null) {
                throw new IllegalArgumentException(
                        String.format("Master row data is missing value for primary field '%s'. Ensure this field is included in Master Grid columns.", this.primaryFieldName)
                );
            }

            Path<?> pathToCheck;
            if (params.getDetailMasterReferenceField() != null) {
                pathToCheck = root.get(params.getDetailMasterReferenceField()).get(this.primaryFieldName);
            } else {
                pathToCheck = root.get(params.getDetailMasterIdField());
            }

            TypeValueSynchronizer.Result<?> sync = TypeValueSynchronizer.synchronizeTypes(pathToCheck, String.valueOf(masterIdValue));
            masterRowPredicate = cb.equal(sync.getSynchronizedPath(), sync.getSynchronizedValue());
        }

        return masterRowPredicate;
    }

    /**
     * Creates a predicate for the "Quick Filter" logic (multi-word search).
     * <p>
     * This method splits the search string into individual words. For a row to match,
     * <b>every word</b> from the search query must be found in at least one of the
     * configured searchable fields.
     * </p>
     *
     * @param cb          the {@link CriteriaBuilder} used to construct the query
     * @param root        the {@link Root} entity
     * @param quickFilter the raw search string input
     * @return the constructed {@link Predicate}, or {@code null} if the filter is empty
     */
    protected Predicate createQuickFilterPredicate(@NonNull CriteriaBuilder cb, @NonNull Root<E> root, String quickFilter) {
        if (quickFilter == null || quickFilter.isEmpty()) {
            return null;
        }
        
        // parse quick filter value to words
        List<String> words = this.quickFilterParser.apply(quickFilter);
        if (words == null || words.isEmpty()) {
            return null;
        }
        if (this.quickFilterMatcher != null) {
            return this.quickFilterMatcher.apply(cb, root, words);
        }
        
        // predicates for each row
        List<Predicate> wordsPredicates = new ArrayList<>(words.size());
        for (String word : words) {
            Expression<String> wordExpression = cb.literal(word);
            
            // transform word expression according to quick filter config
            if (this.quickFilterTrimInput) {
                wordExpression = cb.trim(wordExpression);
            }
            if (!this.quickFilterCaseSensitive) {
                wordExpression = cb.lower(wordExpression);
            }
            if (this.quickFilterTextFormatter != null) {
                wordExpression = this.quickFilterTextFormatter.apply(cb, wordExpression);
            }

            List<Predicate> rowPredicatesForWord = new ArrayList<>(this.quickFilterSearchInFields.size());
            for (FieldPath<E, String> field : this.quickFilterSearchInFields) {
                Expression<String> path = field.getPath(root);

                // transform path expression according to quick filter config
                if (this.quickFilterTrimInput) {
                    path = cb.trim(path);
                }
                if (!this.quickFilterCaseSensitive) {
                    path = cb.lower(path);
                }
                if (this.quickFilterTextFormatter != null) {
                    path = this.quickFilterTextFormatter.apply(cb, path);
                }
                
                Predicate rowWordPredicate = cb.like(path, cb.concat(cb.concat("%", wordExpression), "%"));
                rowPredicatesForWord.add(rowWordPredicate);
            }

            // A word matches the row if the string value of any of the columns contains the word (OR)
            Predicate wordPredicate = cb.or(rowPredicatesForWord.toArray(new Predicate[0]));
            wordsPredicates.add(wordPredicate);
        }
        
        // All words must match the row for it to be included (AND)
        return cb.and(wordsPredicates.toArray(new Predicate[0]));
    }

    /**
     * Applies the given {@link CriteriaQuery} by setting its select, where, group by,
     * having, and order by clauses based on the provided {@link QueryContext}, then executes it.
     *
     * @param query        the criteria query to configure
     * @param queryContext the context containing metadata for selections, filters, grouping, etc.
     * @return a list of results returned by the executed query
     */
    @NonNull
    protected List<Tuple> apply(@NonNull CriteriaQuery<Tuple> query, @NonNull QueryContext<E> queryContext) {
        // select
        query.multiselect(queryContext.getSelections().stream().map(s -> s.getExpression().alias(s.getAlias())).collect(Collectors.toList()));
        // where
        if (!queryContext.getWherePredicates().isEmpty()) {
            Predicate[] predicates = queryContext.getWherePredicates().stream().map(WherePredicateMetadata::getPredicate).toArray(Predicate[]::new);
            query.where(predicates);
        }
        // group by
        if (!queryContext.getGrouping().isEmpty()) {
            query.groupBy(queryContext.getGrouping().stream().map(GroupingMetadata::getGropingExpression).collect(Collectors.toList()));
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
     * Creates selections for the query when grid is in tree-data mode.
     *
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     * @return created selections
     */
    @NonNull
    protected List<SelectionMetadata> selectTreeData(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        Root<E> root = queryContext.getRoot();
        
        List<SelectionMetadata> selections = new ArrayList<>();
        
        // add each non-aggregated field to selections as basic selection
        this.colDefs.values().stream()
                .filter(cd -> request.getValueCols().stream().noneMatch(vc -> vc.getField().equals(cd.getFieldName()))) // filter out the aggregated ones
                .forEach(colDef -> {
                    Path<?> field = colDef.getField().getPath(root);
                    selections.add(
                            SelectionMetadata.builder()
                                    .alias(colDef.getFieldName())
                                    .expression(field)
                                    .build()
                    );
                });

        Expression<Boolean> isServerSideGroupSelection = this.createTreeDataIsServerSideGroupExpression(queryContext);
        selections.add(
                SelectionMetadata
                        .builder()
                        .alias(this.isServerSideGroupFieldName)
                        .expression(isServerSideGroupSelection)
                        .isServerSideGroupSelection(true)
                        .build()
        );
        
        // aggregation columns
        if (!request.getValueCols().isEmpty()) {
            // add aggregation expressions
            for (ColumnVO aggColumn : request.getValueCols()) {
                Expression<?> aggExpression = this.createTreeDataAggregationExpression(queryContext, isServerSideGroupSelection, aggColumn, request);
                selections.add(
                        SelectionMetadata
                                .builder()
                                .alias(aggColumn.getField())
                                .expression(aggExpression)
                                .isAggregationSelection(true)
                                .build()
                );
            }
        }
        
        // add child counts
        if (this.getChildCount) {
            Expression<Long> countExpression = this.createTreeDataGetChildCountExpression(queryContext, isServerSideGroupSelection, request);
            selections.add(
                    SelectionMetadata
                            .builder()
                            .alias(this.getChildCountFieldName)
                            .expression(countExpression)
                            .isChildCountSelection(true)
                            .build()
            );
        }
        
        return selections;
    }

    /**
     * Creates selections for the query when grid is in master-detail mode.
     *
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     * @return created selections
     */
    @NonNull
    protected List<SelectionMetadata> selectMasterDetail(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        if (request.getGroupKeys().isEmpty()) {
            return this.selectBasic(queryContext, request);
        } else {
            return this.selectGrouping(queryContext, request);
        }
    }

    /**
     * Creates selections for the query when grid is in pivoting mode.
     *
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     * @return created selections
     */
    @NonNull
    protected List<SelectionMetadata> selectPivoting(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        Root<E> root = queryContext.getRoot();
        PivotingContext pivotingContext = this.createPivotingContext(queryContext, request);
        queryContext.setPivotingContext(pivotingContext);
        
        List<SelectionMetadata> selections = new ArrayList<>();
        
        // group columns
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
            ColumnVO groupCol = request.getRowGroupCols().get(i);
            ColDef<E, ?> groupColDef = this.colDefs.get(groupCol.getField());
            Path<?> groupExpression = groupColDef.getField().getPath(root);

            SelectionMetadata groupSelectionMetadata = SelectionMetadata
                    .builder()
                    .alias(groupCol.getField())
                    .expression(groupExpression)
                    .isGroupingSelection(true)
                    .build();
            selections.add(groupSelectionMetadata);
        }

        // pivoting selections
        pivotingContext.getColumnNamesToExpression()
                .entrySet()
                .stream()
                .map(entry -> 
                        SelectionMetadata
                            .builder()
                            .alias(entry.getKey())
                            .expression(entry.getValue())
                            .isPivotingSelection(true)
                            .isAggregationSelection(true)
                            .build()
                )
                .forEach(selections::add);
        
        return selections;
    }

    /**
     * Creates selections for the query when grid is in grouping mode.
     *
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     * @return created selections
     */
    @NonNull
    protected List<SelectionMetadata> selectGrouping(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        List<SelectionMetadata> selections = new ArrayList<>();

        boolean hasUnexpandedGroups = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (hasUnexpandedGroups) {
            // group columns
            for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
                ColumnVO groupCol = request.getRowGroupCols().get(i);
                ColDef<E, ?> groupColDef = this.colDefs.get(groupCol.getField());
                Expression<?> groupExpression = groupColDef.getField().getPath(root);

                SelectionMetadata groupSelectionMetadata = SelectionMetadata
                        .builder()
                        .alias(groupCol.getField())
                        .expression(groupExpression)
                        .isGroupingSelection(true)
                        .build();
                selections.add(groupSelectionMetadata);
            }

            // count children
            if (this.getChildCount) {
                Expression<Long> childCountExpression = cb.count(root);
                selections.add(
                        SelectionMetadata.builder()
                                .alias(this.getChildCountFieldName)
                                .expression(childCountExpression)
                                .isChildCountSelection(true)
                                .build()
                );
            }

            // aggregated columns
            for (ColumnVO columnVO : request.getValueCols()) {
                ColDef<E, ?> valueColDef = this.colDefs.get(columnVO.getField());
                Path<?> path = valueColDef.getField().getPath(root);
                var aggregateFunction = this.aggFuncs.get(columnVO.getAggFunc());
                Expression<?> aggregatedField = aggregateFunction.apply(cb, path);
                selections.add(
                        SelectionMetadata
                                .builder()
                                .alias(columnVO.getField())
                                .expression(aggregatedField)
                                .isAggregationSelection(true)
                                .build()
                );
            }
        } else {
            // groups are already expanded
            // just select columns
            for (ColDef<E, ?> colDef : this.colDefs.values()) {
                Path<?> field = colDef.getField().getPath(root);
                selections.add(
                        SelectionMetadata.builder()
                                .alias(colDef.getFieldName())
                                .expression(field)
                                .build()
                );
            }
        }
        
        return selections;
    }

    /**
     * Creates selections for the query when grid is in basic mode.
     *
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     * @return created selections
     */
    @NonNull
    protected List<SelectionMetadata> selectBasic(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        Root<E> root = queryContext.getRoot();
        // just select col defs
        return this.colDefs.values()
                .stream()
                .map(colDef -> {
                    Path<?> field = colDef.getField().getPath(root);
                    return SelectionMetadata.builder().alias(colDef.getFieldName()).expression(field).build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Creates the filtering criteria (WHERE clause) for the query when grid is in tree-data mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return created where predicates
     */
    @NonNull
    protected List<WherePredicateMetadata> whereTreeData(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        // unwrap from context
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        List<WherePredicateMetadata> wherePredicateMetadata = new ArrayList<>();
        
        // create predicate for current tree level
        Predicate treePredicate;
        if (request.getGroupKeys().isEmpty()) {
            // only parent records
            Predicate treeRootPredicate;
            if (this.treeDataParentReferenceField != null) {
                treeRootPredicate = cb.isNull(root.get(this.treeDataParentReferenceField));
            } else {
                treeRootPredicate = cb.isNull(root.get(this.treeDataParentIdField));
            }

            treePredicate = treeRootPredicate;
        } else {
            // filter by parent record
            String parentKey = request.getGroupKeys().get(request.getGroupKeys().size() - 1);

            Predicate treeParentPredicate;
            if (this.treeDataParentReferenceField != null) {
                Path<?> parentIdPath = root.get(this.treeDataParentReferenceField).get(this.primaryFieldName);
                // try to synchronize col and key to same data type to prevent errors
                // for example, group key is date as string, but field is date, need to parse to date and then compare
                TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(parentIdPath, parentKey);
                treeParentPredicate = cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());
            } else {
                // try to synchronize col and key to same data type to prevent errors
                // for example, group key is date as string, but field is date, need to parse to date and then compare
                TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(root.get(this.treeDataParentIdField), parentKey);
                treeParentPredicate =  cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());
            }
            treePredicate = treeParentPredicate;
        }
        wherePredicateMetadata.add(
                WherePredicateMetadata
                        .builder()
                        .predicate(treePredicate)
                        .isTreeDataPredicate(true)
                        .build()
        );
        
        
        // check if any filters are present
        boolean areAnyFiltersPresent = (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) || this.isExternalFilterPresent || this.isQuickFilterPresent;
        if (areAnyFiltersPresent) {
            // A group will be included if:
            // 1. it has a parent that passes the filter, or
            // 2. its own data passes the filter, or
            // 3. it has any children that pass the filter

            // 1. first check if parent passes the filter
            Predicate parentPredicate = this.createTreeDataParentMatchPredicate(queryContext, request);
            // 2. check if its own data passes the filter
            Predicate ownDataPredicate = this.createTreeDataOwnDataPredicate(queryContext, request);
            // 3. check if any children pass the filter
            Predicate childrenPredicate = this.createTreeDataChildrenMatchPredicate(queryContext, request);

            Predicate treeDataFilteringPredicate = cb.or(parentPredicate, ownDataPredicate, childrenPredicate);
            wherePredicateMetadata.add(
                    WherePredicateMetadata
                            .builder()
                            .predicate(treeDataFilteringPredicate)
                            .isFilterPredicate(true)
                            .build()
            );
        }
        
        return wherePredicateMetadata;
    }

    /**
     * Creates the filtering criteria (WHERE clause) for the query when grid is in master-detail mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return created where predicates
     */
    @NonNull
    protected List<WherePredicateMetadata> whereMasterDetail(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        if (request.getGroupKeys().isEmpty()) {
            return this.whereBasic(queryContext, request);
        } else {
            return this.whereGrouping(queryContext, request);
        }
    }

    /**
     * Creates the filtering criteria (WHERE clause) for the query when grid is in pivoting mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return created where predicates
     */
    @NonNull
    protected List<WherePredicateMetadata> wherePivoting(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        List<WherePredicateMetadata> wherePredicates = new ArrayList<>();

        // expanded groups
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size(); i++) {
            String groupKey = request.getGroupKeys().get(i);
            String groupCol = request.getRowGroupCols().get(i).getField();
            ColDef<E, ?> groupColDef = this.colDefs.get(groupCol);

            // try to synchronize col and key to same data type to prevent errors
            // for example, group key is date as string, but field is date, need to parse to date and then compare
            TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(groupColDef.getField().getPath(root), groupKey);
            Predicate groupPredicate = cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());

            // wrap in predicate info object
            WherePredicateMetadata groupPredicateInfo = WherePredicateMetadata
                    .builder()
                    .predicate(groupPredicate)
                    .isGroupPredicate(true)
                    .groupKey(synchronizedValueType.getSynchronizedValue())
                    .groupCol(groupCol)
                    .build();
            wherePredicates.add(groupPredicateInfo);
        }
        
        // pivot values are all aggregates, so nothing else to do in where clause
        // the rest of filtering should be done in having clause
        
        return wherePredicates;
    }

    /**
     * Creates the filtering criteria (WHERE clause) for the query when grid is in grouping mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return created where predicates
     */
    @NonNull
    protected List<WherePredicateMetadata> whereGrouping(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        List<WherePredicateMetadata> wherePredicates = new ArrayList<>();
        
        // where expanded groups predicates
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size(); i++) {
            String groupKey = request.getGroupKeys().get(i);
            String groupCol = request.getRowGroupCols().get(i).getField();
            ColDef<E, ?> groupColDef = this.colDefs.get(groupCol);

            // try to synchronize col and key to same data type to prevent errors
            // for example, group key is date as string, but field is date, need to parse to date and then compare
            TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(groupColDef.getField().getPath(root), groupKey);
            Predicate groupPredicate = cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());

            // wrap in predicate info object
            WherePredicateMetadata groupPredicateInfo = WherePredicateMetadata
                    .builder()
                    .predicate(groupPredicate)
                    .isGroupPredicate(true)
                    .groupKey(synchronizedValueType.getSynchronizedValue())
                    .groupCol(groupCol)
                    .build();
            wherePredicates.add(groupPredicateInfo);
        }
        
        boolean hasAnyFilteringOnAggregatedColumns = !this.enableAdvancedFilter &&
                request.getFilterModel().keySet().stream().anyMatch(k -> request.getValueCols().stream().anyMatch(vc -> vc.getField().equals(k)));
        boolean hasAnyFilteringOnNonAggregatedColumns = this.enableAdvancedFilter ||
                request.getFilterModel().keySet().stream().noneMatch(k -> request.getValueCols().stream().anyMatch(vc -> vc.getField().equals(k)));
        boolean hasUnexpandedGroups = request.getRowGroupCols().size() > request.getGroupKeys().size();
        
        
        if (this.groupAggFiltering) {
            if (hasUnexpandedGroups && hasAnyFilteringOnAggregatedColumns) {
                
                if (this.groupAggFilteringExpandedParentsMatch(queryContext, request)) {
                    // if parent groups match, all child nodes pass, no additional filtering needed
                    return wherePredicates;
                }

                // match leaf nodes
                Predicate leafNodesCheck = this.groupAggFilteringCreateLeafNodesPredicate(queryContext, request);
                // match aggregates on child groups
                Predicate unexpandedChildrenGroupsCheck = this.groupAggFilteringCreateUnexpandedChildGroupsPredicate(queryContext, request);

                wherePredicates.add(
                        WherePredicateMetadata
                                .builder()
                                .predicate(cb.or(leafNodesCheck, unexpandedChildrenGroupsCheck))
                                .build()
                );
            }
            if (!hasUnexpandedGroups && hasAnyFilteringOnNonAggregatedColumns) {
                wherePredicates.addAll(this.whereBasic(queryContext, request));
            }
            
            return wherePredicates;
        }
        
        
        if (hasUnexpandedGroups && this.suppressAggFilteredOnly) {
            // ignore applied filters until we reach the leaf nodes
            return wherePredicates;
        } else {
            // apply filtering to leaf nodes
            wherePredicates.addAll(this.whereBasic(queryContext, request));
        }

        return wherePredicates;
    }


    /**
     * Creates the filtering criteria (WHERE clause) for the query when grid is in basic mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return created where predicates
     */
    @NonNull
    protected List<WherePredicateMetadata> whereBasic(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        List<WherePredicateMetadata> wherePredicates = new ArrayList<>(3);

        // external filter
        if (this.isExternalFilterPresent) {
            Predicate externalFilterPredicate = this.doesExternalFilterPass.apply(cb, root, request.getExternalFilter());
            if (externalFilterPredicate != null) {
                wherePredicates.add(
                        WherePredicateMetadata.builder()
                                .predicate(externalFilterPredicate)
                                .isExternalFilterPredicate(true)
                                .build()
                );
            }
        }
        // quick filter
        if (this.isQuickFilterPresent) {
            Predicate quickFilterPredicate = this.createQuickFilterPredicate(cb, root, request.getQuickFilter());
            if (quickFilterPredicate != null) {
                wherePredicates.add(
                        WherePredicateMetadata.builder()
                                .predicate(quickFilterPredicate)
                                .isQuickFilterPredicate(true)
                                .build()
                );
            }
        }
        // filter where
        if (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) {
            if (this.enableAdvancedFilter) {
                Predicate advancedFilterPredicate = this.createAdvancedFilterPredicate(cb, root, request.getFilterModel());
                wherePredicates.add(
                        WherePredicateMetadata.builder()
                                .predicate(advancedFilterPredicate)
                                .isAdvancedFilterPredicate(true)
                                .build()
                );
            } else {
                Predicate columnFilterPredicate = this.createColumnFilterPredicate(cb, root, request.getFilterModel());
                wherePredicates.add(
                        WherePredicateMetadata.builder()
                                .predicate(columnFilterPredicate)
                                .isColumnFilterPredicate(true)
                                .build()
                );
            }
        }

        return wherePredicates;
    }

    /**
     * Checks if any expanded parent group satisfies the current aggregation filters.
     * This is used to determine if all children of a parent group should be 
     * automatically included in the results.
     *
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     * @return true if any expanded parent group matches the aggregation filters
     */
    protected boolean groupAggFilteringExpandedParentsMatch(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        if (request.getGroupKeys().isEmpty()) {
            // no parents
            return false;
        }

        CriteriaQuery<Boolean> mainQuery = cb.createQuery(Boolean.class);

        Expression<Boolean> parentMatchExpression = cb.literal(false);
        for (int i = 0; i < request.getGroupKeys().size(); i++) {
            String key = request.getGroupKeys().get(i);

            Subquery<Integer> expandedParentSubquery = mainQuery.subquery(Integer.class);
            Root<E> expandedParentRoot = expandedParentSubquery.from(this.entityClass);
            expandedParentSubquery.select(cb.literal(1));

            expandedParentSubquery.groupBy(
                    request.getRowGroupCols().stream()
                            .map(col -> this.colDefs.get(col.getField()))
                            .map(colDef -> colDef.getField().getPath(expandedParentRoot))
                            .limit(i + 1L)
                            .collect(Collectors.toList())
            );

            expandedParentSubquery.where(
                    request.getRowGroupCols().stream()
                            .map(col -> {
                                ColDef<E, ?> colDef = this.colDefs.get(col.getField());
                                Path<?> expandedParentGroupPath = colDef.getField().getPath(expandedParentRoot);

                                // try to synchronize col and key to same data type to prevent errors
                                // for example, group key is date as string, but field is date, need to parse to date and then compare
                                TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(expandedParentGroupPath, key);
                                return cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());
                            })
                            .limit(i + 1L)
                            .collect(Collectors.toList())
                            .toArray(Predicate[]::new)
            );

            expandedParentSubquery.having(
                    request.getValueCols().stream()
                            .filter(vc -> request.getFilterModel().containsKey(vc.getField()))
                            .map(vc -> {
                                // create aggregation expression
                                ColDef<E, ?> colDef = this.colDefs.get(vc.getField());
                                var aggFunc = this.aggFuncs.get(vc.getAggFunc());
                                Expression<?> aggExpr = aggFunc.apply(cb, colDef.getField().getPath(expandedParentRoot));

                                // having predicate
                                @SuppressWarnings("unchecked") 
                                Map<String, Object> filterModel = (Map<String, Object>) request.getFilterModel().get(vc.getField());
                                IFilter<?, ?, ?> filter = colDef.getFilter();
                                return filter.toPredicate(cb, (Expression) aggExpr, filterModel);
                            })
                            .toArray(Predicate[]::new)
            );

            parentMatchExpression = cb.or(parentMatchExpression, cb.exists(expandedParentSubquery));
        }

        mainQuery.select(parentMatchExpression);

        return this.entityManager.createQuery(mainQuery).getSingleResult();
    }


    /**
     * Creates a predicate to identify if the current group's parents meet the 
     * aggregation filter criteria within the main query.
     *
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     * @return a predicate representing the expanded parent group filter match
     */
    @NonNull
    protected Predicate groupAggFilteringCreateExpandedParentsPredicate(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        AbstractQuery<?> query = queryContext.getQuery();
        Root<E> root = queryContext.getRoot();
        
        if (request.getGroupKeys().isEmpty()) {
            return cb.disjunction();
        }
        
        List<Predicate> expandedParentGroupsPredicates = new ArrayList<>(request.getGroupKeys().size());
        for (int i = 0; i < request.getGroupKeys().size(); i++) {
            
            Subquery<Integer> expandedParentSubquery = query.subquery(Integer.class);
            Root<E> expandedParentRoot = expandedParentSubquery.from(this.entityClass);
            expandedParentSubquery.select(cb.literal(1));

            expandedParentSubquery.groupBy(
                    request.getRowGroupCols().stream()
                            .map(col -> this.colDefs.get(col.getField()))
                            .map(colDef -> colDef.getField().getPath(expandedParentRoot))
                            .limit(i + 1L)
                            .collect(Collectors.toList())
            );

            expandedParentSubquery.where(
                    request.getRowGroupCols().stream()
                            .map(col -> {
                                ColDef<E, ?> colDef = this.colDefs.get(col.getField());
                                Path<?> subqueryGroupColumnPath = colDef.getField().getPath(expandedParentRoot);
                                Path<?> parentQueryGroupColumnPath = colDef.getField().getPath(root);
                                return cb.equal(subqueryGroupColumnPath, parentQueryGroupColumnPath);
                            })
                            .limit(i + 1L)
                            .collect(Collectors.toList())
                            .toArray(Predicate[]::new)
            );

            expandedParentSubquery.having(
                    request.getValueCols().stream()
                            .filter(vc -> request.getFilterModel().containsKey(vc.getField()))
                            .map(vc -> {
                                ColDef<E, ?> colDef = this.colDefs.get(vc.getField());
                                // create aggregation expression
                                Expression<?> aggExpr = this.aggFuncs.get(vc.getAggFunc()).apply(cb, colDef.getField().getPath(expandedParentRoot));
        
                                // having predicate
                                IFilter<?, ?, ?> filter = colDef.getFilter();
                                return filter.toPredicate(cb, (Expression) aggExpr, (Map<String, Object>) request.getFilterModel().get(vc.getField()));
                            })
                            .toArray(Predicate[]::new)
            );

            expandedParentGroupsPredicates.add(cb.exists(expandedParentSubquery));
        }
        
        return cb.or(expandedParentGroupsPredicates.toArray(new Predicate[0]));
    }


    /**
     * Creates a predicate to check if any unexpanded child groups (nested groups 
     * deeper than the current level) satisfy the aggregation filters.
     *
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     * @return a predicate that evaluates to true if any hidden child group matches the filters
     */
    @NonNull
    protected Predicate groupAggFilteringCreateUnexpandedChildGroupsPredicate(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        AbstractQuery<?> query = queryContext.getQuery();
        Root<E> root = queryContext.getRoot();

        List<Predicate> unexpandedChildGroupsPredicates = new ArrayList<>(request.getGroupKeys().size());
        for (int i = request.getGroupKeys().size(); i < request.getRowGroupCols().size(); i++) {

            Subquery<Integer> unexpandedChildGroupSubquery = query.subquery(Integer.class);
            Root<E> unexpandedChildGroupRoot = unexpandedChildGroupSubquery.from(this.entityClass);
            unexpandedChildGroupSubquery.select(cb.literal(1));

            unexpandedChildGroupSubquery.groupBy(
                    request.getRowGroupCols().stream()
                            .map(col -> this.colDefs.get(col.getField()))
                            .map(colDef -> colDef.getField().getPath(unexpandedChildGroupRoot))
                            .skip(request.getGroupKeys().size())
                            .limit(i - request.getGroupKeys().size() + 1L)
                            .collect(Collectors.toList())
            );

            unexpandedChildGroupSubquery.where(
                    request.getRowGroupCols().stream()
                            .map(col -> {
                                ColDef<E, ?> colDef = this.colDefs.get(col.getField());
                                Path<?> subqueryGroupColumnPath = colDef.getField().getPath(unexpandedChildGroupRoot);
                                Path<?> parentQueryGroupColumnPath = colDef.getField().getPath(root);
                                return cb.equal(subqueryGroupColumnPath, parentQueryGroupColumnPath);
                            })
                            .skip(request.getGroupKeys().size())
                            .limit(i - request.getGroupKeys().size() + 1L)
                            .collect(Collectors.toList())
                            .toArray(Predicate[]::new)
            );

            unexpandedChildGroupSubquery.having(
                    request.getValueCols().stream()
                            .filter(vc -> request.getFilterModel().containsKey(vc.getField()))
                            .map(vc -> {
                                ColDef<E, ?> colDef = this.colDefs.get(vc.getField());
                                // create aggregation expression
                                Expression<?> aggExpr = this.aggFuncs.get(vc.getAggFunc()).apply(cb, colDef.getField().getPath(unexpandedChildGroupRoot));
                                
                                // having predicate
                                IFilter<?, ?, ?> filter = colDef.getFilter();
                                return filter.toPredicate(cb, (Expression) aggExpr, (Map<String, Object>) request.getFilterModel().get(vc.getField()));
                            })
                            .toArray(Predicate[]::new)
            );

            unexpandedChildGroupsPredicates.add(cb.exists(unexpandedChildGroupSubquery));
        }

        return cb.or(unexpandedChildGroupsPredicates.toArray(new Predicate[0]));
    }

    /**
     * Creates a predicate to verify if any individual leaf nodes (raw data rows) 
     * belonging to the current group level satisfy the provided filters.
     *
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     * @return a predicate that checks for the existence of matching leaf nodes
     */
    @NonNull
    protected Predicate groupAggFilteringCreateLeafNodesPredicate(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        AbstractQuery<?> query = queryContext.getQuery();
        Root<E> root = queryContext.getRoot();
        
        Subquery<Integer> leafNodeExistsSubquery = query.subquery(Integer.class);
        Root<E> leafNodeRoot = leafNodeExistsSubquery.from(this.entityClass);
        leafNodeExistsSubquery.select(cb.literal(1));

        List<Predicate> leafNodeExistsSubqueryPredicates = new ArrayList<>();
        request.getRowGroupCols().stream()
                .map(col -> {
                    ColDef<E, ?> colDef = this.colDefs.get(col.getField());
                    Path<?> subqueryGroupColumnPath = colDef.getField().getPath(leafNodeRoot);
                    Path<?> parentQueryGroupColumnPath = colDef.getField().getPath(root);
                    return cb.equal(subqueryGroupColumnPath, parentQueryGroupColumnPath);
                })
                .limit(request.getGroupKeys().size() + 1L)
                .forEach(leafNodeExistsSubqueryPredicates::add);
        request.getValueCols().stream()
                .filter(vc -> request.getFilterModel().containsKey(vc.getField()))
                .forEach(vc -> {
                    ColDef<E, ?> colDef = this.colDefs.get(vc.getField());
                    Predicate predicate = colDef.getFilter().toPredicate(cb, (Expression) colDef.getField().getPath(leafNodeRoot), (Map<String, Object>) request.getFilterModel().get(vc.getField()));
                    leafNodeExistsSubqueryPredicates.add(predicate);
                });
        leafNodeExistsSubquery.where(leafNodeExistsSubqueryPredicates.toArray(Predicate[]::new));
        
        return cb.exists(leafNodeExistsSubquery);
    }

    /**
     * Creates predicates for the HAVING clause when grid is in grouping mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return having predicates
     */
    @NonNull
    protected List<HavingMetadata> havingGrouping(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        // no need to have 'having' clause in grouping for now
        // filtering on aggregated values is done within where clause by subqueries (need to check all levels of tree)
        return List.of();
    }

    /**
     * Creates predicates for the HAVING clause when grid is in pivoting mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return having predicates
     */
    @NonNull
    protected List<HavingMetadata> havingPivoting(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        // todo: check how pivoting having clause should work
        return List.of();
    }


    /**
     * Creates an expression that counts how many children match the node has (respecting filters).
     * Used to show the total number of items hidden inside a non-leaf node.
     * When node is leaf node, the expression returns null.
     *
     * @param hasChildrenPredicate A condition checking if the current row is a leaf node or not.
     * @param request request.
     * @param queryContext query context.
     * @return expression returning the child count for non-leaf nodes, or null otherwise.
     */
    @NonNull
    protected Expression<Long> createTreeDataGetChildCountExpression(
            @NonNull QueryContext<E> queryContext,
            @NonNull Expression<Boolean> hasChildrenPredicate,
            @NonNull ServerSideGetRowsRequest request) {
        
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        AbstractQuery<?> query = queryContext.getQuery();
        Root<E> root = queryContext.getRoot();
        
        Subquery<Long> countChildrenSubquery = query.subquery(Long.class);
        Root<E> countChildrenRoot = countChildrenSubquery.from(this.entityClass);

        countChildrenSubquery.select(cb.count(countChildrenRoot));

        List<Predicate> predicates = new ArrayList<>();
        Predicate childrenPathPredicate = cb.like(
                countChildrenRoot.get(this.treeDataDataPathFieldName),
                cb.concat(root.get(this.treeDataDataPathFieldName), this.treeDataDataPathSeparator + "%")
        );
        predicates.add(childrenPathPredicate);

        if (!this.suppressAggFilteredOnly) {
            // external filter
            if (this.isExternalFilterPresent) {
                Predicate externalFilterPredicate = this.doesExternalFilterPass.apply(cb, countChildrenRoot, request.getExternalFilter());
                if (externalFilterPredicate != null) {
                    predicates.add(externalFilterPredicate);
                }
            }
            // quick filter
            if (this.isQuickFilterPresent) {
                Predicate quickFilterPredicate = this.createQuickFilterPredicate(cb, countChildrenRoot, request.getQuickFilter());
                if (quickFilterPredicate != null) {
                    predicates.add(quickFilterPredicate);
                }
            }
            // filter where
            if (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) {
                Predicate filterPredicate;
                if (this.enableAdvancedFilter) {
                    filterPredicate = this.createAdvancedFilterPredicate(cb, countChildrenRoot, request.getFilterModel());
                } else {
                    filterPredicate = this.createColumnFilterPredicate(cb, countChildrenRoot, request.getFilterModel());
                }
                predicates.add(filterPredicate);
            }
        }
        countChildrenSubquery.where(cb.and(predicates.toArray(Predicate[]::new)));

        return cb.<Long>selectCase()
                .when(hasChildrenPredicate, countChildrenSubquery)    // count when non-leaf node
                .otherwise(0L);                                 // no children when leaf node
    }

    /**
     * Creates boolean expression that tells you if row is group (has children, is non-leaf)
     * 
     * @param queryContext  query context
     * @return  expression
     */
    @NonNull
    protected Expression<Boolean> createTreeDataIsServerSideGroupExpression(@NonNull QueryContext<E> queryContext) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        // selection to find out whether it has any children
        Expression<Boolean> isServerSideGroupSelection;
        if (this.treeDataChildrenField != null) {
            isServerSideGroupSelection = cb.isNotEmpty(root.get(this.treeDataChildrenField));
        } else {
            // Subquery: Select count from Entity where parent = root
            Subquery<Long> subquery = cb.createTupleQuery().subquery(Long.class);
            Root<E> subRoot = subquery.from(this.entityClass);
            subquery.select(cb.count(subRoot));
            if (this.treeDataParentReferenceField != null) {
                // compare parent reference directly with root
                subquery.where(cb.equal(subRoot.get(this.treeDataParentReferenceField), root));
            } else {
                // no reference field, just parent id
                subquery.where(
                        cb.equal(
                                subRoot.get(this.treeDataParentIdField),
                                root.get(this.primaryFieldName))
                );
            }

            isServerSideGroupSelection = cb.exists(subquery);
        }
        
        return isServerSideGroupSelection;
    }

    /**
     * Creates an expression that aggregates values from all matching children.
     * When node is leaf node, the expression returns the row's own value for that column.
     *
     * @param aggColumn for column
     * @param hasChildrenPredicate A condition checking if the current row is a leaf node or not.
     * @param request request.
     * @param queryContext query context.
     * @return expression returning the aggregated value for non-leaf nodes, or the own value otherwise.
     */
    @NonNull
    protected Expression<?> createTreeDataAggregationExpression(
            @NonNull QueryContext<E> queryContext,
            @NonNull Expression<Boolean> hasChildrenPredicate,
            @NonNull ColumnVO aggColumn,
            @NonNull ServerSideGetRowsRequest request
    ) {
        ColDef<E, ?> aggColumnColDef = this.colDefs.get(aggColumn.getField());
        
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        AbstractQuery<?> query = queryContext.getQuery();
        
        Subquery<?> treeAggregationSubquery = query.subquery(Object.class);
        Root<E> treeAggregationRoot = treeAggregationSubquery.from(this.entityClass);

        var aggregationFunction = this.aggFuncs.get(aggColumn.getAggFunc());
        Expression<?> aggregationSelection = aggregationFunction.apply(cb, aggColumnColDef.getField().getPath(treeAggregationRoot));
        treeAggregationSubquery.select((Expression) aggregationSelection);

        List<Predicate> predicates = new ArrayList<>();
        Predicate childrenPathPredicate = cb.like(
                treeAggregationRoot.get(this.treeDataDataPathFieldName),
                cb.concat(root.get(this.treeDataDataPathFieldName), this.treeDataDataPathSeparator + "%")
        );
        predicates.add(childrenPathPredicate);

        if (!this.suppressAggFilteredOnly) {
            // external filter
            if (this.isExternalFilterPresent) {
                Predicate externalFilterPredicate = this.doesExternalFilterPass.apply(cb, treeAggregationRoot, request.getExternalFilter());
                if (externalFilterPredicate != null) {
                    predicates.add(externalFilterPredicate);
                }
            }
            // quick filter
            if (this.isQuickFilterPresent) {
                Predicate quickFilterPredicate = this.createQuickFilterPredicate(cb, treeAggregationRoot, request.getQuickFilter());
                if (quickFilterPredicate != null) {
                    predicates.add(quickFilterPredicate);
                }
            }
            // filter where
            if (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) {
                Predicate filterPredicate;
                if (this.enableAdvancedFilter) {
                    filterPredicate = this.createAdvancedFilterPredicate(cb, treeAggregationRoot, request.getFilterModel());
                } else {
                    filterPredicate = this.createColumnFilterPredicate(cb, treeAggregationRoot, request.getFilterModel());
                }
                predicates.add(filterPredicate);
            }
        }
        treeAggregationSubquery.where(cb.and(predicates.toArray(Predicate[]::new)));
        
        return cb.selectCase()
                .when(hasChildrenPredicate, treeAggregationSubquery)        // aggregation when non-leaf node
                .otherwise(aggColumnColDef.getField().getPath(root));            // no aggregation on leaf nodes
    }

    /**
     * Creates predicate that checks if any parent in the current path matches the filter criteria.
     *
     * @param request The grid request containing the active filters and group keys.
     * @param queryContext Helper for tracking query state and parameters.
     * @return A condition (Predicate) that is true if at least one parent matches the filters.
     */
    @NonNull
    protected Predicate createTreeDataParentMatchPredicate(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        AbstractQuery<?> query = queryContext.getQuery();
        
        if (request.getGroupKeys().isEmpty()) {
            // no parents, no parents match
            return cb.disjunction();
        }
        
        Subquery<Integer> parentMatchSubquery = query.subquery(Integer.class);
        Root<E> parentRoot = parentMatchSubquery.from(this.entityClass);
        
        // generate parent id predicate: id = 1 or id = 2....
        Predicate parentPrimaryKeyPredicate = cb.or(
                request.getGroupKeys().stream()
                        .map(k -> {
                            TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(parentRoot.get(this.primaryFieldName), k);
                            return cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());
                        })
                        .toArray(Predicate[]::new)
        );
        
        // generate filter predicates
        List<Predicate> predicates = new ArrayList<>(3);
        // external filter
        if (this.isExternalFilterPresent) {
            Predicate externalFilterPredicate = this.doesExternalFilterPass.apply(cb, parentRoot, request.getExternalFilter());
            if (externalFilterPredicate != null) {
                predicates.add(externalFilterPredicate);
            }
        }
        // quick filter
        if (this.isQuickFilterPresent) {
            Predicate quickFilterPredicate = this.createQuickFilterPredicate(cb, parentRoot, request.getQuickFilter());
            if (quickFilterPredicate != null) {
                predicates.add(quickFilterPredicate);
            }
        }
        // filter where
        if (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) {
            Predicate filterPredicate;
            if (this.enableAdvancedFilter) {
                filterPredicate = this.createAdvancedFilterPredicate(cb, parentRoot, request.getFilterModel());
            } else {
                filterPredicate = this.createColumnFilterPredicate(cb, parentRoot, request.getFilterModel());
            }
            predicates.add(filterPredicate);
        }
        
        parentMatchSubquery
                .select(cb.literal(1))
                .where(
                    parentPrimaryKeyPredicate, 
                    cb.and(predicates.toArray(Predicate[]::new))
                );
        
        return cb.exists(parentMatchSubquery);
    }


    /**
     * Creates a predicate that evaluates whether the current row itself (the node) matches 
     * all active filter criteria.
     *
     * @param queryContext Helper for tracking query state
     * @param request The grid request.
     * @return A Predicate representing the combined filter criteria for the current node.
     */
    @NonNull
    protected Predicate createTreeDataOwnDataPredicate(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        // generate filter predicates
        List<Predicate> predicates = new ArrayList<>(3);
        // external filter
        if (this.isExternalFilterPresent) {
            Predicate externalFilterPredicate = this.doesExternalFilterPass.apply(cb, root, request.getExternalFilter());
            if (externalFilterPredicate != null) {
                predicates.add(externalFilterPredicate);
            }
        }
        // quick filter
        if (this.isQuickFilterPresent) {
            Predicate quickFilterPredicate = this.createQuickFilterPredicate(cb, root, request.getQuickFilter());
            if (quickFilterPredicate != null) {
                predicates.add(quickFilterPredicate);
            }
        }
        // filter where
        if (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) {
            Predicate filterPredicate;
            if (this.enableAdvancedFilter) {
                // advanced filter
                filterPredicate = this.createAdvancedFilterPredicate(cb, root, request.getFilterModel());
            } else {
                // column filter
                filterPredicate = this.createColumnFilterPredicate(cb, root, request.getFilterModel());
            }
            predicates.add(filterPredicate);
        }
        
        return cb.and(predicates.toArray(Predicate[]::new));
    }


    /**
     * Creates predicate that checks if any child further down the tree matches the filter criteria.
     *
     * @param request The grid request containing the active filters.
     * @param queryContext Helper for tracking query state and parameters.
     * @return A condition (Predicate) that is true if at least one descendant matches the filters.
     */
    @NonNull
    protected Predicate createTreeDataChildrenMatchPredicate(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        AbstractQuery<?> query = queryContext.getQuery();
        
        Subquery<Integer> childrenMatchSubquery = query.subquery(Integer.class);
        Root<E> childrenRoot = childrenMatchSubquery.from(this.entityClass);

        // generate children predicate: path starts with parent path
        Predicate childrenPathPredicate = cb.like(
                childrenRoot.get(this.treeDataDataPathFieldName),
                cb.concat(root.get(this.treeDataDataPathFieldName), this.treeDataDataPathSeparator + "%")
        );
        
        // generate filter predicates
        List<Predicate> predicates = new ArrayList<>(3);
        // external filter
        if (this.isExternalFilterPresent) {
            Predicate externalFilterPredicate = this.doesExternalFilterPass.apply(cb, childrenRoot, request.getExternalFilter());
            if (externalFilterPredicate != null) {
                predicates.add(externalFilterPredicate);
            }
        }
        // quick filter
        if (this.isQuickFilterPresent) {
            Predicate quickFilterPredicate = this.createQuickFilterPredicate(cb, childrenRoot, request.getQuickFilter());
            if (quickFilterPredicate != null) {
                predicates.add(quickFilterPredicate);
            }
        }
        // filter where
        if (request.getFilterModel() != null && !request.getFilterModel().isEmpty()) {
            Predicate filterPredicate;
            if (this.enableAdvancedFilter) {
                // advanced filter
                filterPredicate = this.createAdvancedFilterPredicate(cb, childrenRoot, request.getFilterModel());
            } else {
                // column filter
                filterPredicate = this.createColumnFilterPredicate(cb, childrenRoot, request.getFilterModel());
            }
            predicates.add(filterPredicate);
        }

        childrenMatchSubquery
                .select(cb.literal(1))
                .where(
                        childrenPathPredicate,
                        cb.and(predicates.toArray(Predicate[]::new))
                );

        return cb.exists(childrenMatchSubquery);
    }

    /**
     * Creates the grouping rules for the query when grid is in tree-data mode (empty).
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return empty list (tree data does not have any grouping)
     */
    @NonNull
    protected List<GroupingMetadata> groupByTreeData(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        // tree data does not have any grouping
        return List.of();
    }


    /**
     * Creates the grouping rules for the query when grid is in master-detail mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return list of grouping expressions
     */
    @NonNull
    protected List<GroupingMetadata> groupByMasterDetail(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        if (request.getRowGroupCols().isEmpty()) {
            // no grouping
            return List.of();
        } else {
            return this.groupByGrouping(queryContext, request);
        }
    }

    /**
     * Creates the grouping rules for the query when grid is in pivot mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return list of grouping expressions
     */
    @NonNull
    protected List<GroupingMetadata> groupByPivoting(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        Root<E> root = queryContext.getRoot();
        
        List<GroupingMetadata> groupings = new ArrayList<>(request.getGroupKeys().size() + 1);
        // there is always grouping in pivoting
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
            String groupCol = request.getRowGroupCols().get(i).getField();
            ColDef<E, ?> groupColDef = this.colDefs.get(groupCol);
            GroupingMetadata groupingMetadata = GroupingMetadata
                    .builder()
                    .gropingExpression(groupColDef.getField().getPath(root))
                    .column(groupCol)
                    .build();

            groupings.add(groupingMetadata);
        }
        
        return groupings;
    }

    /**
     * Creates the grouping rules for the query when grid is in grouping mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return list of grouping expressions
     */
    @NonNull
    protected List<GroupingMetadata> groupByGrouping(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        Root<E> root = queryContext.getRoot();
        
        List<GroupingMetadata> groupings = new ArrayList<>(request.getGroupKeys().size() + 1);
        // master-detail can be used together with groups
        boolean hasUnexpandedGroups = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (hasUnexpandedGroups) {
            for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
                String groupCol = request.getRowGroupCols().get(i).getField();
                ColDef<E, ?> groupColDef = this.colDefs.get(groupCol);
                GroupingMetadata groupingMetadata = GroupingMetadata
                        .builder()
                        .gropingExpression(groupColDef.getField().getPath(root))
                        .column(groupCol)
                        .build();

                groupings.add(groupingMetadata);
            }
        }

        return groupings;
    }
    

    /**
     * Creates the sort order for the query results when grid is in basic mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return list of orders
     */
    @NonNull
    protected List<OrderMetadata> orderByBasic(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        return request.getSortModel().stream()
                .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))
                .map(model -> {
                    ColDef<E, ?> colDef = this.colDefs.get(model.getColId());
                    Expression<?> field = colDef.getField().getPath(root);
                    if ("absolute".equals(model.getType())) {
                        field = cb.abs((Expression) field);
                    }
                    Order order = model.getSort() == SortDirection.asc ? cb.asc(field) : cb.desc(field);
                    return OrderMetadata.builder()
                            .order(order)
                            .colId(model.getColId())
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Creates the sort order for the query results when grid is in tree-data mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return list of orders
     */
    @NonNull
    protected List<OrderMetadata> orderByTreeData(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        return this.orderByBasic(queryContext, request);
    }

    /**
     * Creates the sort order for the query results when grid is in master-detail mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return list of orders
     */
    @NonNull
    protected List<OrderMetadata> orderByMasterDetail(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        return this.orderByBasic(queryContext, request);
    }

    /**
     * Creates the sort order for the query results when grid is in grouping mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return list of orders
     */
    @NonNull
    protected List<OrderMetadata> orderByGrouping(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();

        boolean hasUnexpandedGroups = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (hasUnexpandedGroups) {
            // order groups
            Stream<OrderMetadata> groupOrders = request.getSortModel().stream()
                    .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))    // ignore auto-generated column
                    .filter(model -> request.getRowGroupCols().stream().anyMatch(rg -> rg.getField().equals(model.getColId()))) // present in group columns
                    .map(sortModel -> {
                        ColDef<E, ?> colDef = this.colDefs.get(sortModel.getColId());
                        Expression<?> groupingColumnExpression = colDef.getField().getPath(root);
                        if ("absolute".equals(sortModel.getType())) {
                            groupingColumnExpression = cb.abs((Expression) groupingColumnExpression);
                        }
                        Order order = sortModel.getSort() == SortDirection.asc ? cb.asc(groupingColumnExpression) : cb.desc(groupingColumnExpression);
                        return OrderMetadata.builder()
                                .order(order)
                                .colId(sortModel.getColId())
                                .build();
                    })
                    .limit(request.getGroupKeys().size() + 1L);   // don't order groups that are not expanded yet
            
            // order aggregated columns
            Stream<OrderMetadata> aggregationOrders = request.getSortModel().stream()
                    .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))    // ignore auto-generated column
                    .filter(model -> request.getValueCols().stream().anyMatch(vc -> vc.getField().equals(model.getColId())))  // also present in value cols
                    .map(sortModelAgg -> {
                        // corresponding aggregation expression must already be in context
                        Expression<?> aggregationExpression = queryContext.getSelections().stream()
                                .filter(s -> s.getAlias().equals(sortModelAgg.getColId()))
                                .map(SelectionMetadata::getExpression)
                                .findFirst()
                                .orElseThrow();
                        if ("absolute".equals(sortModelAgg.getType())) {
                            aggregationExpression = cb.abs((Expression) aggregationExpression);
                        }
                        
                        Order order = sortModelAgg.getSort() == SortDirection.asc ? cb.asc(aggregationExpression) : cb.desc(aggregationExpression);
                        return OrderMetadata.builder()
                                .order(order)
                                .colId(sortModelAgg.getColId())
                                .build();
                    });
            
            return Stream.concat(groupOrders, aggregationOrders).collect(Collectors.toList());
        } else {
            // all groups expanded, we are on the leaf level, basic ordering
            return orderByBasic(queryContext, request);
        }
    }

    /**
     * Creates the sort order for the query results when grid is in pivoting mode.
     *
     * @param queryContext  the current query state container
     * @param request       the server-side request parameters from the grid
     * @return list of orders
     */
    @NonNull
    protected List<OrderMetadata> orderByPivoting(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        PivotingContext pivotingContext = queryContext.getPivotingContext();
        
        // pivoting groups orders
        Stream<OrderMetadata> pivotingGroupOrders = request.getSortModel().stream()
                .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))    // ignore auto-generated column
                .filter(model -> request.getRowGroupCols().stream().anyMatch(rg -> rg.getField().equals(model.getColId()))) // present in group columns
                .map(sortModel -> {
                    ColDef<E, ?> colDef = this.colDefs.get(sortModel.getColId());
                    Expression<?> groupingColumnExpression = colDef.getField().getPath(root);
                    if ("absolute".equals(sortModel.getType())) {
                        groupingColumnExpression = cb.abs((Expression) groupingColumnExpression);
                    }
                    Order order = sortModel.getSort() == SortDirection.asc ? cb.asc(groupingColumnExpression) : cb.desc(groupingColumnExpression);
                    return OrderMetadata.builder()
                            .order(order)
                            .colId(sortModel.getColId())
                            .build();
                })
                .limit(request.getGroupKeys().size() + 1L);   // don't order groups that are not expanded yet
        
        // pivoting aggregations columns orders
        Stream<OrderMetadata> pivotingAggregationOrders = request.getSortModel().stream()
                .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))
                .filter(model -> pivotingContext.getColumnNamesToExpression().containsKey(model.getColId()))   // pivoting selection
                .map(sortModel -> {
                    Expression<?> pivotingExpression = pivotingContext.getColumnNamesToExpression().get(sortModel.getColId());
                    if ("absolute".equals(sortModel.getType())) {
                        pivotingExpression = cb.abs((Expression) pivotingExpression);
                    }
                    Order order = sortModel.getSort() == SortDirection.asc ? cb.asc(pivotingExpression) : cb.desc(pivotingExpression);
                    return OrderMetadata.builder()
                            .order(order)
                            .colId(sortModel.getColId())
                            .build();
                });

        return Stream.concat(pivotingGroupOrders, pivotingAggregationOrders).collect(Collectors.toList());
    }

    /**
     * Sets pagination parameters in the {@link QueryContext} based on the request's row range.
     * <p>
     * Configures the starting index and the maximum number of results to fetch from the data source.
     *
     * @param request      the AG Grid server-side row request containing {@code startRow} and {@code endRow}
     * @param queryContext the query context to populate with pagination (offset and limit)
     */
    protected void limitOffset(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) {
        queryContext.setFirstResult(request.getStartRow());
        queryContext.setMaxResults(request.getEndRow() - request.getStartRow());
    }

    /**
     * Converts a list of JPA {@link Tuple} objects to a list of maps keyed by their aliases.
     *
     * @param tuples the list of JPA tuples to convert
     * @return a list of maps where each map represents a tuple with alias-value pairs
     */
    @NonNull
    protected List<Map<String, Object>> tupleToMap(@NonNull List<Tuple> tuples) {
        if (tuples.isEmpty()) {
            return new ArrayList<>(0);
        }

        List<TupleElement<?>> elements = tuples.get(0).getElements();
        int columnCount = elements.size();
        
        String[] aliases = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            aliases[i] = elements.get(i).getAlias();
        }

        List<Map<String, Object>> result = new ArrayList<>(tuples.size());
        for (Tuple tuple : tuples) {
            // when master detail eager, 1 more element will be in columns (collection of detail records)
            Map<String, Object> map = new HashMap<>(columnCount + ((this.masterDetail && !this.masterDetailLazy) ? 1 : 0));

            for (int i = 0; i < columnCount; i++) {
                String alias = aliases[i];
                Object value = tuple.get(i);
                if (alias == null) {
                    continue;
                }

                // if dot notation is not suppressed and field contains dot notation, create embedded map
                if (!this.suppressFieldDotNotation && alias.contains(".")) {
                    String[] parts = alias.split("\\.");
                    
                    Map<String, Object> currentLevel = map;
                    for (int insertionLevel = 0; insertionLevel < parts.length - 1; insertionLevel++) {
                        String part = parts[insertionLevel];
                        
                        Object existing = currentLevel.get(part);
                        if (existing instanceof Map) {
                            // exists already
                            @SuppressWarnings("unchecked")
                            Map<String, Object> nextLevel = (Map<String, Object>) existing;
                            // move level down
                            currentLevel = nextLevel;
                        } else {
                            // Create new nested map and link it
                            Map<String, Object> newMap = new HashMap<>();
                            currentLevel.put(part, newMap);
                            // move level down
                            currentLevel = newMap;
                        }
                    }

                    // put value to the level of insertion
                    currentLevel.put(parts[parts.length - 1], value);
                } else {
                    // simple scenario
                    map.put(alias, value);
                }
            }
            result.add(map);
        }
        
        return result;
    }
    
    @NonNull
    protected Predicate createAdvancedFilterPredicate(@NonNull CriteriaBuilder cb, @NonNull Root<E> root, @NonNull Map<String, Object> filterModel) {
        if (this.isColumnFilter(filterModel)) {
            throw new IllegalArgumentException("Can not create advanced filter when filter is in column-filter format");
        }

        AdvancedFilterModel<E> advancedFilterModel = this.recognizeAdvancedFilter(filterModel);
        return advancedFilterModel.toPredicate(cb, root);
    }
    
    @NonNull
    protected Predicate createColumnFilterPredicate(@NonNull CriteriaBuilder cb, @NonNull Root<E> root, @NonNull Map<String, Object> filterModel) {
        if (!this.isColumnFilter(filterModel)) {
            throw new IllegalArgumentException("Can not create column filter when filter is not in column-filter format");
        }
        
        List<Predicate> predicates = new ArrayList<>(filterModel.size());
        for (var entry : filterModel.entrySet()) {
            String columnName = entry.getKey();
            Map<String, Object> filterMap = (Map<String, Object>) entry.getValue();

            // find col def
            ColDef<E, ?> colDef = Optional.ofNullable(this.colDefs.get(columnName))
                    .orElseThrow(() -> new IllegalArgumentException("Column " + columnName + " not found in col defs"));
            // filter of given column
            IFilter<?, ?, ?> filter = colDef.getFilter();
            if (filter == null) {
                throw new IllegalArgumentException("Column " + columnName + " is not filterable field!");
            }
            // predicate from filter
            Predicate predicate = filter.toPredicate(cb, (Expression) colDef.getField().getPath(root), filterMap);
            predicates.add(predicate);
        }

        return cb.and(predicates.toArray(new Predicate[0]));
    }

    /**
     * Determines if the received map structure is column filter
     * if so, should have this structure
     * columnName: {filterModel}
     * 
     * @param filterModel: filter model as Map
     * @return whether filter model is column filter or not
     */
    protected boolean isColumnFilter(Map<String, Object> filterModel) {
        if (filterModel == null) {
            return false;
        }
        return filterModel.values().stream().allMatch(Map.class::isInstance);
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
    @NonNull
    protected AdvancedFilterModel<E> recognizeAdvancedFilter(@NonNull Map<String, Object> filter) {
        if (!this.enableAdvancedFilter) {
            throw new IllegalArgumentException("Can not perform advanced filtering, enableAdvancedFilter is set to false!");
        }
        
        String filterType = filter.get("filterType").toString();
        if (filterType.equals("join")) {
            // join
            JoinAdvancedFilterModel<E> joinAdvancedFilterModel = new JoinAdvancedFilterModel<>();
            joinAdvancedFilterModel.setType(JoinOperator.valueOf(filter.get("type").toString()));
            joinAdvancedFilterModel.setConditions(((List<Map<String, Object>>) filter.get("conditions")).stream().map(this::recognizeAdvancedFilter).collect(Collectors.toList()));

            return joinAdvancedFilterModel;
        } else {
            // column
            String colId = filter.get("colId").toString();
            if (!this.colDefs.containsKey(colId)) {
                throw new IllegalArgumentException("Can not filter on column not defined in col defs!");
            }
            ColDef<E, ?> columnField = this.colDefs.get(colId);
            IFilter<?, ?, ?> columnFilter = columnField.getFilter();
            // assert the filter has enabled filtering
            if (columnFilter == null) {
                throw new IllegalArgumentException("Can not filter on column which has filtering turned-off");
            }
            
            switch (filterType) {
                case "text": case "object": {
                    if (!(columnFilter instanceof AgTextColumnFilter)) {
                        throw new IllegalArgumentException("Can not apply text filter on non-text column");
                    }
                    
                    ColDef<E, String> textColumnField = (ColDef<E, String>) columnField;
                    AgTextColumnFilter textColumnFilter = (AgTextColumnFilter) textColumnField.getFilter();
                    
                    TextAdvancedFilterModel<E> textAdvancedFilterModel = new TextAdvancedFilterModel<>(textColumnField.getField());
                    textAdvancedFilterModel.setType(TextAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    textAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).orElse(null));
                    if (textColumnFilter.getFilterParams() != null) {
                        textAdvancedFilterModel.setFilterParams(textAdvancedFilterModel.getFilterParams());
                    }
                    
                    return textAdvancedFilterModel;
                }
                case "date": case "dateString": {
                    if (!(columnFilter instanceof AgDateColumnFilter)) {
                        throw new IllegalArgumentException("Can not apply date filter on non-date column");
                    }

                    ColDef<E, ?> dateColumnField = columnField;
                    AgDateColumnFilter<?> dateColumnFilter = (AgDateColumnFilter<?>) dateColumnField.getFilter();
                    
                    DateAdvancedFilterModel<E, ?> dateAdvancedFilterModel = new DateAdvancedFilterModel<>(dateColumnField.getField());
                    dateAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    dateAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(f -> LocalDate.parse(f, DATE_FORMATTER_FOR_DATE_ADVANCED_FILTER)).orElse(null));
                    if (dateColumnFilter.getFilterParams() != null) {
                        dateAdvancedFilterModel.setFilterParams(dateColumnFilter.getFilterParams());
                    }
                    return dateAdvancedFilterModel;
                }
                case "number": {
                    if (!(columnFilter instanceof AgNumberColumnFilter)) {
                        throw new IllegalArgumentException("Can not apply number filter on non-number column");
                    }
                    
                    ColDef<E, ? extends Number> numberColumnField = (ColDef<E, ? extends Number>) columnField;
                    AgNumberColumnFilter<?> numberColumnFilter = (AgNumberColumnFilter<?>) columnField.getFilter();
                    
                    NumberAdvancedFilterModel<E, ?> numberAdvancedFilterModel = new NumberAdvancedFilterModel<>(numberColumnField.getField());
                    numberAdvancedFilterModel.setType(ScalarAdvancedFilterModelType.valueOf(filter.get("type").toString()));
                    numberAdvancedFilterModel.setFilter(Optional.ofNullable(filter.get("filter")).map(Object::toString).map(BigDecimal::new).orElse(null));
                    if (numberColumnFilter.getFilterParams() != null) {
                        numberAdvancedFilterModel.setFilterParams(numberColumnFilter.getFilterParams());
                    }
                    return numberAdvancedFilterModel;
                }
                case "boolean": {
                    ColDef<E, Boolean> booleanColDef = (ColDef<E, Boolean>) columnField;
                    
                    BooleanAdvancedFilterModel<E> booleanAdvancedFilterModel = new BooleanAdvancedFilterModel<>(booleanColDef.getField());
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

    protected void validateRequest(@NonNull ServerSideGetRowsRequest request) {
        List<InvalidRequestException.ValidationError> errors = new ArrayList<>();

        // validate groups cols
        if (!request.getRowGroupCols().isEmpty()) {
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
        if (!request.getValueCols().isEmpty()) {
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

            // validate that agg functions exist in global aggFuncs map
            List<ColumnVO> valueColsWithNonExistentAggFuncs = request.getValueCols()
                    .stream()
                    .filter(valueCol -> valueCol.getAggFunc() != null)
                    .filter(valueCol -> !this.aggFuncs.containsKey(valueCol.getAggFunc()))
                    .collect(Collectors.toList());
            if (!valueColsWithNonExistentAggFuncs.isEmpty()) {
                for (ColumnVO col : valueColsWithNonExistentAggFuncs) {
                    errors.add(new InvalidRequestException.ValidationError(
                            "valueCols",
                            String.format("Aggregation function '%s' for column '%s' does not exist.",
                                    col.getAggFunc(),
                                    col.getField()),
                            col
                    ));
                }
            }

            // validate agg functions
            List<ColumnVO> valueColsNotAllowedAggregations = request.getValueCols()
                    .stream()
                    .filter(valueCol -> this.colDefs.containsKey(valueCol.getField()))
                    .filter(valueCol -> this.colDefs.get(valueCol.getField()).isEnableValue())
                    .filter(valueCol -> this.colDefs.get(valueCol.getField()).getAllowedAggFuncs() != null)
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
        if (!request.getPivotCols().isEmpty()) {
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
        if (!request.getSortModel().isEmpty()) {
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

    @NonNull
    protected PivotingContext createPivotingContext(@NonNull QueryContext<E> queryContext, @NonNull ServerSideGetRowsRequest request) throws OnPivotMaxColumnsExceededException {
        if (!request.isPivotMode() || request.getPivotCols().isEmpty()) {
            throw new IllegalStateException("Can not create pivoting context when pivoting is turned off");
        }
        
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        if (this.pivotMaxGeneratedColumns != null) {
            long numberOfPivotColumns = this.countPivotColumnsToBeGenerated(cb, request);
            if (numberOfPivotColumns > this.pivotMaxGeneratedColumns) {
                throw new OnPivotMaxColumnsExceededException(this.pivotMaxGeneratedColumns, numberOfPivotColumns);
            }
        }

        PivotingContext pivotingContext = new PivotingContext();
        // distinct values for pivoting
        Map<String, List<Object>> pivotValues = this.getPivotValues(cb ,request);
        // pair pivot columns with values
        List<Set<Pair<String, Object>>> pivotPairs = this.createPivotPairs(pivotValues);
        // cartesian product of pivot pairs
        List<List<Pair<String, Object>>> cartesianProduct = cartesianProduct(pivotPairs);
        // for each column name its expression
        Map<String, Expression<?>> columnNamesToExpression = this.createPivotingExpressions(cb, root, request, cartesianProduct);
        // result fields are column names
        List<String> pivotingResultFields = new ArrayList<>(columnNamesToExpression.keySet());

        pivotingContext.setPivotValues(pivotValues);
        pivotingContext.setPivotPairs(pivotPairs);
        pivotingContext.setCartesianProduct(cartesianProduct);
        pivotingContext.setColumnNamesToExpression(columnNamesToExpression);
        pivotingContext.setPivotingResultFields(pivotingResultFields);

        return pivotingContext;
    }

    /**
     * For each pivoting column fetch distinct values
     * @param cb        criteria builder
     * @param request   request
     * @return map where key is column name and value is distinct column values
     */
    @NonNull
    protected Map<String, List<Object>> getPivotValues(@NonNull CriteriaBuilder cb, @NonNull ServerSideGetRowsRequest request) {
        Map<String, List<Object>> pivotValues = new LinkedHashMap<>(request.getPivotCols().size());
        for (ColumnVO column : request.getPivotCols()) {
            String field = column.getField();
            ColDef<E, ?> colDef = this.colDefs.get(column.getField());

            CriteriaQuery<Object> query = cb.createQuery(Object.class);
            Root<E> root = query.from(this.entityClass);

            // select
            Path<?> path = colDef.getField().getPath(root);
            query.multiselect(path).distinct(true);
            query.orderBy(cb.asc(path));

            // result
            List<Object> result = this.entityManager.createQuery(query).getResultList();
            pivotValues.put(field, result);
        }

        return pivotValues;
    }

    /**
     * <p>Creates pivot pairs from pivot values</p>
     * <p>For example, for input:</p>
     * <code>
     *     {
     *         book: [Book1, Book2],
     *         product: [Product1, Product2]
     *     }
     * </code>
     * <p>Output will be:</p>
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
    @NonNull
    protected List<Set<Pair<String, Object>>> createPivotPairs(@NonNull Map<String, List<Object>> pivotValues) {
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
    @NonNull
    protected String originalColNameFromPivoted(@NonNull String pivotedName) {
        return pivotedName.substring(pivotedName.lastIndexOf(this.serverSidePivotResultFieldSeparator) + 1);
    }

    /**
     * Calculates the product of the distinct counts of all pivot columns in a single query using the Criteria API.
     * <p>
     * This method dynamically constructs a query that computes the product of distinct counts for all fields specified 
     * as pivot columns in the current request and multiplies it with amount of value cols. 
     * It uses subqueries to calculate the distinct count for each field and 
     * combines them into a single product expression.
     * </p>
     *
     * @return The product of distinct counts for all pivot columns.
     *         Returns 0 if pivot mode is disabled or no pivot columns are defined in the request.
     */
    protected long countPivotColumnsToBeGenerated(@NonNull CriteriaBuilder cb, @NonNull ServerSideGetRowsRequest request) {
        if (!request.isPivotMode() || request.getPivotCols().isEmpty()) {
            return 0;
        }

        CriteriaQuery<Long> mainQuery = cb.createQuery(Long.class);

        Expression<Long> productExpression = cb.literal((long) request.getValueCols().size());
        for (ColumnVO pivotCol : request.getPivotCols()) {
            ColDef<E, ?> colDef = this.colDefs.get(pivotCol.getField());
            // Subquery for count(distinct <field>)
            Subquery<Long> subquery = mainQuery.subquery(Long.class);
            Root<E> subRoot = subquery.from(this.entityClass);
            subquery.select(cb.countDistinct(colDef.getField().getPath(subRoot)));

            productExpression = cb.prod(productExpression, subquery);
        }

        mainQuery.select(productExpression);

        return this.entityManager.createQuery(mainQuery).getSingleResult();
    }

    @NonNull
    protected Map<String, Expression<?>> createPivotingExpressions(@NonNull CriteriaBuilder cb, @NonNull Root<E> root, @NonNull ServerSideGetRowsRequest request, @NonNull List<List<Pair<String, Object>>> cartesianProduct) {
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
                        ColDef<E, ?> colDef = this.colDefs.get(columnVO.getField());
                        Path<?> field = colDef.getField().getPath(root);

                        CriteriaBuilder.Case<?> caseExpression = null;
                        for (Pair<String, Object> pair : pairs) {
                            ColDef<E, ?> pairKeyColDef = this.colDefs.get(pair.getKey());
                            if (caseExpression == null) {
                                caseExpression = cb.selectCase()
                                        .when(cb.equal(pairKeyColDef.getField().getPath(root), pair.getValue()), field);
                            } else {
                                caseExpression = cb.selectCase()
                                        .when(cb.equal(pairKeyColDef.getField().getPath(root), pair.getValue()), caseExpression);
                            }
                        }
                        Objects.requireNonNull(caseExpression);

                        // wrap case expression onto aggregation
                        var aggregateFunction = this.aggFuncs.get(columnVO.getAggFunc());
                        Expression<?> aggregatedField = aggregateFunction.apply(cb, caseExpression);

                        String columnName = alias + this.serverSidePivotResultFieldSeparator + columnVO.getField();
                        pivotingExpressions.put(columnName, aggregatedField);
                    });
        });

        return pivotingExpressions;
    }
    
    public static class Builder<E, D> {
        private static final String DEFAULT_SERVER_SIDE_PIVOT_RESULT_FIELD_SEPARATOR = "_";
        private static final Function<String, List<String>> DEFAULT_QUICK_FILTER_PARSER = input -> Arrays.asList(input.trim().split("\\s+")); 
        
        private final Class<E> entityClass;
        private final Class<D> detailClass;
        private final EntityManager entityManager;

        private String primaryFieldName;
        private String serverSidePivotResultFieldSeparator = DEFAULT_SERVER_SIDE_PIVOT_RESULT_FIELD_SEPARATOR;
        private Integer pivotMaxGeneratedColumns;
        private boolean enableAdvancedFilter;
        private boolean paginateChildRows;
        private boolean groupAggFiltering;
        private boolean suppressAggFilteredOnly;
        private boolean isExternalFilterPresent;
        private final Map<String, BiFunction<CriteriaBuilder, Expression<?>, Expression<?>>> aggFuncs = Arrays.stream(AggregationFunction.values())
                .collect(Collectors.toMap(Enum::name, AggregationFunction::getCreateAggregateFunction));
        private TriFunction<CriteriaBuilder, Root<E>, Object, Predicate> doesExternalFilterPass;
        private boolean suppressFieldDotNotation;
        protected boolean getChildCount;
        protected String getChildCountFieldName;

        protected boolean isQuickFilterPresent;
        protected Function<String, List<String>> quickFilterParser = DEFAULT_QUICK_FILTER_PARSER;
        protected TriFunction<CriteriaBuilder, Root<E>, List<String>, Predicate> quickFilterMatcher;
        protected List<FieldPath<E, String>> quickFilterSearchInFields;
        protected boolean quickFilterTrimInput;
        protected boolean quickFilterCaseSensitive;
        protected BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> quickFilterTextFormatter;
        
        private boolean treeData;
        private String isServerSideGroupFieldName;
        private String treeDataParentReferenceField;
        private String treeDataParentIdField;
        private String treeDataChildrenField;
        private String treeDataDataPathFieldName;
        private String treeDataDataPathSeparator;
        
        private boolean masterDetail;
        private boolean masterDetailLazy = true;
        private String masterDetailRowDataFieldName;
        private MasterDetailParams<E, D> masterDetailParams;
        private Function<Map<String, Object>, MasterDetailParams<E, D>> dynamicMasterDetailParams;
        private boolean grandTotalRow;
        
        private Map<String, ColDef<E, ?>> colDefs;


        protected Builder(Class<E> entityClass, EntityManager entityManager) {
            this.entityClass = entityClass;
            this.detailClass = null;
            this.entityManager = entityManager;
        }
        
        protected Builder(Class<E> entityClass, Class<D> detailClass, EntityManager entityManager) {
            this.entityClass = entityClass;
            this.detailClass = detailClass;
            this.entityManager = entityManager;

            this.masterDetail = true;
        }
        
        @NonNull
        public Builder<E, D> primaryFieldName(@NonNull String primaryFieldName) {
            this.primaryFieldName = primaryFieldName;
            return this;
        }

        @NonNull
        public Builder<E, D> serverSidePivotResultFieldSeparator(@NonNull String separator) {
            if (separator.isEmpty()) {
                throw new IllegalArgumentException("Server side pivot result field separator cannot be null or empty");
            }
            this.serverSidePivotResultFieldSeparator = separator;
            return this;
        }

        @NonNull
        public Builder<E, D> pivotMaxGeneratedColumns(Integer pivotMaxGeneratedColumns) {
            if (pivotMaxGeneratedColumns != null && pivotMaxGeneratedColumns <= 0) {
                throw new IllegalArgumentException("pivot max generated columns must be greater than zero");
            }
            this.pivotMaxGeneratedColumns = pivotMaxGeneratedColumns;
            return this;
        }
        
        @SafeVarargs
        @NonNull
        public final Builder<E, D> colDefs(@NonNull ColDef<E, ?>... colDefs) {
            this.colDefs = new HashMap<>(colDefs.length);
            for (ColDef<E, ?> colDef : colDefs) {
                this.colDefs.put(colDef.getFieldName(), colDef);
            }
            return this;
        }
        
        @NonNull
        public Builder<E, D> colDefs(@NonNull Collection<ColDef<E, ?>> colDefs) {
            this.colDefs = new HashMap<>(colDefs.size());
            for (ColDef<E, ?> colDef : colDefs) {
                this.colDefs.put(colDef.getFieldName(), colDef);
            }
            return this;
        }
        
        @NonNull
        public Builder<E, D> enableAdvancedFilter(boolean enableAdvancedFilter) {
            this.enableAdvancedFilter = enableAdvancedFilter;
            return this;
        }
        
        @NonNull
        public Builder<E, D> paginateChildRows(boolean paginateChildRows) {
            this.paginateChildRows = paginateChildRows;
            return this;
        }

        @NonNull
        public Builder<E, D> suppressFieldDotNotation(boolean suppressFieldDotNotation) {
            this.suppressFieldDotNotation = suppressFieldDotNotation;
            return this;
        }

        @NonNull
        public Builder<E, D> isQuickFilterPresent(boolean isQuickFilterPresent) {
            this.isQuickFilterPresent = isQuickFilterPresent;
            return this;
        }

        @NonNull
        public Builder<E, D> quickFilterParser(@NonNull Function<String, List<String>> quickFilterParser) {
            this.quickFilterParser = quickFilterParser;
            return this;
        }

        @NonNull
        public Builder<E, D> quickFilterMatcher(@NonNull TriFunction<CriteriaBuilder, Root<E>, List<String>, Predicate> quickFilterMatcher) {
            this.quickFilterMatcher = quickFilterMatcher;
            return this;
        }

        @NonNull
        public Builder<E, D> quickFilterSearchInFields(@NonNull List<FieldPath<E, String>> quickFilterSearchInFields) {
            this.quickFilterSearchInFields = quickFilterSearchInFields;
            return this;
        }

        @SafeVarargs
        @NonNull
        public final Builder<E, D> quickFilterSearchInFields(@NonNull FieldPath<E, String>... quickFilterSearchInFields) {
            this.quickFilterSearchInFields = Arrays.asList(quickFilterSearchInFields);
            return this;
        }

        @NonNull
        public Builder<E, D> quickFilterTrimInput(boolean quickFilterTrimInput) {
            this.quickFilterTrimInput = quickFilterTrimInput;
            return this;
        }

        @NonNull
        public Builder<E, D> quickFilterCaseSensitive(boolean quickFilterCaseSensitive) {
            this.quickFilterCaseSensitive = quickFilterCaseSensitive;
            return this;
        }

        @NonNull
        public Builder<E, D> quickFilterTextFormatter(@NonNull BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> quickFilterTextFormatter) {
            this.quickFilterTextFormatter = quickFilterTextFormatter;
            return this;
        }
        
        @NonNull
        public Builder<E, D> suppressAggFilteredOnly(boolean suppressAggFilteredOnly) {
            this.suppressAggFilteredOnly = suppressAggFilteredOnly;
            return this;
        }

        @NonNull
        public Builder<E, D> getChildCount(boolean getChildCount) {
            this.getChildCount = getChildCount;
            return this;
        }

        @NonNull
        public Builder<E, D> getChildCountFieldName(@NonNull String getChildCountFieldName) {
            this.getChildCountFieldName = getChildCountFieldName;
            return this;
        }
        
        @NonNull
        public Builder<E, D> isExternalFilterPresent(boolean isExternalFilterPresent) {
            this.isExternalFilterPresent = isExternalFilterPresent;
            return this;
        }
        
        @NonNull
        public Builder<E, D> doesExternalFilterPass(@NonNull TriFunction<CriteriaBuilder, Root<E>, Object, Predicate> doesExternalFilterPass) {
            this.doesExternalFilterPass = doesExternalFilterPass;
            return this;
        }
        
        
        @NonNull
        public Builder<E, D> groupAggFiltering(boolean groupAggFiltering) {
            this.groupAggFiltering = groupAggFiltering;
            return this;
        }
        
        @NonNull
        public Builder<E, D> treeData(boolean treeData) {
            this.treeData = treeData;
            return this;
        }
        
        @NonNull
        public Builder<E, D> isServerSideGroupFieldName(@NonNull String isServerSideGroupFieldName) {
            this.isServerSideGroupFieldName = isServerSideGroupFieldName;
            return this;
        }

        @NonNull
        public Builder<E, D> treeDataParentReferenceField(@NonNull String treeDataParentReferenceField) {
            this.treeDataParentReferenceField = treeDataParentReferenceField;
            return this;
        }

        @NonNull
        public Builder<E, D> treeDataParentIdField(@NonNull String treeDataParentIdField) {
            this.treeDataParentIdField = treeDataParentIdField;
            return this;
        }

        @NonNull
        public Builder<E, D> treeDataChildrenField(@NonNull String treeDataChildrenField) {
            this.treeDataChildrenField = treeDataChildrenField;
            return this;
        }

        @NonNull
        public Builder<E, D> treeDataDataPathFieldName(@NonNull String treeDataDataPathFieldName) {
            this.treeDataDataPathFieldName = treeDataDataPathFieldName;
            return this;
        }

        @NonNull
        public Builder<E, D> treeDataDataPathSeparator(@NonNull String treeDataDataPathSeparator) {
            this.treeDataDataPathSeparator = treeDataDataPathSeparator;
            return this;
        }
        
        @NonNull
        public Builder<E, D> masterDetail(boolean masterDetail) {
            this.masterDetail = masterDetail;
            return this;
        }

        @NonNull
        public Builder<E, D> masterDetailLazy(boolean masterDetailLazy) {
            this.masterDetailLazy = masterDetailLazy;
            return this;
        }
        
        @NonNull
        public Builder<E, D> masterDetailRowDataFieldName(@NonNull String masterDetailRowDataFieldName) {
            this.masterDetailRowDataFieldName = masterDetailRowDataFieldName;
            return this;
        }

        @NonNull
        public Builder<E, D> masterDetailParams(@NonNull MasterDetailParams<E, D> masterDetailParams) {
            this.masterDetailParams = masterDetailParams;
            return this;
        }

        @NonNull
        public Builder<E, D> dynamicMasterDetailParams(@NonNull Function<Map<String, Object>, MasterDetailParams<E, D>> dynamicMasterDetailParams) {
            this.dynamicMasterDetailParams = dynamicMasterDetailParams;
            return this;
        }
        
        @NonNull
        public Builder<E, D> grandTotalRow(boolean grandTotalRow) {
            this.grandTotalRow = grandTotalRow;
            return this;
        }

        
        @NonNull
        public Builder<E, D> registerCustomAggFunction(@NonNull String name, @NonNull BiFunction<CriteriaBuilder, Expression<?>, Expression<?>> function) {
            this.aggFuncs.put(name, function);
            return this;
        }

        @NonNull
        public QueryBuilder<E, D> build() {
            this.validateBeforeBuild();
            return new QueryBuilder<>(this);
        }
        
        private void validateBeforeBuild() {
            // colDefs args validation
            if (this.colDefs == null || this.colDefs.isEmpty()) {
                throw new IllegalArgumentException("colDefs cannot be null or empty");
            }
            // validate col defs aggregation functions
            List<ColDef<E, ?>> colDefsWithUnrecognizedAggFunctions = this.colDefs.values().stream()
                    .filter(cd -> cd.getAllowedAggFuncs() != null)
                    .filter(cd -> cd.getAllowedAggFuncs().stream().anyMatch(f -> !this.aggFuncs.containsKey(f)))
                    .collect(Collectors.toList());
            if (!colDefsWithUnrecognizedAggFunctions.isEmpty()) {
                String details = colDefsWithUnrecognizedAggFunctions.stream()
                        .map(cd -> {
                            List<String> unknownFuncs = cd.getAllowedAggFuncs().stream()
                                    .filter(f -> !this.aggFuncs.containsKey(f))
                                    .collect(Collectors.toList());
                            return String.format(
                                    "Column '%s': %s",
                                    cd.getFieldName(),
                                    unknownFuncs
                            );
                        })
                        .collect(Collectors.joining("; "));
                
                throw new IllegalStateException("Found unrecognized aggregation functions: " + details);
            }
            
            // tree data arguments validation
            if (this.treeData) {
                this.validateTreeDataArgs();
            }
            
            if (this.masterDetail) {
                if (this.masterDetailParams == null && this.dynamicMasterDetailParams == null) {
                    throw new IllegalStateException("When masterDetail is set to true, masterDetailParams or dynamicMasterDetailParams must be provided");
                }
                if (this.dynamicMasterDetailParams == null && this.primaryFieldName == null) {
                    throw new IllegalStateException("Must provide primaryFieldName for master-detail relationship");
                }
                if (!this.masterDetailLazy) {
                    if (this.masterDetailRowDataFieldName == null) {
                        throw new IllegalStateException("When masterDetailLazy is set to false, masterDetailRowDataFieldName must be provided");
                    } else if (this.dynamicMasterDetailParams == null && this.masterDetailParams.detailColDefs.containsKey(this.masterDetailRowDataFieldName)) {
                        throw new IllegalStateException("masterDetailRowDataFieldName '" + this.masterDetailRowDataFieldName + "' collides with existing detailColDef");
                    }
                }
            }
            
            if (this.getChildCount) {
                if (this.getChildCountFieldName == null) {
                    throw new IllegalStateException("When getChildCount is set to true, provide field name in which it should be stored in");
                }
                if (this.colDefs.containsKey(this.getChildCountFieldName)) {
                    throw new IllegalStateException("getChildCountFieldName collides with existing colDef");
                }
            }
            
            if (this.isExternalFilterPresent) {
                if (this.doesExternalFilterPass == null) {
                    throw new IllegalStateException("When isExternalFilterPresent is set to true, doesExternalFilterPass function must be provided");
                }
            }
            
            if (this.isQuickFilterPresent) {
                if (this.quickFilterParser == null) {
                    throw new IllegalStateException("When isQuickFilterPresent is set to true, quickFilterParser function must be provided");
                }
                
                if (this.quickFilterMatcher == null) {
                    if (this.quickFilterSearchInFields == null || this.quickFilterSearchInFields.isEmpty()) {
                        throw new IllegalStateException("When isQuickFilterPresent is set to true, quickFilterSearchInFields function must be provided");
                    }
                }
            }
        }
        
        private void validateTreeDataArgs() {
            List<String> treeDataErrorMessages = new ArrayList<>();
            if (this.primaryFieldName == null) {
                treeDataErrorMessages.add("When treeData is set to true, primaryFieldName must be provided");
            }
            if (this.isServerSideGroupFieldName == null) {
                treeDataErrorMessages.add("When treeData is set to true, isServerSideGroupFieldName must be provided");
            }
            if (this.colDefs.containsKey(this.isServerSideGroupFieldName)) {
                treeDataErrorMessages.add("isServerSideGroupFieldName '" + this.isServerSideGroupFieldName + "' collides with existing colDef");
            }

            if (this.treeDataParentReferenceField == null && this.treeDataParentIdField == null) {
                treeDataErrorMessages.add("When treeData is set to true, either treeDataParentReferenceField or treeDataParentIdField must be provided");
            }
            
            if ((this.treeDataDataPathFieldName == null) != (this.treeDataDataPathSeparator == null)) {
                treeDataErrorMessages.add("When treeData is set to true and you want to use tree data filtering, both treeDataDataPathFieldName and treeDataDataPathSeparator must be provided");
            }

            if (!treeDataErrorMessages.isEmpty()) {
                throw new IllegalStateException(String.join("\n", treeDataErrorMessages));
            }
        }
    }

    @Getter
    public static class MasterDetailParams<P, C> {
        private final Class<C> detailClass;
        private final Map<String, ColDef<C, ?>> detailColDefs;
        private final SingularAttribute<C, P> detailMasterReferenceField;
        private final SingularAttribute<C, ?> detailMasterIdField;
        private final TriFunction<CriteriaBuilder, Root<C>, Map<String, Object>, Predicate> createMasterRowPredicate;
        
        @NonNull
        public static <P, C> Builder<P, C> builder() {
            return new Builder<>();
        }

        private MasterDetailParams(Builder<P, C> builder) {
            this.detailClass = builder.detailClass;
            this.detailColDefs = builder.detailColDefs;
            this.detailMasterReferenceField = builder.detailMasterReferenceField;
            this.detailMasterIdField = builder.detailMasterIdField;
            this.createMasterRowPredicate = builder.createMasterRowPredicate;
        }

        public static class Builder<P, C> {
            private Class<C> detailClass;
            private Map<String, ColDef<C, ?>> detailColDefs;
            private SingularAttribute<C, P> detailMasterReferenceField;
            private SingularAttribute<C, ?> detailMasterIdField;
            private TriFunction<CriteriaBuilder, Root<C>, Map<String, Object>, Predicate> createMasterRowPredicate;
            
            private Builder() {}

            @NonNull
            public Builder<P, C> detailClass(@NonNull Class<C> detailClass) {
                this.detailClass = detailClass;
                return this;
            }

            @SafeVarargs
            @NonNull
            public final Builder<P, C> detailColDefs(@NonNull ColDef<C, ?>... colDefs) {
                this.detailColDefs = new HashMap<>(colDefs.length);
                for (ColDef<C, ?> colDef : colDefs) {
                    this.detailColDefs.put(colDef.getFieldName(), colDef);
                }
                return this;
            }

            @NonNull
            public Builder<P, C> detailColDefs(@NonNull Collection<ColDef<C, ?>> colDefs) {
                this.detailColDefs = new HashMap<>(colDefs.size());
                for (ColDef<C, ?> colDef : colDefs) {
                    this.detailColDefs.put(colDef.getFieldName(), colDef);
                }
                return this;
            }

            @NonNull
            public Builder<P, C> detailMasterReferenceField(@NonNull SingularAttribute<C, P> detailMasterReferenceField) {
                this.detailMasterReferenceField = detailMasterReferenceField;
                return this;
            }

            @NonNull
            public Builder<P, C>  detailMasterIdField(@NonNull SingularAttribute<C, ?> detailMasterIdField) {
                this.detailMasterIdField = detailMasterIdField;
                return this;
            }

            @NonNull
            public Builder<P, C> createMasterRowPredicate(@NonNull TriFunction<CriteriaBuilder, Root<C>, Map<String, Object>, Predicate> createMasterRowPredicate) {
                this.createMasterRowPredicate = createMasterRowPredicate;
                return this;
            }

            @NonNull
            public MasterDetailParams<P, C> build() {
                this.validateMasterDetailArgs();
                return new MasterDetailParams<>(this);
            }

            private void validateMasterDetailArgs() {
                List<String> masterDetailErrorMessages = new ArrayList<>();
                if (this.detailClass == null) {
                    masterDetailErrorMessages.add("When masterDetail is set to true, detailClass must be provided");
                }
                if (this.detailColDefs == null || this.detailColDefs.isEmpty()) {
                    masterDetailErrorMessages.add("When masterDetail is set to true, detailColDefs must be provided");
                }

                if (this.createMasterRowPredicate == null) {
                    if (this.detailMasterReferenceField == null && this.detailMasterIdField == null) {
                        masterDetailErrorMessages.add("Must provide either createMasterRowPredicate, detailMasterReferenceField or detailMasterIdField for master-detail relationship");
                    }
                }

                if (!masterDetailErrorMessages.isEmpty()) {
                    throw new IllegalStateException(String.join("\n", masterDetailErrorMessages));
                }
            }
        }
    }
    
    
}
