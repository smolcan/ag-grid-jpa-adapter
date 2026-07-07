package io.github.smolcan.aggrid.jpa.adapter.utils;

import lombok.Getter;
import lombok.NonNull;

import java.util.Objects;

@Getter
public class Pair<K, V> {
    private final K key;
    private final V value;

    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Pair<?, ?> pair = (Pair<?, ?>) o;
        return Objects.equals(key, pair.key) && Objects.equals(value, pair.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, value);
    }

    @Override
    @NonNull
    public String toString() {
        return "(" + key + ", " + value + ")";
    }

    @NonNull
    public static <K, V> Pair<K, V> of(K key, V value) {
        return new Pair<>(key, value);
    }
}