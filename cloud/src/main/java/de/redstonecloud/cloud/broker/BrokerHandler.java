package de.redstonecloud.cloud.broker;

import de.redstonecloud.api.components.Request;
import de.redstonecloud.api.redis.broker.message.Message;

public class BrokerHandler {

    public static void handle(Message message) {
        String[] args = message.getArguments();

        Request.retrieveRequest(args[0]).ifPresent(request -> request.handle(message));
    }
}
