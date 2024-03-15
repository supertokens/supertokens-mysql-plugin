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

package io.supertokens.storage.mysql.test;

import io.supertokens.ProcessState;
import io.supertokens.authRecipe.AuthRecipe;
import io.supertokens.authRecipe.UserPaginationContainer;
import io.supertokens.featureflag.EE_FEATURES;
import io.supertokens.featureflag.FeatureFlagTestContent;
import io.supertokens.pluginInterface.authRecipe.AuthRecipeUserInfo;
import io.supertokens.pluginInterface.authRecipe.LoginMethod;
import io.supertokens.pluginInterface.multitenancy.TenantIdentifier;
import io.supertokens.storage.mysql.Start;
import io.supertokens.storageLayer.StorageLayer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class UserPaginationTest {
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
    public void testUserPaginationWithSameTimeJoined() throws Exception {
        String[] args = {"../"};
        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        Start start = (Start) StorageLayer.getBaseStorage(process.getProcess());

        Set<String> userIds = new HashSet<>();

        long timeJoined = System.currentTimeMillis();
        for (int i = 0; i < 100; i++) {
            String userId = io.supertokens.utils.Utils.getUUID();
            start.signUp(TenantIdentifier.BASE_TENANT, userId, "test"+i+"@example.com", new LoginMethod.ThirdParty("google", userId), timeJoined);
            userIds.add(userId);
        }

        // Test ascending
        {
            Set<String> paginationUserIds = new HashSet<>();
            UserPaginationContainer usersRes = AuthRecipe.getUsers(process.getProcess(), 10,
                    "ASC", null, null, null);

            while (true) {
                for (AuthRecipeUserInfo user : usersRes.users) {
                    paginationUserIds.add(user.getSupertokensUserId());
                }

                if (usersRes.nextPaginationToken == null) {
                    break;
                }
                usersRes = AuthRecipe.getUsers(process.getProcess(), 10,
                        "ASC", usersRes.nextPaginationToken, null, null);
            }

            assertEquals(userIds.size(), paginationUserIds.size());
            assertEquals(userIds, paginationUserIds);
        }

        // Test descending
        {
            Set<String> paginationUserIds = new HashSet<>();
            UserPaginationContainer usersRes = AuthRecipe.getUsers(process.getProcess(), 10,
                    "DESC", null, null, null);

            while (true) {
                for (AuthRecipeUserInfo user : usersRes.users) {
                    paginationUserIds.add(user.getSupertokensUserId());
                }

                if (usersRes.nextPaginationToken == null) {
                    break;
                }
                usersRes = AuthRecipe.getUsers(process.getProcess(), 10,
                        "DESC", usersRes.nextPaginationToken, null, null);
            }

            assertEquals(userIds.size(), paginationUserIds.size());
            assertEquals(userIds, paginationUserIds);
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }
}
