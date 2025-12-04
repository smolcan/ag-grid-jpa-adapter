package io.github.smolcan.aggrid.jpa.adapter.utils;

import java.util.Objects;
import java.util.function.Function;

// Java Utils does not have TriFunction, so need to define my own :)
@FunctionalInterface
public interface TriFunction<A, B, C, R> {
    R apply(A a, B b, C c);

    default <V> TriFunction<A, B, C, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after);
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}