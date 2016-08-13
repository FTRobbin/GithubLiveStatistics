package Crawler;

import Data.GithubRepo;
import Data.GithubUser;
import Main.SystemConfig;
import Main.TaskWrapper;
import com.google.gson.*;
import org.apache.http.Header;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/6/16.
 */

public class CrawlerTask implements Callable<TaskWrapper> {

    private static final String ENTITY = "Crawler.CrawlerTask";

    public final int taskId;
    private final SystemConfig config;
    private final CrawlerData data;
    private final CloseableHttpClient client;

    public static class CrawlerTaskFactory {
        private static final String ENTITY = "Crawler.CrawlerTaskFactory";
        private int taskId;
        private final SystemConfig config;
        private final CrawlerData.CrawlerDataFactory dataFactory;
        private final CloseableHttpClient client;

        CrawlerTaskFactory(SystemConfig config) {
            this.taskId = 0;
            this.config = config;
            this.dataFactory = CrawlerData.getCrawlerDataFactory(config);
            PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
            cm.setMaxTotal(config.crawlerConfig.connCnt);
            cm.setDefaultMaxPerRoute(config.crawlerConfig.connCnt);
            client = HttpClients.custom().setConnectionManager(cm).build();
            config.logger.log(Level.FINER, ENTITY, "HtttpClient created with maximum connection total & maximum connection pre route = " + cm.getDefaultMaxPerRoute());
        }

        private int getTaskId() {
            return taskId++;
        }

        public CrawlerTask getCrawlerTask() {
            CrawlerTask ret = new CrawlerTask(getTaskId(), config, dataFactory.getNextCrawlerData(), client);
            config.logger.log(Level.FINER, ENTITY, "New crawler task issued taskId = " + ret.taskId + " on new CrawlerData dataId = " + ret.data.dataId);
            return ret;
        }

        public CrawlerTask getCrawlerTask(CrawlerData data) {
            data.reinitialize();
            CrawlerTask ret = new CrawlerTask(getTaskId(), config, data, client);
            config.logger.log(Level.FINER, ENTITY, "New crawler task issued taskId = " + ret.taskId + " relaunched on CrawlerData dataId = " + ret.data.dataId);
            return ret;
        }

        public void close() {
            try {
                client.close();
                config.logger.log(Level.INFO, ENTITY, "HttpClient closed");
            } catch (IOException ie) {
                config.logger.logErr(Level.WARNING, ENTITY, "Failed to close HttpClient, exit anyway", ie);
            }
        }
    }

    public static CrawlerTaskFactory getCrawlerTaskFactory(SystemConfig config) {
        return new CrawlerTaskFactory(config);
    }

    private CrawlerTask(int taskId, SystemConfig config, CrawlerData data, CloseableHttpClient client) {
        this.taskId = taskId;
        this.config = config;
        this.data = data;
        this.client = client;
    }

    private String getEntity() {
        return ENTITY + "#" + this.taskId;
    }

    private JsonElement callAPI(String url) throws IOException {
        HttpGet httpget = new HttpGet(data.key.authAPI(url));
        {
            int retry = 0;
            while (true) {
                boolean wait = false;
                try {
                    CloseableHttpResponse response = client.execute(httpget);
                    try {
                        Reader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                        JsonStreamParser parser = new JsonStreamParser(reader);
                        JsonElement ret = parser.next();
                        if (config.logLevel.intValue() <= Level.FINEST.intValue()) {
                            String limit = response.getFirstHeader("X-RateLimit-Limit").getValue(),
                                    remains = response.getFirstHeader("X-RateLimit-Remaining").getValue();
                            config.logger.log(Level.FINEST, getEntity(), "X-RateLimit-Limit : " + limit + " X-RateLimit-Remaining : " + remains);
                        }
                        response.close();
                        if (ret.isJsonObject()) {
                            String docUrl = "";
                            try {
                                docUrl = ret.getAsJsonObject().get("documentation_url").getAsString();
                            } catch (Exception e) {
                                /* ignored */
                            }
                            if (docUrl.equals("https://developer.github.com/v3/#rate-limiting")) {
                                throw new HttpResponseException(403, "Rate limit dummy");
                            }
                        }
                        return ret;
                    } catch (JsonParseException je) {
                        config.logger.logErr(Level.SEVERE, getEntity(), "Failed to parse API response", je);
                        wait = true;
                    } catch (HttpResponseException re) {
                        if (re.getStatusCode() == 403) {
                            config.logger.log(Level.WARNING, getEntity(), "Rate limit exceeded for key " + data.key.toString());
                            data.key = config.crawlerConfig.keyPool.issueNewKey(data.key);
                            httpget = new HttpGet(data.key.authAPI(url));
                        } else {
                            if (config.logLevel.intValue() <= Level.FINER.intValue()) {
                                String info = response.getProtocolVersion().toString() + " " + response.getStatusLine().getStatusCode() + " " + response.getStatusLine().getReasonPhrase() + "\n";
                                Header[] array = response.getAllHeaders();
                                for (Header header : array) {
                                    info += header.getName() + " : " + header.getValue() + "\n";
                                }
                                config.logger.log(Level.SEVERE, getEntity(), "Unexpected http response : \n" + info);
                            }
                            throw re;
                        }
                    }
                } catch (ClientProtocolException pe) {
                    config.logger.logErr(Level.SEVERE, getEntity(), "Client protocol error", pe);
                    wait = true;
                } catch (IOException ie) {
                    config.logger.logErr(Level.SEVERE, getEntity(), "Unexpected IO error", ie);
                    wait = true;
                }
                ++retry;
                if (retry >= config.crawlerConfig.retryCnt) {
                    throw new IOException("Failed to retrve given URL");
                } else {
                    if (wait) {
                        try {
                            Thread.sleep(config.crawlerConfig.reconnect);
                        } catch (InterruptedException ie) {
                            /* ignored */
                        }
                    }
                }
            }
        }
    }

    List<GithubUser> getUsers() throws IOException {
        String url = CrawlerUtility.getUserAPIURL(data.startId);
        config.logger.log(Level.FINEST, getEntity(), "Ready to crawl userAPIURL \'" + url + "\'");
        JsonElement obj = callAPI(url);
        config.logger.log(Level.FINEST, getEntity(), "Successfully crawled Json object from userAPIURL \'" + url + "\'");
        JsonArray users;
        try {
            users = obj.getAsJsonArray();
        } catch (IllegalStateException ise) {
            config.logger.log(Level.SEVERE, getEntity(), "Failed to parse API url : \'" + data.key.authAPI(url) +  "\' response received as array : \n" + obj.toString());
            throw ise;
        }
        int n = users.size();
        if (n == 0) {
            data.setNoMoreUsers();
            return null;
        }
        List<GithubUser> ret = new ArrayList<>();
        for (int i = 0; i < n; ++i) {
            JsonObject userJ = users.get(i).getAsJsonObject();
            if (data.inUserBlock(userJ.get("id").getAsInt())) {
                GithubUser user = new GithubUser(userJ);
                ret.add(user);
            }
        }
        return ret;
    }

    List<GithubRepo> getRepo(GithubUser user) throws IOException {
        List<GithubRepo> ret = new ArrayList<>();
        int pageNum = 1;
        while (true) {
            String url = CrawlerUtility.getRepoAPIURL(user.login, pageNum);
            config.logger.log(Level.FINEST, getEntity(), "Ready to crawl repoAPIURL \'" + url + "\'");
            JsonElement obj = callAPI(url);
            config.logger.log(Level.FINEST, getEntity(), "Successfully crawled Json object from repoAPIURL \'" + url + "\'");
            JsonArray repos;
            try {
                repos = obj.getAsJsonArray();
            } catch (IllegalStateException ise) {
                config.logger.log(Level.SEVERE, getEntity(), "Failed to parse API url : \'" + data.key.authAPI(url) +  "\' response received as array : \n" + obj.toString());
                //throw ise;
                return new LinkedList<>();
            }
            int n = repos.size();
            for (int i = 0; i < n; ++i) {
                JsonObject repoJ = repos.get(i).getAsJsonObject();
                try {
                    GithubRepo repo = new GithubRepo(repoJ);
                    ret.add(repo);
                } catch (NumberFormatException ne) {
                    config.logger.logErr(Level.SEVERE, getEntity(), "Error due to date in certain repo created_at : " + repoJ.getAsJsonPrimitive("created_at").getAsString() + " updated_at : " + repoJ.getAsJsonPrimitive("updated_at").getAsString(), ne);
                } catch (ParseException pe) {
                    config.logger.logErr(Level.SEVERE, getEntity(), "Failed to parse the date in certain repo", pe);
                }
            }
            if (n < CrawlerConfig.REPOPAGESIZE) {
                break;
            }
            ++pageNum;
        }
        return ret;
    }

    @Override
    public TaskWrapper call() throws Exception {
        try {
            if (data.state == CrawlerData.START) {
                List<GithubUser> users = getUsers();
                if (data.isNoMoreUsers()) {
                    return new CrawlerWrapper(data);
                }
                data.addUser(users);
                data.completeUsers();
            }
            config.logger.log(Level.FINEST, getEntity(), "Succefully crawled " + data.crawledUsers.size() + " users for data #" + data.dataId);
            while (data.state < data.crawledUsers.size()) {
                GithubUser user = data.crawledUsers.get(data.state);
                List<GithubRepo> repos = getRepo(user);
                data.addRepo(repos);
                config.logger.log(Level.FINEST, getEntity(), "Succefully crawled " + repos.size() + " repos for user #" + user.id);
            }
            data.completeRepo();
            return new CrawlerWrapper(data);
        } catch (Exception e) {
            return new CrawlerWrapper(data, e);
        }
    }
}
