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

package io.supertokens.storage.mysql.queries.multitenancy;

import io.supertokens.pluginInterface.RowMapper;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.pluginInterface.multitenancy.*;
import io.supertokens.storage.mysql.Start;
import io.supertokens.storage.mysql.queries.utils.JsonUtils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static io.supertokens.storage.mysql.QueryExecutorTemplate.execute;
import static io.supertokens.storage.mysql.QueryExecutorTemplate.update;
import static io.supertokens.storage.mysql.config.Config.getConfig;

public class TenantConfigSQLHelper {
    public static class TenantConfigRowMapper implements RowMapper<TenantConfig, ResultSet> {
        ThirdPartyConfig.Provider[] providers;
        String[] firstFactors;
        String[] requiredSecondaryFactors;

        private TenantConfigRowMapper(ThirdPartyConfig.Provider[] providers, String[] firstFactors, String[] requiredSecondaryFactors) {
            this.providers = providers;
            this.firstFactors = firstFactors;
            this.requiredSecondaryFactors = requiredSecondaryFactors;
        }

        public static TenantConfigRowMapper getInstance(ThirdPartyConfig.Provider[] providers, String[] firstFactors, String[] requiredSecondaryFactors) {
            return new TenantConfigRowMapper(providers, firstFactors, requiredSecondaryFactors);
        }

        @Override
        public TenantConfig map(ResultSet result) throws StorageQueryException {
            try {
                return new TenantConfig(
                        new TenantIdentifier(result.getString("connection_uri_domain"), result.getString("app_id"), result.getString("tenant_id")),
                        new EmailPasswordConfig(result.getBoolean("email_password_enabled")),
                        new ThirdPartyConfig(result.getBoolean("third_party_enabled"), this.providers),
                        new PasswordlessConfig(result.getBoolean("passwordless_enabled")),
                        firstFactors.length == 0 ? null : firstFactors,
                        requiredSecondaryFactors.length == 0 ? null : requiredSecondaryFactors,
                        JsonUtils.stringToJsonObject(result.getString("core_config"))
                );
            } catch (Exception e) {
                throw new StorageQueryException(e);
            }
        }
    }

    public static TenantConfig[] selectAll(Start start, HashMap<TenantIdentifier, HashMap<String, ThirdPartyConfig.Provider>> providerMap, HashMap<TenantIdentifier, String[]> firstFactorsMap, HashMap<TenantIdentifier, String[]> requiredSecondaryFactorsMap)
            throws SQLException, StorageQueryException {
        String QUERY = "SELECT connection_uri_domain, app_id, tenant_id, core_config,"
                + " email_password_enabled, passwordless_enabled, third_party_enabled FROM "
                + getConfig(start).getTenantConfigsTable() + ";";

        TenantConfig[] tenantConfigs = execute(start, QUERY, pst -> {}, result -> {
            List<TenantConfig> temp = new ArrayList<>();
            while (result.next()) {
                TenantIdentifier tenantIdentifier = new TenantIdentifier(result.getString("connection_uri_domain"), result.getString("app_id"), result.getString("tenant_id"));
                ThirdPartyConfig.Provider[] providers = null;
                if (providerMap.containsKey(tenantIdentifier)) {
                    providers = providerMap.get(tenantIdentifier).values().toArray(new ThirdPartyConfig.Provider[0]);
                }
                String[] firstFactors = firstFactorsMap.containsKey(tenantIdentifier) ? firstFactorsMap.get(tenantIdentifier) : new String[0];

                String[] requiredSecondaryFactors = requiredSecondaryFactorsMap.containsKey(tenantIdentifier) ? requiredSecondaryFactorsMap.get(tenantIdentifier) : new String[0];

                temp.add(TenantConfigSQLHelper.TenantConfigRowMapper.getInstance(providers, firstFactors, requiredSecondaryFactors).mapOrThrow(result));
            }
            TenantConfig[] finalResult = new TenantConfig[temp.size()];
            for (int i = 0; i < temp.size(); i++) {
                finalResult[i] = temp.get(i);
            }
            return finalResult;
        });
        return tenantConfigs;
    }

    public static void create(Start start, Connection sqlCon, TenantConfig tenantConfig)
            throws SQLException, StorageTransactionLogicException {
        String QUERY = "INSERT INTO " + getConfig(start).getTenantConfigsTable()
                + "(connection_uri_domain, app_id, tenant_id, core_config,"
                + " email_password_enabled, passwordless_enabled, third_party_enabled)"
                + " VALUES(?, ?, ?, ?, ?, ?, ?)";

        try {
            update(sqlCon, QUERY, pst -> {
                pst.setString(1, tenantConfig.tenantIdentifier.getConnectionUriDomain());
                pst.setString(2, tenantConfig.tenantIdentifier.getAppId());
                pst.setString(3, tenantConfig.tenantIdentifier.getTenantId());
                pst.setString(4, tenantConfig.coreConfig.toString());
                pst.setBoolean(5, tenantConfig.emailPasswordConfig.enabled);
                pst.setBoolean(6, tenantConfig.passwordlessConfig.enabled);
                pst.setBoolean(7, tenantConfig.thirdPartyConfig.enabled);
            });
        } catch (StorageQueryException e) {
            throw new StorageTransactionLogicException(e);
        }
    }

}
