package de.redstonecloud.api.redis.broker;

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.message.Message;
import de.redstonecloud.api.redis.broker.packet.Packet;
import de.redstonecloud.api.redis.broker.packet.PacketRegistry;
import de.redstonecloud.api.util.Keys;
import lombok.Getter;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisPubSub;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

@Getter
public class Broker {
    public static final Gson GSON = new Gson();

    protected static Broker instance;

    public static Broker get() {
        return instance;
    }

    protected PacketRegistry packetRegistry;

    protected String mainRoute;
    protected Jedis subscriber;
    protected JedisPool pool;

    protected Map<String, CopyOnWriteArrayList<Consumer<Packet>>> packetConsumers;
    protected Map<Integer, ResponseContainer<?>> pendingPacketResponses;

    protected Map<String, CopyOnWriteArrayList<Consumer<Message>>> messageConsumers;
    protected Map<Integer, Consumer<Message>> pendingMessageResponses;

    private final ExecutorService publishExecutor = Executors.newFixedThreadPool(8);
    private final ScheduledExecutorService batchExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "Redis-Packet-Batcher");
        thread.setDaemon(true);
        return thread;
    });
    private final ExecutorService inboundExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "Redis-Subscriber-Dispatcher");
        thread.setDaemon(true);
        return thread;
    });
    private final BlockingQueue<InboundPayload> inboundQueue = new LinkedBlockingQueue<>();
    private final AtomicLong lastQueueWarnMillis = new AtomicLong(0L);
    private static final int QUEUE_WARN_THRESHOLD = 10_000;
    private static final long BATCH_INTERVAL_MS = 100L;
    private final ConcurrentHashMap<String, ConcurrentLinkedQueue<JsonArray>> packetBatchQueues = new ConcurrentHashMap<>();
    private BrokerJedisPubSub pubsub;
    private volatile boolean running = false;

    public Broker(String mainRoute, PacketRegistry packetRegistry, String... routes) {
        Preconditions.checkArgument(instance == null, "Broker already initialized");
        Preconditions.checkArgument(routes.length > 0, "Routes should not be empty");
        instance = this;

        this.mainRoute = mainRoute;

        this.packetRegistry = packetRegistry;

        this.packetConsumers = new ConcurrentHashMap<>();
        this.pendingPacketResponses = new ConcurrentHashMap<>();

        this.messageConsumers = new ConcurrentHashMap<>();
        this.pendingMessageResponses = new ConcurrentHashMap<>();

        initJedis(routes);
    }

    private void initJedis(String... routes) {
        String address = System.getenv(Keys.ENV_REDIS_IP) != null ? System.getenv(Keys.ENV_REDIS_IP) : System.getProperty(Keys.PROPERTY_REDIS_IP);
        int port = Integer.parseInt(System.getenv(Keys.ENV_REDIS_PORT) != null ? System.getenv(Keys.ENV_REDIS_PORT) : System.getProperty(Keys.PROPERTY_REDIS_PORT));
        int db = Integer.parseInt(System.getenv(Keys.ENV_REDIS_DB) != null ? System.getenv(Keys.ENV_REDIS_DB) : System.getProperty(Keys.PROPERTY_REDIS_DB));

        JedisPoolConfig config = new JedisPoolConfig();
        config.setMinIdle(4);
        config.setMaxIdle(8);
        config.setMaxTotal(16);
        config.setBlockWhenExhausted(true);
        config.setTestOnBorrow(true);
        config.setMaxWait(Duration.ofSeconds(1));
        config.setTestOnReturn(true);

        this.pool = new JedisPool(config, address, port, 0, null, db);

        running = true;
        startInboundDispatcher();
        startBatcher();
        new Thread(() -> {
            while (running) {
                try (Jedis jedis = new Jedis(address, port, 0)) {
                    jedis.select(db);

                    this.subscriber = jedis;
                    this.pubsub = new BrokerJedisPubSub();
                    jedis.subscribe(pubsub, routes);
                } catch (Exception e) {
                    if (!running) {
                        break;
                    }

                    System.err.println("[BROKER] Redis subscriber connection lost, retrying in 1s");
                    e.printStackTrace();

                    try {
                        Thread.sleep(1000L); // backoff before reconnect
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }, "Redis-Subscriber").start();
    }

    public void publish(Packet packet) {
        enqueuePacket(packet);
    }

    public void publish(Message message) {
        publishInternal(message.getTo().toLowerCase(), message.toJson());
    }

    public void publishImmediately(Packet packet) {
        publishInternal(packet.getTo().toLowerCase(), packet.finalDocument().toString());
    }

    private void enqueuePacket(Packet packet) {
        String channel = packet.getTo().toLowerCase();
        packetBatchQueues
                .computeIfAbsent(channel, k -> new ConcurrentLinkedQueue<>())
                .add(packet.finalDocument());
    }

    private void startBatcher() {
        batchExecutor.scheduleAtFixedRate(this::flushPacketBatches, BATCH_INTERVAL_MS, BATCH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    private void flushPacketBatches() {
        for (Map.Entry<String, ConcurrentLinkedQueue<JsonArray>> entry : packetBatchQueues.entrySet()) {
            ConcurrentLinkedQueue<JsonArray> queue = entry.getValue();
            if (queue.isEmpty()) {
                continue;
            }

            JsonArray batch = new JsonArray();
            batch.add("batch");

            JsonArray packets = new JsonArray();
            JsonArray doc;
            while ((doc = queue.poll()) != null) {
                packets.add(doc);
            }

            if (packets.size() == 0) {
                continue;
            }

            batch.add(packets);
            publishInternal(entry.getKey(), batch.toString());
        }
    }

    private void publishInternal(String channel, String payload) {
        publishExecutor.submit(() -> {
            int attempt = 0;
            while (true) {
                try (Jedis publisher = this.pool.getResource()) {
                    publisher.publish(channel, payload);
                    return;
                } catch (Exception e) {
                    attempt++;
                    if (attempt >= 3) {
                        System.err.println("[BROKER] Failed to publish message after " + attempt + " attempts");
                        e.printStackTrace();
                        return;
                    }

                    try {
                        Thread.sleep(50L * attempt);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        });
    }

    public void listen(String channel, Consumer<Packet> callback) {
        this.packetConsumers.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public void listenM(String channel, Consumer<Message> callback) {
        this.messageConsumers.computeIfAbsent(channel, k -> new CopyOnWriteArrayList<>()).add(callback);
    }

    public void shutdown() {
        running = false;
        BrokerJedisPubSub currentPubSub = this.pubsub;
        if (currentPubSub != null) {
            currentPubSub.unsubscribe();
        }
        if (this.subscriber != null) {
            this.subscriber.close();
        }
        this.pool.close();
        this.publishExecutor.shutdown();
        this.batchExecutor.shutdown();
        this.inboundExecutor.shutdown();
    }

    public void addPendingResponse(int id, ResponseContainer<?> callback) {
        Preconditions.checkArgument(!this.pendingPacketResponses.containsKey(id), "A message with the same id is already waiting for a response");
        this.pendingPacketResponses.put(id, callback);
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() ->
                Optional.ofNullable(this.pendingPacketResponses.remove(id))
                        .ifPresent(responseContainer -> responseContainer.consumer().accept(null)));
    }

    public void addPendingResponse(int id, Consumer<Message> callback) {
        Preconditions.checkArgument(!this.pendingMessageResponses.containsKey(id), "A message with the same id is already waiting for a response");
        this.pendingMessageResponses.put(id, callback);
        CompletableFuture.delayedExecutor(5, TimeUnit.SECONDS).execute(() ->
                Optional.ofNullable(this.pendingMessageResponses.remove(id))
                        .ifPresent(consumer -> consumer.accept(null)));
    }

    private void startInboundDispatcher() {
        inboundExecutor.submit(() -> {
            while (running || !inboundQueue.isEmpty()) {
                try {
                    InboundPayload payload = inboundQueue.poll(250, TimeUnit.MILLISECONDS);
                    if (payload == null) {
                        continue;
                    }
                    handleInbound(payload.channel(), payload.messageString());
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    System.err.println("[BROKER] Failed to dispatch inbound message");
                    e.printStackTrace();
                }
            }
        });
    }

    private void handleInbound(String channel, String messageString) {
        JsonArray array = GSON.fromJson(messageString, JsonArray.class);

        String type = array.get(0).getAsString();

        switch (type) {
            case "packet" -> handlePacketInbound(channel, array);
            case "batch" -> {
                if (array.size() < 2 || !array.get(1).isJsonArray()) {
                    System.out.println("[BROKER] Received invalid batch: " + messageString);
                    return;
                }

                JsonArray packets = array.get(1).getAsJsonArray();
                for (int i = 0; i < packets.size(); i++) {
                    if (!packets.get(i).isJsonArray()) {
                        continue;
                    }
                    handlePacketInbound(channel, packets.get(i).getAsJsonArray());
                }
            }
            case "message" -> {
                Message message = Message.fromJson(array);

                Optional.ofNullable(pendingMessageResponses.remove(message.getId()))
                        .ifPresent(consumer -> {
                            try {
                                consumer.accept(message);
                            } catch (Exception e) {
                                System.err.println("[BROKER] Message response handler failed (route=" + channel + ", id=" + message.getId() + ")");
                                e.printStackTrace();
                            }
                        });

                CopyOnWriteArrayList<Consumer<Message>> messageListeners = messageConsumers.get(channel);
                if (messageListeners != null) {
                    messageListeners.forEach(consumer -> {
                        try {
                            consumer.accept(message);
                        } catch (Exception e) {
                            System.err.println("[BROKER] Message handler failed (route=" + channel + ", id=" + message.getId() + ")");
                            e.printStackTrace();
                        }
                    });
                }

                CopyOnWriteArrayList<Consumer<Message>> wildcardMessageListeners = messageConsumers.get("");
                if (wildcardMessageListeners != null) {
                    wildcardMessageListeners.forEach(consumer -> {
                        try {
                            consumer.accept(message);
                        } catch (Exception e) {
                            System.err.println("[BROKER] Message wildcard handler failed (route=" + channel + ", id=" + message.getId() + ")");
                            e.printStackTrace();
                        }
                    });
                }
            }
            default -> System.out.println("[BROKER] Received unknown message type " + type);
        }
    }

    @SuppressWarnings("unchecked")
    private void handlePacketInbound(String channel, JsonArray array) {
        Packet packet = packetRegistry.create(array);

        if (packet == null) {
            System.out.println("[BROKER] Received invalid packet: " + array);
            return;
        }

        Optional.ofNullable(pendingPacketResponses.remove(packet.getSessionId()))
                .ifPresent(responseContainer -> {
                    Consumer<? extends Packet> consumer = responseContainer.consumer();
                    Class<? extends Packet> packetClass = responseContainer.packetClass();

                    if (packetClass.isInstance(packet)) {
                        try {
                            ((Consumer<Packet>) consumer).accept(packetClass.cast(packet));
                        } catch (Exception e) {
                            System.err.println("[BROKER] Packet response handler failed (route=" + channel + ", session=" + packet.getSessionId() + ")");
                            e.printStackTrace();
                        }
                    }
                });

        CopyOnWriteArrayList<Consumer<Packet>> packetListeners = packetConsumers.get(channel);
        if (packetListeners != null) {
            packetListeners.forEach(consumer -> {
                try {
                    consumer.accept(packet);
                } catch (Exception e) {
                    System.err.println("[BROKER] Packet handler failed (route=" + channel + ", session=" + packet.getSessionId() + ")");
                    e.printStackTrace();
                }
            });
        }

        CopyOnWriteArrayList<Consumer<Packet>> wildcardPacketListeners = packetConsumers.get("");
        if (wildcardPacketListeners != null) {
            wildcardPacketListeners.forEach(consumer -> {
                try {
                    consumer.accept(packet);
                } catch (Exception e) {
                    System.err.println("[BROKER] Packet wildcard handler failed (route=" + channel + ", session=" + packet.getSessionId() + ")");
                    e.printStackTrace();
                }
            });
        }
    }

    private record InboundPayload(String channel, String messageString) {}

    @SuppressWarnings("unchecked")
    private class BrokerJedisPubSub extends JedisPubSub {
        @Override
        public void onMessage(String channel, String messageString) {
            inboundQueue.offer(new InboundPayload(channel, messageString));

            if (inboundQueue.size() > QUEUE_WARN_THRESHOLD) {
                long now = System.currentTimeMillis();
                long lastWarn = lastQueueWarnMillis.get();
                if (now - lastWarn > 10_000L && lastQueueWarnMillis.compareAndSet(lastWarn, now)) {
                    System.err.println("[BROKER] Inbound queue depth is high: " + inboundQueue.size());
                }
            }
        }
    }
}
