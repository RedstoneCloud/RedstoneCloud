package de.redstonecloud.api.redis.broker;

import com.google.common.base.Preconditions;
import de.redstonecloud.api.redis.broker.message.Message;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Getter
public class Broker {
    protected static Broker instance;

    public static Broker get() {
        return instance;
    }

    protected String mainRoute;
    protected Jedis publisher;
    protected Jedis subscriber;

    protected Object2ObjectOpenHashMap<String, ObjectArrayList<Consumer<Message>>> consumers;
    protected Int2ObjectOpenHashMap<Consumer<Message>> pendingResponses;

    public Broker(String mainRoute, String... routes) {
        Preconditions.checkArgument(instance == null, "Broker already initialized");
        Preconditions.checkArgument(routes.length > 0, "Routes should not be empty");
        instance = this;

        this.mainRoute = mainRoute;

        this.consumers = new Object2ObjectOpenHashMap<>();
        this.pendingResponses = new Int2ObjectOpenHashMap<>();

        initJedis(routes);
    }

    private void initJedis(String... routes) {
        int port = Integer.parseInt(System.getenv("REDIS_PORT") != null ? System.getenv("REDIS_PORT") : System.getProperty("redis.port"));
        this.publisher = new Jedis("127.0.0.1", port);
        this.subscriber = new Jedis("127.0.0.1", port);
        new Thread(() -> {
            try {
                this.subscriber.subscribe(new BrokerJedisPubSub(), routes);
            } catch (Exception e) {}
        }).start();
    }

    public void publish(Message message) {
        this.publisher.publish(message.getTo(), message.toJson());
    }

    public void listen(String channel, Consumer<Message> callback) {
        this.consumers.computeIfAbsent(channel, k -> new ObjectArrayList<>()).add(callback);
    }

    public void shutdown() {
        this.publisher.close();
        this.subscriber.close();
    }

    public void addPendingResponse(int id, Consumer<Message> callback) {
        Preconditions.checkArgument(!this.pendingResponses.containsKey(id), "A message with the same id is already waiting for a response");
        this.pendingResponses.put(id, callback);
        CompletableFuture.delayedExecutor(2, TimeUnit.SECONDS).execute(() ->
                Optional.ofNullable(this.pendingResponses.remove(id))
                        .ifPresent(consumer -> consumer.accept(null)));
    }


    private class BrokerJedisPubSub extends JedisPubSub {
        @Override
        public void onMessage(String channel, String messageString) {
            Message message = Message.fromJson(messageString);

            Optional.ofNullable(pendingResponses.remove(message.getId()))
                    .ifPresent(consumer -> consumer.accept(message));

            consumers.getOrDefault(channel, new ObjectArrayList<>())
                    .forEach(consumer -> consumer.accept(message));
        }
    }
}
