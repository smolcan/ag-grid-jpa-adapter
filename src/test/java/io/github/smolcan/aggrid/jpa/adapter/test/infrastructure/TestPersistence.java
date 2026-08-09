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
 *   ./mvnw test -Dtest.jpa.provider=ECLIPSELINK -Dtest.database=H2
 * </pre>
 * Both providers are on the test classpath at the same time; the one to boot is selected
 * explicitly through {@code jakarta.persistence.provider}, since {@code persistence.xml}
 * deliberately names none.
 */
public final class TestPersistence {

    private static final String PERSISTENCE_UNIT = "aggrid-adapter-test";
    private static final AtomicLong DB_SEQUENCE = new AtomicLong();

    public enum JpaProvider {
        HIBERNATE,
        ECLIPSELINK
    }

    public enum TestDatabase {
        H2
        // POSTGRES, MYSQL via Testcontainers planned (compatibility matrix phase)
    }

    private TestPersistence() {
    }

    private static JpaProvider activeProvider() {
        return JpaProvider.valueOf(System.getProperty("test.jpa.provider", JpaProvider.HIBERNATE.name()).toUpperCase());
    }

    private static TestDatabase activeDatabase() {
        return TestDatabase.valueOf(System.getProperty("test.database", TestDatabase.H2.name()).toUpperCase());
    }

    public static EntityManagerFactory createEntityManagerFactory() {
        JpaProvider provider = activeProvider();
        TestDatabase database = activeDatabase();

        Map<String, Object> properties = new HashMap<>();
        properties.put("jakarta.persistence.schema-generation.database.action", "drop-and-create");

        if (database == TestDatabase.H2) {
            // unique database name per factory so test classes never share state
            String dbName = "aggrid_test_" + DB_SEQUENCE.incrementAndGet();
            // routed through CountingDriver so QueryCountTest can count statements on any provider
            properties.put("jakarta.persistence.jdbc.driver", CountingDriver.class.getName());
            properties.put("jakarta.persistence.jdbc.url", CountingDriver.URL_PREFIX + "h2:mem:" + dbName + ";DB_CLOSE_DELAY=-1");
            properties.put("jakarta.persistence.jdbc.user", "sa");
            properties.put("jakarta.persistence.jdbc.password", "");
        }

        if (provider == JpaProvider.HIBERNATE) {
            properties.put("jakarta.persistence.provider", "org.hibernate.jpa.HibernatePersistenceProvider");
            // statistics power the query-count regression tests
            properties.put("hibernate.generate_statistics", "true");
        } else if (provider == JpaProvider.ECLIPSELINK) {
            properties.put("jakarta.persistence.provider", "org.eclipse.persistence.jpa.PersistenceProvider");
            // surefire runs without a -javaagent, so dynamic weaving is unavailable anyway
            properties.put("eclipselink.weaving", "false");
            // no second-level cache: every scenario must be answered from the database,
            // the way it is under Hibernate's default configuration
            properties.put("eclipselink.cache.shared.default", "false");
            // drop-and-create logs a failing DROP per table against a fresh in-memory database
            properties.put("eclipselink.logging.level", "SEVERE");
            if (database == TestDatabase.H2) {
                properties.put("eclipselink.target-database", "org.eclipse.persistence.platform.database.H2Platform");
            }
        }

        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
    }
}
