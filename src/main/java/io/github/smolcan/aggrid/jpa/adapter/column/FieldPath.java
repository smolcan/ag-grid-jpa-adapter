package io.github.smolcan.aggrid.jpa.adapter.column;

import jakarta.persistence.criteria.From;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.SingularAttribute;
import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("java:S119")
public class FieldPath<E, T> {

    @Getter
    private final List<Attribute<?, ?>> hops;

    private FieldPath(List<Attribute<?, ?>> hops) {
        this.hops = Collections.unmodifiableList(hops);
    }

    public static <E, T> FieldPath<E, T> of(@NonNull SingularAttribute<E, T> attribute) {
        List<Attribute<?, ?>> hops = new ArrayList<>(1);
        hops.add(attribute);
        return new FieldPath<>(hops);
    }

    public <N> FieldPath<E, N> to(@NonNull SingularAttribute<? super T, N> next) {
        List<Attribute<?, ?>> extended = new ArrayList<>(this.hops.size() + 1);
        extended.addAll(this.hops);
        extended.add(next);
        return new FieldPath<>(extended);
    }

    @NonNull
    public String getName() {
        return this.hops.stream().map(Attribute::getName).collect(Collectors.joining("."));
    }
    
    @NonNull
    public Path<T> getPath(@NonNull Root<E> root) {
        From<?, ?> currentFrom = root;
        Path<?> currentPath = root;
        boolean canJoin = true;
        for (int i = 0; i < this.hops.size() - 1; i++) {
            Attribute<?, ?> hop = this.hops.get(i);
            if (canJoin && hop.isAssociation()) {
                // to-one association -> reuse or create a LEFT JOIN
                currentFrom = reuseOrCreateLeftJoin(currentFrom, hop.getName());
                currentPath = currentFrom;
            } else {
                // embedded (or anything nested under one) -> navigate without a join
                currentPath = currentPath.get(hop.getName());
                canJoin = false;
            }
        }

        return currentPath.get(this.hops.get(this.hops.size() - 1).getName());
    }

    @NonNull
    private static Join<?, ?> reuseOrCreateLeftJoin(@NonNull From<?, ?> from, @NonNull String attributeName) {
        for (Join<?, ?> join : from.getJoins()) {
            if (join.getAttribute().getName().equals(attributeName) && join.getJoinType() == JoinType.LEFT) {
                return join;
            }
        }
        return from.join(attributeName, JoinType.LEFT);
    }
}
