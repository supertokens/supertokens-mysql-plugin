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
 *
 */

package io.supertokens.storage.mysql.test;

import io.supertokens.ProcessState;
import io.supertokens.storage.mysql.annotations.DashboardInfo;
import io.supertokens.storage.mysql.config.MySQLConfig;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MySQLConfigTest {

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
    public void testMatchConfigPropertiesDescription() throws Exception {
        String[] args = { "../" };

        TestingProcessManager.TestingProcess process = TestingProcessManager.start(args);
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STARTED));

        // Skipping mysql_config_version because it doesn't
        // have a description in the config.yaml file
        String[] ignoredProperties = { "mysql_config_version" };

        // Match the descriptions in the config.yaml file with the descriptions in the
        // CoreConfig class
        matchYamlAndConfigDescriptions("./config.yaml", ignoredProperties);

        // Match the descriptions in the devConfig.yaml file with the descriptions in
        // the CoreConfig class
        String[] devConfigIgnoredProperties = Arrays.copyOf(ignoredProperties, ignoredProperties.length + 2);
        // We ignore these properties in devConfig.yaml because it has a different
        // description
        // in devConfig.yaml and has a default value
        devConfigIgnoredProperties[ignoredProperties.length] = "mysql_user";
        devConfigIgnoredProperties[ignoredProperties.length + 1] = "mysql_password";
        matchYamlAndConfigDescriptions("./devConfig.yaml", devConfigIgnoredProperties);

        process.kill();
        assertNotNull(process.checkOrWaitForEvent(ProcessState.PROCESS_STATE.STOPPED));
    }

    private void matchYamlAndConfigDescriptions(String path, String[] ignoreProperties) throws Exception {
        try (BufferedReader reader = new BufferedReader(new FileReader(path))) {
            // Get the content of the file as string
            String content = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            // Find the line that contains 'mysql_config_version', and then split
            // the file after that line
            String allProperties = content.split("mysql_config_version:\\s*\\d+\n")[1];

            // Split by all the other allProperties string by new line
            String[] properties = allProperties.split("\n\n");
            // This will contain the description of each property from the yaml file
            Map<String, String> propertyDescriptions = new HashMap<String, String>();

            System.out.println("Last property: " + properties[properties.length - 1] + "\n\n");

            for (int i = 0; i < properties.length; i++) {
                String possibleProperty = properties[i].trim();
                String[] lines = possibleProperty.split("\n");
                // This ensures that it is a property with a description as a comment
                // at the top
                if (lines[lines.length - 1].endsWith(":")) {
                    String propertyKeyString = lines[lines.length - 1];
                    // Remove the comment "# " from the start
                    String propertyKey = propertyKeyString.substring(2, propertyKeyString.length() - 1);
                    String propertyDescription = "";
                    // Remove the comment "# " from the start and merge all the lines to form the
                    // description
                    for (int j = 0; j < lines.length - 1; j++) {
                        propertyDescription = propertyDescription + " " + lines[j].substring(2);
                    }
                    propertyDescription = propertyDescription.trim();

                    propertyDescriptions.put(propertyKey, propertyDescription);
                }
            }

            for (String fieldId : MySQLConfig.getValidFields()) {
                if (Arrays.asList(ignoreProperties).contains(fieldId)) {
                    continue;
                }

                Field field = MySQLConfig.class.getDeclaredField(fieldId);

                // Skip fields that are not annotated with JsonProperty
                if (!field.isAnnotationPresent(JsonProperty.class)) {
                    continue;
                }

                String valueInfo = "";
                if (field.getType() == String.class) {
                    valueInfo = "string value.";
                } else if (field.getType() == int.class || field.getType() == Integer.class) {
                    valueInfo = "integer value.";
                } else if (field.getType() == long.class) {
                    valueInfo = "long value.";
                } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                    valueInfo = "boolean value.";
                }

                System.out.println(field.getName());
                String descriptionInConfig = field.getAnnotation(DashboardInfo.class).description();
                descriptionInConfig = "(DIFFERENT_ACROSS_TENANTS" + (field.getAnnotation(DashboardInfo.class).isOptional() ? " | OPTIONAL" : " | COMPULSORY")
                        + (field.getAnnotation(DashboardInfo.class).isOptional() ? " | Default: " + field.getAnnotation(DashboardInfo.class).defaultValue() : "")
                        + ") "
                        + valueInfo + " "
                        + descriptionInConfig;
                String descriptionInYaml = propertyDescriptions.get(fieldId);

                if (descriptionInYaml == null) {
                    fail("Unable to find description or property for " + fieldId + " in " + path + " file");
                }

                // Assert that description in yaml contains the description in config
                assertEquals("For " + fieldId, descriptionInYaml.trim(), descriptionInConfig.trim());
            }
        }
    }
}