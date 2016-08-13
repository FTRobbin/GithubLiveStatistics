package Database;

import Cloner.ClonerData;
import Cloner.ClonerWrapper;
import Crawler.CrawlerData;
import Crawler.CrawlerWrapper;
import Data.ClonerRepo;
import Data.GithubRepo;
import Data.GithubUser;
import Main.TaskWrapper;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by RobbinNi on 7/7/16.
 */
public class SQLStatementsData {

    private static final int START = Integer.MIN_VALUE, COMPLETE = Integer.MAX_VALUE;
    private static AtomicInteger idIssued = new AtomicInteger(0);
    private static FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");

    public int state;
    public final int dataId;
    public final List<String> SQLStatements;
    public final TaskWrapper.TaskType type;
    public final List<String[]> response;
    public int[] updateCnt;
    public final boolean isUpdate;
    public final RowOperation rowOperation;
    public TaskWrapper origin;
    public int retryCnt;

    private SQLStatementsData(TaskWrapper.TaskType type, boolean isUpdate, RowOperation rowOperation, TaskWrapper origin) {
        this.state = START;
        this.dataId = idIssued.getAndIncrement();
        this.SQLStatements = new LinkedList<>();
        this.type = type;
        this.response = new LinkedList<>();
        this.isUpdate = isUpdate;
        this.rowOperation = rowOperation;
        this.origin = origin;
        this.retryCnt = 0;
    }

    public static SQLStatementsData newCrawlerUpdateData(CrawlerData cdata) {
        SQLStatementsData data = new SQLStatementsData(TaskWrapper.TaskType.SQLCRAWLERUPDATE, true, null, new CrawlerWrapper(cdata));
        if (cdata.crawledUsers.size() > 0){
            String update = "insert into GithubLiveStatistics.GithubUsers (id, login, crawled_at) values ";
            boolean first = true;
            for (GithubUser user : cdata.crawledUsers) {
                String block = "(" + user.id + ",\'" + user.login + "\',\'" + format.format(user.crawledAt) + "\')";
                if (!first) {
                    update += "," + block;
                } else {
                    update += block;
                    first = false;
                }
            }
            update += " on duplicate key update crawled_at = values(crawled_at);";
            data.SQLStatements.add(update);
        }
        if (cdata.crawledRepos.size() > 0){
            boolean empty = true;
            String update = "insert into GithubLiveStatistics.GithubRepos (id, ownerid, url, crawled_at, created_at, updated_at, issued, clonestate) values ";
            boolean first = true;
            for (List<GithubRepo> list : cdata.crawledRepos) {
                for (GithubRepo repo : list) {
                    String block = "(" + repo.id + "," + repo.ownerid + ",\'" + repo.url + "\',\'" + format.format(repo.crawledAt) + "\',\'" + format.format(repo.createdAt) + "\',\'" + format.format(repo.updatedAt) + "\'," + "false" + "," + "0" + ")";
                    if (!first) {
                        update += "," + block;
                    } else {
                        update += block;
                        first = false;
                    }
                    empty = false;
                }
            }
            update += " on duplicate key update crawled_at = values(crawled_at),  updated_at = values(updated_at);";
            if (!empty) {
                data.SQLStatements.add(update);
            }
        }
        return data;
    }

    public static SQLStatementsData newClonerQueryData(int size) {
        SQLStatementsData data = new SQLStatementsData(TaskWrapper.TaskType.SQLCLONERQUERY, false, MarkIssued.getOper(), null);
        String query = "select id, ownerid, url, issued from GithubLiveStatistics.GithubRepos where issued = false && clonestate = 0 limit " + size + ";";
        data.SQLStatements.add(query);
        return data;
    }

    public static SQLStatementsData newClonerUpdateData(ClonerData cdata) {
        SQLStatementsData data = new SQLStatementsData(TaskWrapper.TaskType.SQLCLONERUPDATE, true, null, new ClonerWrapper(cdata));
        if (cdata.repos.size() > 0){
            String update = "insert into GithubLiveStatistics.GithubRepos (id, cloned_at, issued, clonestate) values ";
            boolean first = true;
            ListIterator<Integer> it = cdata.result.listIterator();
            for (ClonerRepo repo : cdata.repos) {
                String clonedAt = repo.clonedAt == null ? "null" : "\'" + format.format(repo.clonedAt) + "\'";
                String block = "(" + repo.id + "," + clonedAt + ",false," + it.next() + ")";
                if (!first) {
                    update += "," + block;
                } else {
                    update += block;
                    first = false;
                }
            }
            update += " on duplicate key update cloned_at = values(cloned_at),  issued = values(issued), clonestate = values(clonestate);";
            data.SQLStatements.add(update);
        }
        return data;
    }

    public static SQLStatementsData newDatabaseInitialization() {
        SQLStatementsData data = new SQLStatementsData(TaskWrapper.TaskType.SQLINIT, true, null, null);
        data.SQLStatements.add("drop database if exists GithubLiveStatistics;");
        data.SQLStatements.add("create database if not exists GithubLiveStatistics;");
        data.SQLStatements.add("create table GithubLiveStatistics.GithubUsers (id int, login varchar(40), crawled_at datetime, primary key(id), unique (login));");
        data.SQLStatements.add("create table GithubLiveStatistics.GithubRepos (id int, ownerid int, url varchar(255), crawled_at datetime, created_at datetime, updated_at datetime, cloned_at datetime, issued boolean, clonestate int, primary key(id), foreign key (ownerid) references GithubLiveStatistics.GithubUsers(id));");
        data.SQLStatements.add("create index repoid on GithubLiveStatistics.GithubRepos(id) using hash;");
        return data;
    }

    public void reinitialize() {
        response.clear();
        updateCnt = null;
    }

    public void setComplete() {
        state = COMPLETE;
    }

    public boolean isComplete() {
        return state == COMPLETE;
    }

}
