package Database;

import Main.SystemConfig;
import Main.TaskWrapper;
import org.apache.tomcat.jdbc.pool.DataSource;

import java.sql.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * Created by RobbinNi on 7/7/16.
 */

public class DatabaseTask implements Callable<TaskWrapper> {

    private static final String ENTITY = "Database.DatabaseTask";

    public final int taskId;
    private final SystemConfig config;
    private final SQLStatementsData data;
    private final DataSource database;

    public static class DatabaseTaskFactory {
        private static final String ENTITY = "Database.DatabaseTaskFactory";
        private int taskId;
        private final SystemConfig config;
        private final DataSource database;

        DatabaseTaskFactory(SystemConfig config) {
            this.taskId = 0;
            this.config = config;
            this.database = config.databaseConfig.database;
        }

        private int getTaskId() {
            return taskId++;
        }

        public DatabaseTask getDatabaseTask(SQLStatementsData data) {
            DatabaseTask ret = new DatabaseTask(getTaskId(), config, data, database);
            config.logger.log(Level.FINER, ENTITY, "New database task issued taskId = " + ret.taskId + " on SQLStatementData dataId = " + ret.data.dataId);
            return ret;
        }

        public void close() {
            database.close(true);
        }
    }

    private DatabaseTask(int taskId, SystemConfig config, SQLStatementsData data, DataSource database) {
        this.taskId = taskId;
        this.config = config;
        this.data = data;
        this.database = database;
    }

    public static DatabaseTaskFactory getDatabaseTaskFactory(SystemConfig config) {
        return new DatabaseTaskFactory(config);
    }

    public String getEntity() {
        return ENTITY + "#" + taskId;
    }

    @Override
    public TaskWrapper call() throws Exception {
        try {
            Connection con = null;
            try {
                con = database.getConnection();
                config.logger.log(Level.FINEST, getEntity(), "Connected to database");
                Statement st;
                if (data.isUpdate) {
                    st = con.createStatement();
                    for (String upd : data.SQLStatements) {
                        st.addBatch(upd);
                    }
                    data.updateCnt = st.executeBatch();
                } else {
                    if (data.rowOperation != null) {
                        st = con.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE);
                    } else {
                        st = con.createStatement();
                    }
                    ResultSet ret = st.executeQuery(data.SQLStatements.get(0));

                    ResultSetMetaData meta = ret.getMetaData();
                    int col = meta.getColumnCount();
                    while (ret.next()) {
                        if (data.rowOperation != null) {
                            data.rowOperation.operate(ret);
                            ret.updateRow();
                        }
                        String[] row = new String[col];
                        for (int i = 1; i <= col; ++i) {
                            row[i - 1] = ret.getString(i);
                        }
                        data.response.add(row);
                    }
                }
                data.setComplete();
            } catch (SQLException se) {
                String statement = "";
                for (String s : data.SQLStatements) {
                    statement += s + "\n";
                }
                config.logger.logErr(Level.SEVERE, getEntity(), "Received SQLException when executing data#" + data.dataId + " with SQLStatements : \n" + statement, se);
                throw se;
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se) {
                    /* ignored */
                    }
                }
            }
        } catch (Exception e) {
            return new DatabaseWrapper(data, e);
        }
        return new DatabaseWrapper(data);
    }

}
