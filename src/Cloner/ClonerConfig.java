package Cloner;

import GLSUtility.ConfigUtility;
import GLSUtility.LogService;

import java.util.Properties;

/**
 * Created by RobbinNi on 7/8/16.
 */
public class ClonerConfig {

    private static final String ENTITY = "Cloner.ClonerConfig";

    private final boolean loaded;
    public int clonerBlockSize;
    public int clonerQueryBlockSize;
    public String clonerRoot;
    public int clonerCnt;
    public int clonerPoolSize;
    public int curTaskCnt;
    public int clonerTaskRetry;

    public ClonerConfig(LogService logger, Properties config) {

        clonerRoot = ConfigUtility.loadString(ENTITY, "clonerroot", "GithubListStatistics/", logger, config);
        clonerBlockSize = ConfigUtility.loadInteger(ENTITY, "clonerblock", 20, logger, config);
        clonerCnt = ConfigUtility.loadInteger(ENTITY, "clonercnt", 5, logger, config);
        clonerPoolSize = ConfigUtility.loadInteger(ENTITY, "clonerpool", 200, logger, config);
        clonerQueryBlockSize = ConfigUtility.loadInteger(ENTITY, "clonerqueryblock", 100, logger, config);
        clonerTaskRetry = ConfigUtility.loadInteger(ENTITY, "clonertaskretry", 5, logger, config);

        curTaskCnt = 0;

        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
