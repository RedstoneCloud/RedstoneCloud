package de.redstonecloud.cloud.player;

import com.google.common.net.HostAndPort;
import de.redstonecloud.api.components.ICloudPlayer;
import de.redstonecloud.api.components.ICloudServer;
import de.redstonecloud.api.redis.cache.Cacheable;
import de.redstonecloud.cloud.server.Server;
import lombok.Builder;
import lombok.Setter;
import org.json.JSONObject;

@Setter
@Builder
public class CloudPlayer implements ICloudPlayer, Cacheable {
    protected String name;
    protected HostAndPort address;
    private Server network;
    private Server server;
    protected String uuid;

    @Override
    public String toString() {
        JSONObject obj = new JSONObject()
                .put("name", name)
                .put("uuid", uuid)
                .put("address", address.toString())
                .put("network", network != null ? network.getName() : "null")
                .put("server", server != null ? server.getName() : "null");

        return obj.toString();
    }


    @Override
    public HostAndPort getAddress() {
        return address;
    }

    @Override
    public ICloudServer getConnectedNetwork() {
        return network;
    }

    @Override
    public ICloudServer getConnectedServer() {
        return server;
    }

    public void setConnectedServer(Server srv) {
        if(server != null) {
            server.players.remove(uuid);
            server.updateCache();
        }
        server = srv;
        updateCache();
        if(srv != null && !srv.players.contains(uuid)) {
            srv.players.add(uuid);
            srv.updateCache();
        }
    }

    public void setConnectedNetwork(Server srv) {
        if(network != null) {
            network.players.remove(uuid);
            network.updateCache();
        }
        network = srv;
        updateCache();
        if(srv != null && !srv.players.contains(uuid)) {
            srv.players.add(uuid);
            srv.updateCache();
        }
    }

    @Override
    public String getUUID() {
        return uuid;
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
    public String getName() {
        return name;
    }

    @Override
    public String cacheKey() {
        return "player:" + uuid;
    }
}
