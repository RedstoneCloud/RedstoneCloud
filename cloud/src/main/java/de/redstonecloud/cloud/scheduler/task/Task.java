package de.redstonecloud.cloud.scheduler.task;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(chain = true)
public abstract class Task implements Runnable {
    @Setter(AccessLevel.NONE)
    protected TaskHandler<?> handler;

    protected abstract void onRun(long currentMillis);

    public void onCancel() {

    }

    public void onError(Throwable throwable) {

    }

    @Override
    public void run() {
        this.onRun(System.currentTimeMillis());
    }

    public void cancel() {
        if (this.handler != null) {
            this.handler.cancel();
        }
    }

    public void setHandler(TaskHandler<?> handler) {
        if (this.handler != null) {
            throw new RuntimeException("Cannot change task handler");
        }
        this.handler = handler;
    }
}
