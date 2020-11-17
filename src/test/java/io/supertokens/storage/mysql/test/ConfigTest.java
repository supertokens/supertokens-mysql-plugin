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

package io.supertokens.storage.mysql.test;

import io.supertokens.ProcessState;
import io.supertokens.storage.mysql.ConnectionPoolTestContent;
import io.supertokens.storage.mysql.Start;
import io.supertokens.storage.mysql.config.Config;
import io.supertokens.storage.mysql.config.MySQLConfig;
import io.supertokens.storageLayer.StorageLayer;
import junit.framework.TestCase;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


public class ConfigTest {

    @Rule
    public TestRule watchman = Utils.getOnFailure();

    @AfterClass
    public static void afterTesting() {
        Utils.afterTesting();
    }

    @Before
    public void beforeEach() {
        Utils.reset();
    }

    @Test
    public void testThatDefaultConfigLoadsCorrectly() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        MySQLConfig config = Config.getConfig((Start) StorageLayer.getStorage(process.getProcess()));

        checkConfig(config);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));

    }

    @Test
    public void testThatCustomConfigLoadsCorrectly() throws Exception {
        String[] args = {"../"};

        Utils.setValueInConfig("mysql_connection_pool_size", "5");

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        MySQLConfig config = Config.getConfig((Start) StorageLayer.getStorage(process.getProcess()));
        assertEquals(config.getConnectionPoolSize(), 5);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testThatInvalidConfigThrowsRightError() throws Exception {
        String[] args = {"../"};

        //'mysql_user is not set properly in the config file

        Utils.commentConfigValue("mysql_user");

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);

        ProcessState.EventAndException e = process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.INIT_FAILURE);
        assertNotNull(e);
        TestCase.assertEquals(e.exception.getMessage(),
                "'mysql_user' is not set in the config.yaml file. Please set this value and restart SuperTokens");

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));

        Utils.reset();


        //'mysql_password is not set properly in the config file

        Utils.commentConfigValue("mysql_password");
        process = TestingProcessManager.start(args);

        e = process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.INIT_FAILURE);
        assertNotNull(e);
        TestCase.assertEquals(e.exception.getMessage(),
                "'mysql_password' is not set in the config.yaml file. Please set this value and restart SuperTokens");

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));


        Utils.reset();


        //mysql_connection_pool_size is not set properly in the config file

        Utils.setValueInConfig("mysql_connection_pool_size", "-1");
        process = TestingProcessManager.start(args);

        e = process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.INIT_FAILURE);
        assertNotNull(e);
        TestCase.assertEquals(e.exception.getMessage(),
                "'mysql_connection_pool_size' in the config.yaml file must be > 0");

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));


    }

    @Test
    public void testThatMissingConfigFileThrowsError() throws Exception {
        String[] args = {"../"};

        ProcessBuilder pb = new ProcessBuilder("rm", "-r", "config.yaml");
        pb.directory(new File(args[0]));
        Process process1 = pb.start();
        process1.waitFor();

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);

        ProcessState.EventAndException e = process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.INIT_FAILURE);
        assertNotNull(e);
        TestCase.assertEquals(e.exception.getMessage(),
                "java.io.FileNotFoundException: ../config.yaml (No such file or directory)");

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));


    }

    @Test
    public void testCustomLocationForConfigLoadsCorrectly() throws Exception {
        String[] args = {"../", "configFile=../temp/config.yaml"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        ProcessState.EventAndException e = process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.INIT_FAILURE);
        assertNotNull(e);
        TestCase.assertEquals(e.exception.getMessage(), "configPath option must be an absolute path only");

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));

        //absolute path
        File f = new File("../temp/config.yaml");
        args = new String[]{"../", "configFile=" + f.getAbsolutePath()};

        process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        MySQLConfig config = Config.getConfig((Start) StorageLayer.getStorage(process.getProcess()));
        checkConfig(config);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testBadPortInput() throws Exception {
        String[] args = {"../"};

        Utils.setValueInConfig("mysql_port", "8989");
        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        process.getProcess().waitToInitStorageModule();
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.WAITING_TO_INIT_STORAGE_MODULE));

        ConnectionPoolTestContent.getInstance((Start) StorageLayer.getStorage(process.getProcess()))
                .setKeyValue(ConnectionPoolTestContent.TIME_TO_WAIT_TO_INIT, 5000);
        ConnectionPoolTestContent.getInstance((Start) StorageLayer.getStorage(process.getProcess()))
                .setKeyValue(ConnectionPoolTestContent.RETRY_INTERVAL_IF_INIT_FAILS, 2000);
        process.getProcess().proceedWithInitingStorageModule();

        ProcessState.EventAndException e = process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.INIT_FAILURE, 7000);
        assertNotNull(e);
        assertEquals(e.exception.getMessage(),
                "Error connecting to MySQL instance. Please make sure that MySQL is running and that you have " +
                        "specified the correct values for 'mysql_host' and 'mysql_port' in your config file");

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void storageDisabledAndThenEnabled() throws Exception {
        String[] args = {"../"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        process.getProcess().waitToInitStorageModule();
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.WAITING_TO_INIT_STORAGE_MODULE));

        StorageLayer.getStorage(process.getProcess()).setStorageLayerEnabled(false);
        ConnectionPoolTestContent.getInstance((Start) StorageLayer.getStorage(process.getProcess()))
                .setKeyValue(ConnectionPoolTestContent.TIME_TO_WAIT_TO_INIT, 10000);
        ConnectionPoolTestContent.getInstance((Start) StorageLayer.getStorage(process.getProcess()))
                .setKeyValue(ConnectionPoolTestContent.RETRY_INTERVAL_IF_INIT_FAILS, 2000);
        process.getProcess().proceedWithInitingStorageModule();

        Thread.sleep(5000);
        StorageLayer.getStorage(process.getProcess()).setStorageLayerEnabled(true);

        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testBadHostInput() throws Exception {
        String[] args = {"../"};

        Utils.setValueInConfig("mysql_host", "random");

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        ProcessState.EventAndException e = process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.INIT_FAILURE);
        assertNotNull(e);

        assertEquals(
                "Failed to initialize pool: Could not connect to address=(host=random)(port=3306)(type=master) : " +
                        "Socket fail to connect to host:random, port:3306. random",
                e.exception.getMessage());

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));

    }

    @Test
    public void testThatChangeInTableNameIsCorrect() throws Exception {
        String[] args = {"../"};

        Utils.setValueInConfig("mysql_key_value_table_name", "key_value_table");
        Utils.setValueInConfig("mysql_session_info_table_name", "session_info_table");
        Utils.setValueInConfig("mysql_emailpassword_users_table_name", "users");
        Utils.setValueInConfig("mysql_emailpassword_pswd_reset_tokens_table_name", "password_reset");

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));
        MySQLConfig config = Config.getConfig((Start) StorageLayer.getStorage(process.getProcess()));

        assertEquals("change in KeyValueTable name not reflected", config.getKeyValueTable(), "key_value_table");
        assertEquals("change in SessionInfoTable name not reflected", config.getSessionInfoTable(),
                "session_info_table");
        assertEquals("change in table name not reflected", config.getUsersTable(), "users");
        assertEquals("change in table name not reflected", config.getPasswordResetTokensTable(),
                "password_reset");

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    public static void checkConfig(MySQLConfig config) {

        assertEquals("Config connectionPoolSize did not match default", config.getConnectionPoolSize(), 10);
        assertEquals("Config databaseName does not match default", config.getDatabaseName(), "supertokens");
        assertEquals("Config keyValue table does not match default", config.getKeyValueTable(), "key_value");
        assertEquals("Config hostName does not match default ", config.getHostName(), "localhost");
        assertEquals("Config port does not match default", config.getPort(), 3306);
        assertEquals("Config sessionInfoTable does not match default", config.getSessionInfoTable(), "session_info");
        assertEquals("Config user does not match default", config.getUser(), "root");
        assertEquals("Config password does not match default", config.getPassword(), "root");
        assertEquals("Config keyValue table does not match default", config.getUsersTable(), "emailpassword_users");
        assertEquals("Config keyValue table does not match default", config.getPasswordResetTokensTable(),
                "emailpassword_pswd_reset_tokens");
    }

}
