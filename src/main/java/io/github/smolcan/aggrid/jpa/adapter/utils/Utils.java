package io.github.smolcan.aggrid.jpa.adapter.utils;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Utils {

    /** Escape character for LIKE patterns. Not a backslash, which MySQL and MariaDB treat specially. */
    public static final char LIKE_ESCAPE_CHAR = '!';

    private Utils() {}

    /**
     * Escapes {@code %}, {@code _} and {@link #LIKE_ESCAPE_CHAR}, so that the value matches literally in a LIKE pattern.
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

    /**
     * Whether the value needs an ESCAPE clause to match literally: it holds a wildcard, the escape character or a backslash.
     *
     * @param value the text that goes into the pattern
     * @return true if it needs the ESCAPE clause
     */
    public static boolean needsLikeEscape(@NonNull String value) {
        for (char c : value.toCharArray()) {
            if (c == LIKE_ESCAPE_CHAR || c == '%' || c == '_' || c == '\\') {
                return true;
            }
        }
        return false;
    }

    /**
     * LIKE predicate with the ESCAPE clause only when asked for, as some databases (Oracle) are slower with it.
     *
     * @param cb criteria builder
     * @param value the value to match
     * @param pattern the pattern
     * @param escape whether to add the ESCAPE clause
     * @return the predicate
     */
    @NonNull
    public static Predicate like(@NonNull CriteriaBuilder cb, @NonNull Expression<String> value, @NonNull Expression<String> pattern, boolean escape) {
        return escape ? cb.like(value, pattern, LIKE_ESCAPE_CHAR) : cb.like(value, pattern);
    }

    /**
     * NOT LIKE counterpart of {@link #like(CriteriaBuilder, Expression, Expression, boolean)}.
     *
     * @param cb criteria builder
     * @param value the value to match
     * @param pattern the pattern
     * @param escape whether to add the ESCAPE clause
     * @return the predicate
     */
    @NonNull
    public static Predicate notLike(@NonNull CriteriaBuilder cb, @NonNull Expression<String> value, @NonNull Expression<String> pattern, boolean escape) {
        return escape ? cb.notLike(value, pattern, LIKE_ESCAPE_CHAR) : cb.notLike(value, pattern);
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
