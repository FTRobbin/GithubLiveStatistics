package Main;

/**
 * Created by RobbinNi on 7/7/16.
 */
public abstract class TaskWrapper {

    public enum TaskType {
        MAINCYCLEPOISON,
        MAINCYCLE,
        CRAWLERDATA,
        SQLCRAWLERUPDATE,
        SQLCLONERUPDATE,
        SQLCLONERQUERY,
        CLONERDATA,
        SQLINIT,
    };

    public abstract TaskType getTaskType();

    public static TaskWrapper getDummyWrapper(final TaskType type) {
        return new TaskWrapper() {
            @Override
            public TaskType getTaskType() {
                return type;
            }
        };
    }
}
