package Crawler;

import GLSUtility.LogService;

import java.util.ArrayList;
import java.util.Properties;
import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/6/16.
 */
public class CrawlerKeyPool {

    private final Properties config;
    private final LogService logger;
    private static final String ENTITY = "Crawler.CrawlerKeyPool";

    private int keyCnt, curKeyReq;
    private GithubKey defaultKey;
    private ArrayList<GithubKey> keyList;

    public CrawlerKeyPool(LogService logger, Properties config) {
        this.logger = logger;
        this.config = config;
        keyCnt = 0;
        curKeyReq = 0;
        defaultKey = new NoneKey();
        keyList = new ArrayList<>();
        // if AnonymousKey is provided, replace defaultKey
        if (config.getProperty("oathid") != null && config.getProperty("oathsecret") != null) {
            defaultKey = new AnonymousKey(config.getProperty("oathid"), config.getProperty("oathsecret"));
            logger.log(Level.CONFIG, ENTITY, "AnonymousKey found, replace default empty key");
        }
        if (config.getProperty("keycnt") != null) {
            try {
                int tmpCnt = Integer.valueOf(config.getProperty("keycnt"));
                for (int i = 0; i < tmpCnt; ++i) {
                    String username = config.getProperty("username" + i),
                            token = config.getProperty("token" + i);
                    if (username != null && token != null) {
                        keyList.add(new UserKey(username, token));
                    } else {
                        logger.log(Level.WARNING, ENTITY, "Failed to load user key " + i);
                    }
                }
            } catch (NumberFormatException ne) {
                logger.logErr(Level.WARNING, ENTITY, "Failed to parse \"keycnt\" as an integer, no user key collected", ne);
            }
        } else {
            logger.log(Level.WARNING, ENTITY, "Failed to find property \"keycnt\", no user key collected");
        }
        keyCnt = keyList.size();
        if (keyCnt == 0) {
            logger.log(Level.WARNING, ENTITY, "no user key collected, the default key will be used for all API requests");
        } else {
            logger.log(Level.INFO, ENTITY, keyCnt + " user key(s) loaded");
        }
    }

    public GithubKey issueKey() {
        synchronized (this) {
            if (keyCnt == 0) {
                return defaultKey;
            } else {
                return keyList.get(curKeyReq++ % keyCnt);
            }
        }
    }

    public GithubKey issueNewKey(GithubKey key) {
        synchronized (this) {
            if (keyCnt <= 1) {
                return key;
            } else {
                GithubKey nxt = issueKey();
                if (nxt == key) {
                    nxt = issueKey();
                }
                return nxt;
            }
        }
    }

    public void addUserKey(String username, String token) {
        GithubKey nkey = new UserKey(username, token);
        synchronized (this) {
            keyList.add(nkey);
            ++keyCnt;
        }
    }

    public void changeDefaultKey(String clientid, String clientsecret) {
        GithubKey nkey = new AnonymousKey(clientid, clientsecret);
        synchronized (this) {
            defaultKey = nkey;
        }
    }

    public void writeback() {
        //TODO
    }
}
