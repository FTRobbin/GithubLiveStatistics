package Crawler;

import Main.TaskWrapper;

/**
 * Created by RobbinNi on 7/7/16.
 */
public class CrawlerWrapper extends TaskWrapper {

    public CrawlerData data;
    public Exception e;

    public CrawlerWrapper(CrawlerData data) {
        this.data = data;
        this.e = null;
    }

    public CrawlerWrapper(CrawlerData data, Exception e) {
        this.data = data;
        this.e = e;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CRAWLERDATA;
    }
}
