package io.github.smolcan.aggrid.jpa.adapter.column;

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
    
    public @NonNull String getName() {
        return this.hops.stream().map(Attribute::getName).collect(Collectors.joining("."));
    }
}
