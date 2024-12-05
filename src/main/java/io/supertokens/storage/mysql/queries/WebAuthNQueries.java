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

package io.supertokens.storage.mysql.queries;


import io.supertokens.storage.mysql.Start;
import io.supertokens.storage.mysql.config.Config;

public class WebAuthNQueries {

    static String getQueryToCreateWebAuthNUsersTable(Start start){
        String webAuthNUsersTableName = Config.getConfig(start).getWebAuthNUsersTable();
        return  "CREATE TABLE IF NOT EXISTS " + webAuthNUsersTableName + "(" +
                " app_id VARCHAR(64) DEFAULT 'public' NOT NULL," +
                " user_id CHAR(36) NOT NULL," +
                " email VARCHAR(256) NOT NULL," +
                " rp_id VARCHAR(256) NOT NULL," +
                " time_joined BIGINT NOT NULL," +
                " PRIMARY KEY (app_id, user_id)," +
                " FOREIGN KEY (app_id, user_id) REFERENCES " + Config.getConfig(start).getAppIdToUserIdTable() +
                " (app_id, user_id) ON DELETE CASCADE " +
                ");";
    }

    static String getQueryToCreateWebAuthNUsersToTenantTable(Start start){
        String webAuthNUserToTenantTableName = Config.getConfig(start).getWebAuthNUserToTenantTable();
        return  "CREATE TABLE IF NOT EXISTS  " + webAuthNUserToTenantTableName +" (" +
                " app_id VARCHAR(64) DEFAULT 'public' NOT NULL," +
                " tenant_id VARCHAR(64) DEFAULT 'public' NOT NULL," +
                " user_id CHAR(36) NOT NULL," +
                " email VARCHAR(256) NOT NULL," +
                " UNIQUE (app_id, tenant_id, email)," +
                " PRIMARY KEY (app_id, tenant_id, user_id)," +
                "  FOREIGN KEY (app_id, tenant_id, user_id) " +
                " REFERENCES "+ Config.getConfig(start).getUsersTable()+" (app_id, tenant_id, user_id) ON DELETE CASCADE" +
                ");";
    }

    static String getQueryToCreateWebAuthNGeneratedOptionsTable(Start start){
        String webAuthNGeneratedOptionsTable = Config.getConfig(start).getWebAuthNGeneratedOptionsTable();
        return  "CREATE TABLE IF NOT EXISTS " + webAuthNGeneratedOptionsTable + "(" +
                " app_id VARCHAR(64) DEFAULT 'public' NOT NULL," +
                " tenant_id VARCHAR(64) DEFAULT 'public' NOT NULL," +
                " id CHAR(36) NOT NULL," +
                " challenge VARCHAR(256) NOT NULL," +
                " email VARCHAR(256)," +
                " rp_id VARCHAR(256) NOT NULL," +
                " origin VARCHAR(256) NOT NULL," +
                " expires_at BIGINT NOT NULL," +
                " created_at BIGINT NOT NULL," +
                "  PRIMARY KEY (app_id, tenant_id, id)," +
                "  FOREIGN KEY (app_id, tenant_id) " +
                "  REFERENCES " + Config.getConfig(start).getTenantsTable() + " (app_id, tenant_id) ON DELETE CASCADE" +
                ");";
    }

    static String getQueryToCreateWebAuthNChallengeExpiresIndex(Start start) {
        return  "CREATE INDEX webauthn_user_challenges_expires_at_index ON " +
                Config.getConfig(start).getWebAuthNGeneratedOptionsTable() +
                " (app_id, tenant_id, expires_at);";
    }

    static String getQueryToCreateWebAuthNCredentialsTable(Start start){
        String webAuthNCredentialsTable = Config.getConfig(start).getWebAuthNCredentialsTable();
        return  "CREATE TABLE IF NOT EXISTS "+ webAuthNCredentialsTable + "(" +
                " id VARCHAR(256) NOT NULL," +
                " app_id VARCHAR(64) DEFAULT 'public'," +
                " rp_id VARCHAR(256)," +
                " user_id CHAR(36)," +
                " counter BIGINT NOT NULL," +
                " public_key BLOB NOT NULL," +
                " transports TEXT NOT NULL," +
                " created_at BIGINT NOT NULL," +
                " updated_at BIGINT NOT NULL," +
                "  PRIMARY KEY (app_id, rp_id, id)," +
                "  FOREIGN KEY (app_id, user_id) REFERENCES " +
                Config.getConfig(start).getWebAuthNUsersTable() + " (app_id, user_id) ON DELETE CASCADE" +
                ");";
    }

}
