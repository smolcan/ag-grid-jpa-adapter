package io.github.smolcan.aggrid.jpa.adapter.test.infrastructure;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
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
 *   ./mvnw test -Dtest.jpa.provider=HIBERNATE -Dtest.database=POSTGRES
 * </pre>
 * Both providers are on the test classpath at the same time; the one to boot is selected
 * explicitly through {@code jakarta.persistence.provider}, since {@code persistence.xml}
 * deliberately names none. {@code POSTGRES} starts one Testcontainers server for the JVM and
 * therefore needs a running Docker daemon.
 */
public final class TestPersistence {

    private static final String PERSISTENCE_UNIT = "aggrid-adapter-test";
    private static final AtomicLong DB_SEQUENCE = new AtomicLong();

    public enum JpaProvider {
        HIBERNATE,
        ECLIPSELINK
    }

    public enum TestDatabase {
        H2,
        POSTGRES,
        MARIADB
    }

    private static PostgreSQLContainer<?> postgres;
    private static MariaDBContainer<?> mariadb;

    private TestPersistence() {
    }

    private static JpaProvider activeProvider() {
        return JpaProvider.valueOf(System.getProperty("test.jpa.provider", JpaProvider.HIBERNATE.name()).toUpperCase());
    }

    private static TestDatabase activeDatabase() {
        return TestDatabase.valueOf(System.getProperty("test.database", TestDatabase.POSTGRES.name()).toUpperCase());
    }

    /**
     * Where NULL lands in an ORDER BY. H2 and MariaDB treat it as the smallest value, so it leads
     * ascending and trails descending; Postgres and Oracle treat it as the largest and do the
     * opposite. JPA 3.1 has no way to ask for one or the other, so sorting tests that involve nulls
     * have to expect whichever the database does.
     */
    public static boolean nullsSortLow() {
        return activeDatabase() == TestDatabase.H2 || activeDatabase() == TestDatabase.MARIADB;
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
        } else if (database == TestDatabase.POSTGRES) {
            PostgreSQLContainer<?> container = startPostgres();
            // one schema per factory plays the role the unique in-memory database plays for H2
            String schema = "aggrid_test_" + DB_SEQUENCE.incrementAndGet();
            createSchema(container.getJdbcUrl(), container.getUsername(), container.getPassword(), schema);
            // EclipseLink declares UUID columns as native uuid but binds their values as varchar.
            // H2 coerces between the two, Postgres rejects it, so the server is asked to infer
            // parameter types instead. Only that provider needs it, so the others stay strict.
            String parameterTyping = provider == JpaProvider.ECLIPSELINK ? "&stringtype=unspecified" : "";
            properties.put("jakarta.persistence.jdbc.driver", CountingDriver.class.getName());
            properties.put("jakarta.persistence.jdbc.url", CountingDriver.URL_PREFIX + "postgresql://"
                    + container.getHost() + ":" + container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)
                    + "/" + container.getDatabaseName() + "?currentSchema=" + schema + parameterTyping);
            properties.put("jakarta.persistence.jdbc.user", container.getUsername());
            properties.put("jakarta.persistence.jdbc.password", container.getPassword());
        } else if (database == TestDatabase.MARIADB) {
            MariaDBContainer<?> container = startMariadb();
            // MariaDB has no schemas below the database, so isolation is a database per factory;
            // creating one needs root, which is also then the simplest account to connect as
            String schema = "aggrid_test_" + DB_SEQUENCE.incrementAndGet();
            createSchema(container.getJdbcUrl(), "root", container.getPassword(), schema);
            properties.put("jakarta.persistence.jdbc.driver", CountingDriver.class.getName());
            properties.put("jakarta.persistence.jdbc.url", CountingDriver.URL_PREFIX + "mariadb://"
                    + container.getHost() + ":" + container.getFirstMappedPort()
                    + "/" + schema);
            properties.put("jakarta.persistence.jdbc.user", "root");
            properties.put("jakarta.persistence.jdbc.password", container.getPassword());
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
            } else if (database == TestDatabase.POSTGRES) {
                properties.put("eclipselink.target-database", "org.eclipse.persistence.platform.database.PostgreSQLPlatform");
            } else if (database == TestDatabase.MARIADB) {
                properties.put("eclipselink.target-database", "org.eclipse.persistence.platform.database.MariaDBPlatform");
            }
        }

        return Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
    }

    /** One server per JVM, shut down by the Testcontainers reaper when the JVM exits. */
    private static synchronized PostgreSQLContainer<?> startPostgres() {
        if (postgres == null) {
            // the C locale sorts by byte value like H2 does; the images default to en_US, whose
            // collation ignores case and would reorder the fixture rows behind the sorting tests
            postgres = new PostgreSQLContainer<>("postgres:17-alpine")
                    .withEnv("POSTGRES_INITDB_ARGS", "--locale=C");
            postgres.start();
        }
        return postgres;
    }

    /** One server per JVM, shut down by the Testcontainers reaper when the JVM exits. */
    private static synchronized MariaDBContainer<?> startMariadb() {
        if (mariadb == null) {
            // a binary collation keeps comparisons and ORDER BY case-sensitive, matching H2 and the
            // C-locale Postgres above; the image defaults to a case-insensitive one
            mariadb = new MariaDBContainer<>("mariadb:11.4")
                    .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_bin");
            mariadb.start();
        }
        return mariadb;
    }

    private static void createSchema(String jdbcUrl, String user, String password, String schema) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, user, password);
             Statement statement = connection.createStatement()) {
            statement.execute("create schema " + schema);
        } catch (SQLException e) {
            throw new IllegalStateException("could not create schema " + schema, e);
        }
    }
}
