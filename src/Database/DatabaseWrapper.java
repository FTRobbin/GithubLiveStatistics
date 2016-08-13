package Database;

import Main.TaskWrapper;

/**
 * Created by RobbinNi on 7/8/16.
 */
public class DatabaseWrapper extends TaskWrapper {

    public SQLStatementsData data;
    public Exception e;

    public DatabaseWrapper(SQLStatementsData data) {
        this.data = data;
        this.e = null;
    }

    public DatabaseWrapper(SQLStatementsData data, Exception e) {
        this.data = data;
        this.e = e;
    }

    @Override
    public TaskType getTaskType() {
        return data.type;
    }
}
