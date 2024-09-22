package de.redstonecloud.cloud.events;

public interface CancellableEvent {

    boolean isCancelled();

    void setCancelled(boolean cancelled);

    void setCancelled();
}