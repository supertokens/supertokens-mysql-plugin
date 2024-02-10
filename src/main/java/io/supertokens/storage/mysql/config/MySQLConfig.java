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

package io.supertokens.storage.mysql.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.supertokens.pluginInterface.exceptions.InvalidConfigException;
import io.supertokens.storage.mysql.annotations.ConnectionPoolProperty;
import io.supertokens.storage.mysql.annotations.IgnoreForAnnotationCheck;
import io.supertokens.storage.mysql.annotations.NotConflictingWithinUserPool;
import io.supertokens.storage.mysql.annotations.UserPoolProperty;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MySQLConfig {

    @JsonProperty
    @IgnoreForAnnotationCheck
    private int mysql_config_version = -1;

    @JsonProperty
    @ConnectionPoolProperty
    private int mysql_connection_pool_size = 10;

    @JsonProperty
    @UserPoolProperty
    private String mysql_host = "localhost";

    @JsonProperty
    @UserPoolProperty
    private int mysql_port = 3306;

    @JsonProperty
    @ConnectionPoolProperty
    private String mysql_user = null;

    @JsonProperty
    @ConnectionPoolProperty
    private String mysql_password = null;

    @JsonProperty
    @UserPoolProperty
    private String mysql_database_name = "supertokens";

    @JsonProperty
    @NotConflictingWithinUserPool
    private String mysql_table_names_prefix = "";

    @JsonProperty
    @NotConflictingWithinUserPool
    private String mysql_key_value_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    private String mysql_session_info_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    private String mysql_emailpassword_users_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    private String mysql_emailpassword_pswd_reset_tokens_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    private String mysql_emailverification_tokens_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    private String mysql_emailverification_verified_emails_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    private String mysql_thirdparty_users_table_name = null;

    @JsonProperty
    @IgnoreForAnnotationCheck
    private String mysql_connection_uri = null;

    @ConnectionPoolProperty
    private String mysql_connection_attributes = "allowPublicKeyRetrieval=true";

    @ConnectionPoolProperty
    private String mysql_connection_scheme = "mysql";

    @JsonProperty
    @ConnectionPoolProperty
    private long mysql_idle_connection_timeout = 60000;

    @JsonProperty
    @ConnectionPoolProperty
    private Integer mysql_minimum_idle_connections = null;

    @IgnoreForAnnotationCheck
    boolean isValidAndNormalised = false;

    public static Set<String> getValidFields() {
        MySQLConfig config = new MySQLConfig();
        JsonObject configObj = new GsonBuilder().serializeNulls().create().toJsonTree(config).getAsJsonObject();

        Set<String> validFields = new HashSet<>();
        for (Map.Entry<String, JsonElement> entry : configObj.entrySet()) {
            validFields.add(entry.getKey());
        }
        return validFields;
    }

    public int getConnectionPoolSize() {
        return mysql_connection_pool_size;
    }

    public String getConnectionScheme() {
        return mysql_connection_scheme;
    }

    public String getConnectionAttributes() {
        return mysql_connection_attributes;
    }

    public String getHostName() {
        return mysql_host;
    }

    public int getPort() {
        return mysql_port;
    }

    public String getUser() {
        return mysql_user;
    }

    public String getPassword() {
        return mysql_password;
    }

    public String getDatabaseName() {
        return mysql_database_name;
    }

    public String getConnectionURI() {
        return mysql_connection_uri;
    }


    public String getUsersTable() {
        return addPrefixToTableName("all_auth_recipe_users");
    }


    public String getAppsTable() {
        return addPrefixToTableName("apps");
    }

    public String getTenantsTable() {
        return addPrefixToTableName("tenants");
    }

    public String getTenantConfigsTable() {
        return addPrefixToTableName("tenant_configs");
    }

    public String getTenantThirdPartyProvidersTable() {
        return addPrefixToTableName("tenant_thirdparty_providers");
    }

    public String getTenantThirdPartyProviderClientsTable() {
        return addPrefixToTableName("tenant_thirdparty_provider_clients");
    }

    public String getKeyValueTable() {
        return mysql_key_value_table_name;
    }

    public String getAppIdToUserIdTable() {
        return addPrefixToTableName("app_id_to_user_id");
    }

    public String getUserLastActiveTable() {
        return addPrefixToTableName("user_last_active");
    }

    public String getAccessTokenSigningKeysTable() {
        return addPrefixToTableName("session_access_token_signing_keys");
    }

    public String getSessionInfoTable() {
        return mysql_session_info_table_name;
    }

    public String getEmailPasswordUserToTenantTable() {
        return addPrefixToTableName("emailpassword_user_to_tenant");
    }

    public String getEmailPasswordUsersTable() {
        return mysql_emailpassword_users_table_name;
    }

    public String getPasswordResetTokensTable() {
        return mysql_emailpassword_pswd_reset_tokens_table_name;
    }

    public String getEmailVerificationTokensTable() {
        return mysql_emailverification_tokens_table_name;
    }

    public String getEmailVerificationTable() {
        return mysql_emailverification_verified_emails_table_name;
    }

    public String getThirdPartyUsersTable() {
        return mysql_thirdparty_users_table_name;
    }

    public long getIdleConnectionTimeout() {
        return mysql_idle_connection_timeout;
    }

    public Integer getMinimumIdleConnections() {
        return mysql_minimum_idle_connections;
    }

    public String getThirdPartyUserToTenantTable() {
        return addPrefixToTableName("thirdparty_user_to_tenant");
    }

    public String getPasswordlessUsersTable() {
        return addPrefixToTableName("passwordless_users");

    }

    public String getPasswordlessUserToTenantTable() {
        return addPrefixToTableName("passwordless_user_to_tenant");
    }

    public String getPasswordlessDevicesTable() {
        return addPrefixToTableName("passwordless_devices");
    }

    public String getPasswordlessCodesTable() {
        return addPrefixToTableName("passwordless_codes");
    }

    public String getJWTSigningKeysTable() {
        return addPrefixToTableName("jwt_signing_keys");
    }

    public String getUserMetadataTable() {
        return addPrefixToTableName("user_metadata");
    }

    public String getRolesTable() {
        return addPrefixToTableName("roles");
    }

    public String getUserRolesPermissionsTable() {
        return addPrefixToTableName("role_permissions");
    }

    public String getUserRolesTable() {
        return addPrefixToTableName("user_roles");
    }

    public String getUserIdMappingTable() {
        return addPrefixToTableName("userid_mapping");
    }

    public String getDashboardUsersTable() {
        return addPrefixToTableName("dashboard_users");
    }

    public String getDashboardSessionsTable() {
        return addPrefixToTableName("dashboard_user_sessions");
    }

    public String getTotpUsersTable() {
        return addPrefixToTableName("totp_users");
    }

    public String getTotpUserDevicesTable() {
        return addPrefixToTableName("totp_user_devices");
    }

    public String getTotpUsedCodesTable() {
        return addPrefixToTableName("totp_used_codes");
    }

    private String addPrefixToTableName(String tableName) {
        return mysql_table_names_prefix + tableName;
    }

    void validateAndNormalise() throws InvalidConfigException {
        if (isValidAndNormalised) {
            return;
        }

        if (mysql_connection_uri != null) {
            try {
                URI ignored = URI.create(mysql_connection_uri);
            } catch (Exception e) {
                throw new InvalidConfigException(
                        "The provided mysql connection URI has an incorrect format. Please use a format like "
                                + "mysql://[user[:[password]]@]host[:port][/dbname][?attr1=val1&attr2=val2...");
            }
        } else {
            if (this.getUser() == null) {
                throw new InvalidConfigException(
                        "'mysql_user' and 'mysql_connection_uri' are not set. Please set at least one of "
                                + "these values");
            }
        }

        if (mysql_connection_pool_size <= 0) {
            throw new InvalidConfigException(
                    "'mysql_connection_pool_size' in the config.yaml file must be > 0");
        }

        if (mysql_minimum_idle_connections != null) {
            if (mysql_minimum_idle_connections < 0) {
                throw new InvalidConfigException(
                        "'mysql_minimum_idle_connections' must be >= 0");
            }

            if (mysql_minimum_idle_connections > mysql_connection_pool_size) {
                throw new InvalidConfigException(
                        "'mysql_minimum_idle_connections' must be less than or equal to "
                                + "'mysql_connection_pool_size'");
            }
        }



        // Normalisation
        if (mysql_connection_uri != null) {
            { // mysql_connection_attributes
                URI uri = URI.create(mysql_connection_uri);
                String query = uri.getQuery();
                if (query != null) {
                    if (query.contains("allowPublicKeyRetrieval=")) {
                        mysql_connection_attributes = query;
                    } else {
                        mysql_connection_attributes = query + "&allowPublicKeyRetrieval=true";
                    }
                }
            }

            { // mysql_host
                if (mysql_connection_uri != null) {
                    URI uri = URI.create(mysql_connection_uri);
                    if (uri.getHost() != null) {
                        mysql_host = uri.getHost();
                    }
                }
            }

            { // mysql_port
                if (mysql_connection_uri != null) {
                    URI uri = URI.create(mysql_connection_uri);
                    mysql_port = uri.getPort();
                }
            }

            { // mysql_connection_scheme
                URI uri = URI.create(mysql_connection_uri);

                // sometimes if the scheme is missing, the host is returned as the scheme. To
                // prevent that,
                // we have a check
                String host = this.getHostName();
                if (uri.getScheme() != null && !uri.getScheme().equals(host)) {
                    mysql_connection_scheme = uri.getScheme();
                }
            }

            { // mysql_user
                if (mysql_user == null) {
                    if (mysql_connection_uri != null) {
                        URI uri = URI.create(mysql_connection_uri);
                        String userInfo = uri.getUserInfo();
                        if (userInfo != null) {
                            String[] userInfoArray = userInfo.split(":");
                            if (userInfoArray.length > 0 && !userInfoArray[0].equals("")) {
                                mysql_user = userInfoArray[0];
                            }
                        }
                    }
                }
            }
            { // mysql_password
                if (mysql_password == null) {
                    if (mysql_connection_uri != null) {
                        URI uri = URI.create(mysql_connection_uri);
                        String userInfo = uri.getUserInfo();
                        if (userInfo != null) {
                            String[] userInfoArray = userInfo.split(":");
                            if (userInfoArray.length > 1 && !userInfoArray[1].equals("")) {
                                mysql_password = userInfoArray[1];
                            }
                        }
                    }
                }
            }

            { // mysql_database_name
                if (mysql_connection_uri != null) {
                    URI uri = URI.create(mysql_connection_uri);
                    String path = uri.getPath();
                    if (path != null && !path.equals("") && !path.equals("/")) {
                        mysql_database_name = path;
                        if (path.startsWith("/")) {
                            mysql_database_name = path.substring(1);
                        }
                    }
                }
            }
        }

        { // mysql_table_names_prefix
            if (mysql_table_names_prefix == null) {
                mysql_table_names_prefix = "";
            }
            mysql_table_names_prefix = mysql_table_names_prefix.trim();
            if (!mysql_table_names_prefix.isEmpty()) {
                mysql_table_names_prefix = mysql_table_names_prefix + "_";
            }
        }

        { // mysql_connection_scheme
            mysql_connection_scheme = mysql_connection_scheme.trim();
        }

        { // mysql_host
            if (mysql_host == null) {
                mysql_host = "localhost";
            }
        }

        { // mysql_port
            if (mysql_port < 0) {
                mysql_port = 3306;
            }
        }

        { // mysql_database_name
            if (mysql_database_name == null) {
                mysql_database_name = "supertokens";
            }
            mysql_database_name = mysql_database_name.trim();
        }

        if (mysql_key_value_table_name == null) {
            mysql_key_value_table_name = addPrefixToTableName("key_value");
        }

        if (mysql_session_info_table_name == null) {
            mysql_session_info_table_name = addPrefixToTableName("session_info");
        }

        if (mysql_emailpassword_users_table_name == null) {
            mysql_emailpassword_users_table_name = addPrefixToTableName("emailpassword_users");
        }

        if (mysql_emailpassword_pswd_reset_tokens_table_name == null) {
            mysql_emailpassword_pswd_reset_tokens_table_name = addPrefixToTableName("emailpassword_pswd_reset_tokens");
        }

        if (mysql_emailverification_tokens_table_name == null) {
            mysql_emailverification_tokens_table_name = addPrefixToTableName("emailverification_tokens");
        }

        if (mysql_emailverification_verified_emails_table_name == null) {
            mysql_emailverification_verified_emails_table_name = addPrefixToTableName("emailverification_verified_emails");
        }

        if (mysql_thirdparty_users_table_name == null) {
            mysql_thirdparty_users_table_name = addPrefixToTableName("thirdparty_users");
        }

        isValidAndNormalised = true;
    }

    public void assertThatConfigFromSameUserPoolIsNotConflicting(MySQLConfig otherConfig) throws InvalidConfigException {
        for (Field field : MySQLConfig.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(NotConflictingWithinUserPool.class)) {
                try {
                    if (!Objects.equals(field.get(this), field.get(otherConfig))) {
                        throw new InvalidConfigException(
                                "You cannot set different values for " + field.getName() +
                                        " for the same user pool");
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    public String getUserPoolId() {
        StringBuilder userPoolId = new StringBuilder();
        for (Field field : MySQLConfig.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(UserPoolProperty.class)) {
                userPoolId.append("|");
                try {
                    if (field.get(this) != null) {
                        userPoolId.append(field.get(this).toString());
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return userPoolId.toString();
    }

    public String getConnectionPoolId() {
        StringBuilder connectionPoolId = new StringBuilder();
        for (Field field : MySQLConfig.class.getDeclaredFields()) {
            if (field.isAnnotationPresent(ConnectionPoolProperty.class)) {
                connectionPoolId.append("|");
                try {
                    if (field.get(this) != null) {
                        connectionPoolId.append(field.get(this).toString());
                    }
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return connectionPoolId.toString();
    }

    public String getTablePrefix() {
        return mysql_table_names_prefix;
    }
}
