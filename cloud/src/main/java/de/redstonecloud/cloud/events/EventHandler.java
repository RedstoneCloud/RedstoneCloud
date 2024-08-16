package de.redstonecloud.cloud.events;

import de.redstonecloud.cloud.logger.Logger;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class EventHandler {

    private final EventManager eventManager;
    private final Class<? extends Event> eventClass;

    private final Map<EventPriority, ArrayList<Consumer<Event>>> priority2handlers = new EnumMap<>(EventPriority.class);

    public EventHandler(Class<? extends Event> eventClass, EventManager eventManager) {
        this.eventClass = eventClass;
        this.eventManager = eventManager;
    }

    public <T extends Event> CompletableFuture<T> handle(T event) {
        CompletableFuture<T> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            for (EventPriority priority : EventPriority.values()) {
                this.handlePriority(priority, event);
            }
        });
        return future;
    }

    private void handlePriority(EventPriority priority, Event event) {
        ArrayList<Consumer<Event>> handlerList = this.priority2handlers.get(priority);
        if (handlerList != null) {
            for (Consumer<Event> eventHandler : handlerList) {
                eventHandler.accept(event);
            }
        }
    }

    public void subscribe(Consumer<Event> handler, EventPriority priority) {
        List<Consumer<Event>> handlerList = this.priority2handlers.computeIfAbsent(priority, priority1 -> new ArrayList<>());
        // Check if event is already registered
        if (!handlerList.contains(handler)) {
            // Handler is not registered yet
            handlerList.add(handler);
        }
    }
}