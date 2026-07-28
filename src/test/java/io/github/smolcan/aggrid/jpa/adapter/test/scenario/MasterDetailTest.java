package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder.MasterDetailParams;
import io.github.smolcan.aggrid.jpa.adapter.request.ServerSideGetRowsRequest;
import io.github.smolcan.aggrid.jpa.adapter.request.SortDirection;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MasterDetailTest extends ScenarioTestBase {

    private QueryBuilder<Product, Long, Trade> masterDetailQueryBuilder(boolean lazy) {
        QueryBuilder.Builder<Product, Long, Trade> builder = QueryBuilder.builder(Product.class, Product_.productId, Trade.class, entityManager)
                .colDefs(
                        ColDef.builder(Product_.productId).build(),
                        ColDef.builder(Product_.name).build()
                )
                .masterDetailParams(MasterDetailParams.<Product, Long, Trade>builder()
                        .detailClass(Trade.class)
                        .detailColDefs(
                                ColDef.builder(Trade_.tradeId).build(),
                                ColDef.builder(Trade_.portfolio).build()
                        )
                        .detailMasterReferenceField(Trade_.product)
                        .build());
        if (!lazy) {
            builder.masterDetailLazy(false).masterDetailRowDataFieldName("detailRows");
        }
        return builder.build();
    }

    private ServerSideGetRowsRequest masterRequest() {
        ServerSideGetRowsRequest request = emptyRequest(0, 100);
        request.getSortModel().add(sortItem("productId", SortDirection.asc));
        return request;
    }

    @SuppressWarnings("unchecked")
    private static List<Long> detailTradeIds(List<Map<String, Object>> detailRows) {
        return detailRows.stream().map(r -> ((Number) r.get("tradeId")).longValue()).collect(Collectors.toList());
    }

    /** Detail columns resolved per master row: Gold gets an extra portfolio column, everything else only tradeId. */
    private static Function<Map<String, Object>, MasterDetailParams<Product, Long, Trade>> dynamicDetailParams() {
        return masterRow -> {
            MasterDetailParams.Builder<Product, Long, Trade> params = MasterDetailParams.<Product, Long, Trade>builder()
                    .detailClass(Trade.class)
                    .detailMasterReferenceField(Trade_.product);
            if ("Gold".equals(masterRow.get("name"))) {
                params.detailColDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(Trade_.portfolio).build());
            } else {
                params.detailColDefs(ColDef.builder(Trade_.tradeId).build());
            }
            return params.build();
        };
    }

    @Test
    void lazyModeReturnsMastersWithoutDetails() {
        LoadSuccessParams result = masterDetailQueryBuilder(true).getRows(masterRequest());
        assertThat(columnValues(result, "name")).containsExactly("Gold", "Silver", "Platinum");
        assertThat(result.getRowData().get(0)).doesNotContainKey("detailRows");
    }

    @Test
    void getDetailRowDataFetchesChildrenOfMasterRow() {
        QueryBuilder<Product, Long, Trade> queryBuilder = masterDetailQueryBuilder(true);
        Map<String, Object> goldRow = queryBuilder.getRows(masterRequest()).getRowData().get(0);

        List<Map<String, Object>> details = queryBuilder.getDetailRowData(goldRow);
        assertThat(detailTradeIds(details)).containsExactlyInAnyOrder(1L, 3L, 6L, 9L);
    }

    @Test
    @SuppressWarnings("unchecked")
    void eagerModeAttachesDetailRowsToEveryMaster() {
        LoadSuccessParams result = masterDetailQueryBuilder(false).getRows(masterRequest());

        List<Map<String, Object>> gold = (List<Map<String, Object>>) result.getRowData().get(0).get("detailRows");
        List<Map<String, Object>> silver = (List<Map<String, Object>>) result.getRowData().get(1).get("detailRows");
        List<Map<String, Object>> platinum = (List<Map<String, Object>>) result.getRowData().get(2).get("detailRows");

        assertThat(detailTradeIds(gold)).containsExactlyInAnyOrder(1L, 3L, 6L, 9L);
        assertThat(detailTradeIds(silver)).containsExactlyInAnyOrder(2L, 5L, 8L, 12L);
        assertThat(detailTradeIds(platinum)).containsExactlyInAnyOrder(4L, 7L, 11L);
    }

    @Test
    void customMasterRowPredicate() {
        QueryBuilder<Product, Long, Trade> queryBuilder = QueryBuilder.builder(Product.class, Product_.productId, Trade.class, entityManager)
                .colDefs(
                        ColDef.builder(Product_.productId).build(),
                        ColDef.builder(Product_.name).build()
                )
                .masterDetailParams(MasterDetailParams.<Product, Long, Trade>builder()
                        .detailClass(Trade.class)
                        // collection overload of detailColDefs
                        .detailColDefs(List.of(ColDef.builder(Trade_.tradeId).build()))
                        .createMasterRowPredicate((cb, root, masterRow) ->
                                cb.equal(root.get(Trade_.product).get(Product_.productId), masterRow.get("productId")))
                        .build())
                .build();

        Map<String, Object> silverRow = queryBuilder.getRows(masterRequest()).getRowData().get(1);
        List<Map<String, Object>> details = queryBuilder.getDetailRowData(silverRow);
        assertThat(detailTradeIds(details)).containsExactlyInAnyOrder(2L, 5L, 8L, 12L);
    }

    @Test
    void detailMasterIdFieldLinksByRawForeignKey() {
        QueryBuilder<Product, Long, Trade> queryBuilder = QueryBuilder.builder(Product.class, Product_.productId, Trade.class, entityManager)
                .colDefs(
                        ColDef.builder(Product_.productId).build(),
                        ColDef.builder(Product_.name).build()
                )
                .masterDetailParams(MasterDetailParams.<Product, Long, Trade>builder()
                        .detailClass(Trade.class)
                        .detailColDefs(ColDef.builder(Trade_.tradeId).build())
                        .detailMasterIdField(Trade_.productId)
                        .build())
                .build();

        Map<String, Object> platinumRow = queryBuilder.getRows(masterRequest()).getRowData().get(2);
        List<Map<String, Object>> details = queryBuilder.getDetailRowData(platinumRow);
        assertThat(detailTradeIds(details)).containsExactlyInAnyOrder(4L, 7L, 11L);
    }

    @Test
    void dynamicMasterDetailParamsVaryColumnsPerMasterRow() {
        QueryBuilder<Product, Long, Trade> queryBuilder = QueryBuilder.builder(Product.class, Product_.productId, Trade.class, entityManager)
                .colDefs(
                        ColDef.builder(Product_.productId).build(),
                        ColDef.builder(Product_.name).build()
                )
                .dynamicMasterDetailParams(dynamicDetailParams())
                .build();

        List<Map<String, Object>> masters = queryBuilder.getRows(masterRequest()).getRowData();

        List<Map<String, Object>> goldDetails = queryBuilder.getDetailRowData(masters.get(0));
        assertThat(detailTradeIds(goldDetails)).containsExactlyInAnyOrder(1L, 3L, 6L, 9L);
        assertThat(goldDetails.get(0)).containsKeys("tradeId", "portfolio");

        List<Map<String, Object>> silverDetails = queryBuilder.getDetailRowData(masters.get(1));
        assertThat(detailTradeIds(silverDetails)).containsExactlyInAnyOrder(2L, 5L, 8L, 12L);
        assertThat(silverDetails.get(0)).containsOnlyKeys("tradeId");
    }

    /**
     * Eager mode normally batches all details into one IN-query, but dynamic params must be resolved
     * per master row, so attaching falls back to one detail query per row.
     */
    @Test
    @SuppressWarnings("unchecked")
    void eagerModeWithDynamicParamsAttachesPerRowDetailColumns() {
        QueryBuilder<Product, Long, Trade> queryBuilder = QueryBuilder.builder(Product.class, Product_.productId, Trade.class, entityManager)
                .colDefs(
                        ColDef.builder(Product_.productId).build(),
                        ColDef.builder(Product_.name).build()
                )
                .dynamicMasterDetailParams(dynamicDetailParams())
                .masterDetailLazy(false)
                .masterDetailRowDataFieldName("detailRows")
                .build();

        List<Map<String, Object>> masters = queryBuilder.getRows(masterRequest()).getRowData();

        List<Map<String, Object>> gold = (List<Map<String, Object>>) masters.get(0).get("detailRows");
        List<Map<String, Object>> silver = (List<Map<String, Object>>) masters.get(1).get("detailRows");
        assertThat(detailTradeIds(gold)).containsExactlyInAnyOrder(1L, 3L, 6L, 9L);
        assertThat(detailTradeIds(silver)).containsExactlyInAnyOrder(2L, 5L, 8L, 12L);
        // the per-row params reached the attached rows, not just getDetailRowData
        assertThat(gold.get(0)).containsKeys("tradeId", "portfolio");
        assertThat(silver.get(0)).containsOnlyKeys("tradeId");
    }

    /** Same per-row fallback as above, triggered by a custom master-row predicate instead of dynamic params. */
    @Test
    @SuppressWarnings("unchecked")
    void eagerModeWithCustomMasterRowPredicateAttachesDetails() {
        QueryBuilder<Product, Long, Trade> queryBuilder = QueryBuilder.builder(Product.class, Product_.productId, Trade.class, entityManager)
                .colDefs(
                        ColDef.builder(Product_.productId).build(),
                        ColDef.builder(Product_.name).build()
                )
                .masterDetailParams(MasterDetailParams.<Product, Long, Trade>builder()
                        .detailClass(Trade.class)
                        .detailColDefs(ColDef.builder(Trade_.tradeId).build())
                        .createMasterRowPredicate((cb, root, masterRow) ->
                                cb.equal(root.get(Trade_.product).get(Product_.productId), masterRow.get("productId")))
                        .build())
                .masterDetailLazy(false)
                .masterDetailRowDataFieldName("detailRows")
                .build();

        List<Map<String, Object>> masters = queryBuilder.getRows(masterRequest()).getRowData();

        assertThat(detailTradeIds((List<Map<String, Object>>) masters.get(0).get("detailRows")))
                .containsExactlyInAnyOrder(1L, 3L, 6L, 9L);
        assertThat(detailTradeIds((List<Map<String, Object>>) masters.get(2).get("detailRows")))
                .containsExactlyInAnyOrder(4L, 7L, 11L);
    }

    @Test
    void masterRowWithoutPrimaryFieldValueIsRejected() {
        QueryBuilder<Product, Long, Trade> queryBuilder = masterDetailQueryBuilder(true);
        assertThatThrownBy(() -> queryBuilder.getDetailRowData(Map.of("name", "Gold")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("primary field");
    }

    @Test
    void getDetailRowDataRequiresMasterDetailMode() {
        assertThatThrownBy(() -> defaultQueryBuilder().getDetailRowData(Map.of("tradeId", 1L)))
                .isInstanceOf(IllegalStateException.class);
    }
}
