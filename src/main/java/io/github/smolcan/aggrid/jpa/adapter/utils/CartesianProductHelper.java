package io.github.smolcan.aggrid.jpa.adapter.utils;

import java.util.*;

public class CartesianProductHelper {

    public static <T> Set<Set<T>> cartesianProduct(Collection<Set<T>> sets) {
        if (sets.size() < 2) {
            throw new IllegalArgumentException("Can't have a product of fewer than two sets (got " + sets.size() + ")");
        }
        
        List<Set<T>> setsList = new ArrayList<>(sets);
        return _cartesianProduct(0, setsList);
    }

    private static <T> Set<Set<T>> _cartesianProduct(int index, List<Set<T>> sets) {
        Set<Set<T>> ret = new HashSet<>();
        if (index == sets.size()) {
            ret.add(new HashSet<>());
        } else {
            for (T obj : sets.get(index)) {
                for (Set<T> set : _cartesianProduct(index + 1, sets)) {
                    Set<T> newSet = new HashSet<>(set);
                    newSet.add(obj);
                    ret.add(newSet);
                }
            }
        }
        return ret;
    }


}
