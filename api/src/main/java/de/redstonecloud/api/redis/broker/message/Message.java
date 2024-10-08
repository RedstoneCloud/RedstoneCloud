package de.redstonecloud.api.redis.broker.message;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import de.redstonecloud.api.redis.broker.Broker;
import de.redstonecloud.api.util.EmptyArrays;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

@Getter
@Setter
@Accessors(chain = true)
@AllArgsConstructor
public class Message {
    public static final Gson GSON = new Gson();

    protected int id;
    protected String from;
    protected String to;
    protected String[] arguments;

    public Message respond(Object... message) {
        return new Builder()
                .setId(this.id)
                .setFrom(this.to)
                .setTo(this.from)
                .append(message)
                .build();
    }

    public void send() {
        this.send(null);
    }

    public void send(Consumer<Message> callback) {
        Broker broker = Broker.get();

        if (callback != null) {
            broker.addPendingResponse(this.id, callback);
        }

        broker.publish(this);
    }

    public String toJson() {
        JsonArray object = new JsonArray();
        object.add(this.id);
        object.add(this.from);
        object.add(this.to);

        JsonArray array = new JsonArray(this.arguments.length);
        for (String argument : this.arguments) {
            array.add(argument);
        }
        object.add(array);

        return object.toString();
    }

    public static Message fromJson(String json) {
        JsonArray object = GSON.fromJson(json, JsonArray.class);

        int messageId = object.get(0).getAsInt();
        String from = object.get(1).getAsString();
        String to = object.get(2).getAsString();
        JsonArray arguments = object.get(3).getAsJsonArray();

        String[] argumentsArray = new String[arguments.size()];
        for (int i = 0; i < arguments.size(); i++) {
            argumentsArray[i] = arguments.get(i).getAsString();
        }

        return new Message(messageId, from, to, argumentsArray);
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    public static class Builder {
        protected int id = ThreadLocalRandom.current().nextInt(0, 1000);
        protected String from = Broker.get().getMainRoute();
        protected String to;
        protected ObjectArrayList<String> arguments = new ObjectArrayList<>();

        public Builder append(Object... arguments) {
            this.arguments.addAll(Arrays.stream(arguments).map(String::valueOf).toList());
            return this;
        }

        public Message build() {
            return new Message(this.id, this.from.toLowerCase(), this.to.toLowerCase(), this.arguments.toArray(EmptyArrays.STRING));
        }

        public void send() {
            this.build().send();
        }

        public void send(Consumer<Message> callback) {
            this.build().send(callback);
        }
    }
}
