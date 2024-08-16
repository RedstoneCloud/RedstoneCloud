package de.redstonecloud.broker;

import redis.broker.message.Message;

public class BrokerHandler {

    public static void handle(Message message) {
        switch (message.getArguments()[0]) {
            case "getplayer" -> {}
            case "updateplayer" -> {}
            case "deleteplayer" -> {}
            case "getserver" -> {}
            case "updateserver" -> {}
            case "deleteserver" -> {}
        }
    }
}
