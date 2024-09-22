package de.redstonecloud.cloud.scheduler.task;

import com.google.common.base.Preconditions;
import de.redstonecloud.cloud.scheduler.TaskScheduler;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@Getter
@RequiredArgsConstructor
public class TaskHandler<T extends Runnable> {
    protected final int taskId;
    protected final T task;

    protected TimeUnit unit = TimeUnit.MILLISECONDS;
    protected long delay = 0L;
    protected long period = 0L;
    protected ScheduledFuture<?> future = null;

    public TaskHandler<T> setUnit(TimeUnit unit) {
        this.unit = unit;
        return this.createFuture(true);
    }

    public TaskHandler<T> setDelay(long delay) {
        this.delay = delay;
        return this.createFuture(true);
    }

    public TaskHandler<T> setPeriod(long period) {
        this.period = period;
        return this.createFuture(true);
    }

    public TaskHandler<T> setFuture(ScheduledFuture<?> future) {
        if (this.future != null) {
            this.future.cancel(true);
        }
        this.future = future;
        return this;
    }

    public TaskHandler<T> createFuture() {
        return this.createFuture(false);
    }

    public TaskHandler<T> createFuture(boolean modified) {
        if (modified && this.future == null) {
            return this;
        }

        Preconditions.checkNotNull(unit, "TimeUnit cannot be null");
        Preconditions.checkArgument(delay >= 0 && period >= 0, "Delay and period must be non-negative");

        TaskScheduler scheduler = TaskScheduler.getInstance();
        ScheduledExecutorService executorService = scheduler.getExecutorService();

        scheduler.getTaskHandlers().put(this.taskId, this);

        return this.setFuture(this.period > 0 ? executorService.scheduleAtFixedRate(this::run, this.delay, this.period, this.unit)
                : executorService.schedule(this::run, this.delay, this.unit));
    }

    public void run() {
        try {
            this.task.run();
        } catch (Throwable throwable) {
            if (this.task instanceof Task t)
                t.onError(throwable);
        }
    }

    public void cancel() {
        if (this.future != null) {
            this.future.cancel(false);
        }

        if (this.task instanceof Task t) {
            t.onCancel();
        }

        TaskScheduler.getInstance().getTaskHandlers().remove(this.taskId);
    }
}
