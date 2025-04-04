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

import io.supertokens.Main;
import io.supertokens.pluginInterface.PluginInterfaceTesting;
import io.supertokens.storage.mysql.Start;
import io.supertokens.storage.mysql.queries.MultitenancyQueries;
import io.supertokens.storageLayer.StorageLayer;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.junit.rules.TestRule;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public abstract class Utils extends Mockito {

    private static ByteArrayOutputStream byteArrayOutputStream;

    public static void afterTesting() {
        String installDir = "../";
        try {
            // we remove the license key file
            ProcessBuilder pb = new ProcessBuilder("rm", "licenseKey");
            pb.directory(new File(installDir));
            Process process = pb.start();
            process.waitFor();

            // remove config.yaml file
            String workerId = System.getProperty("org.gradle.test.worker");
            pb = new ProcessBuilder("rm", "config" + workerId + ".yaml");
            pb.directory(new File(installDir));
            process = pb.start();
            process.waitFor();

            // remove webserver-temp folders created by tomcat
            final File webserverTemp = new File(installDir + "webserver-temp");
            try {
                FileUtils.deleteDirectory(webserverTemp);
            } catch (Exception ignored) {
            }

            // remove .started folder created by processes
            final File dotStartedFolder = new File(installDir + ".started" + workerId);
            try {
                FileUtils.deleteDirectory(dotStartedFolder);
            } catch (Exception ignored) {
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void reset() {
        Main.isTesting = true;
        PluginInterfaceTesting.isTesting = true;
        Start.isTesting = true;
        Main.makeConsolePrintSilent = true;
        MultitenancyQueries.simulateErrorInAddingTenantIdInTargetStorage_forTesting = false;

        String installDir = "../";
        String workerId = System.getProperty("org.gradle.test.worker");
        try {
            // if the default config is not the same as the current config, we must reset
            // the storage layer
            File ogConfig = new File("../temp/config.yaml");
            File currentConfig = new File("../config" + workerId + ".yaml");
            if (currentConfig.isFile()) {
                byte[] ogConfigContent = Files.readAllBytes(ogConfig.toPath());
                byte[] currentConfigContent = Files.readAllBytes(currentConfig.toPath());
                if (!Arrays.equals(ogConfigContent, currentConfigContent)) {
                    StorageLayer.close();
                }
            }

            ProcessBuilder pb = new ProcessBuilder("cp", "temp/config.yaml", "./config" + workerId + ".yaml");
            pb.directory(new File(installDir));
            Process process = pb.start();
            process.waitFor();

            TestingProcessManager.killAll();
            TestingProcessManager.deleteAllInformation();
            TestingProcessManager.killAll();

            byteArrayOutputStream = new ByteArrayOutputStream();
            System.setErr(new PrintStream(byteArrayOutputStream));
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.gc();
    }

    static void stopLicenseKeyFromUpdatingToLatest(TestingProcessManager.TestingProcess process) {
        try {
            List<String> licenseKey = Files.readAllLines(Paths.get("../licenseKey"));
            String escapedKey = licenseKey.get(0).replaceAll("\"", "\\\\\"");
            final HttpsURLConnection mockCon = mock(HttpsURLConnection.class);
            InputStream inputStrm = new ByteArrayInputStream(
                    ("{\"latestLicenseKey\": \"" + escapedKey + "\"}").getBytes(StandardCharsets.UTF_8));
            when(mockCon.getInputStream()).thenReturn(inputStrm);
            when(mockCon.getResponseCode()).thenReturn(200);
            when(mockCon.getOutputStream()).thenReturn(new OutputStream() {
                @Override
                public void write(int b) {
                }
            });

        } catch (Exception ignored) {

        }
    }

    public static void setValueInConfig(String key, String value) throws FileNotFoundException, IOException {
        // we close the storage layer since there might be a change in the db related
        // config.
        StorageLayer.close();

        String oldStr = "\n((#\\s)?)" + key + "(:|((:\\s).+))\n";
        String newStr = "\n" + key + ": " + value + "\n";
        StringBuilder originalFileContent = new StringBuilder();
        String workerId = System.getProperty("org.gradle.test.worker");
        try (BufferedReader reader = new BufferedReader(new FileReader("../config" + workerId + ".yaml"))) {
            String currentReadingLine = reader.readLine();
            while (currentReadingLine != null) {
                originalFileContent.append(currentReadingLine).append(System.lineSeparator());
                currentReadingLine = reader.readLine();
            }
            String modifiedFileContent = originalFileContent.toString().replaceAll(oldStr, newStr);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("../config" + workerId + ".yaml"))) {
                writer.write(modifiedFileContent);
            }
        }
    }

    public static void commentConfigValue(String key) throws IOException {
        // we close the storage layer since there might be a change in the db related
        // config.
        StorageLayer.close();

        String oldStr = "\n((#\\s)?)" + key + "(:|((:\\s).+))\n";
        String newStr = "\n# " + key + ":";

        StringBuilder originalFileContent = new StringBuilder();
        String workerId = System.getProperty("org.gradle.test.worker");
        try (BufferedReader reader = new BufferedReader(new FileReader("../config" + workerId + ".yaml"))) {
            String currentReadingLine = reader.readLine();
            while (currentReadingLine != null) {
                originalFileContent.append(currentReadingLine).append(System.lineSeparator());
                currentReadingLine = reader.readLine();
            }
            String modifiedFileContent = originalFileContent.toString().replaceAll(oldStr, newStr);
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("../config" + workerId + ".yaml"))) {
                writer.write(modifiedFileContent);
            }
        }

    }

    public static TestRule getOnFailure() {
        return new TestWatcher() {
            @Override
            protected void failed(Throwable e, Description description) {
                System.out.println(byteArrayOutputStream.toString(StandardCharsets.UTF_8));
            }
        };
    }

    public static TestRule retryFlakyTest() {
        return new TestRule() {
            private final int retryCount = 10;

            public Statement apply(Statement base, Description description) {
                return statement(base, description);
            }

            private Statement statement(final Statement base, final Description description) {
                return new Statement() {
                    @Override
                    public void evaluate() throws Throwable {
                        Throwable caughtThrowable = null;

                        // implement retry logic here
                        for (int i = 0; i < retryCount; i++) {
                            try {
                                base.evaluate();
                                return;
                            } catch (Throwable t) {
                                caughtThrowable = t;
                                System.err.println(description.getDisplayName() + ": run " + (i+1) + " failed");
                                TestingProcessManager.killAll();
                                Thread.sleep(1000 + new Random().nextInt(3000));
                            }
                        }
                        System.err.println(description.getDisplayName() + ": giving up after " + retryCount + " failures");
                        throw caughtThrowable;
                    }
                };
            }
        };
    }

}