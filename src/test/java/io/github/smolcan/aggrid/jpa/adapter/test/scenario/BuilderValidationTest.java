package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder.MasterDetailParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuilderValidationTest extends ScenarioTestBase {

    @Test
    void rejectsUnknownAggFunctionInColDef() {
        assertThatThrownBy(() -> QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.currentValue).enableValue(true).allowedAggFuncs("bogus").build()
                )
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unrecognized aggregation functions");
    }

    @Test
    void acceptsColDefsAsCollection() {
        QueryBuilder<Trade, Long, Void> queryBuilder = QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(java.util.List.of(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).build()
                ))
                .build();

        assertThat(queryBuilder.getRows(sortedByIdRequest(0, 100)).getRowData()).hasSize(12);
    }

    @Test
    void rejectsMissingColDefs() {
        assertThatThrownBy(() -> QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager).build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("colDefs");
    }

    @Test
    void rejectsChildCountWithoutFieldName() {
        assertThatThrownBy(() -> QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(ColDef.builder(Trade_.tradeId).build())
                .getChildCount(true)
                .build())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rejectsChildCountFieldCollidingWithColDef() {
        assertThatThrownBy(() -> QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(ColDef.builder(Trade_.tradeId).build())
                .getChildCount(true)
                .getChildCountFieldName("tradeId")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("collides");
    }

    @Test
    void rejectsIncompleteTreeDataConfiguration() {
        assertThatThrownBy(() -> QueryBuilder.builder(Employee.class, Employee_.employeeId, entityManager)
                .colDefs(ColDef.builder(Employee_.employeeId).build())
                .treeData(true)
                .isServerSideGroupFieldName("isGroup")
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("treeDataParentReferenceField");
    }

    @Test
    void rejectsEagerMasterDetailWithoutRowDataFieldName() {
        assertThatThrownBy(() -> QueryBuilder.builder(Product.class, Product_.productId, Trade.class, entityManager)
                .colDefs(ColDef.builder(Product_.productId).build())
                .masterDetailParams(MasterDetailParams.<Product, Long, Trade>builder()
                        .detailClass(Trade.class)
                        .detailColDefs(ColDef.builder(Trade_.tradeId).build())
                        .detailMasterReferenceField(Trade_.product)
                        .build())
                .masterDetailLazy(false)
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("masterDetailRowDataFieldName");
    }

    @Test
    void masterDetailParamsRequireRelationship() {
        assertThatThrownBy(() -> MasterDetailParams.<Product, Long, Trade>builder()
                .detailClass(Trade.class)
                .detailColDefs(ColDef.builder(Trade_.tradeId).build())
                .build())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("master-detail relationship");
    }
}
