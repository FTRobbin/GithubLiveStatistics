package Cloner;

import Data.ClonerRepo;
import Main.SystemConfig;
import javafx.util.Pair;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Created by RobbinNi on 7/8/16.
 */
public class ClonerUrlPool {

    private Queue<List<ClonerRepo>> repoPool;
    private int remains;
    private SystemConfig config;
    public boolean underway;

    public ClonerUrlPool(SystemConfig config) {
        this.repoPool = new LinkedList<>();
        this.remains = 0;
        this.config = config;
        this.underway = false;
    }

    public int getPoolRemaining() {
        return remains;
    }

    public void addUrlsToPool(List<String[]> queryResult) {
        List<ClonerRepo> cur = null;
        for (String[] s : queryResult) {
            ClonerRepo repo = new ClonerRepo(Integer.valueOf(s[0]), Integer.valueOf(s[1]), s[2]);
            if (cur == null) {
                cur = new LinkedList<>();
            }
            ++remains;
            cur.add(repo);
            if (cur.size() == config.clonerConfig.clonerBlockSize) {
                repoPool.add(cur);
                cur = null;
            }
        }
        if (cur != null) {
            repoPool.add(cur);
        }
    }

    public List<ClonerRepo> getUrls() {
        List<ClonerRepo> ret = repoPool.poll();
        remains -= ret.size();
        return ret;
    }
}
