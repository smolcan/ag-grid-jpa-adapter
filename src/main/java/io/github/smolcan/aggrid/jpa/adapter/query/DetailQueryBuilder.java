package io.github.smolcan.aggrid.jpa.adapter.query;

import io.github.smolcan.aggrid.jpa.adapter.query.metadata.QueryContext;
import io.github.smolcan.aggrid.jpa.adapter.query.metadata.WherePredicateMetadata;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.utils.TypeValueSynchronizer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.*;

import java.util.List;
import java.util.Map;

public class DetailQueryBuilder<D, M> extends QueryBuilder<D> {

    private final Class<D> detailClass;
    private final Class<M> masterClass;

    private final String masterPrimaryFieldName;
    private final String detailMasterReferenceField;
    private final String detailMasterIdField;
    private final CreateMasterRowPredicate<D> createMasterRowPredicate;
    
    public static <D, M> Builder<D, M> builder(Class<D> detailClass, Class<M> masterClass, EntityManager entityManager) {
        return new Builder<>(detailClass, masterClass, entityManager);
    }
    
    public DetailQueryBuilder(Builder<D, M> builder) {
        super(builder);
        this.detailClass = builder.detailClass;
        this.masterClass = builder.masterClass;
        this.masterPrimaryFieldName = builder.masterPrimaryFieldName;
        this.detailMasterReferenceField = builder.detailMasterReferenceField;
        this.detailMasterIdField = builder.detailMasterIdField;
        this.createMasterRowPredicate = builder.createMasterRowPredicate;
    }


    @Override
    public LoadSuccessParams getDetailRowData(ServerSideGetRowsRequest request, Map<String, Object> masterRow) {
        this.validateRequest(request);

        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<D> root = query.from(this.detailClass);

        // record all the context we put into query
        QueryContext queryContext = new QueryContext();
        // if pivoting, load all information needed for pivoting into pivoting context
        queryContext.setPivotingContext(this.createPivotingContext(cb, root, request));

        this.select(cb, root, request, queryContext);
        this.where(cb, root, request, queryContext);
        this.applyMasterRowPredicate(cb, root, masterRow, queryContext);
        this.groupBy(cb, root, request, queryContext);
        this.having(cb, request, queryContext);
        this.orderBy(cb, request, queryContext);
        this.limitOffset(request, queryContext);
        
        List<Tuple> data = this.apply(query, queryContext);
        LoadSuccessParams loadSuccessParams = new LoadSuccessParams();
        loadSuccessParams.setRowData(this.tupleToMap(data));
        loadSuccessParams.setPivotResultFields(queryContext.getPivotingContext().getPivotingResultFields());
        return loadSuccessParams;
    }

    protected void applyMasterRowPredicate(CriteriaBuilder cb, Root<D> root, Map<String, Object> masterRow, QueryContext queryContext) {
        // add to wherePredicates predicate for parent
        Predicate parentPredicate;
        if (this.createMasterRowPredicate != null) {
            // must have provided predicate function
            parentPredicate = this.createMasterRowPredicate.apply(cb, root, masterRow);
        } else {
            Object masterIdValue = masterRow.get(this.masterPrimaryFieldName);
            if (masterIdValue == null) {
                throw new IllegalArgumentException(
                        String.format("Master row data is missing value for primary field '%s'. Ensure this field is included in Master Grid columns.", this.masterPrimaryFieldName)
                );
            }

            Path<?> pathToCheck;
            if (this.detailMasterReferenceField != null) {
                Path<M> masterPath = root.get(this.detailMasterReferenceField);
                pathToCheck = masterPath.get(this.masterPrimaryFieldName);
            } else {
                pathToCheck = root.get(this.detailMasterIdField);
            }

            TypeValueSynchronizer.Result<?> sync = TypeValueSynchronizer.synchronizeTypes(pathToCheck, String.valueOf(masterIdValue));
            parentPredicate = cb.equal(sync.getSynchronizedPath(), sync.getSynchronizedValue());
        }
        
        queryContext.getWherePredicates()
                .add(
                        0,
                        WherePredicateMetadata.builder(parentPredicate)
                                .isMasterDetailPredicate(true)
                                .build()
                );
    }

    public static class Builder<D, M> extends QueryBuilder.Builder<D> {
        
        private final Class<D> detailClass;
        private final Class<M> masterClass;

        private String masterPrimaryFieldName;
        private String detailMasterReferenceField;
        private String detailMasterIdField;
        private CreateMasterRowPredicate<D> createMasterRowPredicate;
        
        
        private Builder(Class<D> detailClass, Class<M> masterClass, EntityManager entityManager) {
            super(detailClass, entityManager);
            this.detailClass = detailClass;
            this.masterClass = masterClass;
        }

        public Builder<D, M> masterPrimaryFieldName(String masterPrimaryFieldName) {
            this.masterPrimaryFieldName = masterPrimaryFieldName;
            return this;
        }
        
        public Builder<D, M> detailMasterReferenceField(String detailMasterReferenceField) {
            this.detailMasterReferenceField = detailMasterReferenceField;
            return this;
        }

        public Builder<D, M> detailMasterIdField(String detailMasterIdField) {
            this.detailMasterIdField = detailMasterIdField;
            return this;
        }
        
        public Builder<D, M> createMasterRowPredicate(CreateMasterRowPredicate<D> createMasterRowPredicate) {
            this.createMasterRowPredicate = createMasterRowPredicate;
            return this;
        }
        
    }
    
    
    @FunctionalInterface
    public interface CreateMasterRowPredicate<D> {
        Predicate apply(CriteriaBuilder cb, Root<D> detailRoot, Map<String, Object> masterRow);
    }
    
}
