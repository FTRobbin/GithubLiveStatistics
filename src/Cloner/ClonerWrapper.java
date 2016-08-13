package Cloner;

import Main.TaskWrapper;

/**
 * Created by RobbinNi on 7/8/16.
 */
public class ClonerWrapper extends TaskWrapper {
    public ClonerData data;
    public Exception e;

    public ClonerWrapper(ClonerData data) {
        this.data = data;
        this.e = null;
    }

    public ClonerWrapper(ClonerData data, Exception e) {
        this.data = data;
        this.e = e;
    }

    @Override
    public TaskType getTaskType() {
        return TaskType.CLONERDATA;
    }
}
