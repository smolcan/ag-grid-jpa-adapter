package io.github.smolcan.aggrid.jpa.adapter.test.infrastructure;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.Statement;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

public final class CountingDriver implements Driver {

    public static final String URL_PREFIX = "jdbc:counting:";

    private static final AtomicLong EXECUTED_STATEMENTS = new AtomicLong();

    static {
        try {
            DriverManager.registerDriver(new CountingDriver());
        } catch (SQLException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    /** Number of SQL statements executed on any connection while {@code action} ran. */
    public static long countStatements(Runnable action) {
        long before = EXECUTED_STATEMENTS.get();
        action.run();
        return EXECUTED_STATEMENTS.get() - before;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            // contract: a driver returns null for URLs it does not handle
            return null;
        }
        Connection connection = DriverManager.getConnection("jdbc:" + url.substring(URL_PREFIX.length()), info);
        return (Connection) Proxy.newProxyInstance(
                CountingDriver.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                new CountingConnection(connection)
        );
    }

    @Override
    public boolean acceptsURL(String url) {
        return url != null && url.startsWith(URL_PREFIX);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 1;
    }

    @Override
    public int getMinorVersion() {
        return 0;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException();
    }

    private static Object invokeUnwrapped(Object target, Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /** Wraps every statement the connection hands out so their executions are counted. */
    private static final class CountingConnection implements InvocationHandler {

        private final Connection delegate;

        private CountingConnection(Connection delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = invokeUnwrapped(delegate, method, args);
            if (!(result instanceof Statement)) {
                return result;
            }
            // return type is Statement / PreparedStatement / CallableStatement, matching what the caller expects
            return Proxy.newProxyInstance(
                    CountingDriver.class.getClassLoader(),
                    new Class<?>[]{method.getReturnType()},
                    new CountingStatement((Statement) result)
            );
        }
    }

    private static final class CountingStatement implements InvocationHandler {

        private final Statement delegate;

        private CountingStatement(Statement delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            // execute, executeQuery, executeUpdate, executeLargeUpdate, executeBatch
            if (method.getName().startsWith("execute")) {
                EXECUTED_STATEMENTS.incrementAndGet();
            }
            return invokeUnwrapped(delegate, method, args);
        }
    }
}
