package Crawler;

import GLSUtility.ConfigUtility;
import GLSUtility.LogService;

import java.util.Properties;

/**
 * Created by RobbinNi on 7/6/16.
 */
public class CrawlerConfig {

    public static final int USERBLOCK = 30;
    public static final int REPOPAGESIZE = 30;

    private final Properties config;
    private final LogService logger;
    private static final String ENTITY = "Crawler.CrawlerConfig";

    public final CrawlerKeyPool keyPool;

    public int reconnect; //How long to wait if a connection error happens
    private int lastUser; //Biggest user id ever crawled
    public int connCnt; //Maximum number of connections for crawling
    public int crawlerCnt; //Maximum number of concurrent crawlers
    public int retryCnt; //How many time to retry if a connection error happens
    public int crawlerTaskRetry;

    public int curTaskCnt;

    private boolean allUsersCrawled;

    //TODO : when to crawl all users all over again?

    private final boolean loaded;

    public CrawlerConfig(LogService logger, Properties config) {
        this.logger = logger;
        this.config = config;

        keyPool = new CrawlerKeyPool(logger, config);

        reconnect = ConfigUtility.loadInteger(ENTITY, "reconnect", 6000, logger, config);
        lastUser = ConfigUtility.loadInteger(ENTITY, "lastuser", 0, logger, config);
        connCnt = ConfigUtility.loadInteger(ENTITY, "conncnt", 5, logger, config);
        crawlerCnt = ConfigUtility.loadInteger(ENTITY, "crawlercnt", 5, logger, config);
        retryCnt = ConfigUtility.loadInteger(ENTITY, "retrycnt", 5, logger, config);
        crawlerTaskRetry = ConfigUtility.loadInteger(ENTITY, "crawlertaskretry", 5, logger, config);

        curTaskCnt = 0;

        allUsersCrawled = false;

        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public int getLastUser() {
        int ret = lastUser;
        lastUser += USERBLOCK;
        return ret;
    }

    public boolean isAllUsersCrawled() {
        return allUsersCrawled;
    }

    public void allUsersCrawled() {
        //TODO
        allUsersCrawled = true;
    }

    public void writeBack() {
        //TODO
    }
}
