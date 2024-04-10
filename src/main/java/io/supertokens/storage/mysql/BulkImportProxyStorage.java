/*
 *    Copyright (c) 2024, VRAI Labs and/or its affiliates. All rights reserved.
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
 */

package io.supertokens.storage.mysql;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import com.google.gson.JsonObject;

import io.supertokens.pluginInterface.LOG_LEVEL;
import io.supertokens.pluginInterface.exceptions.InvalidConfigException;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.pluginInterface.multitenancy.TenantIdentifier;
import io.supertokens.pluginInterface.sqlStorage.TransactionConnection;
import io.supertokens.storage.mysql.config.Config;


/**
 * BulkImportProxyStorage is a class extending Start, serving as a Storage instance in the bulk import user cronjob.
 * This cronjob extensively utilizes existing queries to import users, all of which internally operate within transactions.
 * 
 * For the purpose of bulkimport cronjob, we aim to employ a single connection for all queries and rollback any operations in case of query failures.
 * To achieve this, we override the startTransactionHelper method to utilize the same connection and prevent automatic query commits even upon transaction success.
 * Subsequently, the cronjob is responsible for committing the transaction after ensuring the successful execution of all queries.
 */

public class BulkImportProxyStorage extends Start {
    private BulkImportProxyConnection connection;

    public synchronized Connection getTransactionConnection() throws SQLException {
        if (this.connection == null) {
            Connection con = ConnectionPool.getConnectionForProxyStorage(this);
            this.connection = new BulkImportProxyConnection(con);
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
        }
        return this.connection;
    }

    @Override
    protected <T> T startTransactionHelper(TransactionLogic<T> logic, TransactionIsolationLevel isolationLevel)
            throws StorageQueryException, StorageTransactionLogicException, SQLException {
        return logic.mainLogicAndCommit(new TransactionConnection(getTransactionConnection()));
    }

    @Override
    public void commitTransaction(TransactionConnection con) throws StorageQueryException {
        // We do not want to commit the queries when using the BulkImportProxyStorage to be able to rollback everything 
        // if any query fails while importing the user
    }

    @Override
    public void loadConfig(JsonObject configJson, Set<LOG_LEVEL> logLevels, TenantIdentifier tenantIdentifier)
            throws InvalidConfigException {
        // We are overriding the loadConfig method to set the connection pool size
        // to 1 to avoid creating many connections for the bulk import cronjob
        configJson.addProperty("postgresql_connection_pool_size", 1);
        Config.loadConfig(this, configJson, logLevels, tenantIdentifier);
    }

    @Override
    public void closeConnectionForBulkImportProxyStorage() throws StorageQueryException {
        try {
            this.connection.close();
            this.connection = null;
        } catch (SQLException e) {
            throw new StorageQueryException(e);
        }
    }

    @Override
    public void commitTransactionForBulkImportProxyStorage() throws StorageQueryException {
        try {
            this.connection.commitForBulkImportProxyStorage();
        } catch (SQLException e) {
            throw new StorageQueryException(e);
        }
    }

    @Override
    public void rollbackTransactionForBulkImportProxyStorage() throws StorageQueryException {
        try {
            this.connection.rollbackForBulkImportProxyStorage();
        } catch (SQLException e) {
            throw new StorageQueryException(e);
        }
    }
}
