package de.redstonecloud.shared.server;

import com.google.common.net.HostAndPort;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import de.redstonecloud.api.components.ICloudServer;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.api.components.cache.ServerData;
import de.redstonecloud.api.redis.cache.Cacheable;
import de.redstonecloud.api.util.Keys;
import de.redstonecloud.shared.startmethods.IStartMethod;
import de.redstonecloud.shared.startmethods.StartMethods;
import de.redstonecloud.shared.utils.CurrentInstance;
import de.redstonecloud.shared.utils.Directories;
import lombok.*;
import lombok.experimental.SuperBuilder;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents a cloud server instance with lifecycle management capabilities.
 * Handles server preparation, starting, stopping, and cleanup operations.
 */
@SuperBuilder
@Getter
@Log4j2
public abstract class Server implements ICloudServer, Cacheable {
    protected static final Gson GSON = new Gson();

    protected final Template template;
    protected final UUID uuid;
    protected final ServerType type;

    protected String name;

    @Builder.Default
    protected String address = "127.0.0.1";
    protected int port;

    protected final long createdAt;

    private String directory;

    private IStartMethod startMethod;
    private final StartMethods selectedMethod;

    @Builder.Default
    protected final Map<String, String> env = new HashMap<>();

    @Builder.Default
    protected final List<UUID> players = new CopyOnWriteArrayList<>();

    @Builder.Default
    @Getter(AccessLevel.NONE)
    private final AtomicReference<ServerStatus> statusRef = new AtomicReference<>(ServerStatus.NONE);

    @Builder.Default
    @Setter
    private volatile long lastPlayerUpdate = System.currentTimeMillis();

    @Builder.Default
    @Getter(AccessLevel.PUBLIC)
    protected String nodeId = "";

    protected abstract void writeConsoleRemove(String command);
    public abstract void initName(Integer forceId);
    protected abstract void prepareRemote();
    protected abstract void proxyNotify();
    protected abstract void finalizeShutdown();
    protected abstract void startRemote();
    protected abstract void killRemote();
    protected abstract void stopRemote();
    protected abstract void sendStatusRemote(ServerStatus newStatus);

    @Override
    public String toString() {
        JsonObject obj = new ServerData(
                name,
                uuid,
                template.getName(),
                getStatus() != null ? getStatus().name() : ServerStatus.NONE.name(),
                type.name(),
                port,
                type.isProxy(),
                GSON.fromJson(GSON.toJson(players), JsonArray.class),
                new JsonObject(),
                nodeId,
                address
        ).toJson();

        return obj.toString();
    }

    @Override
    public String cacheKey() {
        return Keys.CACHE_PREFIX_SERVER + name.toUpperCase();
    }

    @Override
    public void setStatus(ServerStatus newStatus) {
        ServerStatus oldStatus = statusRef.getAndSet(newStatus);
        if (oldStatus != newStatus) {
            updateCache();
        }

        sendStatusRemote(newStatus);
    }

    public void setStatusLocally(ServerStatus newStatus) {
        ServerStatus oldStatus = statusRef.getAndSet(newStatus);
        if (oldStatus != newStatus) {
            updateCache();
        }
    }

    public ServerStatus getStatus() {
        return statusRef.get();
    }

    /**
     * Writes a command to the server console.
     * Only works if the server is in an active state.
     *
     * @param command The command to execute
     */
    public void writeConsole(String command) {
        ServerStatus currentStatus = getStatus();
        if (currentStatus != ServerStatus.STARTING &&
                currentStatus != ServerStatus.RUNNING &&
                currentStatus != ServerStatus.STOPPING) {
            log.warn("Attempted to write console command to server {} in invalid state: {}", name, currentStatus);
            return;
        }

        if (!isLocal()) {
            writeConsoleRemove(command);
            return;
        }

        if (!startMethod.isActive()) {
            log.warn("Cannot write to console for server {}: process not alive", name);
            return;
        }

        startMethod.writeCommand(command);
    }

    public boolean isLocal() {
        return nodeId == null || nodeId.isEmpty() || nodeId.equals(CurrentInstance.getNODE_ID());
    }

    public boolean isMaster() {
        return nodeId == null || nodeId.isEmpty();
    }

    /**
     * Prepares the server by copying template files and configuring ports.
     */

    public void prepare() {
        if (getStatus().ordinal() >= ServerStatus.PREPARED.ordinal()) {
            log.debug("Server {} already prepared, skipping", name);
            return;
        }

        log.info("Preparing {}", name);

        if (!isLocal()) {
            prepareRemote();
            return;
        }

        try {
            startMethod = selectedMethod.getTargetClass().newInstance();
            String basePath = (template.isStaticServer() ? Directories.SERVERS_DIR : Directories.TEMP_DIR).getAbsolutePath();
            Path serverDir = Path.of(basePath, name);

            startMethod.setDirectory(serverDir.toString());
            directory = serverDir.toString();

            Path templatePath = Path.of(Directories.TEMPLATES_DIR.getAbsolutePath(), template.getName());

            startMethod.prepare(templatePath.toString(), type.startCommand(), env);
            configureServerPort(serverDir);

            File typePluginsFolder = new File(Directories.PLUGINS_DIR, type.name());
            File targetPluginsFolder = new File(serverDir.toFile(), "plugins");

            if (typePluginsFolder.exists() && typePluginsFolder.isDirectory()) {
                if (!targetPluginsFolder.exists()) {
                    targetPluginsFolder.mkdirs();
                }

                FileUtils.copyDirectory(typePluginsFolder, targetPluginsFolder);
                log.info("Successfully copied plugins for type: {}", type.name());
            }

            setStatus(ServerStatus.PREPARED);
        } catch (Exception e) {
            log.error("Failed to prepare server {}", name, e);
            throw new RuntimeException("Server preparation failed", e);
        }
    }

    private void configureServerPort(Path serverDir) throws IOException {
        try {
            startMethod.setPort(port);
            Path portConfigFile = serverDir.resolve(type.portSettingFile());

            if (!Files.exists(portConfigFile)) {
                log.warn("Port configuration file not found: {}", portConfigFile.toAbsolutePath());
                return;
            }

            String content = Files.readString(portConfigFile, StandardCharsets.UTF_8);
            content = content.replace(type.portSettingPlaceholder(), String.valueOf(port));
            Files.writeString(portConfigFile, content, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Error configuring port for server {}: {}", name, e.getMessage());
            throw e;
        }
    }

    /**
     * Handles server exit cleanup and notification.
     */
    public void onExit() {
        log.info("Server {} exited", name);

        setStatus(ServerStatus.STOPPED);

        if(startMethod != null) {
            startMethod.cleanup();
            if(startMethod.isLoggerEnabled()) {
                startMethod.disableLogging();
                CurrentInstance.currentLogServer = null;
            }
        }

        proxyNotify();
        saveLogs();
        cleanupServerDirectory();
        resetCache();
        finalizeShutdown();
    }

    private void saveLogs() {
        if(!isLocal()) return;

        if (template.isStaticServer() || type.logsPath() == null) {
            return;
        }

        Path logSource = Path.of(directory, type.logsPath());
        if (!Files.exists(logSource)) {
            log.debug("No log file found for server {}", name);
            return;
        }

        try {
            String logFileName = String.format("%s_%d.log", name, System.currentTimeMillis());
            Path logDestination = Path.of(Directories.LOGS_DIR.getAbsolutePath(), logFileName);
            Files.createDirectories(logDestination.getParent());
            Files.copy(logSource, logDestination, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved logs for server {} to {}", name, logDestination);
        } catch (IOException e) {
            log.error("Failed to copy log file for server {}", name, e);
        }
    }

    private void cleanupServerDirectory() {
        if(!isLocal()) return;
        if (template.isStaticServer()) {
            return;
        }

        try {
            FileUtils.deleteDirectory(new File(directory));
            log.debug("Deleted temporary directory for server {}", name);
        } catch (IOException e) {
            log.error("Failed to delete server directory for {}", name, e);
        }
    }

    @Override
    public void start() {
        ServerStatus currentStatus = getStatus();
        if (currentStatus != ServerStatus.PREPARED && currentStatus.ordinal() >= ServerStatus.STARTING.ordinal()) {
            log.warn("Cannot start server {} in state {}", name, currentStatus);
            return;
        }

        if (name == null) {
            throw new IllegalStateException("Server name not initialized. Call initName() before starting the server.");
        }

        log.info("Starting {}", name);

        if (!isLocal()) {
            startRemote();
            return;
        }

        setStatus(ServerStatus.STARTING);

        try {
            startMethod.setOnExit(this::onExit);
            startMethod.start();
            scheduleStartupTimeout();
        } catch (Exception e) {
            log.error("Failed to start server {}", name, e);
            setStatus(ServerStatus.STOPPED);
        }
    }

    private void scheduleStartupTimeout() {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (getStatus().ordinal() <= ServerStatus.STARTING.ordinal()) {
                    log.error("Server {} failed to start within timeout period, killing server", name);
                    kill();
                }
                timer.cancel();
            }
        }, template.getMaxBootTimeMs());
    }

    public void kill() {
        log.info("Killing {}", name);
        if (!isLocal()) {
            killRemote();
            return;
        }
        stop();
        startMethod.kill(template.getShutdownTimeMs());
    }

    @Override
    public void stop() {
        log.info("Stopping {}", name);

        if (!isLocal()) {
            stopRemote();
            return;
        }

        if (getStatus() != ServerStatus.RUNNING) {
            log.warn("Attempted to stop server {} in invalid state: {}", name, getStatus());
            return;
        }

        startMethod.stop(type.stopCommand());
        setStatus(ServerStatus.STOPPING);
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public HostAndPort getAddress() {
        return HostAndPort.fromParts(address, port);
    }
}