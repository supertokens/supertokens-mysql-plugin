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
import io.supertokens.pluginInterface.exceptions.QuitProgramFromPluginException;

import java.net.URI;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MySQLConfig {

    @JsonProperty
    private int mysql_config_version = -1;

    @JsonProperty
    private int mysql_connection_pool_size = 10;

    @JsonProperty
    private String mysql_host = null;

    @JsonProperty
    private int mysql_port = -1;

    @JsonProperty
    private String mysql_user = null;

    @JsonProperty
    private String mysql_password = null;

    @JsonProperty
    private String mysql_database_name = null;

    @JsonProperty
    private String mysql_key_value_table_name = null;

    @JsonProperty
    private String mysql_session_info_table_name = null;

    @JsonProperty
    private String mysql_emailpassword_users_table_name = null;

    @JsonProperty
    private String mysql_emailpassword_pswd_reset_tokens_table_name = null;

    @JsonProperty
    private String mysql_emailverification_tokens_table_name = null;

    @JsonProperty
    private String mysql_emailverification_verified_emails_table_name = null;

    @JsonProperty
    private String mysql_thirdparty_users_table_name = null;

    @JsonProperty
    private String mysql_table_names_prefix = "";

    @JsonProperty
    private String mysql_connection_uri = null;

    public int getConnectionPoolSize() {
        return mysql_connection_pool_size;
    }

    public String getConnectionScheme() {
        if (mysql_connection_uri != null) {
            URI uri = URI.create(mysql_connection_uri);

            // sometimes if the scheme is missing, the host is returned as the scheme. To prevent that,
            // we have a check
            String host = this.getHostName();
            if (uri.getScheme() != null && !uri.getScheme().equals(host)) {
                return uri.getScheme();
            }
        }
        return "mysql";
    }

    public String getConnectionAttributes() {
        if (mysql_connection_uri != null) {
            URI uri = URI.create(mysql_connection_uri);
            String query = uri.getQuery();
            if (query != null) {
                if (query.contains("allowPublicKeyRetrieval=")) {
                    return query;
                } else {
                    return query + "&allowPublicKeyRetrieval=true";
                }
            }
        }
        return "allowPublicKeyRetrieval=true";
    }

    public String getHostName() {
        if (mysql_host == null) {
            if (mysql_connection_uri != null) {
                URI uri = URI.create(mysql_connection_uri);
                if (uri.getHost() != null) {
                    return uri.getHost();
                }
            }
            return "localhost";
        }
        return mysql_host;
    }

    public int getPort() {
        if (mysql_port == -1) {
            if (mysql_connection_uri != null) {
                URI uri = URI.create(mysql_connection_uri);
                return uri.getPort();
            }
            return 3306;
        }
        return mysql_port;
    }

    public String getUser() {
        if (mysql_user == null) {
            if (mysql_connection_uri != null) {
                URI uri = URI.create(mysql_connection_uri);
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] userInfoArray = userInfo.split(":");
                    if (userInfoArray.length > 0 && !userInfoArray[0].equals("")) {
                        return userInfoArray[0];
                    }
                }
            }
            return null;
        }
        return mysql_user;
    }

    public String getPassword() {
        if (mysql_password == null) {
            if (mysql_connection_uri != null) {
                URI uri = URI.create(mysql_connection_uri);
                String userInfo = uri.getUserInfo();
                if (userInfo != null) {
                    String[] userInfoArray = userInfo.split(":");
                    if (userInfoArray.length > 1 && !userInfoArray[1].equals("")) {
                        return userInfoArray[1];
                    }
                }
            }
            return null;
        }
        return mysql_password;
    }

    public String getConnectionURI() {
        return mysql_connection_uri;
    }

    public String getDatabaseName() {
        if (mysql_database_name == null) {
            if (mysql_connection_uri != null) {
                URI uri = URI.create(mysql_connection_uri);
                String path = uri.getPath();
                if (path != null && !path.equals("") && !path.equals("/")) {
                    if (path.startsWith("/")) {
                        return path.substring(1);
                    }
                    return path;
                }
            }
            return "supertokens";
        }
        return mysql_database_name;
    }


    public String getKeyValueTable() {
        String tableName = "key_value";
        if (mysql_key_value_table_name != null) {
            return mysql_key_value_table_name;
        }
        return addPrefixToTableName(tableName);
    }

    public String getSessionInfoTable() {
        String tableName = "session_info";
        if (mysql_session_info_table_name != null) {
            return mysql_session_info_table_name;
        }
        return addPrefixToTableName(tableName);
    }

    public String getUsersTable() {
        String tableName = "emailpassword_users";
        if (mysql_emailpassword_users_table_name != null) {
            return mysql_emailpassword_users_table_name;
        }
        return addPrefixToTableName(tableName);
    }

    public String getPasswordResetTokensTable() {
        String tableName = "emailpassword_pswd_reset_tokens";
        if (mysql_emailpassword_pswd_reset_tokens_table_name != null) {
            return mysql_emailpassword_pswd_reset_tokens_table_name;
        }
        return addPrefixToTableName(tableName);
    }

    public String getEmailVerificationTokensTable() {
        String tableName = "emailverification_tokens";
        if (mysql_emailverification_tokens_table_name != null) {
            return mysql_emailverification_tokens_table_name;
        }
        return addPrefixToTableName(tableName);
    }

    public String getEmailVerificationTable() {
        String tableName = "emailverification_verified_emails";
        if (mysql_emailverification_verified_emails_table_name != null) {
            return mysql_emailverification_verified_emails_table_name;
        }
        return addPrefixToTableName(tableName);
    }

    public String getThirdPartyUsersTable() {
        String tableName = "thirdparty_users";
        if (mysql_thirdparty_users_table_name != null) {
            return mysql_thirdparty_users_table_name;
        }
        return addPrefixToTableName(tableName);
    }

    private String addPrefixToTableName(String tableName) {
        if (!mysql_table_names_prefix.trim().equals("")) {
            return mysql_table_names_prefix.trim() + "_" + tableName;
        }
        return tableName;
    }

    void validateAndInitialise() {
        if (mysql_connection_uri != null) {
            try {
                URI ignored = URI.create(mysql_connection_uri);
            } catch (Exception e) {
                throw new QuitProgramFromPluginException(
                        "The provided mysql connection URI has an incorrect format. Please use a format like " +
                                "mysql://[user[:[password]]@]host[:port][/dbname][?attr1=val1&attr2=val2...");
            }
        } else {
            if (this.getUser() == null) {
                throw new QuitProgramFromPluginException(
                        "'mysql_user' and 'mysql_connection_uri' are not set. Please set at least one of " +
                                "these values");
            }
        }
        if (getConnectionPoolSize() <= 0) {
            throw new QuitProgramFromPluginException(
                    "'mysql_connection_pool_size' in the config.yaml file must be > 0");
        }
    }

}