package io.github.smolcan.aggrid.jpa.adapter.utils;

import jakarta.persistence.criteria.*;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Utils {

    private Utils() {}

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

    
    @NonNull
    public static Path<?> getPath(@NonNull From<?, ?> from, @NonNull String fieldName) {
        if (!fieldName.contains(".")) {
            // no dot notation in fieldName
            return from.get(fieldName);
        }

        String[] parts = fieldName.split("\\.");
        From<?, ?> currentFrom = from;
        
        for (int i = 0; i < parts.length - 1; i++) {
            String relation = parts[i];

            Join<?, ?> foundJoin = null;
            Set<? extends Join<?, ?>> joins = currentFrom.getJoins();

            for (Join<?, ?> join : joins) {
                if (join.getAttribute().getName().equals(relation) && join.getJoinType() == JoinType.LEFT) {
                    foundJoin = join;
                    break;
                }
            }
            
            if (foundJoin != null) {
                currentFrom = foundJoin;
            } else {
                currentFrom = currentFrom.join(relation, JoinType.LEFT);
            }
        }
        
        return currentFrom.get(parts[parts.length - 1]);
    }
}
