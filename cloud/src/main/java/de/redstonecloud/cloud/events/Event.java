package de.redstonecloud.cloud.events;

import com.google.common.base.Preconditions;

public abstract class Event {
    private boolean cancelled = false;

    public Event() {
    }

    public boolean isCancelled() {
        Preconditions.checkArgument(this instanceof CancellableEvent, "Event is not Cancellable");
        return this.cancelled;
    }

    public void setCancelled(boolean cancelled) {
        Preconditions.checkArgument(this instanceof CancellableEvent, "Event is not Cancellable");
        this.cancelled = cancelled;
    }

    public void setCancelled() {
        Preconditions.checkArgument(this instanceof CancellableEvent, "Event is not Cancellable");
        this.cancelled = true;
    }
}