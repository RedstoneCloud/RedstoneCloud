package de.redstonecloud.api.components;

import com.google.common.net.HostAndPort;

public interface ICloudServer extends Nameable {

    long getCreatedAt();

    HostAndPort getAddress();

    ServerStatus getStatus();

    void setStatus(ServerStatus status);

    void start();

    void stop();
}
