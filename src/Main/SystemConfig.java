package Main;

import Cloner.ClonerConfig;
import Crawler.CrawlerConfig;
import Database.DatabaseConfig;
import GLSUtility.ConfigUtility;
import GLSUtility.LogService;

import java.io.*;
import java.util.Properties;
import java.util.logging.Level;


/**
 * Created by RobbinNi on 7/6/16.
 */
public class SystemConfig {

    private final boolean loaded;

    public final LogService logger;
    private static final String ENTITY = "Main.SystemConfig";
    private static final String configFile = "GithubLiveStatistics.properties";

    public final Properties config;
    public final CrawlerConfig crawlerConfig;
    public final DatabaseConfig databaseConfig;
    public final ClonerConfig clonerConfig;

    public Level logLevel;
    public boolean writeBackOnClose;
    public boolean initDatabaseOnStart;
    public boolean clearClonerRootOnStart;
    public long mainCyclePeriod;

    public int portNum;


    public SystemConfig(LogService logger) {
        this.logger = logger;
        logger.log(Level.INFO, ENTITY, "Loading configuration file " + configFile);
        config = new Properties();
        File configF = new File(configFile);
        try {
            if (configF.exists()) {
                InputStream ism = new FileInputStream(configF);
                config.load(ism);
                ism.close();
            } else {
                logger.log(Level.WARNING, ENTITY, "Configuration file " + configFile + " does not exist, using default values");
            }
        } catch (IOException ie) {
            logger.logErr(Level.SEVERE, ENTITY, "Failed to load configuration file", ie);
            crawlerConfig = null;
            databaseConfig = null;
            clonerConfig = null;
            loaded = false;
            return;
        }

        // System configuration
        try {
            logLevel = Level.parse(config.getProperty("loglevel"));
        } catch (NullPointerException ne) {
            logger.log(Level.WARNING, ENTITY, "Property \"loglevel\" undefined, using default value Level.ALL");
            logLevel = Level.ALL;
        } catch (IllegalArgumentException ar) {
            logger.log(Level.WARNING, ENTITY, "Property \"loglevel\" cannot be parsed, using default value Level.ALL");
            logLevel = Level.ALL;
        } finally {
            logger.setLogLevel(logLevel);
            logger.log(Level.FINER, ENTITY, "Property loglevel = " + logLevel.getName());
        }

        writeBackOnClose = ConfigUtility.loadBoolean(ENTITY, "writeback", false, logger, config);
        initDatabaseOnStart = ConfigUtility.loadBoolean(ENTITY, "initdbonstart", false, logger, config);
        clearClonerRootOnStart = ConfigUtility.loadBoolean(ENTITY, "clearclonerrootonstart", false, logger, config);

        mainCyclePeriod = GLSUtility.ConfigUtility.loadInteger(ENTITY, "maincycle", 1000, logger, config);
        portNum = ConfigUtility.loadInteger(ENTITY, "portnum", 10007, logger, config);

        crawlerConfig = new CrawlerConfig(logger, config);
        if (!crawlerConfig.isLoaded()) {
            databaseConfig = null;
            clonerConfig = null;
            loaded = false;
            return;
        }

        databaseConfig = new DatabaseConfig(logger, config);
        if (!databaseConfig.isLoaded()) {
            clonerConfig = null;
            loaded = false;
            return;
        }

        clonerConfig = new ClonerConfig(logger, config);
        if (!clonerConfig.isLoaded()) {
            loaded = false;
            return;
        }

        logger.log(Level.INFO, ENTITY, "Successfully loaded system configuration");
        loaded = true;
    }


    public void close() {
        if (writeBackOnClose) {
            writeBack();
        }
    }
    public void writeBack() {
        crawlerConfig.writeBack();
        config.setProperty("loglevel", logLevel.getName());
        config.setProperty("writeback", Boolean.toString(writeBackOnClose));
        logger.log(Level.INFO, ENTITY, "Writing back system configuration to configuration file GithubLiveStatistics.properties");
        PrintStream osm;
        try {
            File configF = new File(configFile);
            if (!configF.exists()) {
                configF.createNewFile();
            }
            osm = new PrintStream(configF);
            config.list(osm);
            osm.close();
            if (osm.checkError()) {
                logger.log(Level.SEVERE, ENTITY, "Failed to write back configuration file");
            } else {
                logger.log(Level.INFO, ENTITY, "Successfully write back configuration file");
            }
        } catch (IOException ie) {
            logger.logErr(Level.SEVERE, ENTITY, "Failed to write back configuration file", ie);
        }
    }

    public boolean isLoaded() {
        return loaded;
    }
}
