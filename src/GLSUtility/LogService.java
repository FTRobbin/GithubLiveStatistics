package GLSUtility;

import org.apache.commons.lang3.time.FastDateFormat;

import java.io.PrintWriter;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/6/16.
 */
public class LogService {
    private final FastDateFormat format = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss");
    private final BlockingQueue<String> queue;
    private final LoggerThread loggerThread;
    private final PrintWriter writer;
    private Level filter;
    private volatile boolean isShutdown;
    private int reserves;

    public LogService(PrintWriter writer, Level filter) {
        this.queue = new LinkedBlockingDeque<String>();
        this.loggerThread = new LoggerThread();
        this.writer = writer;
        this.filter = filter;
        isShutdown = false;
        reserves = 0;
    }

    public void setLogLevel(Level lev) {
        this.filter = lev;
    }

    public void start() {
        loggerThread.start();
    }

    public void stop() {
        synchronized (this) {
            isShutdown = true;
        }
        loggerThread.interrupt();
    }

    public void log(Level lev, String msg) {
        if (lev.intValue() >= filter.intValue()) {
            String entity = Thread.currentThread().getName();
            log(lev, entity, msg);
        }
    }

    public void log(Level lev, String entity, String msg) {
        if (lev.intValue() >= filter.intValue()) {
            synchronized (this) {
                if (isShutdown) {
                    throw new IllegalStateException("The log service has been shutdown.");
                }
                ++reserves;
            }
            String fullmsg = (format.format(new Date())) + " [" + entity + "] " + lev.getName() + " : " + msg;
            try {
                queue.put(fullmsg);
            } catch (InterruptedException e) {
            /* keep the interruption */
                Thread.currentThread().interrupt();
            }
        }
    }

    public void logErr(Level lev, String entity, String msg, Exception ex) {
        if (lev.intValue() >= filter.intValue()) {
            synchronized (this) {
                if (isShutdown) {
                    throw new IllegalStateException("The log service has been shutdown.");
                }
                ++reserves;
            }
            String fullmsg = (format.format(new Date())) + " [" + entity + "] " + lev.getName() + " : " + msg + "\n"
                            + "Exception Class : " + ex.getClass().toString()
                            + "With error message : " + ex.getMessage() + "\n";
            if (filter.intValue() <= Level.FINER.intValue()) {
                fullmsg += "Stacktrace : \n";
                StackTraceElement[] stack = ex.getStackTrace();
                for (StackTraceElement e : stack) {
                    fullmsg += e.toString() + "\n";
                }
            }
            try {
                queue.put(fullmsg);
            } catch (InterruptedException e) {
            /* keep the interruption */
                Thread.currentThread().interrupt();
            }
        }
    }

    private class LoggerThread extends Thread {
        public void run() {
            try {
                while (true) {
                    try {
                        synchronized (LogService.this) {
                            if (isShutdown && reserves == 0) {
                                break;
                            }
                        }
                        String msg = queue.take();
                        synchronized (LogService.this) {
                            --reserves;
                        }
                        writer.println(msg);
                    } catch (InterruptedException e) { /* retry */ }
                }
            } finally {
                writer.close();
            }
        }
    }
}
