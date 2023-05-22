/*
 *    Copyright (c) 2021, VRAI Labs and/or its affiliates. All rights reserved.
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
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.pluginInterface.passwordless.PasswordlessCode;
import io.supertokens.pluginInterface.passwordless.PasswordlessDevice;
import io.supertokens.pluginInterface.passwordless.UserInfo;
import io.supertokens.pluginInterface.sqlStorage.SQLStorage.TransactionIsolationLevel;
import io.supertokens.storage.mysql.ConnectionPool;
import io.supertokens.storage.mysql.Start;

import javax.annotation.Nonnull;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.supertokens.pluginInterface.RECIPE_ID.PASSWORDLESS;
import static io.supertokens.storage.mysql.QueryExecutorTemplate.execute;
import static io.supertokens.storage.mysql.QueryExecutorTemplate.update;
import static io.supertokens.storage.mysql.config.Config.getConfig;

public class PasswordlessQueries {
    public static String getQueryToCreateUsersTable(Start start) {
        return "CREATE TABLE IF NOT EXISTS " + getConfig(start).getPasswordlessUsersTable() + " ("
                + "user_id CHAR(36) NOT NULL," + "email VARCHAR(256) UNIQUE," + "phone_number VARCHAR(256) UNIQUE,"
                + "time_joined BIGINT UNSIGNED NOT NULL," + "PRIMARY KEY (user_id));";
    }

    public static String getQueryToCreateDevicesTable(Start start) {
        return "CREATE TABLE IF NOT EXISTS " + getConfig(start).getPasswordlessDevicesTable() + " ("
                + "device_id_hash CHAR(44) NOT NULL," + "email VARCHAR(256)," + "phone_number VARCHAR(256),"
                + "link_code_salt CHAR(44) NOT NULL," + "failed_attempts INT UNSIGNED NOT NULL,"
                + "PRIMARY KEY (device_id_hash));";
    }

    public static String getQueryToCreateCodesTable(Start start) {
        return "CREATE TABLE IF NOT EXISTS " + getConfig(start).getPasswordlessCodesTable() + " ("
                + "code_id CHAR(36) NOT NULL," + "device_id_hash CHAR(44) NOT NULL,"
                + "link_code_hash CHAR(44) NOT NULL UNIQUE," + "created_at BIGINT UNSIGNED NOT NULL,"
                + "PRIMARY KEY (code_id)," + "FOREIGN KEY (device_id_hash) REFERENCES "
                + getConfig(start).getPasswordlessDevicesTable()
                + "(device_id_hash) ON DELETE CASCADE ON UPDATE CASCADE);";
    }

    public static String getQueryToCreateDeviceEmailIndex(Start start) {
        return "CREATE INDEX passwordless_devices_email_index ON " + getConfig(start).getPasswordlessDevicesTable()
                + " (email);"; // USING hash
    }

    public static String getQueryToCreateDevicePhoneNumberIndex(Start start) {
        return "CREATE INDEX passwordless_devices_phone_number_index ON "
                + getConfig(start).getPasswordlessDevicesTable() + " (phone_number);"; // USING hash
    }

    public static String getQueryToCreateCodeCreatedAtIndex(Start start) {
        return "CREATE INDEX passwordless_codes_created_at_index ON " + getConfig(start).getPasswordlessCodesTable()
                + "(created_at);";
    }

    public static void createDeviceWithCode(Start start, String email, String phoneNumber, String linkCodeSalt,
            PasswordlessCode code) throws StorageTransactionLogicException, StorageQueryException {
        start.startTransaction(con -> {
            Connection sqlCon = (Connection) con.getConnection();
            try {
                String QUERY = "INSERT INTO " + getConfig(start).getPasswordlessDevicesTable()
                        + "(device_id_hash, email, phone_number, link_code_salt, failed_attempts)"
                        + " VALUES(?, ?, ?, ?, 0)";
                update(sqlCon, QUERY, pst -> {
                    pst.setString(1, code.deviceIdHash);
                    pst.setString(2, email);
                    pst.setString(3, phoneNumber);
                    pst.setString(4, linkCodeSalt);
                });

                PasswordlessQueries.createCode_Transaction(start, sqlCon, code);
                sqlCon.commit();
            } catch (SQLException throwables) {
                throw new StorageTransactionLogicException(throwables);
            }
            return null;
        }, TransactionIsolationLevel.REPEATABLE_READ);
    }

    public static PasswordlessDevice getDevice_Transaction(Start start, Connection con, String deviceIdHash)
            throws StorageQueryException, SQLException {

        String QUERY = "SELECT device_id_hash, email, phone_number, link_code_salt, failed_attempts FROM "
                + getConfig(start).getPasswordlessDevicesTable() + " WHERE device_id_hash = ? FOR UPDATE";
        return execute(con, QUERY, pst -> pst.setString(1, deviceIdHash), result -> {
            if (result.next()) {
                return PasswordlessDeviceRowMapper.getInstance().mapOrThrow(result);
            }
            return null;
        });
    }

    public static void incrementDeviceFailedAttemptCount_Transaction(Start start, Connection con, String deviceIdHash)
            throws SQLException, StorageQueryException {
        String QUERY = "UPDATE " + getConfig(start).getPasswordlessDevicesTable()
                + " SET failed_attempts = failed_attempts + 1 WHERE device_id_hash = ?";

        update(con, QUERY, pst -> pst.setString(1, deviceIdHash));
    }

    public static void deleteDevice_Transaction(Start start, Connection con, String deviceIdHash)
            throws SQLException, StorageQueryException {
        String QUERY = "DELETE FROM " + getConfig(start).getPasswordlessDevicesTable() + " WHERE device_id_hash = ?";
        update(con, QUERY, pst -> pst.setString(1, deviceIdHash));
    }

    public static void deleteDevicesByPhoneNumber_Transaction(Start start, Connection con, @Nonnull String phoneNumber)
            throws SQLException, StorageQueryException {

        String QUERY = "DELETE FROM " + getConfig(start).getPasswordlessDevicesTable() + " WHERE phone_number = ?";

        update(con, QUERY, pst -> pst.setString(1, phoneNumber));
    }

    public static void deleteDevicesByEmail_Transaction(Start start, Connection con, @Nonnull String email)
            throws SQLException, StorageQueryException {

        String QUERY = "DELETE FROM " + getConfig(start).getPasswordlessDevicesTable() + " WHERE email = ?";

        update(con, QUERY, pst -> pst.setString(1, email));

    }

    private static void createCode_Transaction(Start start, Connection con, PasswordlessCode code)
            throws SQLException, StorageQueryException {
        String QUERY = "INSERT INTO " + getConfig(start).getPasswordlessCodesTable()
                + "(code_id, device_id_hash, link_code_hash, created_at)" + " VALUES(?, ?, ?, ?)";
        update(con, QUERY, pst -> {
            pst.setString(1, code.id);
            pst.setString(2, code.deviceIdHash);
            pst.setString(3, code.linkCodeHash);
            pst.setLong(4, code.createdAt);
        });
    }

    public static void createCode(Start start, PasswordlessCode code)
            throws StorageTransactionLogicException, StorageQueryException {
        start.startTransaction(con -> {
            Connection sqlCon = (Connection) con.getConnection();

            try {
                PasswordlessQueries.createCode_Transaction(start, sqlCon, code);
                sqlCon.commit();
            } catch (SQLException e) {
                throw new StorageTransactionLogicException(e);
            }
            return null;
        });
    }

    public static PasswordlessCode[] getCodesOfDevice_Transaction(Start start, Connection con, String deviceIdHash)
            throws StorageQueryException, SQLException {
        // We do not lock here, since the device is already locked earlier in the transaction.
        String QUERY = "SELECT code_id, device_id_hash, link_code_hash, created_at FROM "
                + getConfig(start).getPasswordlessCodesTable() + " WHERE device_id_hash = ?";

        return execute(con, QUERY, pst -> pst.setString(1, deviceIdHash), result -> {
            List<PasswordlessCode> temp = new ArrayList<>();
            while (result.next()) {
                temp.add(PasswordlessCodeRowMapper.getInstance().mapOrThrow(result));
            }
            return temp.toArray(PasswordlessCode[]::new);
        });
    }

    public static PasswordlessCode getCodeByLinkCodeHash_Transaction(Start start, Connection con, String linkCodeHash)
            throws StorageQueryException, SQLException {
        // We do not lock here, since the device is already locked earlier in the transaction.
        String QUERY = "SELECT code_id, device_id_hash, link_code_hash, created_at FROM "
                + getConfig(start).getPasswordlessCodesTable() + " WHERE link_code_hash = ?";

        return execute(con, QUERY, pst -> pst.setString(1, linkCodeHash), result -> {
            if (result.next()) {
                return PasswordlessCodeRowMapper.getInstance().mapOrThrow(result);
            }
            return null;
        });
    }

    public static void deleteCode_Transaction(Start start, Connection con, String codeId)
            throws SQLException, StorageQueryException {
        String QUERY = "DELETE FROM " + getConfig(start).getPasswordlessCodesTable() + " WHERE code_id = ?";

        update(con, QUERY, pst -> pst.setString(1, codeId));
    }

    public static UserInfo createUser(Start start, String id, @javax.annotation.Nullable String email,
                                  @javax.annotation.Nullable String phoneNumber, long timeJoined)
            throws StorageTransactionLogicException, StorageQueryException {
        return start.startTransaction(con -> {
            Connection sqlCon = (Connection) con.getConnection();
            try {
                {
                    String QUERY = "INSERT INTO " + getConfig(start).getUsersTable()
                            + "(user_id, recipe_id, time_joined)" + " VALUES(?, ?, ?)";
                    update(sqlCon, QUERY, pst -> {
                        pst.setString(1, id);
                        pst.setString(2, PASSWORDLESS.toString());
                        pst.setLong(3, timeJoined);
                    });
                }

                {
                    String QUERY = "INSERT INTO " + getConfig(start).getPasswordlessUsersTable()
                            + "(user_id, email, phone_number, time_joined)" + " VALUES(?, ?, ?, ?)";
                    update(sqlCon, QUERY, pst -> {
                        pst.setString(1, id);
                        pst.setString(2, email);
                        pst.setString(3, phoneNumber);
                        pst.setLong(4, timeJoined);
                    });
                }
                sqlCon.commit();
                return new UserInfo(id, email, phoneNumber, timeJoined, new String[0]);
            } catch (SQLException throwables) {
                throw new StorageTransactionLogicException(throwables);
            }
        });
    }

    public static void deleteUser(Start start, String userId)
            throws StorageTransactionLogicException, StorageQueryException {
        start.startTransaction(con -> {
            Connection sqlCon = (Connection) con.getConnection();
            try {
                {
                    String QUERY = "DELETE FROM " + getConfig(start).getUsersTable()
                            + " WHERE user_id = ? AND recipe_id = ?";

                    update(sqlCon, QUERY, pst -> {
                        pst.setString(1, userId);
                        pst.setString(2, PASSWORDLESS.toString());
                    });
                }

                // Even if the user is changed after we read it here (which is unlikely),
                // we'd only leave devices that will be cleaned up later automatically when they expire.
                UserInfo user = getUserById(start, userId);
                {
                    String QUERY = "DELETE FROM " + getConfig(start).getPasswordlessUsersTable() + " WHERE user_id = ?";

                    update(sqlCon, QUERY, pst -> pst.setString(1, userId));
                }

                if (user != null) {
                    if (user.email != null) {
                        deleteDevicesByEmail_Transaction(start, sqlCon, user.email);
                    }
                    if (user.phoneNumber != null) {
                        deleteDevicesByPhoneNumber_Transaction(start, sqlCon, user.phoneNumber);
                    }
                }

                sqlCon.commit();
            } catch (SQLException throwables) {
                throw new StorageTransactionLogicException(throwables);
            }
            return null;
        });
    }

    public static int updateUserEmail_Transaction(Start start, Connection con, String userId, String email)
            throws SQLException, StorageQueryException {
        String QUERY = "UPDATE " + getConfig(start).getPasswordlessUsersTable() + " SET email = ? WHERE user_id = ?";

        return update(con, QUERY, pst -> {
            pst.setString(1, email);
            pst.setString(2, userId);

        });
    }

    public static int updateUserPhoneNumber_Transaction(Start start, Connection con, String userId, String phoneNumber)
            throws SQLException, StorageQueryException {
        String QUERY = "UPDATE " + getConfig(start).getPasswordlessUsersTable()
                + " SET phone_number = ? WHERE user_id = ?";

        return update(con, QUERY, pst -> {
            pst.setString(1, phoneNumber);
            pst.setString(2, userId);
        });
    }

    public static PasswordlessDevice getDevice(Start start, String deviceIdHash)
            throws StorageQueryException, SQLException {
        try (Connection con = ConnectionPool.getConnection(start)) {
            String QUERY = "SELECT device_id_hash, email, phone_number, link_code_salt, failed_attempts FROM "
                    + getConfig(start).getPasswordlessDevicesTable() + " WHERE device_id_hash = ?";
            return execute(con, QUERY, pst -> pst.setString(1, deviceIdHash), result -> {
                if (result.next()) {
                    return PasswordlessDeviceRowMapper.getInstance().mapOrThrow(result);
                }
                return null;
            });
        }
    }

    public static PasswordlessDevice[] getDevicesByEmail(Start start, @Nonnull String email)
            throws StorageQueryException, SQLException {
        String QUERY = "SELECT device_id_hash, email, phone_number, link_code_salt, failed_attempts FROM "
                + getConfig(start).getPasswordlessDevicesTable() + " WHERE email = ?";

        return execute(start, QUERY, pst -> pst.setString(1, email), result -> {
            List<PasswordlessDevice> temp = new ArrayList<>();
            while (result.next()) {
                temp.add(PasswordlessDeviceRowMapper.getInstance().mapOrThrow(result));
            }
            return temp.toArray(PasswordlessDevice[]::new);
        });
    }

    public static PasswordlessDevice[] getDevicesByPhoneNumber(Start start, @Nonnull String phoneNumber)
            throws StorageQueryException, SQLException {
        String QUERY = "SELECT device_id_hash, email, phone_number, link_code_salt, failed_attempts FROM "
                + getConfig(start).getPasswordlessDevicesTable() + " WHERE phone_number = ?";

        return execute(start, QUERY, pst -> pst.setString(1, phoneNumber), result -> {
            List<PasswordlessDevice> temp = new ArrayList<>();
            while (result.next()) {
                temp.add(PasswordlessDeviceRowMapper.getInstance().mapOrThrow(result));
            }
            return temp.toArray(PasswordlessDevice[]::new);
        });
    }

    public static PasswordlessCode[] getCodesOfDevice(Start start, String deviceIdHash)
            throws StorageQueryException, SQLException {
        try (Connection con = ConnectionPool.getConnection(start)) {
            // We can call the transaction version here because it doesn't lock anything.
            return PasswordlessQueries.getCodesOfDevice_Transaction(start, con, deviceIdHash);
        }
    }

    public static PasswordlessCode[] getCodesBefore(Start start, long time) throws StorageQueryException, SQLException {
        String QUERY = "SELECT code_id, device_id_hash, link_code_hash, created_at FROM "
                + getConfig(start).getPasswordlessCodesTable() + " WHERE created_at < ?";

        return execute(start, QUERY, pst -> pst.setLong(1, time), result -> {
            List<PasswordlessCode> temp = new ArrayList<>();
            while (result.next()) {
                temp.add(PasswordlessCodeRowMapper.getInstance().mapOrThrow(result));
            }
            return temp.toArray(PasswordlessCode[]::new);
        });
    }

    public static PasswordlessCode getCode(Start start, String codeId) throws StorageQueryException, SQLException {
        String QUERY = "SELECT code_id, device_id_hash, link_code_hash, created_at FROM "
                + getConfig(start).getPasswordlessCodesTable() + " WHERE code_id = ?";

        return execute(start, QUERY, pst -> pst.setString(1, codeId), result -> {
            if (result.next()) {
                return PasswordlessCodeRowMapper.getInstance().mapOrThrow(result);
            }
            return null;
        });
    }

    public static PasswordlessCode getCodeByLinkCodeHash(Start start, String linkCodeHash)
            throws StorageQueryException, SQLException {
        try (Connection con = ConnectionPool.getConnection(start)) {
            // We can call the transaction version here because it doesn't lock anything.
            return PasswordlessQueries.getCodeByLinkCodeHash_Transaction(start, con, linkCodeHash);
        }
    }

    public static List<UserInfo> getUsersByIdList(Start start, List<String> ids)
            throws SQLException, StorageQueryException {

        if (ids.size() > 0) {
            StringBuilder QUERY = new StringBuilder("SELECT user_id, email, phone_number, time_joined FROM "
                    + getConfig(start).getPasswordlessUsersTable());
            QUERY.append(" WHERE user_id IN (");
            for (int i = 0; i < ids.size(); i++) {

                QUERY.append("?");
                if (i != ids.size() - 1) {
                    // not the last element
                    QUERY.append(",");
                }
            }
            QUERY.append(")");

            return execute(start, QUERY.toString(), pst -> {
                for (int i = 0; i < ids.size(); i++) {
                    // i+1 cause this starts with 1 and not 0
                    pst.setString(i + 1, ids.get(i));
                }
            }, result -> {
                List<UserInfo> finalResult = new ArrayList<>();
                while (result.next()) {
                    finalResult.add(UserInfoRowMapper.getInstance().mapOrThrow(result));
                }
                return finalResult;
            });
        }
        return Collections.emptyList();
    }

    public static UserInfo getUserById(Start start, String userId) throws StorageQueryException, SQLException {
        List<String> input = new ArrayList<>();
        input.add(userId);
        List<UserInfo> result = getUsersByIdList(start, input);
        if (result.size() == 1) {
            return result.get(0);
        }
        return null;
    }

    public static UserInfo getUserByEmail(Start start, @Nonnull String email)
            throws StorageQueryException, SQLException {
        String QUERY = "SELECT user_id, email, phone_number, time_joined FROM "
                + getConfig(start).getPasswordlessUsersTable() + " WHERE email = ?";

        return execute(start, QUERY, pst -> pst.setString(1, email), result -> {
            if (result.next()) {
                return UserInfoRowMapper.getInstance().mapOrThrow(result);
            }
            return null;
        });
    }

    public static UserInfo getUserByPhoneNumber(Start start, @Nonnull String phoneNumber)
            throws StorageQueryException, SQLException {
        String QUERY = "SELECT user_id, email, phone_number, time_joined FROM "
                + getConfig(start).getPasswordlessUsersTable() + " WHERE phone_number = ?";

        return execute(start, QUERY, pst -> pst.setString(1, phoneNumber), result -> {
            if (result.next()) {
                return UserInfoRowMapper.getInstance().mapOrThrow(result);
            }
            return null;
        });
    }

    private static class PasswordlessDeviceRowMapper implements RowMapper<PasswordlessDevice, ResultSet> {
        private static final PasswordlessDeviceRowMapper INSTANCE = new PasswordlessDeviceRowMapper();

        private PasswordlessDeviceRowMapper() {
        }

        private static PasswordlessDeviceRowMapper getInstance() {
            return INSTANCE;
        }

        @Override
        public PasswordlessDevice map(ResultSet result) throws Exception {
            return new PasswordlessDevice(result.getString("device_id_hash"), result.getString("email"),
                    result.getString("phone_number"), result.getString("link_code_salt"),
                    result.getInt("failed_attempts"));
        }
    }

    private static class PasswordlessCodeRowMapper implements RowMapper<PasswordlessCode, ResultSet> {
        private static final PasswordlessCodeRowMapper INSTANCE = new PasswordlessCodeRowMapper();

        private PasswordlessCodeRowMapper() {
        }

        private static PasswordlessCodeRowMapper getInstance() {
            return INSTANCE;
        }

        @Override
        public PasswordlessCode map(ResultSet result) throws Exception {
            return new PasswordlessCode(result.getString("code_id"), result.getString("device_id_hash"),
                    result.getString("link_code_hash"), result.getLong("created_at"));
        }
    }

    private static class UserInfoRowMapper implements RowMapper<UserInfo, ResultSet> {
        private static final UserInfoRowMapper INSTANCE = new UserInfoRowMapper();

        private UserInfoRowMapper() {
        }

        private static UserInfoRowMapper getInstance() {
            return INSTANCE;
        }

        @Override
        public UserInfo map(ResultSet result) throws Exception {
            return new UserInfo(result.getString("user_id"), result.getString("email"),
                    result.getString("phone_number"), result.getLong("time_joined"), new String[0]);
        }
    }
}
