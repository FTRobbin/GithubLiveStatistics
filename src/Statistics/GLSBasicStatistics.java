package Statistics;

import Cloner.ClonerData;
import Crawler.CrawlerData;
import Data.GithubRepo;

import java.util.Date;
import java.util.List;
import java.util.Observable;
import java.util.Observer;

/**
 * Created by RobbinNi on 7/9/16.
 */
public class GLSBasicStatistics implements Observer {

    private Date startTime, curretTime;
    private int totUserCrawled, totRepoCrawled, totRepoClonedTried, totRepoClonedSucced;

    public GLSBasicStatistics() {
        UpdateEvent crawler = UpdateEvent.findEventName("CrawlerRaw"),
                    cloner = UpdateEvent.findEventName("ClonerRaw");
        if (crawler != null) {
            crawler.addObserver(this);
        }
        if (cloner != null) {
            cloner.addObserver(this);
        }
        startTime = new Date();
        totRepoCrawled = 0;
        totUserCrawled = 0;
        totRepoClonedSucced = 0;
        totRepoClonedTried = 0;
    }

    @Override
    public void update(Observable o, Object arg) {
        UpdateEvent e = (UpdateEvent)o;
        if (e.name.equals("CrawlerRaw")) {
            CrawlerData data = (CrawlerData)arg;
            totUserCrawled += data.crawledUsers.size();
            for (List<GithubRepo> list : data.crawledRepos) {
                totRepoCrawled += list.size();
            }
        } else if (e.name.equals("ClonerRaw")) {
            ClonerData data = (ClonerData) arg;
            totRepoClonedTried += data.tot;
            totRepoClonedSucced += data.succ;
        }
    }

    public String getReport() {
        String report = "\nGLSBasicStatistics Report\n";
        curretTime = new Date();
        double secElapsed = (curretTime.getTime() - startTime.getTime()) / 1000;
        report += "Users Crawled : tot " + totUserCrawled + " avg " + (int)(secElapsed / totUserCrawled * 1000000) / 1000000.0 + "\n";
        report += "Repos Crawled : tot " + totRepoCrawled + " avg " + (int)(secElapsed / totRepoCrawled * 1000000) / 1000000.0 + "\n";
        report += "Repos Cloned Tried : tot " + totRepoClonedTried + " avg " + (int)(secElapsed / totRepoClonedTried * 1000000) / 1000000.0 + "\n";
        report += "Repos Cloned Succed : tot " + totRepoClonedSucced + " avg " + (int)(secElapsed / totRepoClonedSucced * 1000000) / 1000000.0 + "\n";
        return report;
    }
}
