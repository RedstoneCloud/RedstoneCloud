package de.redstonecloud.api.components;

import com.google.common.net.HostAndPort;

public interface ICloudPlayer extends Nameable {

    HostAndPort getAddress();

    ICloudServer getConnectedNetwork();

    ICloudServer getConnectedServer();

    String getUUID();

    void sendMessage(String message);

    void connect(String server);

    default void disconnect() {
        this.disconnect(null);
    }

    void disconnect(String reason);
}
