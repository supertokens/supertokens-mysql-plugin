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

import io.supertokens.pluginInterface.RowMapper;
import io.supertokens.pluginInterface.bulkimport.BulkImportStorage.BULK_IMPORT_USER_STATUS;
import io.supertokens.pluginInterface.bulkimport.BulkImportUser;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.pluginInterface.multitenancy.AppIdentifier;
import io.supertokens.storage.mysql.PreparedStatementValueSetter;
import io.supertokens.storage.mysql.Start;
import io.supertokens.storage.mysql.config.Config;
import io.supertokens.storage.mysql.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.supertokens.storage.mysql.QueryExecutorTemplate.*;

public class BulkImportQueries {
    static String getQueryToCreateBulkImportUsersTable(Start start) {
        String tableName = Config.getConfig(start).getBulkImportUsersTable();
        return "CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + "id CHAR(36),"
                + "app_id VARCHAR(64) NOT NULL DEFAULT 'public',"
                + "primary_user_id VARCHAR(36),"
                + "raw_data TEXT NOT NULL,"
                + "status VARCHAR(128) DEFAULT 'NEW',"
                + "error_msg TEXT,"
                + "created_at BIGINT NOT NULL, "
                + "updated_at BIGINT NOT NULL,"
                + "PRIMARY KEY (app_id, id),"
                + "FOREIGN KEY (app_id)"
                + " REFERENCES " + Config.getConfig(start).getAppsTable() + " (app_id) ON DELETE CASCADE"
                + " );";
    }

    public static String getQueryToCreateStatusUpdatedAtIndex(Start start) {
        return "CREATE INDEX bulk_import_users_status_updated_at_index ON "
                + Config.getConfig(start).getBulkImportUsersTable() + " (app_id, status, updated_at)";
    }

    public static String getQueryToCreatePaginationIndex1(Start start) {
        return "CREATE INDEX bulk_import_users_pagination_index1 ON "
                + Config.getConfig(start).getBulkImportUsersTable() + " (app_id, status, created_at DESC, id DESC)";
    }

    public static String getQueryToCreatePaginationIndex2(Start start) {
        return "CREATE INDEX bulk_import_users_pagination_index2 ON "
                + Config.getConfig(start).getBulkImportUsersTable() + " (app_id, created_at DESC, id DESC)";
    }

    public static void insertBulkImportUsers(Start start, AppIdentifier appIdentifier, List<BulkImportUser> users)
            throws SQLException, StorageQueryException {
        StringBuilder queryBuilder = new StringBuilder(
                "INSERT INTO " + Config.getConfig(start).getBulkImportUsersTable() + " (id, app_id, raw_data, created_at, updated_at) VALUES ");

        int userCount = users.size();

        for (int i = 0; i < userCount; i++) {
            queryBuilder.append(" (?, ?, ?, ?, ?)");

            if (i < userCount - 1) {
                queryBuilder.append(",");
            }
        }

        update(start, queryBuilder.toString(), pst -> {
            int parameterIndex = 1;
            for (BulkImportUser user : users) {
                pst.setString(parameterIndex++, user.id);
                pst.setString(parameterIndex++, appIdentifier.getAppId());
                pst.setString(parameterIndex++, user.toRawDataForDbStorage());
                pst.setLong(parameterIndex++, System.currentTimeMillis());
                pst.setLong(parameterIndex++, System.currentTimeMillis());
            }
        });
    }

    public static void updateBulkImportUserStatus_Transaction(Start start, Connection con, AppIdentifier appIdentifier,
            @Nonnull String bulkImportUserId, @Nonnull BULK_IMPORT_USER_STATUS status, @Nullable String errorMessage)
            throws SQLException, StorageQueryException {
        String query = "UPDATE " + Config.getConfig(start).getBulkImportUsersTable()
                + " SET status = ?, error_msg = ?, updated_at = ? WHERE app_id = ? and id = ?";

        List<Object> parameters = new ArrayList<>();

        parameters.add(status.toString());
        parameters.add(errorMessage);
        parameters.add(System.currentTimeMillis());
        parameters.add(appIdentifier.getAppId());
        parameters.add(bulkImportUserId);

        update(con, query, pst -> {
            for (int i = 0; i < parameters.size(); i++) {
                pst.setObject(i + 1, parameters.get(i));
            }
        });
    }

    public static List<BulkImportUser> getBulkImportUsersAndChangeStatusToProcessing(Start start,
            AppIdentifier appIdentifier,
            @Nonnull Integer limit)
            throws StorageQueryException, StorageTransactionLogicException {

        return start.startTransaction(con -> {
            Connection sqlCon = (Connection) con.getConnection();
            try {
                // NOTE: On average, we take about 66 seconds to process 1000 users. If, for any reason, the bulk import users were marked as processing but couldn't be processed within 10 minutes, we'll attempt to process them again.

                // "FOR UPDATE" ensures that multiple cron jobs don't read the same rows simultaneously.
                // If one process locks the first 1000 rows, others will wait for the lock to be released.
                // "SKIP LOCKED" allows other processes to skip locked rows and select the next 1000 available rows.
                String selectQuery = "SELECT * FROM " + Config.getConfig(start).getBulkImportUsersTable()
                + " WHERE app_id = ?"
                + " AND (status = 'NEW' OR status = 'PROCESSING')" /* 10 mins */
                + " LIMIT ? FOR UPDATE SKIP LOCKED";
    

                List<BulkImportUser> bulkImportUsers = new ArrayList<>();

                execute(sqlCon, selectQuery, pst -> {
                    pst.setString(1, appIdentifier.getAppId());
                    pst.setInt(2, limit);
                }, result -> {
                    while (result.next()) {
                        bulkImportUsers.add(BulkImportUserRowMapper.getInstance().mapOrThrow(result));
                    }
                    return null;
                });

                if (bulkImportUsers.isEmpty()) {
                    return new ArrayList<>();
                }

                String updateQuery = "UPDATE " + Config.getConfig(start).getBulkImportUsersTable()
                        + " SET status = ?, updated_at = ? WHERE app_id = ? AND id IN (" + Utils
                                .generateCommaSeperatedQuestionMarks(bulkImportUsers.size()) + ")";

                update(sqlCon, updateQuery, pst -> {
                    int index = 1;
                    pst.setString(index++, BULK_IMPORT_USER_STATUS.PROCESSING.toString());
                    pst.setLong(index++, System.currentTimeMillis());
                    pst.setString(index++, appIdentifier.getAppId());
                    for (BulkImportUser user : bulkImportUsers) {
                        pst.setObject(index++, user.id);
                    }
                });
                return bulkImportUsers;
            } catch (SQLException throwables) {
                throw new StorageTransactionLogicException(throwables);
            }
        });
    }

    public static List<BulkImportUser> getBulkImportUsers(Start start, AppIdentifier appIdentifier,
            @Nonnull Integer limit, @Nullable BULK_IMPORT_USER_STATUS status,
            @Nullable String bulkImportUserId, @Nullable Long createdAt)
            throws SQLException, StorageQueryException {

        String baseQuery = "SELECT * FROM " + Config.getConfig(start).getBulkImportUsersTable();

        StringBuilder queryBuilder = new StringBuilder(baseQuery);
        List<Object> parameters = new ArrayList<>();

        queryBuilder.append(" WHERE app_id = ?");
        parameters.add(appIdentifier.getAppId());

        if (status != null) {
            queryBuilder.append(" AND status = ?");
            parameters.add(status.toString());
        }

        if (bulkImportUserId != null && createdAt != null) {
            queryBuilder
                    .append(" AND (created_at < ? OR (created_at = ? AND id <= ?))");
            parameters.add(createdAt);
            parameters.add(createdAt);
            parameters.add(bulkImportUserId);
        }

        queryBuilder.append(" ORDER BY created_at DESC, id DESC LIMIT ?");
        parameters.add(limit);

        String query = queryBuilder.toString();

        return execute(start, query, pst -> {
            for (int i = 0; i < parameters.size(); i++) {
                pst.setObject(i + 1, parameters.get(i));
            }
        }, result -> {
            List<BulkImportUser> bulkImportUsers = new ArrayList<>();
            while (result.next()) {
                bulkImportUsers.add(BulkImportUserRowMapper.getInstance().mapOrThrow(result));
            }
            return bulkImportUsers;
        });
    }

    public static List<String> deleteBulkImportUsers(Start start, AppIdentifier appIdentifier,
            @Nonnull String[] bulkImportUserIds) throws SQLException, StorageQueryException {
        if (bulkImportUserIds.length == 0) {
            return new ArrayList<>();
        }

        // This function needs to return the IDs of the deleted users. Since the DELETE query doesn't return the IDs of the deleted entries,
        // we first perform a SELECT query to find all IDs that actually exist in the database. After deletion, we return these IDs.
        String selectQuery = "SELECT id FROM " + Config.getConfig(start).getBulkImportUsersTable()
        + " WHERE app_id = ? AND id IN (" + Utils
                .generateCommaSeperatedQuestionMarks(bulkImportUserIds.length) + ")";

        List<String> deletedIds = new ArrayList<>();

        execute(start, selectQuery, pst -> {
            int index = 1;
            pst.setString(index++, appIdentifier.getAppId());
            for (String id : bulkImportUserIds) {
                pst.setObject(index++, id);
            }
        }, result -> {
            while (result.next()) {
                deletedIds.add(result.getString("id"));
            }
            return null;
        });

        if (deletedIds.isEmpty()) {
            return new ArrayList<>();
        }

        String deleteQuery = "DELETE FROM " + Config.getConfig(start).getBulkImportUsersTable()
                + " WHERE app_id = ? AND id IN (" + Utils.generateCommaSeperatedQuestionMarks(deletedIds.size()) + ")";

        update(start, deleteQuery, pst -> {
            int index = 1;
            pst.setString(index++, appIdentifier.getAppId());
            for (String id : deletedIds) {
                pst.setObject(index++, id);
            }
        });

        return deletedIds;
    }

    public static void deleteBulkImportUser_Transaction(Start start, Connection con, AppIdentifier appIdentifier,
            @Nonnull String bulkImportUserId) throws SQLException, StorageQueryException {
        String query = "DELETE FROM " + Config.getConfig(start).getBulkImportUsersTable()
                + " WHERE app_id = ? AND id = ?";

        update(con, query, pst -> {
            pst.setString(1, appIdentifier.getAppId());
            pst.setString(2, bulkImportUserId);
        });
    }

    public static void updateBulkImportUserPrimaryUserId(Start start, AppIdentifier appIdentifier,
            @Nonnull String bulkImportUserId,
            @Nonnull String primaryUserId) throws SQLException, StorageQueryException {
        String query = "UPDATE " + Config.getConfig(start).getBulkImportUsersTable()
                + " SET primary_user_id = ?, updated_at = ? WHERE app_id = ? and id = ?";

        update(start, query, pst -> {
            pst.setString(1, primaryUserId);
            pst.setLong(2, System.currentTimeMillis());
            pst.setString(3, appIdentifier.getAppId());
            pst.setString(4, bulkImportUserId);
        });
    }

    public static long getBulkImportUsersCount(Start start, AppIdentifier appIdentifier, @Nullable BULK_IMPORT_USER_STATUS status) throws SQLException, StorageQueryException {
        String baseQuery = "SELECT COUNT(*) FROM " + Config.getConfig(start).getBulkImportUsersTable();
        StringBuilder queryBuilder = new StringBuilder(baseQuery);

        List<Object> parameters = new ArrayList<>();

        queryBuilder.append(" WHERE app_id = ?");
        parameters.add(appIdentifier.getAppId());

        if (status != null) {
            queryBuilder.append(" AND status = ?");
            parameters.add(status.toString());
        }

        String query = queryBuilder.toString();

        return execute(start, query, pst -> {
            for (int i = 0; i < parameters.size(); i++) {
                pst.setObject(i + 1, parameters.get(i));
            }
        }, result -> {
            result.next();
            return result.getLong(1);
        });
    }

    public static void updateMultipleBulkImportUsersStatusToError_Transaction(Start start, Connection con, AppIdentifier appIdentifier,
                                                                              @Nonnull Map<String,String> bulkImportUserIdToErrorMessage)
            throws SQLException, StorageQueryException {
        BULK_IMPORT_USER_STATUS errorStatus = BULK_IMPORT_USER_STATUS.FAILED;
        String query = "UPDATE " + Config.getConfig(start).getBulkImportUsersTable()
                + " SET status = ?, error_msg = ?, updated_at = ? WHERE app_id = ? and id = ?";

        List<PreparedStatementValueSetter> errorSetters = new ArrayList<>();

        for(String bulkImportUserId : bulkImportUserIdToErrorMessage.keySet()){
            errorSetters.add(pst -> {
                pst.setString(1, errorStatus.toString());
                pst.setString(2, bulkImportUserIdToErrorMessage.get(bulkImportUserId));
                pst.setLong(3, System.currentTimeMillis());
                pst.setString(4, appIdentifier.getAppId());
                pst.setString(5, bulkImportUserId);
            });
        }

        executeBatch(con, query, errorSetters);
    }

    private static class BulkImportUserRowMapper implements RowMapper<BulkImportUser, ResultSet> {
        private static final BulkImportUserRowMapper INSTANCE = new BulkImportUserRowMapper();

        private BulkImportUserRowMapper() {
        }

        private static BulkImportUserRowMapper getInstance() {
            return INSTANCE;
        }

        @Override
        public BulkImportUser map(ResultSet result) throws Exception {
            return BulkImportUser.fromRawDataFromDbStorage(result.getString("id"), result.getString("raw_data"),
                    BULK_IMPORT_USER_STATUS.valueOf(result.getString("status")),
                    result.getString("primary_user_id"), result.getString("error_msg"), result.getLong("created_at"),
                    result.getLong("updated_at"));
        }
    }
}