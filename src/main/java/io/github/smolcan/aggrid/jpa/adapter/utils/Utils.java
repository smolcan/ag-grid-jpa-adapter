package io.github.smolcan.aggrid.jpa.adapter.utils;

import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Utils {

    /**
     * Escape character of the LIKE patterns built from user input, see {@link #escapeLike(String)}.
     * Not a backslash, which MySQL and MariaDB already treat as an escape of their own.
     */
    public static final char LIKE_ESCAPE_CHAR = '!';

    private Utils() {}

    /**
     * Escapes the LIKE wildcards ({@code %} and {@code _}) and the escape character itself, so that
     * the value matches literally in a LIKE pattern that uses {@link #LIKE_ESCAPE_CHAR}.
     *
     * @param value the text to escape
     * @return the escaped text
     */
    @NonNull
    public static String escapeLike(@NonNull String value) {
        StringBuilder escaped = new StringBuilder(value.length());
        for (char c : value.toCharArray()) {
            if (c == LIKE_ESCAPE_CHAR || c == '%' || c == '_') {
                escaped.append(LIKE_ESCAPE_CHAR);
            }
            escaped.append(c);
        }
        return escaped.toString();
    }

    @NonNull
    public static <T> List<List<T>> cartesianProduct(@NonNull List<Set<T>> sets) {
        return cartesianProduct(0, sets);
    }

    private static <T> List<List<T>> cartesianProduct(int index, List<Set<T>> sets) {
        List<List<T>> result = new ArrayList<>();
        if (index == sets.size()) {
            result.add(new ArrayList<>());
        } else {
            for (T element : sets.get(index)) {
                for (List<T> product : cartesianProduct(index + 1, sets)) {
                    List<T> newProduct = new ArrayList<>(product);
                    newProduct.add(0, element); // Maintain order
                    result.add(newProduct);
                }
            }
        }
        return result;
    }
}
