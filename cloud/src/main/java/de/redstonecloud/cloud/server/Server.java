package de.redstonecloud.cloud.server;

import com.google.common.net.HostAndPort;
import com.google.gson.JsonObject;
import de.redstonecloud.api.components.ICloudServer;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.logger.Logger;
import de.redstonecloud.cloud.utils.Translator;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;
import de.redstonecloud.api.redis.cache.Cacheable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Builder
@Getter
public class Server implements ICloudServer, Cacheable {
    public Template template;
    public String name;
    public int port;
    @Builder.Default
    //TODO: ADD PLAYER CLASS
    public Map<Long, String> players = new HashMap<>();
    @Builder.Default
    private ServerStatus status = ServerStatus.NONE;
    public ServerType type;
    public long createdAt;
    public String directory;
    @Setter
    public ServerLogger logger;

    private Process process;
    private ProcessBuilder processBuilder;

    @Override
    public String toString() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("template", template.getName());
        obj.addProperty("status", status.name());
        obj.addProperty("type", type.name());
        obj.addProperty("port", port);
        obj.addProperty("proxy", type.isProxy());

        return obj.toString();
    }

    public String cacheKey() {
        return "server:" + name.toUpperCase();
    }

    @Override
    public void setStatus(ServerStatus status) {
        ServerStatus old = this.status;
        this.status = status;
        if(old != status) updateCache();
    }

    @Override
    public String getName() {
        return name;
    }

    public void writeConsole(String command) {
        if(status != ServerStatus.STARTING && status != ServerStatus.RUNNING && status != ServerStatus.STOPPING) return;

        PrintWriter stdin = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream())), true);

        stdin.println(command);
    }

    /**
     * SERVER SETUP STUFF
     */

    public void prepare() {
        if(status.getValue() >= ServerStatus.PREPARED.getValue()) {
            return;
        }

        int servId = 1;
        if(ServerManager.getInstance().getServer(template.getName() + "-" + servId) != null) {
            while(ServerManager.getInstance().getServer(template.getName() + "-" + servId) != null) {
                servId++;
            }
        }

        name = template.getName() + "-" + servId;

        RedstoneCloud.getLogger().debug(Translator.translate("cloud.server.prepare", name));

        if(!template.isStaticServer()) directory = Path.of(RedstoneCloud.workingDir + "/tmp/" + name).toString();
        else directory = Path.of(RedstoneCloud.workingDir + "/servers/" + name).toString();

        if(!directory.endsWith("/")) directory += "/";

        new File(directory).mkdir();

        File templateDir = new File(RedstoneCloud.workingDir + "/templates/" + template.getName());

        try {
            FileUtils.copyDirectory(templateDir, new File(directory));
        } catch (Exception e) {
            RedstoneCloud.getLogger().error("Failed to copy files from template to server directory for server " + name + " with template " + template.getName() + ".");
            e.printStackTrace();
        }

        try {
            String content = new String(Files.readAllBytes(Paths.get(directory + type.portSettingFile())), StandardCharsets.UTF_8);
            content = content.replace(type.portSettingPlaceholder(), String.valueOf(port));

            Files.write(Paths.get(directory + type.portSettingFile()), content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            e.printStackTrace();
        }


        setStatus(ServerStatus.PREPARED);
    }

    public void onExit() {
        if(logger != null) logger.cancel();

        RedstoneCloud.getLogger().debug(Translator.translate("cloud.server.exited", name));

        //copy log file to logs dir if server is not static
        if(!getTemplate().isStaticServer() && type.logsPath() != null) {
            synchronized (this) {
                try {
                    Files.copy(Paths.get(directory + "/" + type.logsPath()), Paths.get("./logs/" + name + "_" + System.currentTimeMillis() + ".log"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                FileUtils.deleteDirectory(new File(directory));
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        process.destroy();
        status = ServerStatus.STOPPED;
        resetCache();
        ServerManager.getInstance().remove(this);
    }

    @Override
    public void start() {
        if(status.getValue() != ServerStatus.PREPARED.getValue() && status.getValue() >= ServerStatus.STARTING.getValue()) {
            return;
        }

        RedstoneCloud.getLogger().debug(Translator.translate("cloud.server.starting", name));
        setStatus(ServerStatus.STARTING);

        processBuilder = new ProcessBuilder(
                type.startCommand()
        ).directory(new File(directory));

        //TODO: CHANGE IP IN CLUSTER MODE
        processBuilder.environment().put("REDIS_IP", "127.0.0.1");
        processBuilder.environment().put("REDIS_PORT", String.valueOf(CloudConfig.getCfg().get("redis_port").getAsInt()));
        processBuilder.environment().put("BRIDGE_CFG", CloudConfig.getCfg().get("bridge").getAsJsonObject().toString());

        this.logger = ServerLogger.builder().server(this).build();

        try {
            process = processBuilder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.logger.start();

        process.onExit().thenRun(this::onExit);

        //TODO: server manager stuff
    }

    @Override
    public void stop() {
        RedstoneCloud.getLogger().debug(Translator.translate("cloud.server.stopping", name));
        if(logger != null) logger.cancel();
        if(status.getValue() != ServerStatus.RUNNING.getValue()) {
            return;
        }

        writeConsole("stop");
        writeConsole("wdend");

        status = ServerStatus.STOPPING;
        resetCache();


        try {
            //run code in thread to not block main thread
            new Thread(() -> {
                try {
                    process.waitFor();

                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                    process.destroy();
                    status = ServerStatus.STOPPED;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public HostAndPort getAddress() {
        return HostAndPort.fromParts("0.0.0.0", port);
    }
}