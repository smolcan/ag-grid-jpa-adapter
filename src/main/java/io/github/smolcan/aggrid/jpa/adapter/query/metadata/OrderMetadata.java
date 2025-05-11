package io.github.smolcan.aggrid.jpa.adapter.query.metadata;

import jakarta.persistence.criteria.Order;

import java.util.Objects;

public class OrderMetadata {
    private final Order order;
    private final String colId;
    
    private OrderMetadata(Builder builder) {
        this.order = builder.order;
        this.colId = builder.colId;
    }

    public Order getOrder() {
        return order;
    }

    public String getColId() {
        return colId;
    }

    public static Builder builder(Order order) {
        return new Builder(order);
    }

    public static class Builder {
        private Order order;
        private String colId;
        
        public Builder(Order order) {
            Objects.requireNonNull(order);
            this.order = order;
        }

        public Builder order(Order order) {
            Objects.requireNonNull(order);
            this.order = order;
            return this;
        }

        public Builder colId(String colId) {
            this.colId = colId;
            return this;
        }

        public OrderMetadata build() {
            return new OrderMetadata(this);
        }
    }
}
