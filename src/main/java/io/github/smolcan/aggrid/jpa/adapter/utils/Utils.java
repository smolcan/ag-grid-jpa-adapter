package io.github.smolcan.aggrid.jpa.adapter.utils;

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
}
