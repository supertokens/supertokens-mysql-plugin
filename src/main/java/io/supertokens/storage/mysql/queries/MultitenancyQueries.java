/*
 *    Copyright (c) 2023, VRAI Labs and/or its affiliates. All rights reserved.
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

package io.supertokens.storage.mysql.queries;

import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.pluginInterface.multitenancy.TenantConfig;
import io.supertokens.pluginInterface.multitenancy.TenantIdentifier;
import io.supertokens.pluginInterface.multitenancy.ThirdPartyConfig;
import io.supertokens.pluginInterface.multitenancy.exceptions.DuplicateClientTypeException;
import io.supertokens.pluginInterface.multitenancy.exceptions.DuplicateTenantException;
import io.supertokens.pluginInterface.multitenancy.exceptions.DuplicateThirdPartyIdException;
import io.supertokens.pluginInterface.multitenancy.exceptions.TenantOrAppNotFoundException;
import io.supertokens.storage.mysql.Start;
import io.supertokens.storage.mysql.config.Config;
import io.supertokens.storage.mysql.queries.multitenancy.MfaSqlHelper;
import io.supertokens.storage.mysql.queries.multitenancy.TenantConfigSQLHelper;
import io.supertokens.storage.mysql.queries.multitenancy.ThirdPartyProviderClientSQLHelper;
import io.supertokens.storage.mysql.queries.multitenancy.ThirdPartyProviderSQLHelper;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;

import static io.supertokens.storage.mysql.QueryExecutorTemplate.update;
import static io.supertokens.storage.mysql.config.Config.getConfig;

public class MultitenancyQueries {
    static String getQueryToCreateTenantConfigsTable(Start start) {
        String tenantConfigsTable = Config.getConfig(start).getTenantConfigsTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + tenantConfigsTable + " ("
                + "connection_uri_domain VARCHAR(256) DEFAULT '',"
                + "app_id VARCHAR(64) DEFAULT 'public',"
                + "tenant_id VARCHAR(64) DEFAULT 'public',"
                + "core_config TEXT,"
                + "email_password_enabled BOOLEAN,"
                + "passwordless_enabled BOOLEAN,"
                + "third_party_enabled BOOLEAN,"
                + "PRIMARY KEY (connection_uri_domain, app_id, tenant_id)"
                + ");";
        // @formatter:on
    }

    static String getQueryToCreateTenantThirdPartyProvidersTable(Start start) {
        String tenantThirdPartyProvidersTable = Config.getConfig(start).getTenantThirdPartyProvidersTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + tenantThirdPartyProvidersTable + " ("
                + "connection_uri_domain VARCHAR(256) DEFAULT '',"
                + "app_id VARCHAR(64) DEFAULT 'public',"
                + "tenant_id VARCHAR(64) DEFAULT 'public',"
                + "third_party_id VARCHAR(28) NOT NULL,"
                + "name VARCHAR(64),"
                + "authorization_endpoint TEXT,"
                + "authorization_endpoint_query_params TEXT,"
                + "token_endpoint TEXT,"
                + "token_endpoint_body_params TEXT,"
                + "user_info_endpoint TEXT,"
                + "user_info_endpoint_query_params TEXT,"
                + "user_info_endpoint_headers TEXT,"
                + "jwks_uri TEXT,"
                + "oidc_discovery_endpoint TEXT,"
                + "require_email BOOLEAN,"
                + "user_info_map_from_id_token_payload_user_id VARCHAR(64),"
                + "user_info_map_from_id_token_payload_email VARCHAR(64),"
                + "user_info_map_from_id_token_payload_email_verified VARCHAR(64),"
                + "user_info_map_from_user_info_endpoint_user_id VARCHAR(64),"
                + "user_info_map_from_user_info_endpoint_email VARCHAR(64),"
                + "user_info_map_from_user_info_endpoint_email_verified VARCHAR(64),"
                + "PRIMARY KEY (connection_uri_domain, app_id, tenant_id, third_party_id),"
                + "FOREIGN KEY(connection_uri_domain, app_id, tenant_id)"
                + " REFERENCES " + Config.getConfig(start).getTenantConfigsTable() +  " (connection_uri_domain, app_id, tenant_id) ON DELETE CASCADE"
                + ");";
        // @formatter:on
    }

    static String getQueryToCreateTenantThirdPartyProviderClientsTable(Start start) {
        String tenantThirdPartyProvidersTable = Config.getConfig(start).getTenantThirdPartyProviderClientsTable();
        return "CREATE TABLE IF NOT EXISTS " + tenantThirdPartyProvidersTable + " ("
                + "connection_uri_domain VARCHAR(256) DEFAULT '',"
                + "app_id VARCHAR(64) DEFAULT 'public',"
                + "tenant_id VARCHAR(64) DEFAULT 'public',"
                + "third_party_id VARCHAR(28) NOT NULL,"
                + "client_type VARCHAR(64) NOT NULL DEFAULT '',"
                + "client_id VARCHAR(256) NOT NULL,"
                + "client_secret TEXT,"
                + "scope TEXT,"
                + "force_pkce BOOLEAN,"
                + "additional_config TEXT,"
                + " PRIMARY KEY (connection_uri_domain, app_id, tenant_id, third_party_id, client_type),"
                + "FOREIGN KEY(connection_uri_domain, app_id, tenant_id, third_party_id)"
                + " REFERENCES " + Config.getConfig(start).getTenantThirdPartyProvidersTable() + " (connection_uri_domain, app_id, tenant_id, third_party_id) ON DELETE CASCADE"
                + ");";
    }

    public static String getQueryToCreateFirstFactorsTable(Start start) {
        String tableName = Config.getConfig(start).getTenantFirstFactorsTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "connection_uri_domain VARCHAR(256) DEFAULT '',"
                + "app_id VARCHAR(64) DEFAULT 'public',"
                + "tenant_id VARCHAR(64) DEFAULT 'public',"
                + "factor_id VARCHAR(128),"
                + "PRIMARY KEY (connection_uri_domain, app_id, tenant_id, factor_id),"
                + "FOREIGN KEY (connection_uri_domain, app_id, tenant_id)"
                + " REFERENCES " + Config.getConfig(start).getTenantConfigsTable() +  " (connection_uri_domain, app_id, tenant_id) ON DELETE CASCADE"
                + ");";
        // @formatter:on
    }

    public static String getQueryToCreateRequiredSecondaryFactorsTable(Start start) {
        String tableName = Config.getConfig(start).getTenantRequiredSecondaryFactorsTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "connection_uri_domain VARCHAR(256) DEFAULT '',"
                + "app_id VARCHAR(64) DEFAULT 'public',"
                + "tenant_id VARCHAR(64) DEFAULT 'public',"
                + "factor_id VARCHAR(128),"
                + "PRIMARY KEY (connection_uri_domain, app_id, tenant_id, factor_id),"
                + "FOREIGN KEY (connection_uri_domain, app_id, tenant_id)"
                + " REFERENCES " + Config.getConfig(start).getTenantConfigsTable()
                + " (connection_uri_domain, app_id, tenant_id) ON DELETE CASCADE);";
        // @formatter:on
    }

    private static void executeCreateTenantQueries(Start start, Connection sqlCon, TenantConfig tenantConfig)
            throws SQLException, StorageTransactionLogicException, StorageQueryException {

        try {
            TenantConfigSQLHelper.create(start, sqlCon, tenantConfig);
        } catch (SQLIntegrityConstraintViolationException e) {
            if (start.isPrimaryKeyError(e.getMessage(), Config.getConfig(start).getTenantConfigsTable())) {
                throw new StorageTransactionLogicException(new DuplicateTenantException());
            } else {
                throw e;
            }
        }

        for (ThirdPartyConfig.Provider provider : tenantConfig.thirdPartyConfig.providers) {
            try {
                ThirdPartyProviderSQLHelper.create(start, sqlCon, tenantConfig, provider);
            } catch (SQLIntegrityConstraintViolationException e) {
                if (start.isPrimaryKeyError(e.getMessage(), Config.getConfig(start).getTenantThirdPartyProvidersTable())) {
                    throw new StorageTransactionLogicException(new DuplicateThirdPartyIdException());
                } else {
                    throw e;
                }
            }

            for (ThirdPartyConfig.ProviderClient providerClient : provider.clients) {
                try {
                    ThirdPartyProviderClientSQLHelper.create(start, sqlCon, tenantConfig, provider, providerClient);
                } catch (SQLIntegrityConstraintViolationException e) {
                    if (start.isPrimaryKeyError(e.getMessage(), Config.getConfig(start).getTenantThirdPartyProviderClientsTable())) {
                        throw new StorageTransactionLogicException(new DuplicateClientTypeException());
                    } else {
                        throw e;
                    }
                }
            }
        }

        MfaSqlHelper.createFirstFactors(start, sqlCon, tenantConfig.tenantIdentifier, tenantConfig.firstFactors);
        MfaSqlHelper.createRequiredSecondaryFactors(start, sqlCon, tenantConfig.tenantIdentifier, tenantConfig.requiredSecondaryFactors);
    }

    public static void createTenantConfig(Start start, TenantConfig tenantConfig) throws StorageQueryException, StorageTransactionLogicException {
        start.startTransaction(con -> {
            Connection sqlCon = (Connection) con.getConnection();
            {
                try {
                    executeCreateTenantQueries(start, sqlCon, tenantConfig);
                    sqlCon.commit();
                } catch (SQLException throwables) {
                    throw new StorageTransactionLogicException(throwables);
                }
            }

            return null;
        });
    }

    public static boolean deleteTenantConfig(Start start, TenantIdentifier tenantIdentifier) throws StorageQueryException {
        try {
            String QUERY = "DELETE FROM " + getConfig(start).getTenantConfigsTable()
                    + " WHERE connection_uri_domain = ? AND app_id = ? AND tenant_id = ?";

            int numRows = update(start, QUERY, pst -> {
                pst.setString(1, tenantIdentifier.getConnectionUriDomain());
                pst.setString(2, tenantIdentifier.getAppId());
                pst.setString(3, tenantIdentifier.getTenantId());
            });

            return numRows > 0;

        } catch (SQLException throwables) {
            throw new StorageQueryException(throwables);
        }
    }

    public static void overwriteTenantConfig(Start start, TenantConfig tenantConfig) throws StorageQueryException, StorageTransactionLogicException {
        start.startTransaction(con -> {
            Connection sqlCon = (Connection) con.getConnection();
            {
                try {
                    {
                        String QUERY = "DELETE FROM " + getConfig(start).getTenantConfigsTable()
                                + " WHERE connection_uri_domain = ? AND app_id = ? AND tenant_id = ?;";
                        int rowsAffected = update(sqlCon, QUERY, pst -> {
                            pst.setString(1, tenantConfig.tenantIdentifier.getConnectionUriDomain());
                            pst.setString(2, tenantConfig.tenantIdentifier.getAppId());
                            pst.setString(3, tenantConfig.tenantIdentifier.getTenantId());
                        });
                        if (rowsAffected == 0) {
                            throw new StorageTransactionLogicException(new TenantOrAppNotFoundException(tenantConfig.tenantIdentifier));
                        }
                    }

                    {
                        executeCreateTenantQueries(start, sqlCon, tenantConfig);
                    }

                    sqlCon.commit();

                } catch (SQLException throwables) {
                    throw new StorageTransactionLogicException(throwables);
                }
            }

            return null;
        });
    }

    public static TenantConfig[] getAllTenants(Start start) throws StorageQueryException {
        try {

            // Map TenantIdentifier -> thirdPartyId -> clientType
            HashMap<TenantIdentifier, HashMap<String, HashMap<String, ThirdPartyConfig.ProviderClient>>> providerClientsMap = ThirdPartyProviderClientSQLHelper.selectAll(start);

            // Map (tenantIdentifier) -> thirdPartyId -> provider
            HashMap<TenantIdentifier, HashMap<String, ThirdPartyConfig.Provider>> providerMap = ThirdPartyProviderSQLHelper.selectAll(start, providerClientsMap);

            // Map (tenantIdentifier) -> firstFactors
            HashMap<TenantIdentifier, String[]> firstFactorsMap = MfaSqlHelper.selectAllFirstFactors(start);

            // Map (tenantIdentifier) -> requiredSecondaryFactors
            HashMap<TenantIdentifier, String[]> requiredSecondaryFactorsMap = MfaSqlHelper.selectAllRequiredSecondaryFactors(start);

            return TenantConfigSQLHelper.selectAll(start, providerMap, firstFactorsMap, requiredSecondaryFactorsMap);
        } catch (SQLException throwables) {
            throw new StorageQueryException(throwables);
        }
    }

    public static void addTenantIdInTargetStorage(Start start, TenantIdentifier tenantIdentifier) throws
            StorageTransactionLogicException, StorageQueryException {
        {
            start.startTransaction(con -> {
                Connection sqlCon = (Connection) con.getConnection();
                long currentTime = System.currentTimeMillis();
                try {
                    {
                        // ON CONFLICT DO NOTHING
                        String QUERY = "INSERT INTO " + getConfig(start).getAppsTable()
                                + "(app_id, created_at_time) "
                                + "SELECT ?, ? WHERE NOT EXISTS ("
                                + " SELECT app_id FROM " + getConfig(start).getAppsTable()
                                + " WHERE app_id = ?"
                                + ")";
                        update(sqlCon, QUERY, pst -> {
                            pst.setString(1, tenantIdentifier.getAppId());
                            pst.setLong(2,  currentTime);
                            pst.setString(3, tenantIdentifier.getAppId());
                        });
                    }

                    {
                        // ON CONFLICT DO NOTHING
                        String QUERY = "INSERT INTO " + getConfig(start).getTenantsTable()
                                + "(app_id, tenant_id, created_at_time)"
                                + "SELECT ?, ?, ? WHERE NOT EXISTS ("
                                + " SELECT app_id, tenant_id FROM " + getConfig(start).getTenantsTable()
                                + " WHERE app_id = ? AND tenant_id = ?"
                                + ")";

                        update(sqlCon, QUERY, pst -> {
                            pst.setString(1, tenantIdentifier.getAppId());
                            pst.setString(2, tenantIdentifier.getTenantId());
                            pst.setLong(3,  currentTime);
                            pst.setString(4, tenantIdentifier.getAppId());
                            pst.setString(5, tenantIdentifier.getTenantId());
                        });
                    }

                    sqlCon.commit();
                } catch (SQLException throwables) {
                    throw new StorageTransactionLogicException(throwables);
                }
                return null;
            });
        }
    }

    public static void deleteTenantIdInTargetStorage(Start start, TenantIdentifier tenantIdentifier)
        throws StorageQueryException {
        try {
            if (tenantIdentifier.getTenantId().equals(TenantIdentifier.DEFAULT_TENANT_ID)) {
                // Delete the app
                String QUERY = "DELETE FROM " + getConfig(start).getAppsTable()
                        + " WHERE app_id = ?";

                update(start, QUERY, pst -> {
                    pst.setString(1, tenantIdentifier.getAppId());
                });
            } else {
                // Delete the tenant
                String QUERY = "DELETE FROM " + getConfig(start).getTenantsTable()
                        + " WHERE app_id = ? AND tenant_id = ?";

                update(start, QUERY, pst -> {
                    pst.setString(1, tenantIdentifier.getAppId());
                    pst.setString(2, tenantIdentifier.getTenantId());
                });
            }

        } catch (SQLException throwables) {
            throw new StorageQueryException(throwables);
        }
    }
}
