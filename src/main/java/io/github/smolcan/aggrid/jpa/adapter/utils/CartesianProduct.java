package io.github.smolcan.aggrid.jpa.adapter.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CartesianProduct {
    
    public static <T> List<List<T>> cartesianProduct(List<Set<T>> sets) {
        return _cartesianProduct(0, sets);
    }

    private static <T> List<List<T>> _cartesianProduct(int index, List<Set<T>> sets) {
        List<List<T>> result = new ArrayList<>();
        if (index == sets.size()) {
            result.add(new ArrayList<>());
        } else {
            for (T element : sets.get(index)) {
                for (List<T> product : _cartesianProduct(index + 1, sets)) {
                    List<T> newProduct = new ArrayList<>(product);
                    newProduct.add(0, element); // Maintain order
                    result.add(newProduct);
                }
            }
        }
        return result;
    }
}
