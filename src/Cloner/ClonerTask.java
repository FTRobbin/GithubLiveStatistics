package Cloner;

import Data.ClonerRepo;
import Main.SystemConfig;
import Main.TaskWrapper;
import org.apache.commons.io.FileUtils;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.TransportException;

import java.io.File;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/8/16.
 */
public class ClonerTask implements Callable<TaskWrapper> {

    private static final String ENTITY = "Cloner.ClonerTask";

    public final int taskId;
    private final SystemConfig config;
    private final ClonerData data;

    public static class ClonerTaskFactory {
        private static final String ENTITY = "Cloner.ClonerTaskFactory";
        private int taskId;
        private final SystemConfig config;

        private ClonerTaskFactory(SystemConfig config) {
            this.taskId = 0;
            this.config = config;
        }

        private int getTaskId() {
            return taskId++;
        }

        public ClonerTask getClonerTask(ClonerData data) {
            ClonerTask ret = new ClonerTask(getTaskId(), config, data);
            config.logger.log(Level.FINER, ENTITY, "New cloner task issued taskId = " + ret.taskId + " on ClonerData dataId = " + ret.data.dataId);
            return ret;
        }

        public void close() {
            //TODO
        }
    }

    private ClonerTask(int taskId, SystemConfig config, ClonerData data) {
        this.taskId = taskId;
        this.config = config;
        this.data = data;
    }

    public static ClonerTaskFactory getClonerTaskFactory(SystemConfig config) {
        return new ClonerTaskFactory(config);
    }

    private String getEntity() {
        return ENTITY + "#" + taskId;
    }

    private int cloneRepo(ClonerRepo repo) {
        String url = repo.url;
        File localPath = new File(config.clonerConfig.clonerRoot + ClonerMultiLevelDirectory.getDirectory(repo.ownerid) + url.replace("git://github.com/", "").replace(".git", ""));
        config.logger.log(Level.FINEST, getEntity(), "Start cloning repo \"" + repo.url + "\" to directory \" + " + localPath.getPath() + "\"");
        boolean faultyRepo;
        if (!localPath.exists() && !localPath.mkdirs()) {
            config.logger.log(Level.SEVERE, getEntity(), "Failed to create directory for repo \"" + repo.url + "\" with local path \"" + localPath.getPath() + "\"");
            faultyRepo = false;
        } else {
            try {
                FileUtils.cleanDirectory(localPath);
                Git result = Git.cloneRepository().setURI(url).setDirectory(localPath).call();
                result.close();
                repo.clonedAt = new Date();
                return ClonerData.CLONED;
            } catch (InvalidPathException ipe) {
                config.logger.logErr(Level.WARNING, getEntity(), "Failed to clone remote repository due to non-ascii characters", ipe);
                faultyRepo = true;
            } catch (TransportException te) {
                config.logger.logErr(Level.WARNING, getEntity(), "Failed to clone remote repository due to corrupted repository", te);
                faultyRepo = true;
            } catch (JGitInternalException je) {
                config.logger.logErr(Level.WARNING, getEntity(), "Failed to clone remote repository with JGitInternalException", je);
                faultyRepo = true;
            } catch (GitAPIException ge) {
                config.logger.logErr(Level.WARNING, getEntity(), "Failed to clone remote repository with GitAPIException", ge);
                faultyRepo = true;
            } catch (IllegalStateException ise) {
                config.logger.logErr(Level.WARNING, getEntity(), "Failed to clone remote repository with IllegalStateException", ise);
                faultyRepo = true;
            } catch (IOException ie) {
                config.logger.logErr(Level.WARNING, getEntity(), "Failed to clone remote repository with IOException", ie);
                faultyRepo = true;
            }
        }
        if (faultyRepo) {
            try {
                org.apache.commons.io.FileUtils.deleteDirectory(localPath);
            } catch (IOException ie) {
                config.logger.logErr(Level.SEVERE, getEntity(), "Failed to clean up faulty repository", ie);
            }
            return ClonerData.ERROR;
        } else {
            return ClonerData.UNCLONED;
        }
    }

    @Override
    public TaskWrapper call() throws Exception {
        try {
            while (data.state < data.tot) {
                int ret = cloneRepo(data.repos.get(data.state));
                data.addResult(ret);
            }
            data.setComplete();
            return new ClonerWrapper(data);
        } catch (Exception e) {
            return new ClonerWrapper(data, e);
        }
    }
}
