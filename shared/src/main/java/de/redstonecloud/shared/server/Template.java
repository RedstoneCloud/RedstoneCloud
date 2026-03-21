package de.redstonecloud.shared.server;

import de.redstonecloud.api.components.ServerStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@SuperBuilder
@Getter
public abstract class Template {
    private String name;
    private ServerType type;
    private int maxPlayers;
    private int minServers;
    private int maxServers;
    private boolean staticServer;
    private String raw;

    @Builder.Default
    @Setter
    public int runningServers = 0;

    @Setter
    @Builder.Default
    public boolean stopOnEmpty = false;

    @Setter
    @Builder.Default
    public int shutdownTimeMs = 5000;

    private static final long IDLE_TIMEOUT_MS = 5 * 60 * 1000; // 5 minutes

    @Builder.Default
    private String seperator = "-";

    @Builder.Default
    private long maxBootTimeMs = 60 * 1000; // 1 minute

    @Builder.Default
    private double preStartThreshold = 0.8;

    @Builder.Default
    private List<String> nodes = List.of();

    @Builder.Default
    private AtomicBoolean createInProgress = new AtomicBoolean(false);

    private long createStartedAtMs = 0;

    public void checkServers() {
        Server[] servers = getServers();
        runningServers = servers.length;

        handleIdleServers(servers);

        if (shouldCreateNewServer(servers) && tryBeginCreate()) {
            try {
                createNewServer();
            } finally {
                finishCreate();
            }
        }
    }

    private boolean tryBeginCreate() {
        long now = System.currentTimeMillis();
        if (createInProgress.get()) {
            if (now - createStartedAtMs > maxBootTimeMs) {
                createInProgress.set(false);
            } else {
                return false;
            }
        }
        if (createInProgress.compareAndSet(false, true)) {
            createStartedAtMs = now;
            return true;
        }
        return false;
    }

    private void finishCreate() {
        createStartedAtMs = 0;
        createInProgress.set(false);
    }

    protected abstract Server[] getServers();

    private void handleIdleServers(Server[] servers) {
        if (!stopOnEmpty) return;

        if (runningServers <= minServers) return;

        for (Server server : servers) {
            if (isServerIdle(server)) {
                server.kill();
                runningServers--;
                if (runningServers <= minServers) {
                    return;
                }
            }
        }
    }

    private boolean isServerIdle(Server server) {
        if (server.getStatus() != ServerStatus.RUNNING) return false;
        boolean hasNoPlayers = server.getPlayers().isEmpty();
        boolean exceedsIdleTime = (System.currentTimeMillis() - server.getLastPlayerUpdate()) > IDLE_TIMEOUT_MS;
        return hasNoPlayers && exceedsIdleTime;
    }

    private boolean shouldCreateNewServer(Server[] servers) {
        return (needsMoreServers() || allServersBlocked(servers) || allServersAtOrAboveThreshold(servers))
                && canCreateMoreServers();
    }

    private boolean needsMoreServers() {
        return minServers > 0 && runningServers < minServers;
    }

    private boolean allServersBlocked(Server[] servers) {
        if (minServers <= 0 || servers.length == 0) return false;

        return countBlockedServers(servers) == servers.length;
    }

    private boolean allServersAtOrAboveThreshold(Server[] servers) {
        if (servers.length == 0 || maxPlayers <= 0) return false;

        int threshold = calculatePreStartPlayers();
        boolean hasEligibleServer = false;
        for (Server server : servers) {
            if (isServerBlocked(server)) {
                continue;
            }

            hasEligibleServer = true;
            if (server.getPlayers().size() < threshold) {
                return false;
            }
        }
        return hasEligibleServer;
    }

    private int calculatePreStartPlayers() {
        double threshold = preStartThreshold;
        if (threshold <= 0.0 || threshold > 1.0) {
            threshold = 1.0;
        }

        int value = (int) Math.ceil(maxPlayers * threshold);
        return Math.max(1, value);
    }

    private int countBlockedServers(Server[] servers) {
        int blocked = 0;
        for (Server server : servers) {
            if (isServerBlocked(server)) {
                blocked++;
            }
        }
        return blocked;
    }

    private boolean isServerBlocked(Server server) {
        ServerStatus status = server.getStatus();
        return status != ServerStatus.RUNNING &&
                status != ServerStatus.STARTING &&
                status != ServerStatus.PREPARED;
    }

    private boolean canCreateMoreServers() {
        return runningServers < maxServers;
    }

    protected abstract void createNewServer();

    public Template merge(Template other) {
        this.name = other.name;
        this.type = other.type;
        this.maxPlayers = other.maxPlayers;
        this.minServers = other.minServers;
        this.maxServers = other.maxServers;
        this.staticServer = other.staticServer;
        this.raw = other.raw;
        this.seperator = other.seperator;
        this.nodes = other.nodes;
        this.stopOnEmpty = other.stopOnEmpty;
        this.shutdownTimeMs = other.shutdownTimeMs;
        this.maxBootTimeMs = other.maxBootTimeMs;
        this.preStartThreshold = other.preStartThreshold;
        return this;
    }
}
