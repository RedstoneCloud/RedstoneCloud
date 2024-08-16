package de.redstonecloud.scheduler;

import com.google.common.base.Preconditions;
import de.redstonecloud.scheduler.task.Task;
import de.redstonecloud.scheduler.task.TaskHandler;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import jline.internal.Preconditions;
import lombok.Getter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class TaskScheduler {
    @Getter private static TaskScheduler instance;
    private static final AtomicInteger taskId = new AtomicInteger(0);

    protected final ScheduledExecutorService executorService;

    protected final Int2ObjectOpenHashMap<TaskHandler<?>> taskHandlers = new Int2ObjectOpenHashMap<>();

    public TaskScheduler(ScheduledExecutorService executorService) {
        instance = this;

        this.executorService = executorService;
    }

    public <T extends Runnable> TaskHandler<T> scheduleTask(T task) {
        return this.scheduleTask(task, TimeUnit.MILLISECONDS, 0L, 0L);
    }

    public <T extends Runnable> TaskHandler<T> scheduleDelayedTask(T task, TimeUnit unit, long delay) {
        return this.scheduleTask(task, unit, delay, 0L);
    }

    public <T extends Runnable> TaskHandler<T> scheduleRepeatingTask(T task, TimeUnit unit, long period) {
        return this.scheduleTask(task, unit, 0L, period);
    }

    public <T extends Runnable> TaskHandler<T> scheduleDelayedTask(T task, long delay) {
        return this.scheduleTask(task, TimeUnit.MILLISECONDS, delay, 0L);
    }

    public <T extends Runnable> TaskHandler<T> scheduleRepeatingTask(T task, long period) {
        return this.scheduleTask(task, TimeUnit.MILLISECONDS, 0L, period);
    }

    public <T extends Runnable> TaskHandler<T> scheduleTask(T task, long delay, long period) {
        return this.scheduleTask(task, TimeUnit.MILLISECONDS, delay, period);
    }

    public <T extends Runnable> TaskHandler<T> scheduleTask(T task, TimeUnit unit, long delay, long period) {
        Preconditions.checkNotNull(unit, "TimeUnit cannot be null");
        Preconditions.checkArgument(delay >= 0 && period >= 0, "Delay and period must be non-negative");

        int id = taskId.getAndIncrement();

        TaskHandler<T> handler = new TaskHandler<>(id, task)
                .setUnit(unit)
                .setDelay(delay)
                .setPeriod(period);

        if (task instanceof Task t) {
            t.setHandler(handler);
        }

        return handler.createFuture();
    }

    public void stopScheduler() {
        this.executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }
}
