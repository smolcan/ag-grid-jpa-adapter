package com.aggrid.jpa.adapter.query;

import com.aggrid.jpa.adapter.request.ColumnVO;
import com.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import com.aggrid.jpa.adapter.request.filter.FilterModel;
import com.aggrid.jpa.adapter.request.filter.advanced.AdvancedFilterModel;
import com.aggrid.jpa.adapter.request.filter.simple.*;
import com.aggrid.jpa.adapter.response.LoadSuccessParams;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Integer.MAX_VALUE;

public class QueryBuilder<E> {
    
    private final Class<E> entityClass;
    private final EntityManager entityManager;
    
    public QueryBuilder(Class<E> entityClass, EntityManager entityManager) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
        
    }
    
    public LoadSuccessParams getRows(ServerSideGetRowsRequest request) {
        CriteriaBuilder cb = this.entityManager.getCriteriaBuilder();

        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<E> root = query.from(this.entityClass);
        
        this.select(cb, query, root, request);
        this.where(cb, query, root, request);
        this.groupBy(cb, query, root, request);
        this.orderBy(cb, query, root, request);

        TypedQuery<Tuple> typedQuery = this.entityManager.createQuery(query);
        this.limitOffset(typedQuery, request);

        List<Tuple> data = typedQuery.getResultList();
        
        LoadSuccessParams loadSuccessParams = new LoadSuccessParams();
        loadSuccessParams.setRowData(tupleToMap(data));
        return loadSuccessParams;
    }
    
    
    private void select(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
        // select
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        if (!isGrouping) {
            // SELECT * from root if not grouping
            this.selectAll(cb, query, root);
            return;
        }

        List<Selection<?>> selections = new ArrayList<>();
        // group columns
        for (ColumnVO groupCol : request.getRowGroupCols()) {
            selections.add(root.get(groupCol.getField()).alias(groupCol.getField()));
        }
        // aggregated columns
        for (ColumnVO columnVO : request.getValueCols()) {
            Expression<?> aggregatedField;
            switch (columnVO.getAggFunc()) {
                case avg -> {
                    aggregatedField = cb.avg(root.get(columnVO.getField()));
                }
                case sum -> {
                    aggregatedField = cb.sum(root.get(columnVO.getField()));
                }
                case min -> {
                    aggregatedField = cb.min(root.get(columnVO.getField()));
                }
                case max -> {
                    aggregatedField = cb.max(root.get(columnVO.getField()));
                }
                case count -> {
                    aggregatedField = cb.count(root.get(columnVO.getField()));
                }
                default -> {
                    throw new IllegalArgumentException("unsupported aggregation function: " + columnVO.getAggFunc());
                }
            }
            selections.add(aggregatedField.alias(columnVO.getField()));
        }

        query.multiselect(selections);
    }
    
    private void selectAll(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root) {
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
    
    
    private void where(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
        // where
        List<Predicate> predicates = new ArrayList<>();
        // grouping where
        for (int i = 0; i < request.getGroupKeys().size() && i < request.getRowGroupCols().size(); i++) {
            String groupKey = request.getGroupKeys().get(i);
            String groupCol = request.getRowGroupCols().get(i).getField();

            Predicate groupPredicate = cb.equal(root.get(groupCol), groupKey);
            predicates.add(groupPredicate);
        }
        

        // filter where
        if (request.getFilterModel() != null) {
            // simple filter has format {colId: {...filter object}}
            boolean isSimpleFilter = request.getFilterModel().values().stream().allMatch(value -> value instanceof Map);
            
            if (isSimpleFilter) {
                // simple filter
                request.getFilterModel().forEach((colId, filter) -> {
                    FilterModel simpleFilter = parseSimpleFilter((Map<String, Object>) filter);
                    // todo: simple filter
                });
            } else {
                // advanced filter
                AdvancedFilterModel advancedFilter = parseAdvancedFilter(request.getFilterModel());
                // todo: advanced filter
            }
        }
        
        query.where(predicates.toArray(new Predicate[0]));
    }
    
    private void groupBy(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
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
    
    private void orderBy(CriteriaBuilder cb, CriteriaQuery<Tuple> query, Root<E> root, ServerSideGetRowsRequest request) {
        boolean isGrouping = request.getRowGroupCols().size() > request.getGroupKeys().size();
        int limit = isGrouping ? request.getGroupKeys().size() + 1 : MAX_VALUE;
        
        List<Order> orderByCols = request.getSortModel().stream()
                .filter(model -> {
                    if (isGrouping) {
                        return
                                request.getRowGroupCols().stream().anyMatch(c -> c.getField().equals(model.getColId()))
                                || request.getValueCols().stream().anyMatch(c -> c.getField().equals(model.getColId()));
                    } else {
                        try {
                            root.get(model.getColId());
                            return true;
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }
                })
                .filter(Objects::nonNull)
                .map(sortModel -> {
                    Path<?> field = root.get(sortModel.getColId());
                    if (sortModel.getSort().equalsIgnoreCase("asc")) {
                        return cb.asc(root.get(sortModel.getColId()));
                    } else if (sortModel.getSort().equalsIgnoreCase("desc")) {
                        return cb.desc(root.get(sortModel.getColId()));
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .limit(limit)
                .toList();
        
        query.orderBy(orderByCols);
    }
    
    private void limitOffset(TypedQuery<Tuple> typedQuery, ServerSideGetRowsRequest request) {
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
                .toList();
    }
    
    
    private static FilterModel parseSimpleFilter(Map<String, Object> data) {
        String filterType = Optional.ofNullable(data.get("filterType")).map(Object::toString).orElseThrow(() -> new IllegalArgumentException("no filter type found"));
        switch (filterType) {
            case "date" -> {
                DateFilterModel dateFilterModel = new DateFilterModel();
                dateFilterModel.setType(SimpleFilterModelType.valueOf(data.get("type").toString()));

                LocalDateTime dateFrom = Optional.ofNullable(data.get("dateFrom")).map(Object::toString).map(v -> LocalDateTime.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).orElse(null);
                dateFilterModel.setDateFrom(dateFrom);
                
                LocalDateTime dateTo = Optional.ofNullable(data.get("dateTo")).map(Object::toString).map(v -> LocalDateTime.parse(v, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).orElse(null);
                dateFilterModel.setDateTo(dateTo);
                
                return dateFilterModel;
            }
            case "number" -> {
                NumberFilterModel numberFilterModel = new NumberFilterModel();
                numberFilterModel.setType(SimpleFilterModelType.valueOf(data.get("type").toString()));

                BigDecimal filter = Optional.ofNullable(data.get("filter")).map(Object::toString).map(BigDecimal::new).orElse(null);
                numberFilterModel.setFilter(filter);
                
                BigDecimal filterTo = Optional.ofNullable(data.get("filterTo")).map(Object::toString).map(BigDecimal::new).orElse(null);
                numberFilterModel.setFilterTo(filterTo);
                
                return numberFilterModel;
            }
            case "set" -> {
                SetFilterModel setFilterModel = new SetFilterModel();
                
                List<String> values = Optional.ofNullable(data.get("values")).map(o -> (List<String>) o).orElse(null);
                setFilterModel.setValues(values);
                
                return setFilterModel;
            }
            case "text" -> {
                TextFilterModel textFilterModel = new TextFilterModel();
                textFilterModel.setType(SimpleFilterModelType.valueOf(data.get("type").toString()));

                String filter = Optional.ofNullable(data.get("filter")).map(Object::toString).orElse(null);
                textFilterModel.setFilter(filter);

                String filterTo = Optional.ofNullable(data.get("filterTo")).map(Object::toString).orElse(null);
                textFilterModel.setFilterTo(filterTo);
                
                return textFilterModel;
            }
            default -> {
                throw new UnsupportedOperationException("unsupported filter type: " + data.get("type") + " data: " + data);
            }
        }
    }
    
    
    private static AdvancedFilterModel parseAdvancedFilter(Map<String, Object> data) {
        return null;
    }
    
}
