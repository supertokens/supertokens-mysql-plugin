package io.supertokens.storage.mysql.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.supertokens.pluginInterface.exceptions.QuitProgramFromPluginException;
import io.supertokens.storage.mysql.ResourceDistributor;
import io.supertokens.storage.mysql.Start;
import io.supertokens.storage.mysql.output.Logging;

import java.io.File;
import java.io.IOException;

public class Config extends ResourceDistributor.SingletonResource {

    private static final String RESOURCE_KEY = "io.supertokens.storage.mysql.config.Config";
    private final MySQLConfig config;
    private final Start start;

    private Config(Start start, String configFilePath) {
        this.start = start;
        try {
            config = loadMySQLConfig(configFilePath);
        } catch (IOException e) {
            throw new QuitProgramFromPluginException(e);
        }
    }

    private static Config getInstance(Start start) {
        return (Config) start.getResourceDistributor().getResource(RESOURCE_KEY);
    }

    public static void loadConfig(Start start, String configFilePath) {
        if (getInstance(start) != null) {
            return;
        }
        start.getResourceDistributor().setResource(RESOURCE_KEY, new Config(start, configFilePath));
    }

    public static MySQLConfig getConfig(Start start) {
        if (getInstance(start) == null) {
            throw new QuitProgramFromPluginException("Please call loadConfig() before calling getConfig()");
        }
        return getInstance(start).config;
    }

    private MySQLConfig loadMySQLConfig(String configFilePath) throws IOException {
        Logging.info(start, "Loading MySQL config.");
        final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        MySQLConfig config = mapper.readValue(new File(configFilePath), MySQLConfig.class);
        config.validateAndInitialise();
        return config;
    }

}
