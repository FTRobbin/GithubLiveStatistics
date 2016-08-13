package Cloner;

import Data.ClonerRepo;
import javafx.util.Pair;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by RobbinNi on 7/8/16.
 */
public class ClonerData {

    private static final int START = 0, COMPLETE = Integer.MAX_VALUE;
    public static final int UNCLONED = 0, CLONED = 1, ERROR = -1;
    private static int idIssued = 0;

    public final int dataId;
    public int state;
    public final List<ClonerRepo> repos;
    public final List<Integer> result;
    public int succ;
    public final int tot;
    public int retryCnt;

    private static int getDataId() {
        return idIssued++;
    }

    public ClonerData(List<ClonerRepo> repos) {
        this.dataId = getDataId();
        this.state = START;
        this.repos = repos;
        this.result = new LinkedList<>();
        this.succ = 0;
        this.tot = repos.size();
        this.retryCnt = 0;
    }

    public void setComplete() {
        state = COMPLETE;
    }

    public boolean isComplete() {
        return state == COMPLETE;
    }

    public void addResult(int res) {
        result.add(res);
        ++state;
        if (res == CLONED) {
            ++succ;
        }
    }

}
