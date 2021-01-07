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

@JsonIgnoreProperties(ignoreUnknown = true)
public class MySQLConfig {

    @JsonProperty
    private int mysql_config_version = -1;

    @JsonProperty
    private int mysql_connection_pool_size = 10;

    @JsonProperty
    private String mysql_host = "localhost";

    @JsonProperty
    private int mysql_port = 3306;

    @JsonProperty
    private String mysql_user = null;

    @JsonProperty
    private String mysql_password = null;

    @JsonProperty
    private String mysql_database_name = "supertokens";

    @JsonProperty
    private String mysql_key_value_table_name = "key_value";

    @JsonProperty
    private String mysql_session_info_table_name = "session_info";

    @JsonProperty
    private String mysql_emailpassword_users_table_name = "emailpassword_users";

    @JsonProperty
    private String mysql_emailpassword_pswd_reset_tokens_table_name = "emailpassword_pswd_reset_tokens";

    @JsonProperty
    private String mysql_emailpassword_email_verification_tokens_table_name = "emailpassword_email_verification_tokens";

    public int getConnectionPoolSize() {
        return mysql_connection_pool_size;
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

    public String getKeyValueTable() {
        return mysql_key_value_table_name;
    }

    public String getSessionInfoTable() {
        return mysql_session_info_table_name;
    }

    public String getUsersTable() {
        return mysql_emailpassword_users_table_name;
    }

    public String getPasswordResetTokensTable() {
        return mysql_emailpassword_pswd_reset_tokens_table_name;
    }

    public String getEmailVerificationTokensTable() {
        return mysql_emailpassword_email_verification_tokens_table_name;
    }

    void validateAndInitialise() {
        if (getUser() == null) {
            throw new QuitProgramFromPluginException(
                    "'mysql_user' is not set in the config.yaml file. Please set this value and restart SuperTokens");
        }

        if (getPassword() == null) {
            throw new QuitProgramFromPluginException(
                    "'mysql_password' is not set in the config.yaml file. Please set this value and restart " +
                            "SuperTokens");
        }
        if (getConnectionPoolSize() <= 0) {
            throw new QuitProgramFromPluginException(
                    "'mysql_connection_pool_size' in the config.yaml file must be > 0");
        }
    }

}