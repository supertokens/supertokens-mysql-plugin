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
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.supertokens.pluginInterface.ConfigFieldInfo;
import io.supertokens.pluginInterface.exceptions.InvalidConfigException;
import io.supertokens.storage.mysql.Start;
import io.supertokens.storage.mysql.annotations.*;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.*;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MySQLConfig {

    @JsonProperty
    @IgnoreForAnnotationCheck
    private int mysql_config_version = -1;

    @JsonProperty
    @ConnectionPoolProperty
    @DashboardInfo(
            description = "Defines the connection pool size to MySQL. Please see https://github" +
                    ".com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing",
            defaultValue = "10", isOptional = true, isEditable = true)
    private int mysql_connection_pool_size = 10;

    @JsonProperty
    @UserPoolProperty
    @DashboardInfo(
            description = "Specify the mysql host url here. For example: - \"localhost\" - \"192.168.0.1\" - \"<IP to" +
                    " cloud instance>\" - \"example.com\"",
            defaultValue = "\"localhost\"", isOptional = true)
    private String mysql_host = "localhost";

    @JsonProperty
    @UserPoolProperty
    @DashboardInfo(description = "Specify the port to use when connecting to MySQL instance.", defaultValue = "3306",
            isOptional = true)
    private int mysql_port = 3306;

    @JsonProperty
    @ConnectionPoolProperty
    @DashboardInfo(
            description = "The MySQL user to use to query the database. If the relevant tables are not already " +
                    "created by you, this user should have the ability to create new tables. To see the tables " +
                    "needed, visit: https://supertokens.com/docs/thirdpartyemailpassword/pre-built-ui/setup/database" +
                    "-setup/mysql",
            isOptional = true, defaultValue = "\"root\"")
    private String mysql_user = null;

    @JsonProperty
    @ConnectionPoolProperty
    @DashboardInfo(
            description = "Password for the MySQL user. If you have not set a password make this an empty string.",
            isOptional = true, defaultValue = "no password")
    private String mysql_password = null;

    @JsonProperty
    @UserPoolProperty
    @DashboardInfo(description = "The database name to store SuperTokens related data.",
            defaultValue = "\"supertokens\"", isOptional = true)
    private String mysql_database_name = "supertokens";

    @JsonProperty
    @NotConflictingWithinUserPool
    @DashboardInfo(
            description = "A prefix to add to all table names managed by SuperTokens. An \"_\" will be added between " +
                    "this prefix and the actual table name if the prefix is defined.",
            defaultValue = "\"\"", isOptional = true)
    private String mysql_table_names_prefix = "";

    @JsonProperty
    @NotConflictingWithinUserPool
    @DashboardInfo(
            description = "Specify the name of the table that will store secret keys and app info necessary for the " +
                    "functioning sessions.",
            defaultValue = "\"key_value\"", isOptional = true)
    private String mysql_key_value_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    @DashboardInfo(description = "Specify the name of the table that will store the session info for users.",
            defaultValue = "\"session_info\"", isOptional = true)
    private String mysql_session_info_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    @DashboardInfo(
            description = "Specify the name of the table that will store the user information, along with their email" +
                    " and hashed password.",
            defaultValue = "\"emailpassword_users\"", isOptional = true)
    private String mysql_emailpassword_users_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    @DashboardInfo(description = "Specify the name of the table that will store the password reset tokens for users.",
            defaultValue = "\"emailpassword_pswd_reset_tokens\"", isOptional = true)
    private String mysql_emailpassword_pswd_reset_tokens_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    @DashboardInfo(
            description = "Specify the name of the table that will store the email verification tokens for users.",
            defaultValue = "\"emailverification_tokens\"", isOptional = true)
    private String mysql_emailverification_tokens_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    @DashboardInfo(description = "Specify the name of the table that will store the verified email addresses.",
            defaultValue = "\"emailverification_verified_emails\"", isOptional = true)
    private String mysql_emailverification_verified_emails_table_name = null;

    @JsonProperty
    @NotConflictingWithinUserPool
    @DashboardInfo(description = "Specify the name of the table that will store the thirdparty recipe users.",
            defaultValue = "\"thirdparty_users\"", isOptional = true)
    private String mysql_thirdparty_users_table_name = null;

    @JsonProperty
    @IgnoreForAnnotationCheck
    @DashboardInfo(
            description = "Specify the MySQL connection URI in the following format: " +
                    "mysql://[user[:[password]]@]host[:port][/dbname][?attr1=val1&attr2=val2... Values provided via " +
                    "other configs will override values provided by this config.",
            defaultValue = "null", isOptional = true)
    private String mysql_connection_uri = null;

    @ConnectionPoolProperty
    @DashboardInfo(description = "The connection attributes of the MySQL database.",
            defaultValue = "\"allowPublicKeyRetrieval=true\"", isOptional = true)
    private String mysql_connection_attributes = "allowPublicKeyRetrieval=true";

    @ConnectionPoolProperty
    @DashboardInfo(description = "The scheme of the MySQL database.", defaultValue = "\"postgresql\"",
            isOptional = true)
    private String mysql_connection_scheme = "mysql";

    @JsonProperty
    @ConnectionPoolProperty
    @DashboardInfo(description = "Timeout in milliseconds for the idle connections to be closed.",
            defaultValue = "60000", isOptional = true, isEditable = true)
    private long mysql_idle_connection_timeout = 60000;

    @JsonProperty
    @ConnectionPoolProperty
    @DashboardInfo(
            description = "Minimum number of idle connections to be kept active. If not set, minimum idle connections" +
                    " will be same as the connection pool size.",
            defaultValue = "null", isOptional = true, isEditable = true)
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

    public String getTenantFirstFactorsTable() {
        return addPrefixToTableName("tenant_first_factors");
    }

    public String getTenantRequiredSecondaryFactorsTable() {
        return addPrefixToTableName("tenant_required_secondary_factors");
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

    public String getBulkImportUsersTable() {
        return addPrefixToTableName("bulk_import_users");
    }

    public String getOAuthClientsTable() {
        return addPrefixToTableName("oauth_clients");
    }

    public String getOAuthM2MTokensTable() {
        return addPrefixToTableName("oauth_m2m_tokens");
    }

    public String getOAuthSessionsTable() {
        return addPrefixToTableName("oauth_sessions");
    }

    public String getOAuthLogoutChallengesTable() {
        return addPrefixToTableName("oauth_logout_challenges");
    }

    public String getWebAuthNUsersTable(){ return  addPrefixToTableName("webauthn_users");}

    public String getWebAuthNUserToTenantTable(){ return  addPrefixToTableName("webauthn_user_to_tenant"); }

    public String getWebAuthNGeneratedOptionsTable() { return   addPrefixToTableName("webauthn_generated_options"); }

    public String getWebAuthNCredentialsTable() { return  addPrefixToTableName("webauthn_credentials"); }

    private String addPrefixToTableName(String tableName) {
        return mysql_table_names_prefix + tableName;
    }

    public String getWebAuthNAccountRecoveryTokenTable() { return   addPrefixToTableName("webauthn_account_recovery_tokens"); }

    public static ArrayList<ConfigFieldInfo> getConfigFieldsInfoForDashboard(Start start) {
        ArrayList<ConfigFieldInfo> result = new ArrayList<ConfigFieldInfo>();

        JsonObject tenantConfig = new Gson().toJsonTree(Config.getConfig(start)).getAsJsonObject();

        MySQLConfig defaultConfigObj = new MySQLConfig();
        try {
            defaultConfigObj.validateAndNormalise(true); // skip validation and just populate defaults
        } catch (InvalidConfigException e) {
            throw new IllegalStateException(e); // should never happen
        }

        JsonObject defaultConfig = new Gson().toJsonTree(defaultConfigObj).getAsJsonObject();

        for (String fieldId : MySQLConfig.getValidFields()) {
            try {
                Field field = MySQLConfig.class.getDeclaredField(fieldId);
                if (!field.isAnnotationPresent(DashboardInfo.class)) {
                    continue;
                }

                if (field.getName().endsWith("_table_name")) {
                    continue; // do not show
                }

                String key = field.getName();
                String description = field.isAnnotationPresent(DashboardInfo.class)
                        ? field.getAnnotation(DashboardInfo.class).description()
                        : "";
                boolean isDifferentAcrossTenants = true;

                String valueType = null;

                Class<?> fieldType = field.getType();

                if (fieldType == String.class) {
                    valueType = "string";
                } else if (fieldType == boolean.class) {
                    valueType = "boolean";
                } else if (fieldType == int.class || fieldType == long.class || fieldType == Integer.class) {
                    valueType = "number";
                } else {
                    throw new RuntimeException("Unknown field type " + fieldType.getName());
                }

                JsonElement value = tenantConfig.get(field.getName());

                JsonElement defaultValue = defaultConfig.get(field.getName());
                boolean isNullable = defaultValue == null;

                boolean isEditable = field.getAnnotation(DashboardInfo.class).isEditable();

                result.add(new ConfigFieldInfo(
                        key, valueType, value, description, isDifferentAcrossTenants,
                        null, isNullable, defaultValue, true, isEditable));

            } catch (NoSuchFieldException e) {
                continue;
            }
        }
        return result;
    }

    public void validateAndNormalise() throws InvalidConfigException {
        validateAndNormalise(false);
    }

    private void validateAndNormalise(boolean skipValidation) throws InvalidConfigException {
        if (isValidAndNormalised) {
            return;
        }

        if (!skipValidation) {
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
                            "'mysql_minimum_idle_connections' must be a >= 0");
                }

                if (mysql_minimum_idle_connections > mysql_connection_pool_size) {
                    throw new InvalidConfigException(
                            "'mysql_minimum_idle_connections' must be less than or equal to "
                                    + "'mysql_connection_pool_size'");
                }
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
            mysql_emailverification_verified_emails_table_name = addPrefixToTableName(
                    "emailverification_verified_emails");
        }

        if (mysql_thirdparty_users_table_name == null) {
            mysql_thirdparty_users_table_name = addPrefixToTableName("thirdparty_users");
        }

        isValidAndNormalised = true;
    }

    public void assertThatConfigFromSameUserPoolIsNotConflicting(MySQLConfig otherConfig)
            throws InvalidConfigException {
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
                try {
                    String fieldName = field.getName();
                    String fieldValue = field.get(this) != null ? field.get(this).toString() : null;
                    if (fieldValue == null) {
                        continue;
                    }
                    // To ensure a unique connectionPoolId we include the database password and use the "|db_pass|"
                    // identifier.
                    // This facilitates easy removal of the password from logs when necessary.
                    if (fieldName.equals("mysql_password")) {
                        connectionPoolId.append("|db_pass|" + fieldValue + "|db_pass");
                    } else {
                        connectionPoolId.append("|" + fieldValue);
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
