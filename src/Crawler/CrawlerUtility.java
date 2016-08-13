package Crawler;

/**
 * Created by RobbinNi on 7/6/16.
 */
public final class CrawlerUtility {

    public static String getUserAPIURL(int startid) {
        return "https://api.github.com/users?since=" + startid;
    }

    public static String getRepoAPIURL(String username, int pageNum) {
        return "https://api.github.com/users/" + username + "/repos" + "?page=" + pageNum;
    }
}
