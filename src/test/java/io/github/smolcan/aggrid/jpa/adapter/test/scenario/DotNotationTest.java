package io.github.smolcan.aggrid.jpa.adapter.test.scenario;

import io.github.smolcan.aggrid.jpa.adapter.column.ColDef;
import io.github.smolcan.aggrid.jpa.adapter.column.FieldPath;
import io.github.smolcan.aggrid.jpa.adapter.query.QueryBuilder;
import io.github.smolcan.aggrid.jpa.adapter.response.LoadSuccessParams;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Product_;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade;
import io.github.smolcan.aggrid.jpa.adapter.test.entity.Trade_;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DotNotationTest extends ScenarioTestBase {

    private QueryBuilder<Trade, Long, Void> dotNotationQueryBuilder(boolean suppressFieldDotNotation) {
        return QueryBuilder.builder(Trade.class, Trade_.tradeId, entityManager)
                .colDefs(
                        ColDef.builder(Trade_.tradeId).build(),
                        ColDef.builder(FieldPath.of(Trade_.product).to(Product_.name)).build()
                )
                .suppressFieldDotNotation(suppressFieldDotNotation)
                .build();
    }

    @Test
    void nestedMapsByDefault() {
        LoadSuccessParams result = dotNotationQueryBuilder(false).getRows(sortedByIdRequest(0, 1));
        Map<String, Object> row = result.getRowData().get(0);
        assertThat(row).doesNotContainKey("product.name");
        assertThat(nestedValue(row, "product.name")).isEqualTo("Gold");
    }

    @Test
    void suppressFieldDotNotationReturnsFlatKeys() {
        LoadSuccessParams result = dotNotationQueryBuilder(true).getRows(sortedByIdRequest(0, 1));
        Map<String, Object> row = result.getRowData().get(0);
        assertThat(row).containsEntry("product.name", "Gold");
        assertThat(row).doesNotContainKey("product");
    }
}
