package de.redstonecloud.server;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
public class Template {
    public String name;
    public ServerType type;
    public int maxPlayers;
    public int minServers;
    public int maxServers;
    public boolean staticServer;
    @Setter
    @Builder.Default
    public int runningServers = 0;
    @Setter
    @Builder.Default
    public int runningSinceStart = 0;

    public void checkServers() {
        Server[] servers = ServerManager.getInstance().getServersByTemplate(this);

        boolean create = false;

        runningServers = servers.length;

        if (runningServers < minServers) {
            create = true;
        }

        //if every servers status is not running or starting or preparing, then create a new server
        int blocked = 0;
        for (Server server : servers) {
            if (server.getStatus() != Server.Status.RUNNING && server.getStatus() != Server.Status.STARTING && server.getStatus() != Server.Status.PREPARED) {
                blocked++;
            }
        }

        if (blocked == servers.length) {
            create = true;
        }

        if (runningServers >= maxServers) {
            create = false;
            return;
        }

        if (create) {
            ServerManager.getInstance().startServer(this);
        }
    }
}