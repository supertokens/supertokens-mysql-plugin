/*
 *    Copyright (c) 2020, VRAI Labs and/or its affiliates. All rights reserved.
 *
 *    This software is licensed under the Apache License, Version 2.0 (the
 *    "License") as published by the Apache Software Foundation.
 *
 *    You may not use this file except in compliance with the License. You may
 *    obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *    WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *    License for the specific language governing permissions and limitations
 *    under the License.
 *
 */

package io.supertokens.storage.mysql;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.supertokens.pluginInterface.exceptions.DbInitException;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.storage.mysql.config.Config;
import io.supertokens.storage.mysql.config.MySQLConfig;
import io.supertokens.storage.mysql.output.Logging;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Objects;

public class ConnectionPool extends ResourceDistributor.SingletonResource {

    private static final String RESOURCE_KEY = "io.supertokens.storage.mysql.ConnectionPool";
    private HikariDataSource hikariDataSource = null;

    private final Start start;
    private PostConnectCallback postConnectCallback;

    private ConnectionPool(Start start, PostConnectCallback postConnectCallback) {
        this.start = start;
        this.postConnectCallback = postConnectCallback;
    }

    private synchronized void initialiseHikariDataSource() throws SQLException, StorageQueryException {
        if (this.hikariDataSource != null) {
            return;
        }
        if (!start.enabled) {
            throw new RuntimeException("Connection refused"); // emulates exception thrown by Hikari
        }
        HikariConfig config = new HikariConfig();
        MySQLConfig userConfig = Config.getConfig(start);
        config.setDriverClassName("org.mariadb.jdbc.Driver");

        String scheme = userConfig.getConnectionScheme();

        String hostName = userConfig.getHostName();

        String port = userConfig.getPort() + "";
        if (!port.equals("-1")) {
            port = ":" + port;
        } else {
            port = "";
        }

        String databaseName = userConfig.getDatabaseName();

        String attributes = userConfig.getConnectionAttributes();
        if (!attributes.equals("")) {
            attributes = "?" + attributes;
        }

        config.setJdbcUrl("jdbc:" + scheme + "://" + hostName + port + "/" + databaseName + attributes);

        if (userConfig.getUser() != null) {
            config.setUsername(userConfig.getUser());
        }

        if (userConfig.getPassword() != null && !userConfig.getPassword().equals("")) {
            config.setPassword(userConfig.getPassword());
        }
        config.setMaximumPoolSize(userConfig.getConnectionPoolSize());
        config.setConnectionTimeout(5000);
        if (userConfig.getMinimumIdleConnections() != null) {
            config.setMinimumIdle(userConfig.getMinimumIdleConnections());
            config.setIdleTimeout(userConfig.getIdleConnectionTimeout());
        }
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        // TODO: set maxLifetimeValue to lesser than 10 mins so that the following error doesnt happen:
        // io.supertokens.storage.mysql.HikariLoggingAppender.doAppend(HikariLoggingAppender.java:117) | SuperTokens
        // - Failed to validate connection org.mariadb.jdbc.MariaDbConnection@79af83ae (Connection.setNetworkTimeout
        // cannot be called on a closed connection). Possibly consider using a shorter maxLifetime value.
        config.setPoolName(start.getUserPoolId() + "~" + start.getConnectionPoolId());
        try {
            hikariDataSource = new HikariDataSource(config);
        } catch (Exception e) {
            throw new SQLException(e);
        }

        try {
            try (Connection con = hikariDataSource.getConnection()) {
                this.postConnectCallback.apply(con);
            }
        } catch (StorageQueryException e) {
            // if an exception happens here, we want to set the hikariDataSource to null once again so that
            // whenever the getConnection is called again, we want to re-attempt creation of tables and tenant
            // entries for this storage
            hikariDataSource.close();
            hikariDataSource = null;
            throw e;
        }
    }

    private static int getTimeToWaitToInit(Start start) {
        int actualValue = 3600 * 1000;
        if (Start.isTesting) {
            Integer testValue = ConnectionPoolTestContent.getInstance(start)
                    .getValue(ConnectionPoolTestContent.TIME_TO_WAIT_TO_INIT);
            return Objects.requireNonNullElse(testValue, actualValue);
        }
        return actualValue;
    }

    private static int getRetryIntervalIfInitFails(Start start) {
        int actualValue = 10 * 1000;
        if (Start.isTesting) {
            Integer testValue = ConnectionPoolTestContent.getInstance(start)
                    .getValue(ConnectionPoolTestContent.RETRY_INTERVAL_IF_INIT_FAILS);
            return Objects.requireNonNullElse(testValue, actualValue);
        }
        return actualValue;
    }

    private static ConnectionPool getInstance(Start start) {
        return (ConnectionPool) start.getResourceDistributor().getResource(RESOURCE_KEY);
    }

    private static void removeInstance(Start start) {
        start.getResourceDistributor().removeResource(RESOURCE_KEY);
    }

    static boolean isAlreadyInitialised(Start start) {
        return getInstance(start) != null && getInstance(start).hikariDataSource != null;
    }

    static void initPool(Start start, boolean shouldWait, PostConnectCallback postConnectCallback)
            throws DbInitException, SQLException, StorageQueryException {
        if (isAlreadyInitialised(start)) {
            return;
        }
        Logging.info(start, "Setting up MySQL connection pool.", true);
        boolean longMessagePrinted = false;
        long maxTryTime = System.currentTimeMillis() + getTimeToWaitToInit(start);
        String errorMessage = "Error connecting to MySQL instance. Please make sure that MySQL is running and that "
                + "you have"
                + " specified the correct values for ('mysql_host' and 'mysql_port') or for 'mysql_connection_uri'";
        try {
            ConnectionPool con = new ConnectionPool(start, postConnectCallback);
            start.getResourceDistributor().setResource(RESOURCE_KEY, con);
            while (true) {
                try {
                    con.initialiseHikariDataSource();
                    break;
                } catch (Exception e) {
                    if (!shouldWait) {
                        throw new DbInitException(e);
                    }
                    if (e.getMessage().contains("Connection refused")) {
                        start.handleKillSignalForWhenItHappens();
                        if (System.currentTimeMillis() > maxTryTime) {
                            throw new DbInitException(errorMessage);
                        }
                        if (!longMessagePrinted) {
                            longMessagePrinted = true;
                            Logging.info(start, errorMessage, true);
                        }
                        double minsRemaining = (maxTryTime - System.currentTimeMillis()) / (1000.0 * 60);
                        NumberFormat formatter = new DecimalFormat("#0.0");
                        Logging.info(start,
                                "Trying again in a few seconds for " + formatter.format(minsRemaining) + " mins...",
                                true);
                        try {
                            if (Thread.interrupted()) {
                                throw new InterruptedException();
                            }
                            Thread.sleep(getRetryIntervalIfInitFails(start));
                        } catch (InterruptedException ex) {
                            throw new DbInitException(errorMessage);
                        }
                    } else {
                        throw e;
                    }
                }
            }
        } finally {
            start.removeShutdownHook();
        }
    }

    private static Connection getNewConnection(Start start) throws SQLException, StorageQueryException {
        if (getInstance(start) == null) {
            throw new IllegalStateException("Please call initPool before getConnection");
        }
        if (!start.enabled) {
            throw new SQLException("Storage layer disabled");
        }
        if (getInstance(start).hikariDataSource == null) {
            getInstance(start).initialiseHikariDataSource();
        }
        return getInstance(start).hikariDataSource.getConnection();
    }

    public static Connection getConnectionForProxyStorage(Start start) throws SQLException, StorageQueryException {
        return getNewConnection(start);
    }

    public static Connection getConnection(Start start) throws SQLException, StorageQueryException {
        if (start instanceof BulkImportProxyStorage) {
            return ((BulkImportProxyStorage) start).getTransactionConnection();
        }
        return getNewConnection(start);
    }

    static void close(Start start) {
        if (getInstance(start) == null) {
            return;
        }
        if (getInstance(start).hikariDataSource != null) {
            try {
                getInstance(start).hikariDataSource.close();
            } finally {
                // we mark it as null so that next time it's being initialised, it will be initialised again
                getInstance(start).hikariDataSource = null;
                removeInstance(start);
            }
        }
    }

    @FunctionalInterface
    public static interface PostConnectCallback {
        void apply(Connection connection) throws StorageQueryException;
    }
}
