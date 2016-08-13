package Crawler;

import Data.GithubRepo;
import Data.GithubUser;
import Main.SystemConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/6/16.
 */
public class CrawlerData {

    public static final int START = Integer.MIN_VALUE, COMPLETE = Integer.MAX_VALUE - 1, NOMOREUSERS = Integer.MAX_VALUE;
    public static final String ENTITY = "Crawler.CrawlerData";

    public final int dataId;
    public int state;
    public final int startId;
    public List<GithubUser> crawledUsers;
    public List<List<GithubRepo>> crawledRepos;
    public GithubKey key;
    public int retryCnt;

    static class CrawlerDataFactory {
        private final SystemConfig config;
        private int dataId;
        public static final String ENTITY = "Crawler.CrawlerDataFactory";

        CrawlerDataFactory(SystemConfig config) {
            this.config = config;
            this.dataId = 0;
        }

        private int getDataId() {
            return dataId++;
        }

        CrawlerData getNextCrawlerData() {
            CrawlerData ret = new CrawlerData(getDataId(),
                    config.crawlerConfig.getLastUser(),
                    config.crawlerConfig.keyPool.issueKey());
            config.logger.log(Level.FINER, ENTITY, "New crawler data created startId = " + ret.startId + " dataId = " + ret.dataId);
            return ret;
        }
    }

    private CrawlerData(int dataId, int startId, GithubKey key) {
        this.dataId = dataId;
        this.state = Integer.MIN_VALUE;
        this.startId = startId;
        this.crawledUsers = new ArrayList<>();
        this.crawledRepos = new ArrayList<>();
        this.key = key;
        this.retryCnt = 0;
    }

    public static CrawlerDataFactory getCrawlerDataFactory(SystemConfig config) {
        return new CrawlerDataFactory(config);
    }

    synchronized public void reinitialize() {
        if (state == START) {
            crawledUsers.clear();
        } else {
            crawledRepos.subList(state, crawledRepos.size()).clear();
        }
    }

    synchronized public void addUser(GithubUser user) {
        if (state != START) {
            throw new IllegalStateException();
        }
        crawledUsers.add(user);
    }

    synchronized public void addUser(List<GithubUser> user) {
        if (state != START) {
            throw new IllegalStateException();
        }
        crawledUsers.addAll(user);
    }

    synchronized public void completeUsers() {

        if (state != START) {
            throw new IllegalStateException();
        }
        state = 0;
    }

    synchronized public void addRepo(List<GithubRepo> repos) {
        if (state != crawledRepos.size()) {
            throw new IllegalStateException();
        }
        crawledRepos.add(repos);
        ++state;
    }

    synchronized public void completeRepo() {
        if (state != crawledUsers.size()) {
            throw new IllegalStateException();
        }
        state = COMPLETE;
    }

    synchronized public void setNoMoreUsers() {
        state = NOMOREUSERS;
    }

    synchronized public boolean isNoMoreUsers() {
        return state == NOMOREUSERS;
    }

    synchronized public boolean inUserBlock(int id) {
        return startId < id && id <= startId + CrawlerConfig.USERBLOCK;
    }

    synchronized public boolean isComplete() {
        return state >= COMPLETE;
    }

}
