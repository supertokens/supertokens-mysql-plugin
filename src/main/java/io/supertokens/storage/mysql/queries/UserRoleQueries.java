/*
 *    Copyright (c) 2022, VRAI Labs and/or its affiliates. All rights reserved.
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

public class UserRoleQueries {

    public static String getQueryToCreateRolesTable(Start start) {
        String tableName = Config.getConfig(start).getRolesTable();
        // @formatter:off
            return "CREATE TABLE IF NOT EXISTS " + tableName + " ( role VARCHAR(255) NOT NULL, " +
                    "PRIMARY KEY(role))";
        // @formatter:on
    }

    public static String getQueryToCreateRolePermissionsTable(Start start) {
        String tableName = Config.getConfig(start).getUserRolesPermissionTable();
        // @formatter:off
        return "CREATE TABLE IF NOT EXISTS " + tableName + " ( " +
                "role VARCHAR(255) NOT NULL, " +
                "permission VARCHAR(255) NOT NULL, " +
                "PRIMARY KEY (role, permission), " +
                "FOREIGN KEY (role) REFERENCES " +
                Config.getConfig(start).getRolesTable()+ "(role) ON DELETE CASCADE )";
        // @formatter:on
    }

    public static String getQueryToCreateRolePermissionsPermissionIndex(Start start) {
        return "CREATE INDEX role_permissions_permission_index ON "
                + Config.getConfig(start).getUserRolesPermissionTable() + "(permission);";
    }
//    static String getQueryToCreateRolePermissionsPermissionIndex(Start start) {
//        return "CREATE INDEX role_permissions_permission_index ON " + getConfig(start).getUserRolesPermissionsTable()
//                + "(permission);";
//    }
//
//    public static String getQueryToCreateUserRolesTable(Start start) {
//        String schema = Config.getConfig(start).getTableSchema();
//        String tableName = getConfig(start).getUserRolesTable();
//        // @formatter:off
//        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
//                + "user_id VARCHAR(128) NOT NULL,"
//                + "role VARCHAR(255) NOT NULL,"
//                + "CONSTRAINT " + Utils.getConstraintName(schema, tableName, null, "pkey") + " PRIMARY KEY(user_id,
//                role),"
//                + "CONSTRAINT " + Utils.getConstraintName(schema, tableName, "role", "fkey") + " FOREIGN KEY(role)"
//                + " REFERENCES " + getConfig(start).getRolesTable()
//                +"(role) ON DELETE CASCADE );";
//
//        // @formatter:on
//    }
//
//    public static String getQueryToCreateUserRolesRoleIndex(Start start) {
//        return "CREATE INDEX user_roles_role_index ON " + getConfig(start).getUserRolesTable() + "(role);";
//    }
}
