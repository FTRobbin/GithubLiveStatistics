package Database;

import static GLSUtility.ConfigUtility.*;

import GLSUtility.ConfigUtility;
import GLSUtility.LogService;
import org.apache.tomcat.jdbc.pool.DataSource;
import org.apache.tomcat.jdbc.pool.PoolProperties;

import java.util.Properties;

/**
 * Created by RobbinNi on 7/7/16.
 */
public class DatabaseConfig {

    private final LogService logger;
    private final PoolProperties poolP;
    private static final String ENTITY = "Database.DatabaseConfig";
    public final DataSource database;
    public boolean clearTagOnStart;
    private final boolean loaded;
    public volatile int curTaskCnt;
    public int SQLStatementRetry;

    public DatabaseConfig(LogService logger, Properties config) {
        this.logger = logger;
        poolP = new PoolProperties();
        String url = loadString(ENTITY, "dburl", "jdbc:mysql://localhost:3306/", logger, config);
        String driver = loadString(ENTITY, "dbdriver", "com.mysql.jdbc.Driver", logger, config);
        String dbuser = loadString(ENTITY, "dbusername", "root", logger, config);
        String dbpass = loadString(ENTITY, "dbpassword", "000000", logger, config);
        poolP.setUrl(url);
        poolP.setDriverClassName(driver);
        poolP.setUsername(dbuser);
        poolP.setPassword(dbpass);
        poolP.setFairQueue(false);
        database = new DataSource();
        database.setPoolProperties(poolP);
        clearTagOnStart = ConfigUtility.loadBoolean(ENTITY, "cleartagonstart", true, logger, config);
        SQLStatementRetry = ConfigUtility.loadInteger(ENTITY, "sqlstatementretry", 1, logger, config);
        curTaskCnt = 0;
        loaded = true;
    }

    public boolean isLoaded() {
        return loaded;
    }
}
