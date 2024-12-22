package config;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigLoader {
    private Properties properties;
    private String configPath;

    public ConfigLoader(String configFilePath) {
        setConfigPath(configFilePath);
        properties = new Properties();
        try (FileInputStream input = new FileInputStream(configFilePath)) {
            properties.load(input);
        } catch (IOException ex) {
            System.err.println("Error loading config file: " + ex.getMessage());
        }
    }

    public int getNumOfSlaves() {
        return Integer.parseInt(properties.getProperty("numOfSlaves"));
    }

    public String getSlaveDirPath(String slaveId) {
        String key = slaveId.concat(".dirPath");
        return properties.getProperty(key, "localhost");
    }

    public String getSlaveHost(String slaveId) {
        String key = slaveId.concat(".host");
        return properties.getProperty(key, "localhost");
    }

    public int getSlavePort(String slaveId) {
        String key = slaveId.concat(".port");
        return Integer.parseInt(properties.getProperty(key));
    }

    public String getMasterHost() {
        return properties.getProperty("masterHost", "localhost");
    }

    public int getMasterPort() {
        return Integer.parseInt(properties.getProperty("masterPort", "10000"));
    }

    // public int getMasterPortForClient() {
    //     return Integer.parseInt(properties.getProperty("masterPortForClient", "3184"));
    // }

    public String getSavePath() {
        return properties.getProperty("storageDir");
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public String getConfigPath() {
        return configPath;
    }

    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
}
