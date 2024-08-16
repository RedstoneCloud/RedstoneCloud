package de.redstonecloud.cloud.player;

import com.google.common.net.HostAndPort;
import de.redstonecloud.api.components.ICloudPlayer;
import de.redstonecloud.api.components.ICloudServer;

public class CloudPlayer implements ICloudPlayer {

    @Override
    public HostAndPort getAddress() {
        return null;
    }

    @Override
    public ICloudServer getConnectedNetwork() {
        return null;
    }

    @Override
    public ICloudServer getConnectedServer() {
        return null;
    }

    @Override
    public void sendMessage(String message) {

    }

    @Override
    public void connect(String server) {

    }

    @Override
    public void disconnect(String reason) {

    }

    @Override
    public void getName() {

    }
}
