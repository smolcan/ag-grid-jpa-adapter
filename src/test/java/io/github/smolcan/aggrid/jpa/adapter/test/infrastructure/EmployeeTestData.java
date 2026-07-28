package io.github.smolcan.aggrid.jpa.adapter.test.infrastructure;

import io.github.smolcan.aggrid.jpa.adapter.test.entity.Employee;
import jakarta.persistence.EntityManager;

import java.math.BigDecimal;

/**
 * Deterministic tree dataset (path separator "/"):
 * <pre>
 * 1 Alice 500          (root, group)
 * ├─ 2 Bob 300         (group)
 * │  ├─ 4 Dave 100
 * │  └─ 5 Eve 120
 * ├─ 3 Carol 250       (group)
 * │  └─ 6 Frank 90
 * └─ 7 Grace 200
 * 8 Heidi 400          (root, group)
 * └─ 9 Ivan 150
 * 10 Judy 130          (root, leaf)
 * </pre>
 */
public final class EmployeeTestData {

    private EmployeeTestData() {
    }

    public static void seed(EntityManager em) {
        Employee alice = new Employee(1L, "Alice", new BigDecimal("500.00"), null, "1");
        Employee bob = new Employee(2L, "Bob", new BigDecimal("300.00"), alice, "1/2");
        Employee carol = new Employee(3L, "Carol", new BigDecimal("250.00"), alice, "1/3");
        Employee dave = new Employee(4L, "Dave", new BigDecimal("100.00"), bob, "1/2/4");
        Employee eve = new Employee(5L, "Eve", new BigDecimal("120.00"), bob, "1/2/5");
        Employee frank = new Employee(6L, "Frank", new BigDecimal("90.00"), carol, "1/3/6");
        Employee grace = new Employee(7L, "Grace", new BigDecimal("200.00"), alice, "1/7");
        Employee heidi = new Employee(8L, "Heidi", new BigDecimal("400.00"), null, "8");
        Employee ivan = new Employee(9L, "Ivan", new BigDecimal("150.00"), heidi, "8/9");
        Employee judy = new Employee(10L, "Judy", new BigDecimal("130.00"), null, "10");

        em.persist(alice);
        em.persist(bob);
        em.persist(carol);
        em.persist(dave);
        em.persist(eve);
        em.persist(frank);
        em.persist(grace);
        em.persist(heidi);
        em.persist(ivan);
        em.persist(judy);
    }
}
