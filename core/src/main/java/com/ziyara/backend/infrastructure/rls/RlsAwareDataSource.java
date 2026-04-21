package com.ziyara.backend.infrastructure.rls;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * Wraps the pool {@link DataSource} so each physical connection receives
 * {@code set_config('app.*', ...)} matching {@link RlsSessionContext} before use, and resets on close.
 */
@RequiredArgsConstructor
@Slf4j
public class RlsAwareDataSource implements DataSource {

    private final DataSource delegate;

    @Override
    public Connection getConnection() throws SQLException {
        Connection raw = delegate.getConnection();
        applySessionVariables(raw);
        return wrap(raw);
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        Connection raw = delegate.getConnection(username, password);
        applySessionVariables(raw);
        return wrap(raw);
    }

    private static void applySessionVariables(Connection c) throws SQLException {
        RlsSessionAttributes a = RlsSessionContext.getOrDefault();
        setConfig(c, "app.rls_bypass", a.rlsBypass() ? "1" : "0");
        setConfig(c, "app.current_user_id", a.currentUserId() != null ? a.currentUserId().toString() : "");
        setConfig(c, "app.current_provider_id", a.currentProviderId() != null ? a.currentProviderId().toString() : "");
    }

    private static void resetSessionVariables(Connection c) {
        try {
            setConfig(c, "app.rls_bypass", null);
            setConfig(c, "app.current_user_id", null);
            setConfig(c, "app.current_provider_id", null);
        } catch (SQLException e) {
            log.debug("RLS reset failed (connection may already be broken): {}", e.getMessage());
        }
    }

    private static void setConfig(Connection c, String name, String value) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("select set_config(?, ?, false)")) {
            ps.setString(1, name);
            if (value == null) {
                ps.setNull(2, java.sql.Types.VARCHAR);
            } else {
                ps.setString(2, value);
            }
            ps.execute();
        }
    }

    private static Connection wrap(Connection raw) {
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                new ConnectionInvocationHandler(raw));
    }

    private record ConnectionInvocationHandler(Connection raw) implements InvocationHandler {

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if ("close".equals(method.getName())) {
                resetSessionVariables(raw);
                return method.invoke(raw, args);
            }
            return method.invoke(raw, args);
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }
}
