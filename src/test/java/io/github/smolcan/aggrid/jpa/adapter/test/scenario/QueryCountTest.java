package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder.MasterDetailParams;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import io.github.smolcan.aggrid.jpa.adapter.test.infrastructure.CountingDriver;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression pins on how many SQL statements one adapter call issues,
 * so refactors cannot silently introduce N+1 behaviour. Counting happens in the
 * JDBC driver, so the pins hold for every provider in the matrix.
 */
class QueryCountTest extends ScenarioTestBase {

    private long statementsIssuedBy(Runnable action) {
        return CountingDriver.countStatements(action);
    }

    @Test
    void basicGetRowsIssuesSingleQuery() {
        QueryBuilder<Trade, Long, Void> queryBuilder = defaultQueryBuilder();
        long statements = statementsIssuedBy(() -> queryBuilder.getRows(sortedByIdRequest(0, 100)));
        assertThat(statements).isEqualTo(1);
    }

    @Test
    void countRowsIssuesSingleQuery() {
        QueryBuilder<Trade, Long, Void> queryBuilder = defaultQueryBuilder();
        long statements = statementsIssuedBy(() -> queryBuilder.countRows(emptyRequest(0, 100)));
        assertThat(statements).isEqualTo(1);
    }

    @Test
    void groupedGetRowsIssuesSingleQuery() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build()
                )
                .build();
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setFilterModel(new HashMap<>());
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getValueCols().add(valueCol("currentValue", "sum"));

        long statements = statementsIssuedBy(() -> queryBuilder.getRows(request));
        assertThat(statements).isEqualTo(1);
    }

    @Test
    void eagerMasterDetailBatchesDetailsIntoOneExtraQuery() {
        QueryBuilder<Product, Long, Trade> queryBuilder = QueryBuilder.builder(Product.class, Product_.productId, Trade.class, entityManager)
                .colDefs(
                        ColDef.builder(Product_.productId).build(),
                        ColDef.builder(Product_.name).build()
                )
                .masterDetailParams(MasterDetailParams.<Product, Long, Trade>builder()
                        .detailClass(Trade.class)
                        .detailColDefs(ColDef.builder(Trade_.tradeId).build())
                        .detailMasterReferenceField(Trade_.product)
                        .build())
                .masterDetailLazy(false)
                .masterDetailRowDataFieldName("detailRows")
                .build();

        long statements = statementsIssuedBy(() -> queryBuilder.getRows(emptyRequest(0, 100)));
        // one query for masters, one IN-query for all details (no N+1)
        assertThat(statements).isEqualTo(2);
    }

    @Test
    void pivotingIssuesOneValuesQueryPerPivotColPlusMainQuery() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.portfolio).enableRowGroup(true, key -> key).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).enablePivot(true).build()
                )
                .build();
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.setPivotMode(true);
        request.getRowGroupCols().add(groupCol("portfolio"));
        request.getPivotCols().add(groupCol("product.name"));
        request.getValueCols().add(valueCol("currentValue", "sum"));

        long statements = statementsIssuedBy(() -> queryBuilder.getRows(request));
        assertThat(statements).isEqualTo(2);
    }
}
