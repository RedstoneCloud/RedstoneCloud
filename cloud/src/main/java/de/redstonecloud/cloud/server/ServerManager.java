package de.redstonecloud.cloud.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.cloud.events.defaults.ServerStartEvent;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Getter
public class ServerManager {
    private static ServerManager INSTANCE;

    private Map<String, ServerType> types = new HashMap<>();
    private Map<String,Template> templates = new HashMap<>();
    private Map<String,Server> servers = new HashMap<>();

    public static ServerManager getInstance() {
        return INSTANCE != null ? INSTANCE : new ServerManager();
    }

    private ServerManager() {
        INSTANCE = this;

        loadServerTypes();
        loadTemplates();
    }

    private void loadTemplates() {
        File folder = new File(RedstoneCloud.workingDir + "/template_configs");
        if(!folder.exists()) {
            folder.mkdirs();
        }

        for(File file : folder.listFiles()) {
            if(!file.getName().endsWith(".json")) continue;

            String content = "";
            try {
                content = new String(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(content.isEmpty()) continue;

            JsonObject data = JsonParser.parseString(content).getAsJsonObject();

            Template t = Template.builder()
                    .name(data.get("name").getAsString())
                    .type(types.get(data.get("type").getAsString()))
                    .maxPlayers(data.get("maxPlayers").getAsInt())
                    .minServers(data.get("minServers").getAsInt())
                    .maxServers(data.get("maxServers").getAsInt())
                    .staticServer(data.get("staticServer").getAsBoolean())
                    .build();
            templates.put(data.get("name").getAsString(), t);
        }
    }

    private void loadServerTypes() {
        File folder = new File(RedstoneCloud.workingDir + "/types");
        if(!folder.exists()) {
            folder.mkdirs();
        }

        for(File file : folder.listFiles()) {
            if(!file.getName().endsWith(".json")) continue;

            String content = "";
            try {
                content = new String(Files.readAllBytes(file.toPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            if(content.isEmpty()) continue;

            JsonObject data = JsonParser.parseString(content).getAsJsonObject();
            JsonArray startCommandArray = data.getAsJsonArray("startCommand");

            String[] startCommand = new String[startCommandArray.size()];

            for (int i = 0; i < startCommandArray.size(); i++) {
                startCommand[i] = startCommandArray.get(i).getAsString();
            }

            types.put(data.get("name").getAsString(), new ServerType(
                    data.get("name").getAsString(),
                    startCommand,
                    data.get("isProxy").getAsBoolean(),
                    data.get("logsPath").isJsonNull() ? null : data.get("logsPath").getAsString(),
                    data.get("portSettingFile").getAsString(),
                    data.get("portSettingPlaceholder").getAsString()
            ));
        }
    }

    public void remove(Server server) {
        servers.remove(server.getName().toUpperCase());
    }

    public void add(Server server) {
        servers.put(server.getName().toUpperCase(), server);
    }

    public Server getServer(String name) {
        return servers.get(name.toUpperCase());
    }

    public Template getTemplate(String name) {
        return templates.get(name);
    }

    public Server startServer(Template template) {
        Server srv = Server.builder()
                .template(template)
                .createdAt(System.currentTimeMillis())
                .type(template.getType())
                .port(ThreadLocalRandom.current().nextInt(10000,50000))
                .build();

        srv.prepare();
        add(srv);
        template.setRunningServers(template.getRunningServers() + 1);
        template.setRunningSinceStart(template.getRunningSinceStart() + 1);

        Timer t = new Timer();
        t.schedule(new TimerTask() {
            public void run() {
                srv.start();
                RedstoneCloud.getInstance().getEventManager().callEvent(new ServerStartEvent(srv.getName()));
            }
        }, 1000);

        return srv;
    }

    public boolean stopAll() {
        boolean allStopped = false;
        int stopped = 0;

        if(servers.isEmpty()) return true;

        for(Server server : servers.values().toArray(Server[]::new).clone()) {
            synchronized (server) {
                server.stop();
                stopped++;
            }

            if(stopped == servers.size()) {
                allStopped = true;
            }
        }

        return allStopped;
    }

    public Server[] getServersByTemplate(Template template) {
        return servers.values().stream().filter(server -> server.getTemplate().equals(template)).toArray(Server[]::new);
    }

    public Server[] getServersByType(ServerType type) {
        return servers.values().stream().filter(server -> server.getType().equals(type)).toArray(Server[]::new);
    }

    public BestServerResult[] getBestServer(Template template) {
        ArrayList<BestServerResult> best = new ArrayList<>();
        int min = Integer.MAX_VALUE;

        for (Server server : servers.values()) {
            if (server.getTemplate().equals(template) && server.getStatus() == ServerStatus.RUNNING) {
                //get server with most players
                if(server.getPlayers().size() < min) {
                    min = server.getPlayers().size();
                    best.add(new BestServerResult(server, server.getTemplate().getMaxPlayers() - server.getPlayers().size()));
                }
            }
        }

        //sort best servers by free slots, less free slots first
        best.sort((o1, o2) -> o1.freeSlots - o2.freeSlots);

        return best.toArray(BestServerResult[]::new);
    }

    public int getTemplateFreeSlots(Template template) {
        int freeSlots = 0;

        for (Server server : servers.values()) {
            if (server.getTemplate().equals(template) && server.getStatus() == ServerStatus.RUNNING) {
                freeSlots += server.getTemplate().getMaxPlayers() - server.getPlayers().size();
            }
        }

        return freeSlots;
    }

    public record BestServerResult(Server server, int freeSlots) {
    }
}
