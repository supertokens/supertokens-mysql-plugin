package io.supertokens.storage.mysql.test;

import io.supertokens.ProcessState;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TableCreationTest {

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
    public void checkingCreationOfNewTable() throws InterruptedException {
        String[] args = {"../", "DEV"};

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        assertNotNull(process.checkOrWaitForEventInPlugin(
                io.supertokens.storage.mysql.ProcessState.PROCESS_STATE.CREATING_NEW_TABLE));

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));

        process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        assertNull(process.checkOrWaitForEventInPlugin(
                io.supertokens.storage.mysql.ProcessState.PROCESS_STATE.CREATING_NEW_TABLE, 2000));

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

}
