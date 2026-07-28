package io.github.smolcan.aggrid.jpa.adapter.test.infrastructure;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Creates the {@link EntityManagerFactory} for scenario tests.
 * <p>
 * The JPA provider and database are chosen via system properties so the same scenario
 * tests can run against different combinations (CI matrix), defaulting to Hibernate on
 * in-memory H2 for fast local runs:
 * <pre>
 *   ./mvnw test -Dtest.jpa.provider=HIBERNATE -Dtest.database=H2
 * </pre>
 */
public final class TestPersistence {

    private static final String PERSISTENCE_UNIT = "aggrid-adapter-test";
    private static final AtomicLong DB_SEQUENCE = new AtomicLong();

    public enum JpaProvider {
        HIBERNATE
        // ECLIPSELINK planned (compatibility matrix phase)
    }

    public enum TestDatabase {
        H2
        // POSTGRES, MYSQL via Testcontainers planned (compatibility matrix phase)
    }

    private TestPersistence() {
    }

    public static EntityManagerFactory createEntityManagerFactory() {
        JpaProvider provider = JpaProvider.valueOf(System.getProperty("test.jpa.provider", JpaProvider.HIBERNATE.name()).toUpperCase());
        TestDatabase database = TestDatabase.valueOf(System.getProperty("test.database", TestDatabase.H2.name()).toUpperCase());

        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.schema-generation.database.action", "drop-and-create");

        if (database == TestDatabase.H2) {
            // unique database name per factory so test classes never share state
            String dbName = "aggrid_test_" + DB_SEQUENCE.incrementAndGet();
            properties.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
            properties.put("jakarta.persistence.jdbc.url", "jdbc:h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
            properties.put("jakarta.persistence.jdbc.user", "sa");
            properties.put("jakarta.persistence.jdbc.password", "");
        }

        if (provider == JpaProvider.HIBERNATE) {
            properties.put("jakarta.persistence.provider", "org.hibernate.jpa.HibernatePersistenceProvider");
            // statistics power the query-count regression tests
            properties.put("hibernate.generate_statistics", "true");
        }

        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
    }
}
