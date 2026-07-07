package io.github.smolcan.aggrid.jpa.adapter.utils;

import lombok.NonNull;

import java.util.function.Function;

// Java Utils does not have TriFunction, so need to define my own :)
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);

    @NonNull
    default <V> TriFunction<A, B, C, V> andThen(@NonNull Function<? super R, ? extends V> after) {
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}