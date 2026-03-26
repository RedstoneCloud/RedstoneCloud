package de.redstonecloud.api.redis.broker.packet;

import com.google.gson.JsonArray;
import de.redstonecloud.api.redis.broker.Broker;
import de.redstonecloud.api.redis.broker.ResponseContainer;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Getter
@Setter
@Accessors(chain = true)
public abstract class Packet {
    public abstract int packetId();
    public abstract void serialize(JsonArray data);
    public abstract void deserialize(JsonArray data);

    protected int sessionId = ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE);

    protected String from = Broker.get().getMainRoute();
    protected String to = "cloud";

    public JsonArray finalDocument() {
        JsonArray object = new JsonArray();
        object.add("packet");
        object.add(this.packetId());
        object.add(this.sessionId);
        object.add(this.from.toLowerCase());
        object.add(this.to.toLowerCase());

        JsonArray serialized = new JsonArray();
        this.serialize(serialized);
        object.add(serialized);

        return object;
    }

    public void send() {
        this.send(null, null);
    }

    public <T extends Packet> void send(Class<T> packetType, Consumer<T> callback) {
        Broker broker = Broker.get();

        if (callback != null)
            broker.addPendingResponse(this.sessionId, new ResponseContainer<>(packetType, callback));

        broker.publish(this);
    }

    public void sendImmediately() {
        this.sendImmediately(null, null);
    }

    public <T extends Packet> void sendImmediately(Class<T> packetType, Consumer<T> callback) {
        Broker broker = Broker.get();

        if (callback != null)
            broker.addPendingResponse(this.sessionId, new ResponseContainer<>(packetType, callback));

        broker.publishImmediately(this);
    }
}
