package de.redstonecloud.server;

import com.google.gson.JsonObject;
import de.redstonecloud.RedstoneCloud;
import de.redstonecloud.logger.Logger;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

@Builder
@Getter
public class Server {
    public Template template;
    public String name;
    public int port;
    @Builder.Default
    //TODO: ADD PLAYER CLASS
    public Map<Long, String> players = new HashMap<>();
    public int maxPlayers;
    @Builder.Default
    private Status status = Status.NONE;
    public ServerType type;
    public String directory;
    @Setter
    @Getter
    public ServerLogger logger;

    private Process process;
    private ProcessBuilder processBuilder;

    @Override
    public String toString() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("template", template.getName());
        obj.addProperty("status", status.name());
        obj.addProperty("type", type.getName());
        obj.addProperty("port", port);

        return obj.toString();
    }

    public void updateCache() {
        //CacheManager.setData("server:" + name.toUpperCase(), this.toString());
    }

    public void setStatus(Status status) {
        Status old = this.status;
        this.status = status;
        if(old != status) updateCache();
    }


    public void start() {
        if(status.value != Status.PREPARED.value && status.value >= Status.STARTING.value) {
            return;
        }

        Logger.getInstance().debug("Starting " + name);
        setStatus(Status.STARTING);

        directory = RedstoneCloud.workingDir + "/servers";

        processBuilder = new ProcessBuilder(
                type.getStartCommand()
        ).directory(new File(directory));

        this.logger = ServerLogger.builder().server(this).build();
        this.logger.start();

        try {
            process = processBuilder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        process.onExit().thenRun(this::onExit);

        //TODO: server manager stuff
    }

    public void onExit() {
        if(logger != null) logger.cancel();

        //CacheManager.delData("server:" + name.toUpperCase());

        Logger.getInstance().debug(name + " exited.");

        //copy log file to logs dir if server is not static
        if(!getTemplate().staticServer && type.getLogsPath() != null) {
            synchronized (this) {
                try {
                    Files.copy(Paths.get(directory + "/" + type.getLogsPath()), Paths.get("./logs/" + name + "_" + System.currentTimeMillis() + ".log"), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //then delete server directory

            /*try {
                FileUtils.deleteDirectory(new File(directory));
            } catch (IOException e) {
                e.printStackTrace();
            }*/

        }

        process.destroy();
        status = Status.STOPPED;
        ServerManager.getInstance().remove(this);
    }

    public boolean stop() {
        if(logger != null) logger.cancel();
        if(status.value != Status.RUNNING.value) {
            return false;
        }


        final boolean[] stopped = {false};

        writeConsole("stop");
        writeConsole("wdend");

        status = Status.STOPPING;


        try {
            //run code in thread to not block main thread
            new Thread(() -> {
                try {
                    process.waitFor();

                    if (process.isAlive()) {
                        process.destroyForcibly();
                    }
                    process.destroy();
                    status = Status.STOPPED;
                    stopped[0] = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    stopped[0] = false;
                }
            }).start();
        } catch (Exception e) {
            e.printStackTrace();
            stopped[0] = false;
        }

        return stopped[0];
    }

    public void writeConsole(String command) {
        if(status != Status.STARTING && status != Status.RUNNING && status != Status.STOPPING) return;

        PrintWriter stdin = new PrintWriter(
                new BufferedWriter(
                        new OutputStreamWriter(process.getOutputStream())), true);

        stdin.println(command);
    }

    public void prepare() {
        if(status.value >= Status.PREPARED.value) {
            return;
        }

        int servId = 1;
        if(ServerManager.getInstance().getServer(template.getName() + "-" + servId) != null) {
            while(ServerManager.getInstance().getServer(template.getName() + "-" + servId) != null) {
                servId++;
            }
        }

        name = template.getName() + "-" + servId;

        //TODO: PREPARE SERVERS CONFIG

        status = Status.PREPARED;
    }


    public static enum Status {
        NONE(-1),
        PREPARED(0),
        STARTING(1),
        RUNNING(2),
        STOPPING(3),
        STOPPED(4),
        ERROR(5),
        IN_GAME(6),
        WAITING(7);

        @Getter
        private final int value;

        Status(int value) {
            this.value = value;
        }
    }
}