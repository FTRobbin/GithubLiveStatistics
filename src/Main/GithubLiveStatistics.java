package Main;

import Cloner.ClonerData;
import Cloner.ClonerTask;
import Cloner.ClonerUrlPool;
import Cloner.ClonerWrapper;
import Crawler.CrawlerData;
import Crawler.CrawlerTask;
import Crawler.CrawlerWrapper;
import Database.DatabaseTask;
import Database.DatabaseWrapper;
import Database.SQLStatementsData;
import GLSUtility.LogService;
import Console.ConsoleBack;
import Console.SocketConsole;
import Console.SocketFront;
import Statistics.GLSBasicStatistics;
import Statistics.UpdateEvent;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.BatchUpdateException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.*;
import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/6/16.
 */

public final class GithubLiveStatistics {

    public static final String VERSION = "0.6.2";
    public static final String BUILD = "112";

    private static final long MAINCYCLEDELAY = 1000;
    private static long MAINCYCLECOUNT = 0;
    private boolean isShuttingDown = false;

    public final LogService logger;
    private static final String ENTITY = "Main";
    public final SystemConfig config;
    private final CrawlerTask.CrawlerTaskFactory crawlerFactory;
    private final DatabaseTask.DatabaseTaskFactory databaseTaskFactory;
    private final ClonerTask.ClonerTaskFactory clonerFactory;
    private final ClonerUrlPool clonerBuffer;

    private final Timer mainCycleTimer;
    private TimerTask mainCycle;
    private final Future<TaskWrapper> mainCyclePoison;

    private final ExecutorService crawlerExecutor, databaseExecutor, clonerExecutor;

    public final BlockingQueue<Future<TaskWrapper>> mainQueue;

    private final CompletionService crawlerCompletion, databaseCompletion, clonerCompletion;

    private final Thread consoleIO;

    private final UpdateEvent crawlerRaw, clonerRaw;

    public final GLSBasicStatistics basicStat;

    public GithubLiveStatistics() {
        //Initialization of the system
        logger = new LogService(new PrintWriter(System.out, true), Level.ALL);
        logger.start();
        logger.log(Level.INFO, ENTITY, "Github Live Statistics System " + VERSION + ".Build" + BUILD +  " launched");

        config = new SystemConfig(logger);
        if (!config.isLoaded()) {
            emergencyShutdown();
            throw new RuntimeException("Failed to load system configuration");
        }

        if (config.clearClonerRootOnStart) {
            File rootDir = new File(config.clonerConfig.clonerRoot);
            if (!rootDir.exists() && !rootDir.mkdirs()) {
                logger.log(Level.SEVERE, ENTITY, "Failed to create root directory, system abort");
                emergencyShutdown();
                throw new RuntimeException("Failed to create root directory");
            }
            try {
                FileUtils.deleteDirectory(rootDir);
            } catch (IOException ie) {
                logger.logErr(Level.SEVERE, ENTITY, "Failed to clear root directory, system abort", ie);
                throw new RuntimeException("Failed to clear root directory");
            }
        }

        mainQueue = new LinkedBlockingDeque<>();

        //Database
        databaseTaskFactory = DatabaseTask.getDatabaseTaskFactory(config);

        databaseExecutor = Executors.newCachedThreadPool();

        databaseCompletion = new ExecutorCompletionService<>(databaseExecutor, mainQueue);

        if (config.initDatabaseOnStart) {
            SQLStatementsData init = SQLStatementsData.newDatabaseInitialization();
            DatabaseTask initDatabase = databaseTaskFactory.getDatabaseTask(init);
            logger.log(Level.INFO, ENTITY, "Initializing database");
            boolean error = false;
            try {
                initDatabase.call();
            } catch (Exception e) {
                error = true;
            }
            if (error || !init.isComplete()) {
                emergencyShutdown();
                throw new RuntimeException("Failed to initialize database");
            }
            logger.log(Level.INFO, ENTITY, "Database successfully initialized");
        }

        //Crawler
        crawlerFactory = CrawlerTask.getCrawlerTaskFactory(config);

        crawlerExecutor = Executors.newCachedThreadPool();

        crawlerCompletion = new ExecutorCompletionService<>(crawlerExecutor, mainQueue);

        //Cloner
        clonerFactory = ClonerTask.getClonerTaskFactory(config);

        clonerExecutor = Executors.newCachedThreadPool();

        clonerCompletion = new ExecutorCompletionService(clonerExecutor, mainQueue);

        clonerBuffer = new ClonerUrlPool(config);

        //mainCycle
        mainCycleTimer = new Timer("mainCycleTimer", true);

        mainCycle = new TimerTask() {
            @Override
            public void run() {
                mainQueue.add(new DummyWrapperFuture(TaskWrapper.getDummyWrapper(TaskWrapper.TaskType.MAINCYCLE)));
            }
        };

        mainCyclePoison = new DummyWrapperFuture(TaskWrapper.getDummyWrapper(TaskWrapper.TaskType.MAINCYCLEPOISON));

        mainCycleTimer.scheduleAtFixedRate(mainCycle, MAINCYCLEDELAY, config.mainCyclePeriod);

        crawlerRaw = UpdateEvent.newUpdateEvent("CrawlerRaw");
        clonerRaw = UpdateEvent.newUpdateEvent("ClonerRaw");

        basicStat = new GLSBasicStatistics();

        consoleIO = new Thread(new SocketConsole(new SocketFront(config.portNum, logger), new ConsoleBack(this)));

        consoleIO.start();
    }

    public void startMessageLoop() {
        {
            final Thread main = Thread.currentThread();
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    consoleIO.interrupt();
                    envokeShutdown();
                    try {
                        main.join(60000);
                    } catch (InterruptedException ignored) {
                    }
                }
            });
        }
        while (!isShuttingDown) {
            try {
                Future<TaskWrapper> future = mainQueue.poll(MAINCYCLEDELAY * 2, TimeUnit.MILLISECONDS);
                if (future != null) {
                    TaskWrapper task = future.get();
                    switch (task.getTaskType()) {
                        case MAINCYCLEPOISON: {
                            isShuttingDown = true;
                            break;
                        }
                        case MAINCYCLE: {
                            ++MAINCYCLECOUNT;
                            logger.log(Level.FINER, ENTITY, "Main cycle heartbeat = " + MAINCYCLECOUNT);
                            while (!config.crawlerConfig.isAllUsersCrawled() && config.crawlerConfig.curTaskCnt < config.crawlerConfig.crawlerCnt) {
                                crawlerCompletion.submit(crawlerFactory.getCrawlerTask());
                                ++config.crawlerConfig.curTaskCnt;
                                logger.log(Level.FINEST, ENTITY, "New crawler task submitted, current task cnt : " + config.crawlerConfig.curTaskCnt);
                            }
                            while (clonerBuffer.getPoolRemaining() > 0 && config.clonerConfig.curTaskCnt < config.clonerConfig.clonerCnt) {
                                ClonerData data = new ClonerData((clonerBuffer.getUrls()));
                                logger.log(Level.FINEST, ENTITY, "New cloner data created dataId =" + data.dataId);
                                clonerCompletion.submit(clonerFactory.getClonerTask(data));
                                ++config.clonerConfig.curTaskCnt;
                                logger.log(Level.FINEST, ENTITY, "New cloner task submitted, current task cnt : " + config.clonerConfig.curTaskCnt);
                                logger.log(Level.FINEST, ENTITY, "Cloner buffer pool size remaining : " + clonerBuffer.getPoolRemaining());
                            }
                            if (!clonerBuffer.underway && clonerBuffer.getPoolRemaining() < config.clonerConfig.clonerPoolSize) {
                                databaseCompletion.submit(databaseTaskFactory.getDatabaseTask(SQLStatementsData.newClonerQueryData(config.clonerConfig.clonerQueryBlockSize)));
                                clonerBuffer.underway = true;
                                ++config.databaseConfig.curTaskCnt;
                                logger.log(Level.FINEST, ENTITY, "New cloner pool query submited, current database task cnt : " + config.databaseConfig.curTaskCnt);
                            }
                            break;
                        }
                        case CRAWLERDATA: {
                            CrawlerWrapper ctask = (CrawlerWrapper)task;
                            CrawlerData data = ctask.data;
                            --config.crawlerConfig.curTaskCnt;
                            logger.log(Level.FINEST, ENTITY, "New crawler task returned, current task cnt : " + config.crawlerConfig.curTaskCnt);
                            if (data.isNoMoreUsers()) {
                                //no more users
                                logger.log(Level.INFO, ENTITY, "There are no more users on Github with startId  : " + data.startId);
                                config.crawlerConfig.allUsersCrawled();
                            } else if (data.isComplete()) {
                                logger.log(Level.FINER, ENTITY, "New crawler data #" + data.dataId + " completed with " + data.crawledUsers.size() + " users and " + data.crawledRepos.size() + " repos");
                                if (data.crawledRepos.size() > 0) {
                                    logger.log(Level.FINER, ENTITY, "Send completed crawler data #" + data.dataId + " to database");
                                    DatabaseTask ndtask = databaseTaskFactory.getDatabaseTask(SQLStatementsData.newCrawlerUpdateData(data));
                                    databaseCompletion.submit(ndtask);
                                    ++config.databaseConfig.curTaskCnt;
                                    logger.log(Level.FINEST, ENTITY, "New database task submitted, current task cnt : " + config.databaseConfig.curTaskCnt);
                                }
                            } else {
                                logger.logErr(Level.SEVERE, ENTITY, "Crawler data #" + data.dataId + " failed with unexpected error", ctask.e);
                                ++data.retryCnt;
                                if (data.retryCnt > config.crawlerConfig.crawlerTaskRetry) {
                                    logger.log(Level.SEVERE, ENTITY, "Crawler data #" + data.dataId + " too many retries, aborted");
                                } else {
                                    data.reinitialize();
                                    crawlerCompletion.submit(crawlerFactory.getCrawlerTask(data));
                                    ++config.crawlerConfig.curTaskCnt;
                                    logger.log(Level.FINEST, ENTITY, "New crawler task submitted, current task cnt : " + config.crawlerConfig.curTaskCnt);
                                }
                            }
                            break;
                        }
                        case SQLCRAWLERUPDATE: {
                            DatabaseWrapper dtask = (DatabaseWrapper)task;
                            SQLStatementsData data = dtask.data;
                            --config.databaseConfig.curTaskCnt;
                            logger.log(Level.FINEST, ENTITY, "New database task returned, current task cnt : " + config.databaseConfig.curTaskCnt);
                            if (data.isComplete()) {
                                CrawlerData cdata = ((CrawlerWrapper)data.origin).data;
                                logger.log(Level.FINER, ENTITY, "Successfully wrote SQLStatementdata #" + data.dataId + " to database which originates from crawler data #" + cdata.dataId);
                                crawlerRaw.newEvent();
                                crawlerRaw.notifyObservers(cdata);
                                //TODO Events : new crawled users and repos
                            } else {
                                logger.logErr(Level.SEVERE, ENTITY, "SQLStatement data #" + data.dataId + " failed with unexpected error", dtask.e);
                                ++data.retryCnt;
                                if (data.retryCnt > config.databaseConfig.SQLStatementRetry) {
                                    logger.log(Level.SEVERE, ENTITY, "SQLStatement data #" + data.dataId + " too many retries, aborted");
                                } else {
                                    DatabaseTask ndtask = databaseTaskFactory.getDatabaseTask(data);
                                    databaseCompletion.submit(ndtask);
                                    ++config.databaseConfig.curTaskCnt;
                                    logger.log(Level.FINEST, ENTITY, "New database task submitted, current task cnt : " + config.databaseConfig.curTaskCnt);
                                }
                            }
                            break;
                        }
                        case SQLCLONERQUERY: {
                            DatabaseWrapper dtask = (DatabaseWrapper)task;
                            SQLStatementsData data = dtask.data;
                            --config.databaseConfig.curTaskCnt;
                            logger.log(Level.FINEST, ENTITY, "New database task returned, current task cnt : " + config.databaseConfig.curTaskCnt);
                            if (data.isComplete()) {
                                clonerBuffer.underway = false;
                                if (data.response.size() == 0) {
                                    //no more repos to crawl
                                    logger.log(Level.INFO, ENTITY, "There are no more repos within current database to clone");
                                } else {
                                    clonerBuffer.addUrlsToPool(data.response);
                                    logger.log(Level.FINER, ENTITY, "Successfully added repos to cloner pool, pool size remaining : " + clonerBuffer.getPoolRemaining());
                                }
                            } else {
                                logger.logErr(Level.SEVERE, ENTITY, "SQLStatement data #" + data.dataId + " failed with unexpected error", dtask.e);
                                ++data.retryCnt;
                                if (data.retryCnt > config.databaseConfig.SQLStatementRetry) {
                                    logger.log(Level.SEVERE, ENTITY, "SQLStatement data #" + data.dataId + " too many retries, aborted");
                                } else {
                                    DatabaseTask ndtask = databaseTaskFactory.getDatabaseTask(data);
                                    databaseCompletion.submit(ndtask);
                                    ++config.databaseConfig.curTaskCnt;
                                    logger.log(Level.FINEST, ENTITY, "New database task submitted, current task cnt : " + config.databaseConfig.curTaskCnt);
                                }
                            }
                            break;
                        }
                        case SQLCLONERUPDATE: {
                            DatabaseWrapper dtask = (DatabaseWrapper)task;
                            SQLStatementsData data = dtask.data;
                            --config.databaseConfig.curTaskCnt;
                            logger.log(Level.FINEST, ENTITY, "New database task returned, current task cnt : " + config.databaseConfig.curTaskCnt);
                            if (data.isComplete()) {
                                ClonerData cdata = ((ClonerWrapper)data.origin).data;
                                logger.log(Level.FINER, ENTITY, "Successfully wrote SQLStatementdata #" + data.dataId + " to database which originates from cloner data #" + cdata.dataId);
                                clonerRaw.newEvent();
                                clonerRaw.notifyObservers(cdata);
                                //TODO Events : new cloned repos
                            } else {
                                logger.logErr(Level.SEVERE, ENTITY, "SQLStatement data #" + data.dataId + " failed with unexpected error", dtask.e);
                                ++data.retryCnt;
                                if (data.retryCnt > config.databaseConfig.SQLStatementRetry) {
                                    logger.log(Level.SEVERE, ENTITY, "SQLStatement data #" + data.dataId + " too many retries, aborted");
                                } else {
                                    DatabaseTask ndtask = databaseTaskFactory.getDatabaseTask(data);
                                    databaseCompletion.submit(ndtask);
                                    ++config.databaseConfig.curTaskCnt;
                                    logger.log(Level.FINEST, ENTITY, "New database task submitted, current task cnt : " + config.databaseConfig.curTaskCnt);
                                }
                            }
                            break;
                        }
                        case CLONERDATA: {
                            ClonerWrapper ctask = (ClonerWrapper)task;
                            ClonerData data = ctask.data;
                            --config.clonerConfig.curTaskCnt;
                            logger.log(Level.FINEST, ENTITY, "New cloner task returned, current task cnt : " + config.clonerConfig.curTaskCnt);
                            if (data.isComplete()) {
                                logger.log(Level.FINER, ENTITY, "New cloner data #" + data.dataId + " completed with " + data.succ + "/" + data.tot  + " successful repos");
                                logger.log(Level.FINER, ENTITY, "Send completed cloner data #" + data.dataId + " to database");
                                DatabaseTask ndtask = databaseTaskFactory.getDatabaseTask(SQLStatementsData.newClonerUpdateData(data));
                                databaseCompletion.submit(ndtask);
                                ++config.databaseConfig.curTaskCnt;
                                logger.log(Level.FINEST, ENTITY, "New database task submitted, current task cnt : " + config.databaseConfig.curTaskCnt);
                            } else {
                                logger.logErr(Level.SEVERE, ENTITY, "Cloner data #" + data.dataId + " failed with unexpected error", ctask.e);
                                data.retryCnt++;
                                if (data.retryCnt > config.clonerConfig.clonerTaskRetry) {
                                    logger.log(Level.SEVERE, ENTITY, "Cloner data #" + data.dataId + " too many retries, aborted");
                                } else {
                                    crawlerCompletion.submit(clonerFactory.getClonerTask(data));
                                    ++config.clonerConfig.curTaskCnt;
                                    logger.log(Level.FINEST, ENTITY, "New cloner task submitted, current task cnt : " + config.clonerConfig.curTaskCnt);
                                }
                            }
                            break;
                        }
                        default: {
                            break;
                        }
                    }
                }
            } catch (InterruptedException ie) {
                logger.logErr(Level.WARNING, ENTITY, "Main message loop interrupted", ie);
                //TODO
            } catch (ExecutionException ee) {
                logger.logErr(Level.SEVERE, ENTITY, "Main message loop unexpected execution exception", ee);
                //TODO
            } catch (Exception e) {
                logger.logErr(Level.SEVERE, ENTITY, "Main message loop unexpected exception", e);
                //TODO?
            }
        }
        shutdown();
    }

    public void envokeShutdown() {
        mainCycleTimer.cancel();
        mainQueue.add(mainCyclePoison);
    }

    public void emergencyShutdown() {
        logger.log(Level.INFO, ENTITY, "System shutdown");
        logger.stop();
    }

    public void shutdown() {
        crawlerExecutor.shutdown();
        crawlerFactory.close();
        databaseExecutor.shutdown();
        databaseTaskFactory.close();
        clonerExecutor.shutdown();
        clonerFactory.close();
        config.close();
        logger.log(Level.INFO, ENTITY, "System shutdown");
        logger.stop();
    }

    public static void main(String args[]) {
        (new GithubLiveStatistics()).startMessageLoop();
    }
}
