package de.redstonecloud.cloud.events;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.utils.ThreadFactoryBuilder;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;

import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Event Manager
 * Enables Plugins to subscribe to Events, either vanilla events already implemented
 * or custom ones which are loaded as part of a plugin.
 */
@Getter
public class EventManager {

    private final RedstoneCloud cloud;
    private final ExecutorService threadedExecutor;
    private final Object2ObjectOpenHashMap<Class<? extends Event>, EventHandler> handlerMap = new Object2ObjectOpenHashMap<>();

    public EventManager(RedstoneCloud cloud) {
        this.cloud = cloud;
        ThreadFactoryBuilder builder = ThreadFactoryBuilder.builder()
                .format("EventExecutor - #%d")
                .build();
        this.threadedExecutor = new ThreadPoolExecutor(2, Integer.MAX_VALUE, 60, TimeUnit.SECONDS, new SynchronousQueue<>(true), builder);
    }

    public <T extends Event> void subscribe(Class<T> event, Consumer<T> handler) {
        this.subscribe(event, handler, EventPriority.NORMAL);
    }

    public <T extends Event> void subscribe(Class<T> event, Consumer<T> handler, EventPriority priority) {
        EventHandler eventHandler = this.handlerMap.computeIfAbsent(event, e -> new EventHandler(event, this));
        eventHandler.subscribe((Consumer<Event>) handler, priority);
    }

    public <T extends Event> CompletableFuture<T> callEvent(T event) {
        EventHandler eventHandler = this.handlerMap.computeIfAbsent(event.getClass(), e -> new EventHandler(event.getClass(), this));
        return eventHandler.handle(event);
    }

}