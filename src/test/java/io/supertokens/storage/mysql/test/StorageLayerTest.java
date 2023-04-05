package io.supertokens.storage.mysql.test;

import io.supertokens.ProcessState;
import io.supertokens.pluginInterface.STORAGE_TYPE;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.exceptions.StorageTransactionLogicException;
import io.supertokens.pluginInterface.totp.TOTPDevice;
import io.supertokens.pluginInterface.totp.TOTPUsedCode;
import io.supertokens.pluginInterface.totp.exception.TotpNotEnabledException;
import io.supertokens.pluginInterface.totp.exception.UsedCodeAlreadyExistsException;
import io.supertokens.pluginInterface.totp.sqlStorage.TOTPSQLStorage;
import io.supertokens.storageLayer.StorageLayer;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import static org.junit.Assert.assertNotNull;

public class StorageLayerTest {

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

    // TOTP recipe:

    public static void insertUsedCodeUtil(TOTPSQLStorage storage, TOTPUsedCode usedCode) throws Exception {
        try {
            storage.startTransaction(con -> {
                try {
                    storage.insertUsedCode_Transaction(con, usedCode);
                    storage.commitTransaction(con);
                    return null;
                } catch (TotpNotEnabledException | UsedCodeAlreadyExistsException e) {
                    throw new StorageTransactionLogicException(e);
                }
            });
        } catch (StorageTransactionLogicException e) {
            Exception actual = e.actualException;
            if (actual instanceof TotpNotEnabledException || actual instanceof UsedCodeAlreadyExistsException) {
                throw actual;
            } else {
                throw e;
            }
        }
    }

    @Test
    public void totpCodeLengthTest() throws Exception {
        String[] args = { "../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }
        TOTPSQLStorage storage = StorageLayer.getTOTPStorage(process.getProcess());
        long now = System.currentTimeMillis();
        long nextDay = now + 1000 * 60 * 60 * 24; // 1 day from now

        TOTPDevice d1 = new TOTPDevice("user", "d1", "secret", 30, 1, false);
        storage.createDevice(d1);

        // Try code with length > 8
        try {
            TOTPUsedCode code = new TOTPUsedCode("user", "123456789", true, nextDay, now);
            insertUsedCodeUtil(storage, code);
            assert (false);
        } catch (StorageQueryException e) {
            assert e.getMessage().endsWith("Data too long for column 'code' at row 1");
        }

        // Try code with length < 8
        TOTPUsedCode code = new TOTPUsedCode("user", "12345678", true, nextDay, now);
        insertUsedCodeUtil(storage, code);
    }

}
