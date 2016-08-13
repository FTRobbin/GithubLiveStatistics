package Main;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by RobbinNi on 7/7/16.
 */
public class DummyWrapperFuture implements Future<TaskWrapper> {

    private TaskWrapper content;

    public DummyWrapperFuture(TaskWrapper content) {
        this.content = content;
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return true;
    }

    @Override
    public TaskWrapper get() throws InterruptedException, ExecutionException {
        return content;
    }

    @Override
    public TaskWrapper get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return content;
    }
}
