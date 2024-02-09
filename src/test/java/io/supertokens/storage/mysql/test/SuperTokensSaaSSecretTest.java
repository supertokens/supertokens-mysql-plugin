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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.supertokens.ProcessState;
import io.supertokens.featureflag.EE_FEATURES;
import io.supertokens.featureflag.FeatureFlagTestContent;
import io.supertokens.featureflag.exceptions.FeatureNotEnabledException;
import io.supertokens.multitenancy.Multitenancy;
import io.supertokens.multitenancy.exception.BadPermissionException;
import io.supertokens.multitenancy.exception.CannotModifyBaseConfigException;
import io.supertokens.pluginInterface.STORAGE_TYPE;
import io.supertokens.pluginInterface.exceptions.InvalidConfigException;
import io.supertokens.pluginInterface.exceptions.StorageQueryException;
import io.supertokens.pluginInterface.multitenancy.*;
import io.supertokens.pluginInterface.multitenancy.exceptions.TenantOrAppNotFoundException;
import io.supertokens.storage.mysql.test.httpRequest.HttpRequestForTesting;
import io.supertokens.storage.mysql.test.httpRequest.HttpResponseException;
import io.supertokens.storageLayer.StorageLayer;
import io.supertokens.thirdparty.InvalidProviderConfigException;
import io.supertokens.utils.SemVer;
import org.junit.*;
import org.junit.rules.TestRule;

import java.io.IOException;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;

public class SuperTokensSaaSSecretTest {
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

    private static final String[] PROTECTED_CORE_CONFIG = new String[]{
            "mysql_connection_pool_size",
            "mysql_connection_uri",
            "mysql_host",
            "mysql_port",
            "mysql_user",
            "mysql_password",
            "mysql_database_name",
            "mysql_idle_connection_timeout",
            "mysql_minimum_idle_connections",
    };
    private static final Object[] PROTECTED_CORE_CONFIG_VALUES = new Object[]{
            20, // mysql_connection_pool_size
            "mysql://root:root@localhost:3306/st10", // mysql_connection_uri
            "localhost", // mysql_host
            3306, // mysql_port
            "root", // mysql_user
            "root", // mysql_password
            "st10", // mysql_database_name
            40000, // mysql_idle_connection_timeout
            5, // mysql_minimum_idle_connections
    };

    @Test
    public void testThatTenantCannotSetProtectedConfigIfSuperTokensSaaSSecretIsSet()
            throws InterruptedException, IOException, InvalidConfigException, TenantOrAppNotFoundException,
            InvalidProviderConfigException, StorageQueryException,
            FeatureNotEnabledException, CannotModifyBaseConfigException, HttpResponseException {
        String[] args = {"../"};

        String saasSecret = "hg40239oirjgBHD9450=Beew123--hg40239oirjgBHD9450=Beew123--hg40239oirjgBHD9450=Beew123-";
        String apiKey = "hg40239oirjgBHD9450=Beew123--hg40239oiBeew123-";
        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        Utils.setValueInConfig("supertokens_saas_secret", saasSecret);
        Utils.setValueInConfig("api_keys", apiKey);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{EE_FEATURES.MULTI_TENANCY});
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        for (int i = 0; i < PROTECTED_CORE_CONFIG.length; i++) {
            try {
                JsonObject j = new JsonObject();
                j.addProperty(PROTECTED_CORE_CONFIG[i], "");
                Multitenancy.addNewOrUpdateAppOrTenant(process.main, new TenantConfig(new TenantIdentifier(null, null, "t1"), new EmailPasswordConfig(false),
                        new ThirdPartyConfig(false, new ThirdPartyConfig.Provider[0]),
                        new PasswordlessConfig(false),
                        j), true);
                fail();
            } catch (BadPermissionException e) {
                assertEquals(e.getMessage(), "Not allowed to modify DB related configs.");
            }
        }

        try {
            JsonObject coreConfig = new JsonObject();
            StorageLayer.getBaseStorage(process.getProcess()).modifyConfigToAddANewUserPoolForTesting(coreConfig, 1);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("appId", "a1");
            requestBody.addProperty("emailPasswordEnabled", true);
            requestBody.addProperty("thirdPartyEnabled", true);
            requestBody.addProperty("passwordlessEnabled", true);
            requestBody.add("coreConfig", coreConfig);

            JsonObject response = HttpRequestForTesting.sendJsonRequest(process.getProcess(), "",
                    HttpRequestForTesting.getMultitenantUrl(TenantIdentifier.BASE_TENANT, "/recipe/multitenancy/app"),
                    requestBody, 1000, 2500, null,
                    SemVer.v3_0.get(), "PUT", apiKey, "multitenancy");

            Assert.assertEquals("OK", response.getAsJsonPrimitive("status").getAsString());
            fail();
        } catch (HttpResponseException e) {
            Assert.assertTrue(e.getMessage().contains("Not allowed to modify DB related configs."));
        }

        JsonObject coreConfig = new JsonObject();
        StorageLayer.getBaseStorage(process.getProcess()).modifyConfigToAddANewUserPoolForTesting(coreConfig, 1);

        JsonObject requestBody = new JsonObject();
        requestBody.addProperty("appId", "a1");
        requestBody.addProperty("emailPasswordEnabled", true);
        requestBody.addProperty("thirdPartyEnabled", true);
        requestBody.addProperty("passwordlessEnabled", true);
        requestBody.add("coreConfig", coreConfig);

        JsonObject response = HttpRequestForTesting.sendJsonRequest(process.getProcess(), "",
                HttpRequestForTesting.getMultitenantUrl(TenantIdentifier.BASE_TENANT, "/recipe/multitenancy/app"),
                requestBody, 1000, 2500, null,
                SemVer.v3_0.get(), "PUT", saasSecret, "multitenancy");

        Assert.assertEquals("OK", response.getAsJsonPrimitive("status").getAsString());

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    @Test
    public void testThatTenantCannotGetProtectedConfigIfSuperTokensSaaSSecretIsSet()
            throws InterruptedException, IOException, InvalidConfigException, TenantOrAppNotFoundException,
            InvalidProviderConfigException, StorageQueryException,
            FeatureNotEnabledException, CannotModifyBaseConfigException, BadPermissionException, HttpResponseException {
        String[] args = {"../"};

        String saasSecret = "hg40239oirjgBHD9450=Beew123--hg40239oirjgBHD9450=Beew123--hg40239oirjgBHD9450=Beew123-";
        String apiKey = "hg40239oirjgBHD9450=Beew123--hg40239oiBeew123-";
        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args, false);
        Utils.setValueInConfig("supertokens_saas_secret", saasSecret);
        Utils.setValueInConfig("api_keys", apiKey);
        FeatureFlagTestContent.getInstance(process.getProcess())
                .setKeyValue(FeatureFlagTestContent.ENABLED_FEATURES, new EE_FEATURES[]{EE_FEATURES.MULTI_TENANCY});
        process.startProcess();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        if (StorageLayer.isInMemDb(process.getProcess())) {
            return;
        }

        if (StorageLayer.getStorage(process.getProcess()).getType() != STORAGE_TYPE.SQL) {
            return;
        }

        for (int i = 0; i < PROTECTED_CORE_CONFIG.length; i++) {
            JsonObject j = new JsonObject();
            if (PROTECTED_CORE_CONFIG_VALUES[i] instanceof String) {
                j.addProperty(PROTECTED_CORE_CONFIG[i], (String) PROTECTED_CORE_CONFIG_VALUES[i]);
            } else if (PROTECTED_CORE_CONFIG_VALUES[i] instanceof Integer) {
                j.addProperty(PROTECTED_CORE_CONFIG[i], (Integer) PROTECTED_CORE_CONFIG_VALUES[i]);
            }
            Multitenancy.addNewOrUpdateAppOrTenant(process.main, new TenantIdentifier(null, null, null),
                    new TenantConfig(new TenantIdentifier(null, null, "t" + i), new EmailPasswordConfig(false),
                            new ThirdPartyConfig(false, new ThirdPartyConfig.Provider[0]),
                            new PasswordlessConfig(false),
                            j));

            {
                JsonObject response = HttpRequestForTesting.sendJsonRequest(process.getProcess(), "",
                        HttpRequestForTesting.getMultitenantUrl(TenantIdentifier.BASE_TENANT, "/recipe/multitenancy/tenant/list"),
                        null, 1000, 1000, null,
                        SemVer.v3_0.get(), "GET", apiKey, "multitenancy");

                Assert.assertEquals("OK", response.getAsJsonPrimitive("status").getAsString());

                boolean found = false;
                for (JsonElement tenant : response.get("tenants").getAsJsonArray()) {
                    JsonObject tenantObj = tenant.getAsJsonObject();

                    if (tenantObj.get("tenantId").getAsString().equals("t" + i)) {
                        found = true;

                        assertFalse(tenantObj.get("coreConfig").getAsJsonObject().has(PROTECTED_CORE_CONFIG[i]));
                    }
                }
                Assert.assertTrue(found);
            }

            {
                JsonObject response = HttpRequestForTesting.sendJsonRequest(process.getProcess(), "",
                        HttpRequestForTesting.getMultitenantUrl(TenantIdentifier.BASE_TENANT, "/recipe/multitenancy/tenant/list"),
                        null, 1000, 1000, null,
                        SemVer.v3_0.get(), "GET", saasSecret, "multitenancy");

                Assert.assertEquals("OK", response.getAsJsonPrimitive("status").getAsString());

                boolean found = false;
                for (JsonElement tenant : response.get("tenants").getAsJsonArray()) {
                    JsonObject tenantObj = tenant.getAsJsonObject();

                    if (tenantObj.get("tenantId").getAsString().equals("t" + i)) {
                        found = true;

                        Assert.assertTrue(tenantObj.get("coreConfig").getAsJsonObject().has(PROTECTED_CORE_CONFIG[i]));
                    }
                }
                Assert.assertTrue(found);
            }
        }

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

}