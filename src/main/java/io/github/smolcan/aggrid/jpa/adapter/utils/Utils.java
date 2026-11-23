package io.github.smolcan.aggrid.jpa.adapter.utils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import lombok.NonNull;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
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

    /*
     * and, or and createQuery replace Criteria calls whose 3.x signatures are gone from Jakarta Persistence 4:
     * and/or(Predicate...) and where/having(Predicate...) now take BooleanExpression..., and
     * EntityManager.createQuery(CriteriaQuery) became EntityHandler.createQuery(CriteriaSelect).
     * Compiled against 3.0, those calls throw NoSuchMethodError on a JPA 4 provider; these helpers
     * only use signatures that exist in every version from 2.2 to 4.0.
     */

    /**
     * {@code cb.and(Predicate...)} through the two-argument overload, which every JPA version has.
     * Nests as a balanced tree, so thousands of conditions stay a few levels deep.
     *
     * @param cb criteria builder
     * @param predicates the predicates to combine
     * @return their conjunction, or {@code cb.conjunction()} (true) if there are none
     */
    @NonNull
    public static Predicate and(@NonNull CriteriaBuilder cb, @NonNull List<? extends Predicate> predicates) {
        return predicates.isEmpty() ? cb.conjunction() : combine(cb, predicates, 0, predicates.size(), true);
    }

    /**
     * {@code cb.or(Predicate...)} through the two-argument overload, which every JPA version has.
     *
     * @param cb criteria builder
     * @param predicates the predicates to combine
     * @return their disjunction, or {@code cb.disjunction()} (false) if there are none
     */
    @NonNull
    public static Predicate or(@NonNull CriteriaBuilder cb, @NonNull List<? extends Predicate> predicates) {
        return predicates.isEmpty() ? cb.disjunction() : combine(cb, predicates, 0, predicates.size(), false);
    }

    // Joins the two halves of the range instead of chaining one predicate after another: the tree is log2(n) deep
    // instead of n, and providers walk it recursively (a chain of 5000 overflows the stack in Hibernate).
    private static Predicate combine(@NonNull CriteriaBuilder cb, @NonNull List<? extends Predicate> predicates, int from, int to, boolean and) {
        if (to - from == 1) {
            return predicates.get(from);
        }
        int mid = (from + to) >>> 1;
        Predicate left = combine(cb, predicates, from, mid, and);
        Predicate right = combine(cb, predicates, mid, to, and);
        return and ? cb.and(left, right) : cb.or(left, right);
    }

    /**
     * {@code em.createQuery(criteriaQuery)}. No compiled call links on both 3.x and 4: JPA 4 only has
     * {@code createQuery(CriteriaSelect)}, and {@code CriteriaSelect} doesn't exist before 3.2,
     * so the method is looked up once at runtime.
     *
     * @param <T> the result type
     * @param em entity manager
     * @param query the criteria query
     * @return the typed query
     */
    @NonNull
    @SuppressWarnings("unchecked")
    public static <T> TypedQuery<T> createQuery(@NonNull EntityManager em, @NonNull CriteriaQuery<T> query) {
        try {
            return (TypedQuery<T>) CreateQuery.HANDLE.invoke(em, query);
        } catch (RuntimeException | Error e) {
            throw e;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    /** Looked up on first use, so a failure can't break the other methods of {@link Utils}. */
    private static final class CreateQuery {
        static final MethodHandle HANDLE = find();

        private static MethodHandle find() {
            Method found = null;
            for (Method method : EntityManager.class.getMethods()) {
                Class<?>[] params = method.getParameterTypes();
                if (method.getName().equals("createQuery")
                        && params.length == 1
                        && params[0].isAssignableFrom(CriteriaQuery.class)
                        && TypedQuery.class.isAssignableFrom(method.getReturnType())
                        // 3.2 has both createQuery(CriteriaQuery) and createQuery(CriteriaSelect): prefer the
                        // CriteriaQuery one, which the 3.x call used
                        && (found == null || params[0] == CriteriaQuery.class)) {
                    found = method;
                }
            }
            if (found == null) {
                throw new IllegalStateException("No EntityManager.createQuery method accepts a CriteriaQuery");
            }
            try {
                return MethodHandles.lookup().unreflect(found);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
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
