package io.github.smolcan.aggrid.jpa.adapter.test.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * Self-referencing entity for tree-data scenarios. The manager relation is mapped three ways
 * so every tree-data mode of the adapter can be exercised: {@code manager} (parent reference),
 * {@code managerId} (read-only parent id column), {@code children} (inverse collection),
 * plus {@code path} for data-path mode ("1/2/4" style, separator "/").
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
public class Employee {

    @Id
    private Long employeeId;

    private String name;

    @Column(precision = 20, scale = 2)
    private BigDecimal salary;

    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;

    @Column(name = "manager_id", insertable = false, updatable = false)
    private Long managerId;

    @OneToMany(mappedBy = "manager")
    private List<Employee> children;

    private String path;

    public Employee(Long employeeId, String name, BigDecimal salary, Employee manager, String path) {
        this.employeeId = employeeId;
        this.name = name;
        this.salary = salary;
        this.manager = manager;
        this.path = path;
    }
}
