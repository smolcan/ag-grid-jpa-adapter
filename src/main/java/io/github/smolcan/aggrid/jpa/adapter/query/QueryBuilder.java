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
import static io.github.smolcan.aggrid.jpa.adapter.utils.Utils.getPath;

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

public class QueryBuilder<E> {
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
    protected final List<String> quickFilterSearchInFields;
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
    protected final MasterDetailParams masterDetailParams;
    protected final Function<Map<String, Object>, MasterDetailParams> dynamicMasterDetailParams;


    protected final Map<String, ColDef> colDefs;
    
    public static <E> Builder<E> builder(Class<E> entityClass, EntityManager entityManager) {
        return new Builder<>(entityClass, entityManager);
    }
    
    protected QueryBuilder(Builder<E> builder) {
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
    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) {
        this.validateRequest(request);
        
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<E> root = query.from(this.entityClass);
        
        // record all the context we put into query
        QueryContext<E> queryContext = new QueryContext<>(cb, query, root);
        // if pivoting, load all information needed for pivoting into pivoting context
        queryContext.setPivotingContext(this.createPivotingContext(queryContext, request));
        
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

            // subquery will only select the group column 
            Subquery<?> subquery = query.subquery(getPath(root, countingGroupCol).getJavaType());
            Root<E> subqueryRoot = subquery.from(this.entityClass);
            QueryContext<E> subqueryContext = new QueryContext<>(cb, subquery, subqueryRoot);
            subqueryContext.setPivotingContext(this.createPivotingContext(subqueryContext, request));
            
            this.select(subqueryContext, request);
            this.where(subqueryContext, request);
            this.groupBy(subqueryContext, request);
            this.having(subqueryContext, request);
            
            // select the group column in subquery
            subquery.select((Expression) getPath(subqueryRoot, countingGroupCol)).distinct(true);
            // where
            if (!queryContext.getWherePredicates().isEmpty()) {
                Predicate[] predicates = queryContext.getWherePredicates().stream().map(WherePredicateMetadata::getPredicate).toArray(Predicate[]::new);
                subquery.where(predicates);
            }
            // group by
            if (!queryContext.getGrouping().isEmpty()) {
                subquery.groupBy(queryContext.getGrouping().stream().map(GroupingMetadata::getGropingExpression).collect(Collectors.toList()));
            }
            // having
            if (!queryContext.getHaving().isEmpty()) {
                Predicate[] having = queryContext.getHaving().stream().map(HavingMetadata::getPredicate).toArray(Predicate[]::new);
                subquery.having(having);
            }
            
            // in parent query, count distinct values of column group that are returned in subquery
            query.select(cb.countDistinct(getPath(root, countingGroupCol)));
            query.where(cb.in(getPath(root, countingGroupCol)).value((Subquery) subquery));
            
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
     * Retrieves the detail row data for a specific master row in Master-Detail mode.
     * <p>
     * This method executes a query to fetch child records associated with the provided
     * {@code masterRow}, applying any dynamic class resolution or column definitions if configured.
     *
     * @param masterRow the data of the parent row for which details are being requested
     * @return a list of maps representing the detail rows
     */
    public List<Map<String, Object>> getDetailRowData(Map<String, Object> masterRow) {
        if (!this.masterDetail) {
            throw new IllegalStateException("Please set masterDetail property to true to use detail row data");
        }
        
        // find params for detail grid
        MasterDetailParams masterDetailParams = this.dynamicMasterDetailParams != null
                ? this.dynamicMasterDetailParams.apply(masterRow)   // dynamic
                : this.masterDetailParams;                          // static
        
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<?> root = query.from(masterDetailParams.getDetailClass());
        
        // select
        query.multiselect(
                masterDetailParams.getDetailColDefs().values().stream()
                .map(colDef -> getPath(root, colDef.getField()).alias(colDef.getField()))
                .collect(Collectors.toList())
        );

        // master predicate
        Predicate masterPredicate = this.createMasterRowPredicate(cb, root, masterRow, masterDetailParams);
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
    public List<Object> supplySetFilterValues(String field) {
        ColDef colDef = this.colDefs.get(field);
        if (colDef == null) {
            throw new IllegalArgumentException(String.format("Column definition for field '%s' not found.", field));
        }
        if (colDef.getFilter() == null) {
            throw new IllegalStateException(String.format("Filter not enabled for field '%s'.", field));
        }

        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Object> query = cb.createQuery(Object.class);
        Root<E> root = query.from(this.entityClass);
        Path<?> path = getPath(root, colDef.getField());
        
        // select
        query.select(path).distinct(true);
        // order by asc
        query.orderBy(cb.asc(path));
        
        return this.entityManager.createQuery(query).getResultList();
    }

    /**
     * Determines and sets the fields to be selected in the query.
     * Delegates the selection logic based on the active grid mode. 
     * Sets the selections into queryContext.
     * 
     * @param queryContext the current query state container
     * @param request the server-side request parameters from the grid
     */
    protected void select(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        // select
        List<SelectionMetadata> selections;
        if (this.treeData) {
            // tree data
            selections = this.selectTreeData(queryContext, request);
        } else if (this.masterDetail) {
            // master-detail
            selections = this.selectMasterDetail(queryContext, request);
        } else if (queryContext.getPivotingContext().isPivoting()) {
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
    protected void where(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        List<WherePredicateMetadata> wherePredicates;
        if (this.treeData) {
            // tree data
            wherePredicates = this.whereTreeData(queryContext, request);
        } else if (this.masterDetail) {
            // master detail
            wherePredicates = this.whereMasterDetail(queryContext, request);
        } else if (queryContext.getPivotingContext().isPivoting()) {
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
    protected void groupBy(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        List<GroupingMetadata> groupingMetadata;

        if (this.treeData) {
            // tree data
            groupingMetadata = this.groupByTreeData(queryContext, request);
        } else if (this.masterDetail) {
            // master-detail
            groupingMetadata = this.groupByMasterDetail(queryContext, request);
        } else if (queryContext.getPivotingContext().isPivoting()) {
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
    protected void orderBy(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        List<OrderMetadata> orders;
        if (this.treeData) {
            orders = this.orderByBasic(queryContext, request);
        } else if (this.masterDetail) {
            orders = this.orderByBasic(queryContext, request);
        } else if (queryContext.getPivotingContext().isPivoting()) {
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
    protected void having(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        List<HavingMetadata> havingPredicates;
        if (this.treeData) {
            havingPredicates = List.of();
        } else if (this.masterDetail) {
            havingPredicates = List.of();
        } else if (queryContext.getPivotingContext().isPivoting()) {
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
    protected void attachDetailRowDataToMasters(List<Map<String, Object>> masters) {
        if (masters == null || masters.isEmpty()) {
            return;
        }

        // dynamic params or custom detail function, N+1
        if (this.dynamicMasterDetailParams != null || (this.masterDetailParams != null && this.masterDetailParams.createMasterRowPredicate != null)) {
            for (Map<String, Object> row : masters) {
                row.put(this.masterDetailRowDataFieldName, this.getDetailRowData(row));
            }
            return;
        }
        
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<?> detailRoot = query.from(this.masterDetailParams.getDetailClass());
        
        List<Selection<?>> detailSelections = this.masterDetailParams.getDetailColDefs().values().stream()
                .map(colDef -> getPath(detailRoot, colDef.getField()).alias(colDef.getField()))
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
    protected Predicate createMasterRowPredicate(CriteriaBuilder cb, Root<?> root, Map<String, Object> masterRow, MasterDetailParams params) {
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
    protected Predicate createQuickFilterPredicate(CriteriaBuilder cb, Root<E> root, String quickFilter) {
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
            for (String field : this.quickFilterSearchInFields) {
                Expression<String> path = getPath(root, field).as(String.class);

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
    protected List<Tuple> apply(CriteriaQuery<Tuple> query, QueryContext<E> queryContext) {
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
    
    
    protected List<SelectionMetadata> selectTreeData(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        List<SelectionMetadata> selections = new ArrayList<>();

        // add each non-aggregated field to selections as basic selection
        this.colDefs.values().stream()
                .filter(cd -> request.getValueCols().stream().noneMatch(vc -> vc.getField().equals(cd.getField()))) // filter out the aggregated ones
                .forEach(colDef -> {
                    Path<?> field = getPath(root, colDef.getField());
                    selections.add(SelectionMetadata.builder(field, colDef.getField()).build());
                });

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
        selections.add(
                SelectionMetadata
                        .builder(isServerSideGroupSelection, this.isServerSideGroupFieldName)
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
                                .builder(aggExpression, aggColumn.getField())
                                .isAggregationSelection(true)
                                .build()
                );
            }
        }
        
        // add child counts
        if (this.getChildCount) {
            Expression<?> countExpression = this.createTreeDataGetChildCountExpression(queryContext, isServerSideGroupSelection, request);
            selections.add(
                    SelectionMetadata
                            .builder(countExpression, this.getChildCountFieldName)
                            .isChildCountSelection(true)
                            .build()
            );
        }
        
        return selections;
    }

    protected List<SelectionMetadata> selectMasterDetail(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        if (request.getGroupKeys().isEmpty()) {
            return this.selectBasic(queryContext, request);
        } else {
            return this.selectGrouping(queryContext, request);
        }
    }
    
    protected List<SelectionMetadata> selectPivoting(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        Root<E> root = queryContext.getRoot();
        PivotingContext pivotingContext = queryContext.getPivotingContext();
        
        List<SelectionMetadata> selections = new ArrayList<>();
        
        // group columns
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
            ColumnVO groupCol = request.getRowGroupCols().get(i);
            Expression<?> groupExpression = getPath(root, groupCol.getField());

            SelectionMetadata groupSelectionMetadata = SelectionMetadata
                    .builder(groupExpression, groupCol.getField())
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
                            .builder(entry.getValue(), entry.getKey())
                            .isPivotingSelection(true)
                            .isAggregationSelection(true)
                            .build()
                )
                .forEach(selections::add);
        
        return selections;
    }
    
    protected List<SelectionMetadata> selectGrouping(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        List<SelectionMetadata> selections = new ArrayList<>();

        boolean hasUnexpandedGroups = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (hasUnexpandedGroups) {
            // group columns
            for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
                ColumnVO groupCol = request.getRowGroupCols().get(i);
                Expression<?> groupExpression = getPath(root, groupCol.getField());

                SelectionMetadata groupSelectionMetadata = SelectionMetadata
                        .builder(groupExpression, groupCol.getField())
                        .isGroupingSelection(true)
                        .build();
                selections.add(groupSelectionMetadata);
            }

            // count children
            if (this.getChildCount) {
                Expression<Long> childCountExpression = cb.count(root);
                selections.add(
                        SelectionMetadata.builder(childCountExpression, this.getChildCountFieldName)
                                .isChildCountSelection(true)
                                .build()
                );
            }

            // aggregated columns
            for (ColumnVO columnVO : request.getValueCols()) {
                Expression<?> path = getPath(root, columnVO.getField());
                var aggregateFunction = this.aggFuncs.get(columnVO.getAggFunc());
                Expression<?> aggregatedField = aggregateFunction.apply(cb, path);
                selections.add(
                        SelectionMetadata
                                .builder(aggregatedField, columnVO.getField())
                                .isAggregationSelection(true)
                                .build()
                );
            }
        } else {
            // groups are already expanded
            // just select columns
            for (ColDef colDef : this.colDefs.values()) {
                Path<?> field = getPath(root, colDef.getField());
                selections.add(SelectionMetadata.builder(field, colDef.getField()).build());
            }
        }
        
        return selections;
    }

    protected List<SelectionMetadata> selectBasic(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        Root<E> root = queryContext.getRoot();
        // just select col defs
        return this.colDefs.values()
                .stream()
                .map(colDef -> {
                    Path<?> field = getPath(root, colDef.getField());
                    return SelectionMetadata.builder(field, colDef.getField()).build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Fills the where metadata predicates if when the grid is in tree data mode
     *
     */
    protected List<WherePredicateMetadata> whereTreeData(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
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
                        .builder(treePredicate)
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
                            .builder(treeDataFilteringPredicate)
                            .isFilterPredicate(true)
                            .build()
            );
        }
        
        return wherePredicateMetadata;
    }

    protected List<WherePredicateMetadata> whereMasterDetail(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        if (request.getGroupKeys().isEmpty()) {
            return this.whereBasic(queryContext, request);
        } else {
            return this.whereGrouping(queryContext, request);
        }
    }

    protected List<WherePredicateMetadata> wherePivoting(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        List<WherePredicateMetadata> wherePredicates = new ArrayList<>();

        // expanded groups
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size(); i++) {
            String groupKey = request.getGroupKeys().get(i);
            String groupCol = request.getRowGroupCols().get(i).getField();

            // try to synchronize col and key to same data type to prevent errors
            // for example, group key is date as string, but field is date, need to parse to date and then compare
            TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(getPath(root, groupCol), groupKey);
            Predicate groupPredicate = cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());

            // wrap in predicate info object
            WherePredicateMetadata groupPredicateInfo = WherePredicateMetadata
                    .builder(groupPredicate)
                    .isGroupPredicate(true)
                    .groupKey(synchronizedValueType.getSynchronizedValue())
                    .groupCol(groupCol)
                    .build();
            wherePredicates.add(groupPredicateInfo);
        }
        
        // todo: check how where clause for pivoting should work when filtering
        
        return wherePredicates;
    }

    protected List<WherePredicateMetadata> whereGrouping(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        AbstractQuery<?> query = queryContext.getQuery();
        Root<E> root = queryContext.getRoot();
        
        List<WherePredicateMetadata> wherePredicates = new ArrayList<>();
        
        // where expanded groups predicates
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size(); i++) {
            String groupKey = request.getGroupKeys().get(i);
            String groupCol = request.getRowGroupCols().get(i).getField();

            // try to synchronize col and key to same data type to prevent errors
            // for example, group key is date as string, but field is date, need to parse to date and then compare
            TypeValueSynchronizer.Result<?> synchronizedValueType = TypeValueSynchronizer.synchronizeTypes(getPath(root, groupCol), groupKey);
            Predicate groupPredicate = cb.equal(synchronizedValueType.getSynchronizedPath(), synchronizedValueType.getSynchronizedValue());

            // wrap in predicate info object
            WherePredicateMetadata groupPredicateInfo = WherePredicateMetadata
                    .builder(groupPredicate)
                    .isGroupPredicate(true)
                    .groupKey(synchronizedValueType.getSynchronizedValue())
                    .groupCol(groupCol)
                    .build();
            wherePredicates.add(groupPredicateInfo);
        }
        
        boolean hasAnyFilteringOnAggregatedColumns = !this.enableAdvancedFilter &&
                request.getFilterModel().keySet().stream().anyMatch(k -> request.getValueCols().stream().anyMatch(vc -> vc.getField().equals(k)));
        boolean hasUnexpandedGroups = request.getRowGroupCols().size() > request.getGroupKeys().size();
        
        if (this.groupAggFiltering && hasAnyFilteringOnAggregatedColumns && hasUnexpandedGroups) {
            // apply filtering to group rows by enabling the groupAggFiltering grid option, allowing filters to also apply against the aggregated values.
            // When a group row passes a filter, it also includes all of its descendent rows in the filtered results.
            
            Subquery<Integer> leafNodeExistsSubquery = query.subquery(Integer.class);
            Root<E> leafNodeRoot = leafNodeExistsSubquery.from(this.entityClass);
            leafNodeExistsSubquery.select(cb.literal(1));
            
            List<Predicate> leafNodeExistsSubqueryPredicates = new ArrayList<>();
            request.getRowGroupCols().stream()
                    .map(col -> {
                        Path<?> subqueryGroupColumnPath = getPath(leafNodeRoot, col.getField());
                        Path<?> parentQueryGroupColumnPath = getPath(root, col.getField());
                        return cb.equal(subqueryGroupColumnPath, parentQueryGroupColumnPath);
                    })
                    .limit(request.getGroupKeys().size() + 1)
                    .forEach(leafNodeExistsSubqueryPredicates::add);
            request.getValueCols().stream()
                    .filter(vc -> request.getFilterModel().containsKey(vc.getField()))
                    .forEach(vc -> {
                        ColDef colDef = this.colDefs.get(vc.getField());
                        Predicate predicate = colDef.getFilter().toPredicate(cb, getPath(leafNodeRoot, vc.getField()), (Map<String, Object>) request.getFilterModel().get(vc.getField()));
                        leafNodeExistsSubqueryPredicates.add(predicate);
                    });
            leafNodeExistsSubquery.where(leafNodeExistsSubqueryPredicates.toArray(Predicate[]::new));
            Predicate leafNodePassedPredicate = cb.exists(leafNodeExistsSubquery);
            
            // go through every group in each level under current key and check aggregations
            List<Predicate> groupPassedPredicate = new ArrayList<>(request.getRowGroupCols().size());
            for (int i = 0; i < request.getRowGroupCols().size(); i++) {
                
                // either aggregate passes
                Subquery<Integer> groupLevelMatchSubquery = query.subquery(Integer.class);
                Root<E> groupLevelRoot = groupLevelMatchSubquery.from(this.entityClass);
                groupLevelMatchSubquery.select(cb.literal(1));
                
                // group by current level
                groupLevelMatchSubquery.groupBy(
                        request.getRowGroupCols().stream()
                                .map(col -> getPath(groupLevelRoot, col.getField()))
                                .limit(i + 1)
                                .collect(Collectors.toList())
                );
                
                // where current level from parent
                groupLevelMatchSubquery.where(
                        request.getRowGroupCols().stream()
                                .map(col -> {
                                    Path<?> subqueryGroupColumnPath = getPath(groupLevelRoot, col.getField());
                                    Path<?> parentQueryGroupColumnPath = getPath(root, col.getField());
                                    return cb.equal(subqueryGroupColumnPath, parentQueryGroupColumnPath);
                                })
                                .limit(i + 1)
                                .collect(Collectors.toList())
                                .toArray(Predicate[]::new)
                );

                List<Predicate> havingConditions = new ArrayList<>();
                // for each valueColumn that is in column filter, apply
                request.getValueCols().stream()
                        .filter(vc -> request.getFilterModel().containsKey(vc.getField()))
                        .forEach(vc -> {
                            // create aggregation expression
                            Expression<?> aggExpr = this.aggFuncs.get(vc.getAggFunc()).apply(cb, getPath(groupLevelRoot, vc.getField()));
                            ColDef colDef = this.colDefs.get(vc.getField());

                            Predicate havingPredicate = colDef.getFilter().toPredicate(cb, aggExpr, (Map<String, Object>) request.getFilterModel().get(vc.getField()));
                            havingConditions.add(havingPredicate);
                        });
                groupLevelMatchSubquery.having(havingConditions.toArray(new Predicate[0]));
                
                groupPassedPredicate.add(cb.exists(groupLevelMatchSubquery));
            }
            

            Predicate groupAggFilteringPredicate = cb.or(
                    leafNodePassedPredicate,
                    cb.or(groupPassedPredicate.toArray(new Predicate[0]))
            );
            
            wherePredicates.add(
                    WherePredicateMetadata
                            .builder(groupAggFilteringPredicate)
                            .build()
            );
            
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

    protected List<WherePredicateMetadata> whereBasic(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        List<WherePredicateMetadata> wherePredicates = new ArrayList<>();

        // external filter
        if (this.isExternalFilterPresent) {
            Predicate externalFilterPredicate = this.doesExternalFilterPass.apply(cb, root, request.getExternalFilter());
            if (externalFilterPredicate != null) {
                wherePredicates.add(
                        WherePredicateMetadata.builder(externalFilterPredicate)
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
                        WherePredicateMetadata.builder(quickFilterPredicate)
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
                        WherePredicateMetadata.builder(advancedFilterPredicate)
                                .isAdvancedFilterPredicate(true)
                                .build()
                );
            } else {
                Predicate columnFilterPredicate = this.createColumnFilterPredicate(cb, root, request.getFilterModel());
                wherePredicates.add(
                        WherePredicateMetadata.builder(columnFilterPredicate)
                                .isColumnFilterPredicate(true)
                                .build()
                );
            }
        }

        return wherePredicates;
    }

    protected List<HavingMetadata> havingGrouping(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        // no need to have 'having' clause in grouping for now
        // filtering on aggregated values is done within where clause by subqueries (need to check all levels of tree)
        return List.of();
    }

    protected List<HavingMetadata> havingPivoting(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
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
    protected Expression<?> createTreeDataGetChildCountExpression(
            QueryContext<E> queryContext,
            Expression<Boolean> hasChildrenPredicate, 
            ServerSideGetRowsRequest request) {
        
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
                if (filterPredicate != null) {
                    predicates.add(filterPredicate);
                }
            }
        }
        countChildrenSubquery.where(cb.and(predicates.toArray(Predicate[]::new)));

        return cb.selectCase().when(hasChildrenPredicate, countChildrenSubquery);  // count when non-leaf node
    }

    /**
     * Creates an expression that aggregates values from all matching children.
     * When node is leaf node, the expression returns the row's own value for that column.
     *
     * @return expression returning the aggregated value for non-leaf nodes, or the own value otherwise.
     */
    protected Expression<?> createTreeDataAggregationExpression(
            QueryContext<E> queryContext, 
            Expression<Boolean> hasChildrenPredicate, 
            ColumnVO aggColumn, 
            ServerSideGetRowsRequest request
    ) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        AbstractQuery<?> query = queryContext.getQuery();
        
        Subquery<?> treeAggregationSubquery = query.subquery(Object.class);
        Root<E> treeAggregationRoot = treeAggregationSubquery.from(this.entityClass);

        var aggregationFunction = this.aggFuncs.get(aggColumn.getAggFunc());
        Expression<?> aggregationSelection = aggregationFunction.apply(cb, getPath(treeAggregationRoot, aggColumn.getField()));
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
                if (filterPredicate != null) {
                    predicates.add(filterPredicate);
                }
            }
        }
        treeAggregationSubquery.where(cb.and(predicates.toArray(Predicate[]::new)));
        
        return cb.selectCase()
                .when(hasChildrenPredicate, treeAggregationSubquery)        // aggregation when non-leaf node
                .otherwise(getPath(root, aggColumn.getField()));            // no aggregation on leaf nodes
    }

    /**
     * Creates predicate that checks if any parent in the current path matches the filter criteria.
     *
     * @param request The grid request containing the active filters and group keys.
     * @param queryContext Helper for tracking query state and parameters.
     * @return A condition (Predicate) that is true if at least one parent matches the filters.
     */
    protected Predicate createTreeDataParentMatchPredicate(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
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
            if (filterPredicate != null) {
                predicates.add(filterPredicate);
            }
        }
        
        parentMatchSubquery
                .select(cb.literal(1))
                .where(
                    parentPrimaryKeyPredicate, 
                    cb.and(predicates.toArray(Predicate[]::new))
                );
        
        return cb.exists(parentMatchSubquery);
    }


    protected Predicate createTreeDataOwnDataPredicate(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
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
            if (filterPredicate != null) {
                predicates.add(filterPredicate);
            }
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
    protected Predicate createTreeDataChildrenMatchPredicate(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
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
            if (filterPredicate != null) {
                predicates.add(filterPredicate);
            }
        }

        childrenMatchSubquery
                .select(cb.literal(1))
                .where(
                        childrenPathPredicate,
                        cb.and(predicates.toArray(Predicate[]::new))
                );

        return cb.exists(childrenMatchSubquery);
    }

    protected List<GroupingMetadata> groupByTreeData(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        // tree data does not have any grouping
        return List.of();
    }

    protected List<GroupingMetadata> groupByMasterDetail(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        if (request.getRowGroupCols().isEmpty()) {
            // no grouping
            return List.of();
        } else {
            return this.groupByGrouping(queryContext, request);
        }
    }

    protected List<GroupingMetadata> groupByPivoting(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        Root<E> root = queryContext.getRoot();
        
        List<GroupingMetadata> groupings = new ArrayList<>();
        // there is always grouping in pivoting
        for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
            String groupCol = request.getRowGroupCols().get(i).getField();
            GroupingMetadata groupingMetadata = GroupingMetadata
                    .builder(getPath(root, groupCol))
                    .column(groupCol)
                    .build();

            groupings.add(groupingMetadata);
        }
        
        return groupings;
    }
    
    protected List<GroupingMetadata> groupByGrouping(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        Root<E> root = queryContext.getRoot();
        
        List<GroupingMetadata> groupings = new ArrayList<>();
        // master-detail can be used together with groups
        boolean hasUnexpandedGroups = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (hasUnexpandedGroups) {
            for (int i = 0; i < request.getRowGroupCols().size() && i < request.getGroupKeys().size() + 1; i++) {
                String groupCol = request.getRowGroupCols().get(i).getField();
                GroupingMetadata groupingMetadata = GroupingMetadata
                        .builder(getPath(root, groupCol))
                        .column(groupCol)
                        .build();

                groupings.add(groupingMetadata);
            }
        }

        return groupings;
    }
    
    protected List<OrderMetadata> orderByBasic(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
        return request.getSortModel().stream()
                .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))
                .map(model -> {
                    Expression<?> field = getPath(root, model.getColId());
                    Order order = model.getSort() == SortType.asc ? cb.asc(field) : cb.desc(field);
                    return OrderMetadata.builder(order)
                            .colId(model.getColId())
                            .build();
                })
                .collect(Collectors.toList());
    }

    protected List<OrderMetadata> orderByTreeData(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        return this.orderByBasic(queryContext, request);
    }

    protected List<OrderMetadata> orderByMasterDetail(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        return this.orderByBasic(queryContext, request);
    }

    
    protected List<OrderMetadata> orderByGrouping(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();

        boolean hasUnexpandedGroups = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (hasUnexpandedGroups) {
            // order groups
            Stream<OrderMetadata> groupOrders = request.getSortModel().stream()
                    .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))    // ignore auto-generated column
                    .filter(model -> request.getRowGroupCols().stream().anyMatch(rg -> rg.getField().equals(model.getColId()))) // present in group columns
                    .map(sortModel -> {
                        Expression<?> groupingColumnExpression = getPath(root, sortModel.getColId());
                        Order order = sortModel.getSort() == SortType.asc ? cb.asc(groupingColumnExpression) : cb.desc(groupingColumnExpression);
                        return OrderMetadata.builder(order)
                                .colId(sortModel.getColId())
                                .build();
                    })
                    .limit(request.getGroupKeys().size() + 1);   // don't order groups that are not expanded yet
            
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
                        
                        Order order = sortModelAgg.getSort() == SortType.asc ? cb.asc(aggregationExpression) : cb.desc(aggregationExpression);

                        return OrderMetadata.builder(order)
                                .colId(sortModelAgg.getColId())
                                .build();
                    });
            
            return Stream.concat(groupOrders, aggregationOrders).collect(Collectors.toList());
        } else {
            // all groups expanded, we are on the leaf level, basic ordering
            return orderByBasic(queryContext, request);
        }
    }

    protected List<OrderMetadata> orderByPivoting(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        PivotingContext pivotingContext = queryContext.getPivotingContext();
        
        // pivoting groups orders
        Stream<OrderMetadata> pivotingGroupOrders = request.getSortModel().stream()
                .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))    // ignore auto-generated column
                .filter(model -> request.getRowGroupCols().stream().anyMatch(rg -> rg.getField().equals(model.getColId()))) // present in group columns
                .map(sortModel -> {
                    Expression<?> groupingColumnExpression = getPath(root, sortModel.getColId());
                    Order order = sortModel.getSort() == SortType.asc ? cb.asc(groupingColumnExpression) : cb.desc(groupingColumnExpression);
                    return OrderMetadata.builder(order)
                            .colId(sortModel.getColId())
                            .build();
                })
                .limit(request.getGroupKeys().size() + 1);   // don't order groups that are not expanded yet
        
        // pivoting aggregations columns orders
        Stream<OrderMetadata> pivotingAggregationOrders = request.getSortModel().stream()
                .filter(model -> !AUTO_GROUP_COLUMN_NAME.equalsIgnoreCase(model.getColId()))
                .filter(model -> pivotingContext.getColumnNamesToExpression().containsKey(model.getColId()))   // pivoting selection
                .map(sortModel -> {
                    Expression<?> pivotingExpression = pivotingContext.getColumnNamesToExpression().get(sortModel.getColId());
                    Order order = sortModel.getSort() == SortType.asc ? cb.asc(pivotingExpression) : cb.desc(pivotingExpression);
                    return OrderMetadata.builder(order)
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
    protected void limitOffset(QueryContext<E> queryContext, ServerSideGetRowsRequest request) {
        queryContext.setFirstResult(request.getStartRow());
        queryContext.setMaxResults(request.getEndRow() - request.getStartRow());
    }

    /**
     * Converts a list of JPA {@link Tuple} objects to a list of maps keyed by their aliases.
     *
     * @param tuples the list of JPA tuples to convert
     * @return a list of maps where each map represents a tuple with alias-value pairs
     */
    protected List<Map<String, Object>> tupleToMap(List<Tuple> tuples) {
        if (tuples == null || tuples.isEmpty()) {
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
                    continue;
                }

                // simple scenario
                map.put(alias, value);
            }
            result.add(map);
        }
        
        return result;
    }
    
    protected Predicate createAdvancedFilterPredicate(CriteriaBuilder cb, Root<E> root, Map<String, Object> filterModel) {
        if (this.isColumnFilter(filterModel)) {
            throw new IllegalArgumentException("Can not create advanced filter when filter is in column-filter format");
        }

        AdvancedFilterModel advancedFilterModel = this.recognizeAdvancedFilter(filterModel);
        return advancedFilterModel.toPredicate(cb, root);
    }
    
    protected Predicate createColumnFilterPredicate(CriteriaBuilder cb, Root<E> root, Map<String, Object> filterModel) {
        if (!this.isColumnFilter(filterModel)) {
            throw new IllegalArgumentException("Can not create column filter when filter is not in column-filter format");
        }
        
        List<Predicate> predicates = new ArrayList<>(filterModel.size());
        for (var entry : filterModel.entrySet()) {
            String columnName = entry.getKey();
            Map<String, Object> filterMap = (Map<String, Object>) entry.getValue();

            // find col def
            ColDef colDef = Optional.ofNullable(this.colDefs.get(columnName))
                    .orElseThrow(() -> new IllegalArgumentException("Column " + columnName + " not found in col defs"));
            // filter of given column
            IFilter<?, ?> filter = colDef.getFilter();
            if (filter == null) {
                throw new IllegalArgumentException("Column " + columnName + " is not filterable field!");
            }
            // predicate from filter
            Predicate predicate = filter.toPredicate(cb, getPath(root, columnName), filterMap);
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
    protected AdvancedFilterModel recognizeAdvancedFilter(Map<String, Object> filter) {
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

    protected PivotingContext createPivotingContext(QueryContext<E> queryContext, ServerSideGetRowsRequest request) throws OnPivotMaxColumnsExceededException {
        CriteriaBuilder cb = queryContext.getCriteriaBuilder();
        Root<E> root = queryContext.getRoot();
        
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
            // result fields are column names
            List<String> pivotingResultFields = new ArrayList<>(columnNamesToExpression.keySet());

            pivotingContext.setPivotValues(pivotValues);
            pivotingContext.setPivotPairs(pivotPairs);
            pivotingContext.setCartesianProduct(cartesianProduct);
            pivotingContext.setColumnNamesToExpression(columnNamesToExpression);
            pivotingContext.setPivotingResultFields(pivotingResultFields);
        }

        return pivotingContext;
    }

    /**
     * For each pivoting column fetch distinct values
     * @param cb        criteria builder
     * @param request   request
     * @return map where key is column name and value is distinct column values
     */
    protected Map<String, List<Object>> getPivotValues(CriteriaBuilder cb, ServerSideGetRowsRequest request) {
        Map<String, List<Object>> pivotValues = new LinkedHashMap<>(request.getPivotCols().size());
        for (ColumnVO column : request.getPivotCols()) {
            String field = column.getField();

            CriteriaQuery<Object> query = cb.createQuery(Object.class);
            Root<E> root = query.from(this.entityClass);

            // select
            query.multiselect(getPath(root, field)).distinct(true);
            query.orderBy(cb.asc(getPath(root, field)));

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
    protected List<Set<Pair<String, Object>>> createPivotPairs(Map<String, List<Object>> pivotValues) {
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
    protected String originalColNameFromPivoted(String pivotedName) {
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
    protected long countPivotColumnsToBeGenerated(CriteriaBuilder cb, ServerSideGetRowsRequest request) {
        if (!request.isPivotMode() || request.getPivotCols().isEmpty()) {
            return 0;
        }

        CriteriaQuery<Long> mainQuery = cb.createQuery(Long.class);

        Expression<Long> productExpression = cb.literal(1L);
        for (ColumnVO pivotCol : request.getPivotCols()) {
            // Subquery for count(distinct <field>)
            Subquery<Long> subquery = mainQuery.subquery(Long.class);
            Root<E> subRoot = subquery.from(this.entityClass);
            subquery.select(cb.countDistinct(getPath(subRoot, pivotCol.getField())));

            productExpression = cb.prod(productExpression, subquery.getSelection());
        }

        mainQuery.select(productExpression);

        return this.entityManager.createQuery(mainQuery).getSingleResult();
    }

    protected Map<String, Expression<?>> createPivotingExpressions(CriteriaBuilder cb, Root<?> root, ServerSideGetRowsRequest request, List<List<Pair<String, Object>>> cartesianProduct) {
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

                        Path<?> field = getPath(root, columnVO.getField());

                        CriteriaBuilder.Case<?> caseExpression = null;
                        for (Pair<String, Object> pair : pairs) {
                            if (caseExpression == null) {
                                caseExpression = cb.selectCase()
                                        .when(cb.equal(getPath(root, pair.getKey()), pair.getValue()), field);
                            } else {
                                caseExpression = cb.selectCase()
                                        .when(cb.equal(getPath(root, pair.getKey()), pair.getValue()), caseExpression);
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
    
    public static class Builder<E> {
        private static final String DEFAULT_SERVER_SIDE_PIVOT_RESULT_FIELD_SEPARATOR = "_";
        private static final Function<String, List<String>> DEFAULT_QUICK_FILTER_PARSER = (input) -> Arrays.asList(input.trim().split("\\s+")); 
        
        private final Class<E> entityClass;
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
        protected List<String> quickFilterSearchInFields;
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
        private MasterDetailParams masterDetailParams;
        private Function<Map<String, Object>, MasterDetailParams> dynamicMasterDetailParams;
        
        private Map<String, ColDef> colDefs;


        protected Builder(Class<E> entityClass, EntityManager entityManager) {
            this.entityClass = entityClass;
            this.entityManager = entityManager;
        }
        
        public Builder<E> primaryFieldName(String primaryFieldName) {
            this.primaryFieldName = primaryFieldName;
            return this;
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

        public Builder<E> suppressFieldDotNotation(boolean suppressFieldDotNotation) {
            this.suppressFieldDotNotation = suppressFieldDotNotation;
            return this;
        }

        public Builder<E> isQuickFilterPresent(boolean isQuickFilterPresent) {
            this.isQuickFilterPresent = isQuickFilterPresent;
            return this;
        }

        public Builder<E> quickFilterParser(Function<String, List<String>> quickFilterParser) {
            this.quickFilterParser = quickFilterParser;
            return this;
        }

        public Builder<E> quickFilterMatcher(TriFunction<CriteriaBuilder, Root<E>, List<String>, Predicate> quickFilterMatcher) {
            this.quickFilterMatcher = quickFilterMatcher;
            return this;
        }

        public Builder<E> quickFilterSearchInFields(List<String> quickFilterSearchInFields) {
            this.quickFilterSearchInFields = quickFilterSearchInFields;
            return this;
        }

        public Builder<E> quickFilterSearchInFields(String... quickFilterSearchInFields) {
            this.quickFilterSearchInFields = Arrays.asList(quickFilterSearchInFields);
            return this;
        }

        public Builder<E> quickFilterTrimInput(boolean quickFilterTrimInput) {
            this.quickFilterTrimInput = quickFilterTrimInput;
            return this;
        }

        public Builder<E> quickFilterCaseSensitive(boolean quickFilterCaseSensitive) {
            this.quickFilterCaseSensitive = quickFilterCaseSensitive;
            return this;
        }

        public Builder<E> quickFilterTextFormatter(BiFunction<CriteriaBuilder, Expression<String>, Expression<String>> quickFilterTextFormatter) {
            this.quickFilterTextFormatter = quickFilterTextFormatter;
            return this;
        }
        
        public Builder<E> suppressAggFilteredOnly(boolean suppressAggFilteredOnly) {
            this.suppressAggFilteredOnly = suppressAggFilteredOnly;
            return this;
        }

        public Builder<E> getChildCount(boolean getChildCount) {
            this.getChildCount = getChildCount;
            return this;
        }

        public Builder<E> getChildCountFieldName(String getChildCountFieldName) {
            this.getChildCountFieldName = getChildCountFieldName;
            return this;
        }
        
        public Builder<E> isExternalFilterPresent(boolean isExternalFilterPresent) {
            this.isExternalFilterPresent = isExternalFilterPresent;
            return this;
        }
        
        public Builder<E> doesExternalFilterPass(TriFunction<CriteriaBuilder, Root<E>, Object, Predicate> doesExternalFilterPass) {
            this.doesExternalFilterPass = doesExternalFilterPass;
            return this;
        }
        
        
        public Builder<E> groupAggFiltering(boolean groupAggFiltering) {
            this.groupAggFiltering = groupAggFiltering;
            return this;
        }
        
        public Builder<E> treeData(boolean treeData) {
            this.treeData = treeData;
            return this;
        }
        
        public Builder<E> isServerSideGroupFieldName(String isServerSideGroupFieldName) {
            this.isServerSideGroupFieldName = isServerSideGroupFieldName;
            return this;
        }

        public Builder<E> treeDataParentReferenceField(String treeDataParentReferenceField) {
            this.treeDataParentReferenceField = treeDataParentReferenceField;
            return this;
        }

        public Builder<E> treeDataParentIdField(String treeDataParentIdField) {
            this.treeDataParentIdField = treeDataParentIdField;
            return this;
        }

        public Builder<E> treeDataChildrenField(String treeDataChildrenField) {
            this.treeDataChildrenField = treeDataChildrenField;
            return this;
        }

        public Builder<E> treeDataDataPathFieldName(String treeDataDataPathFieldName) {
            this.treeDataDataPathFieldName = treeDataDataPathFieldName;
            return this;
        }

        public Builder<E> treeDataDataPathSeparator(String treeDataDataPathSeparator) {
            this.treeDataDataPathSeparator = treeDataDataPathSeparator;
            return this;
        }
        
        public Builder<E> masterDetail(boolean masterDetail) {
            this.masterDetail = masterDetail;
            return this;
        }

        public Builder<E> masterDetailLazy(boolean masterDetailLazy) {
            this.masterDetailLazy = masterDetailLazy;
            return this;
        }
        
        public Builder<E> masterDetailRowDataFieldName(String masterDetailRowDataFieldName) {
            this.masterDetailRowDataFieldName = masterDetailRowDataFieldName;
            return this;
        }

        public Builder<E> masterDetailParams(MasterDetailParams masterDetailParams) {
            this.masterDetailParams = masterDetailParams;
            return this;
        }

        public Builder<E> dynamicMasterDetailParams(Function<Map<String, Object>, MasterDetailParams> dynamicMasterDetailParams) {
            this.dynamicMasterDetailParams = dynamicMasterDetailParams;
            return this;
        }
        
        public Builder<E> registerCustomAggFunction(String name, BiFunction<CriteriaBuilder, Expression<?>, Expression<?>> function) {
            this.aggFuncs.put(name, function);
            return this;
        }

        public QueryBuilder<E> build() {
            this.validateBeforeBuild();
            return new QueryBuilder<>(this);
        }
        
        private void validateBeforeBuild() {
            // colDefs args validation
            if (this.colDefs == null || this.colDefs.isEmpty()) {
                throw new IllegalArgumentException("colDefs cannot be null or empty");
            }
            // validate col defs aggregation functions
            List<ColDef> colDefsWithUnrecognizedAggFunctions = this.colDefs.values().stream()
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
                                    cd.getField(),
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
    
    public static class MasterDetailParams {
        
        private final Class<?> detailClass;
        private final Map<String, ColDef> detailColDefs;
        private final String detailMasterReferenceField;
        private final String detailMasterIdField;
        private final TriFunction<CriteriaBuilder, Root<?>, Map<String, Object>, Predicate> createMasterRowPredicate;
        
        public static Builder builder() {
            return new Builder();
        }

        private MasterDetailParams(Builder builder) {
            this.detailClass = builder.detailClass;
            this.detailColDefs = builder.detailColDefs;
            this.detailMasterReferenceField = builder.detailMasterReferenceField;
            this.detailMasterIdField = builder.detailMasterIdField;
            this.createMasterRowPredicate = builder.createMasterRowPredicate;
        }

        public static class Builder {
            private Class<?> detailClass;
            private Map<String, ColDef> detailColDefs;
            private String detailMasterReferenceField;
            private String detailMasterIdField;
            private TriFunction<CriteriaBuilder, Root<?>, Map<String, Object>, Predicate> createMasterRowPredicate;
            
            private Builder() {}

            public Builder detailClass(Class<?> detailClass) {
                this.detailClass = detailClass;
                return this;
            }

            public Builder detailColDefs(ColDef ...colDefs) {
                this.detailColDefs = new HashMap<>(colDefs.length);
                for (ColDef colDef : colDefs) {
                    this.detailColDefs.put(colDef.getField(), colDef);
                }
                return this;
            }

            public Builder detailColDefs(Collection<ColDef> colDefs) {
                this.detailColDefs = new HashMap<>(colDefs.size());
                for (ColDef colDef : colDefs) {
                    this.detailColDefs.put(colDef.getField(), colDef);
                }
                return this;
            }

            public Builder detailMasterReferenceField(String detailMasterReferenceField) {
                this.detailMasterReferenceField = detailMasterReferenceField;
                return this;
            }

            public Builder detailMasterIdField(String detailMasterIdField) {
                this.detailMasterIdField = detailMasterIdField;
                return this;
            }

            public Builder createMasterRowPredicate(TriFunction<CriteriaBuilder, Root<?>, Map<String, Object>, Predicate> createMasterRowPredicate) {
                this.createMasterRowPredicate = createMasterRowPredicate;
                return this;
            }

            public MasterDetailParams build() {
                this.validateMasterDetailArgs();
                return new MasterDetailParams(this);
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

        public Class<?> getDetailClass() {
            return detailClass;
        }
        
        public Map<String, ColDef> getDetailColDefs() {
            return detailColDefs;
        }

        public String getDetailMasterReferenceField() {
            return detailMasterReferenceField;
        }

        public String getDetailMasterIdField() {
            return detailMasterIdField;
        }

        public TriFunction<CriteriaBuilder, Root<?>, Map<String, Object>, Predicate> getCreateMasterRowPredicate() {
            return createMasterRowPredicate;
        }
    }
    
    
}
