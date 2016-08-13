package GLSUtility;

import java.util.Properties;
import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/6/16.
 */
public final class ConfigUtility {

    public static int loadInteger(String entity, String name, int defaultValue, LogService logger, Properties config) {
        int ret = defaultValue;
        try {
            String s = config.getProperty(name);
            if (s != null) {
                ret = Integer.valueOf(s);
            } else {
                logger.log(Level.WARNING, entity, "Property \"" + name + "\" undefined, using default value " + defaultValue);
            }
        } catch (NumberFormatException ne) {
            logger.log(Level.WARNING, entity, "Property \"" + name + "\" cannot be parsed, using default value " + defaultValue);
        } finally {
            logger.log(Level.FINER, entity, "Property " + name + " = " + ret);
        }
        return ret;
    }

    public static String loadString(String entity, String name, String defaultValue, LogService logger, Properties config) {
        String ret = defaultValue;
        String s = config.getProperty(name);
        if (s != null) {
            ret = s;
        } else {
            logger.log(Level.WARNING, entity, "Property \"" + name + "\" undefined, using default value " + defaultValue);
        }
        logger.log(Level.FINER, entity, "Property " + name + " = " + ret);
        return ret;
    }


    public static boolean loadBoolean(String entity, String name, boolean defaultValue, LogService logger, Properties config) {
        boolean ret = defaultValue;
        String s = config.getProperty(name);
        if (s != null) {
            ret = Boolean.valueOf(config.getProperty(name));
        } else {
            logger.log(Level.WARNING, entity, "Property \"" + name + "\" undefined, using default value " + defaultValue);
        }
        logger.log(Level.FINER, entity, "Property " + name + " = " + ret);
        return ret;
    }

}
