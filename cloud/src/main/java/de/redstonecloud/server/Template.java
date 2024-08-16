package de.redstonecloud.server;
import components.ServerStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
public class Template {
    private String name;
    private ServerType type;
    private int maxPlayers;
    private int minServers;
    private int maxServers;
    private boolean staticServer;
    @Builder.Default
    @Setter
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
            if (server.getStatus() != ServerStatus.RUNNING && server.getStatus() != ServerStatus.STARTING && server.getStatus() != ServerStatus.PREPARED) {
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