package de.redstonecloud.cloud.server;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.api.RCClusteringProto;
import de.redstonecloud.api.RCGenericProto;
import de.redstonecloud.api.components.ServerStatus;
import de.redstonecloud.api.util.Keys;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.cluster.ClusterManager;
import de.redstonecloud.cloud.cluster.ClusterNode;
import de.redstonecloud.cloud.config.entires.BridgeSettings;
import de.redstonecloud.cloud.config.entires.RedisSettings;
import de.redstonecloud.cloud.events.defaults.ServerCreateEvent;
import de.redstonecloud.cloud.events.defaults.ServerStartEvent;
import de.redstonecloud.shared.config.SnakeYamlConfig;
import de.redstonecloud.shared.files.TemplateConfig;
import de.redstonecloud.shared.files.TypeConfig;
import de.redstonecloud.shared.files.template.TemplateBehavior;
import de.redstonecloud.shared.files.template.TemplateInfo;
import de.redstonecloud.shared.files.type.TypeDownloads;
import de.redstonecloud.shared.files.type.TypeInfo;
import de.redstonecloud.shared.utils.Directories;
import de.redstonecloud.shared.server.Server;
import de.redstonecloud.shared.server.ServerType;
import de.redstonecloud.shared.server.Template;
import de.redstonecloud.shared.startmethods.StartMethods;
import de.redstonecloud.shared.utils.SharedUtils;
import eu.okaeri.configs.ConfigManager;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Manages server lifecycle, templates, and server types.
 * Singleton pattern ensuring single instance across application.
 */
@Getter
@Log4j2
public class ServerManager {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_SHUTDOWN_TIME_MS = 5000;
    private static final int MIN_PORT = 10000;
    private static final int MAX_PORT = 50000;

    private static volatile ServerManager INSTANCE;

    private final Object2ObjectOpenHashMap<String, ServerType> types = new Object2ObjectOpenHashMap<>();
    private final Object2ObjectOpenHashMap<String, Template> templates = new Object2ObjectOpenHashMap<>();
    private final ConcurrentHashMap<String, Server> servers = new ConcurrentHashMap<>();

    /**
     * Gets the singleton instance of ServerManager.
     * Thread-safe double-checked locking implementation.
     *
     * @return the ServerManager instance
     */
    public static ServerManager getInstance() {
        if (INSTANCE == null) {
            synchronized (ServerManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ServerManager();
                }
            }
        }
        return INSTANCE;
    }

    private ServerManager() {
        log.debug("Initializing ServerManager");
        loadServerTypes();
        loadTemplates();
        log.info("ServerManager initialized with {} types and {} templates",
                types.size(), templates.size());
    }

    /**
     * Loads all template configurations from the template_configs directory.
     */
    private void loadTemplates() {
        Path templatesDir = Directories.TEMPLATE_CONFIGS_DIR.toPath();

        try {
            Files.createDirectories(templatesDir);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(templatesDir, "*.yml")) {
                for (Path file : stream) {
                    Template t = loadTemplate(file);
                    templates.put(t.getName(), t);
                    log.debug("Loaded template: {}", t.getName());
                }
            }

            log.debug("Loaded {} templates", templates.size());
        } catch (IOException e) {
            log.error("Failed to load templates from directory: {}", templatesDir, e);
        }
    }

    private Template loadTemplate(Path file) {
        try {
            TemplateConfig cfg = ConfigManager.create(TemplateConfig.class, it -> {
                it.withConfigurer(new SnakeYamlConfig());
                it.withBindFile(file.toFile());
                it.withRemoveOrphans(true);
                it.saveDefaults();
                it.load(true);
            });

            TemplateInfo info = cfg.info();
            TemplateBehavior behavior = cfg.behavior();

            ServerType type = types.get(info.type());

            if (type == null) {
                log.error("Template {} references unknown server type: {}", info.name(), info.type());
                return null;
            }

            List<String> nodes = new ArrayList<>();
            if (!info.node().isEmpty()) {
                nodes.add(info.node());
            }

            TemplateImpl template = TemplateImpl.builder()
                    .name(info.name())
                    .type(type)
                    .maxPlayers(behavior.maxPlayers())
                    .minServers(behavior.minServers())
                    .maxServers(behavior.maxServers())
                    .staticServer(info.isStatic())
                    .shutdownTimeMs(behavior.shutdownMillis())
                    .maxBootTimeMs(behavior.bootMillis())
                    .stopOnEmpty(behavior.autoStop())
                    .seperator(info.seperator())
                    .raw(SharedUtils.convertYamlToJson(Files.readString(file, StandardCharsets.UTF_8)))
                    .nodes(nodes)
                    .build();

            return template;
        } catch (IOException e) {
            log.error("Failed to read template file: {}", file.getFileName(), e);
        } catch (Exception e) {
            log.error("Failed to parse template file: {}", file.getFileName(), e);
        }

        return null;
    }

    /**
     * Reloads templates, stops running servers if template does no longer exist
     */
    public void reloadTemplates() {
        Object2ObjectOpenHashMap<String, Template> newTemplates = new Object2ObjectOpenHashMap<>();
        Path templatesDir = Directories.TEMPLATE_CONFIGS_DIR.toPath();

        try {
            Files.createDirectories(templatesDir);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(templatesDir, "*.yml")) {
                for (Path file : stream) {
                    Template t = loadTemplate(file);
                    newTemplates.put(t.getName(), t);
                    log.debug("Loaded template: {}", t.getName());
                }
            }

            //stop servers with no existing template anymore
            for (Server server : servers.values()) {
                if (!newTemplates.containsKey(server.getTemplate().getName())) {
                    log.info("Stopping server {} as its template no longer exists", server.getName());
                    server.kill();
                }
            }

            List<String> nodesToNotify = new ArrayList<>();

            Set<String> oldTemplateNames = this.templates.keySet();
            Set<String> newTemplateNames = newTemplates.keySet();

            Set<String> addedTemplates = new HashSet<>(newTemplateNames);
            addedTemplates.removeAll(oldTemplateNames);
            for (String added : addedTemplates) {
                log.info("Added template: {}", added);

                if (newTemplates.get(added).getNodes() != null) {
                    nodesToNotify.addAll(newTemplates.get(added).getNodes());
                }
            }
            Set<String> removedTemplates = new HashSet<>(oldTemplateNames);
            removedTemplates.removeAll(newTemplateNames);
            for (String removed : removedTemplates) {
                log.info("Removed template: {}", removed);

                if (this.templates.get(removed).getNodes() != null) {
                    nodesToNotify.addAll(this.templates.get(removed).getNodes());
                }
            }
            //updated templates
            Set<String> commonTemplates = new HashSet<>(oldTemplateNames);
            commonTemplates.retainAll(newTemplateNames);
            for (String common : commonTemplates) {
                if (!GSON.toJson(this.templates.get(common).getRaw())
                        .equals(GSON.toJson(newTemplates.get(common).getRaw()))) {
                    log.info("Updated template: {}", common);

                    if (newTemplates.get(common).getNodes() != null) {
                        nodesToNotify.addAll(newTemplates.get(common).getNodes());
                    }
                }
            }

            for (Map.Entry<String, Template> entry : newTemplates.entrySet()) {
                if (this.templates.containsKey(entry.getKey())) {
                    this.templates.get(entry.getKey()).merge(entry.getValue());
                } else {
                    this.templates.put(entry.getKey(), entry.getValue());
                }
            }

            for (String oldTemplateName : oldTemplateNames) {
                if (!newTemplates.containsKey(oldTemplateName)) {
                    this.templates.remove(oldTemplateName);
                }
            }

            log.debug("Reloaded {} templates", templates.size());

            //notify nodes if there are
            if (ClusterManager.isCluster()) {
                for (String nodeId : nodesToNotify.stream().distinct().toList()) {
                    List<RCGenericProto.Template> templates = RedstoneCloud.getInstance().getServerManager().getTemplatesForNode(nodeId).stream().map(template -> RCGenericProto.Template.newBuilder()
                            .setName(template.getName())
                            .setData(template.getRaw())
                            .build()
                    ).toList();

                    ClusterManager.getInstance().getNodeById(nodeId).send(RCClusteringProto.Payload.newBuilder()
                            .setTemplateChanges(RCClusteringProto.TempateChanges.newBuilder()
                                    .addAllTemplates(templates)
                                    .build())
                            .build());

                }
            }
        } catch (IOException e) {
            log.error("Failed to load templates from directory: {}", templatesDir, e);
        }
    }

    /**
     * Loads all server type configurations from the types directory.
     */
    private void loadServerTypes() {
        Path typesDir = Directories.TYPES_DIR.toPath();

        try {
            Files.createDirectories(typesDir);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(typesDir, "*.yml")) {
                for (Path file : stream) {
                    ServerType type = loadServerType(file);
                    types.put(type.name(), type);
                    log.debug("Loaded server type: {}", type.name());

                }
            }

            log.debug("Loaded {} server types", types.size());
        } catch (IOException e) {
            log.error("Failed to load server types from directory: {}", typesDir, e);
        }
    }

    private ServerType loadServerType(Path file) {
        try {
            TypeConfig cfg = ConfigManager.create(TypeConfig.class, it -> {
                it.withConfigurer(new SnakeYamlConfig());
                it.withBindFile(file.toFile());
                it.withRemoveOrphans(true);
                it.saveDefaults();
                it.load(true);
            });

            TypeInfo info = cfg.info();
            TypeDownloads downloads = cfg.downloads();

            ServerType serverType = new ServerType(
                    info.name(),
                    info.startCommand().split(" "),
                    info.isProxy(),
                    info.logFile(),
                    info.portFile(),
                    info.portPlaceholder(),
                    info.stopCommand(),
                    SharedUtils.convertYamlToJson(Files.readString(file, StandardCharsets.UTF_8))
            );

            return serverType;
        } catch (IOException e) {
            log.error("Failed to read server type file: {}", file.getFileName(), e);
        } catch (Exception e) {
            log.error("Failed to parse server type file: {}", file.getFileName(), e);
        }

        return null;
    }

    /**
     * Reloads server types from configuration files.
     */
    public void reloadServerTypes() {
        Object2ObjectOpenHashMap<String, ServerType> newTypes = new Object2ObjectOpenHashMap<>();
        Path typesDir = Directories.TYPES_DIR.toPath();

        try {
            Files.createDirectories(typesDir);

            try (DirectoryStream<Path> stream = Files.newDirectoryStream(typesDir, "*.yml")) {
                for (Path file : stream) {
                    ServerType t = loadServerType(file);
                    newTypes.put(t.name(), t);
                    log.debug("Loaded type: {}", t.name());
                }
            }

            //stop servers with no existing template anymore - if template is based on removed type, remove it too
            for (Server server : servers.values()) {
                if (!newTypes.containsKey(server.getType().name())) {
                    log.info("Stopping server {} as its type no longer exists", server.getName());
                    server.kill();

                    //also remove template
                    String templateName = server.getTemplate().getName();
                    if (templates.containsKey(templateName)) {
                        log.info("Removing template {} as its type no longer exists", templateName);
                        templates.remove(templateName);
                    }
                }
            }

            Set<String> oldTypeNames = this.types.keySet();
            Set<String> newTypeNames = newTypes.keySet();

            Set<String> addedTypes = new HashSet<>(newTypeNames);
            addedTypes.removeAll(oldTypeNames);
            for (String added : addedTypes) {
                log.info("Added type: {}", added);
            }
            Set<String> removedTypes = new HashSet<>(oldTypeNames);
            removedTypes.removeAll(newTypeNames);
            for (String removed : removedTypes) {
                log.info("Removed types: {}", removed);
            }
            //updated types
            Set<String> commonTypes = new HashSet<>(oldTypeNames);
            commonTypes.retainAll(newTypeNames);
            for (String common : commonTypes) {
                if (!GSON.toJson(this.types.get(common).raw())
                        .equals(GSON.toJson(newTypes.get(common).raw()))) {
                    log.info("Updated type: {}", common);
                }
            }

            for (Map.Entry<String, ServerType> entry : newTypes.entrySet()) {
                if (this.types.containsKey(entry.getKey())) {
                    this.types.put(entry.getKey(), entry.getValue());
                } else {
                    this.types.put(entry.getKey(), entry.getValue());
                }
            }

            for (String oldTypeName : oldTypeNames) {
                if (!newTypes.containsKey(oldTypeName)) {
                    this.types.remove(oldTypeName);
                }
            }

            log.debug("Reloaded {} types", templates.size());

            //notify nodes if there are
            if (ClusterManager.isCluster()) {
                List<RCGenericProto.Type> types = RedstoneCloud.getInstance().getServerManager().getTypes().clone().values().stream().map(type -> RCGenericProto.Type.newBuilder()
                        .setName(type.name())
                        .setConfig(type.raw())
                        .build()
                ).toList();

                for (ClusterNode node : ClusterManager.getInstance().getNodes()) {
                    node.send(RCClusteringProto.Payload.newBuilder()
                            .setTypeChanges(RCClusteringProto.TypeChanges.newBuilder()
                                    .addAllTypes(types)
                                    .build())
                            .build());
                }
            }
        } catch (IOException e) {
            log.error("Failed to load templates from directory: {}", typesDir, e);
        }
    }

    /**
     * Removes a server from the manager.
     *
     * @param server the server to remove
     */
    public void remove(Server server) {
        if (server == null) {
            log.warn("Attempted to remove null server");
            return;
        }

        servers.remove(server.getName().toUpperCase());
        log.debug("Removed server: {}", server.getName());
    }

    /**
     * Adds a server to the manager.
     *
     * @param server the server to add
     */
    public void add(Server server) {
        if (server == null) {
            log.warn("Attempted to add null server");
            return;
        }

        servers.put(server.getName().toUpperCase(), server);
        log.debug("Added server: {}", server.getName());
    }

    /**
     * Gets a server by name (case-insensitive).
     *
     * @param name the server name
     * @return the server, or null if not found
     */
    public Server getServer(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return servers.get(name.toUpperCase());
    }

    /**
     * Gets a template by name.
     *
     * @param name the template name
     * @return the template, or null if not found
     */
    public Template getTemplate(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        return templates.get(name);
    }

    /**
     * Starts a new server with auto-generated ID.
     *
     * @param template the template to use
     * @return the created server, or null if cancelled
     */
    public Server startServer(Template template) {
        return startServer(template, null);
    }

    /**
     * Starts a new server with specified or auto-generated ID.
     *
     * @param template the template to use
     * @param id       the server ID, or null for auto-generation
     * @return the created server, or null if cancelled
     */
    public Server startServer(Template template, Integer id) {
        if (!RedstoneCloud.isRunning()) {
            log.warn("Cannot start server while cloud is shutting down.");
            return null;
        }
        if (template == null) {
            log.error("Cannot start server: template is null");
            return null;
        }

        String node = template.getNodes() != null && !template.getNodes().isEmpty() ? template.getNodes().getFirst() : "";

        ClusterNode clusterNode = null;
        if(!node.isEmpty() && ClusterManager.isCluster() && ((clusterNode = ClusterManager.getInstance().getNodeById(node)) == null || clusterNode.getStream() == null || clusterNode.isShuttingDown())) {
            log.error("Cannot start server: specified node '{}' is not available", node);
            return null;
        }

        clusterNode = ClusterManager.isCluster() && !node.isEmpty() ? ClusterManager.getInstance().getNodeById(node) : null;

        RedisSettings redisCfg = RedstoneCloud.getConfig().redis();
        BridgeSettings bridgeSettings = RedstoneCloud.getConfig().bridge();

        JsonObject bridgeJson = new JsonObject();

        bridgeJson.addProperty("hub_template", bridgeSettings.hubTemplate());
        bridgeJson.addProperty("hubcommand_desc", bridgeSettings.hubDescription());
        bridgeJson.addProperty("hubcommand_no_hub_available", bridgeSettings.hubNotAvailable());
        bridgeJson.addProperty("fallback_name", bridgeSettings.fallbackServer());
        bridgeJson.addProperty("fallback_on_join", bridgeSettings.fallbackOnJoin());

        ServerImpl server = ServerImpl.builder()
                .address(clusterNode != null ? clusterNode.getAddress() : "127.0.0.1")
                .template(template)
                .uuid(UUID.randomUUID())
                .createdAt(System.currentTimeMillis())
                .type(template.getType())
                .port(generateRandomPort())
                .nodeId(node)
                .env(Map.of(
                        Keys.ENV_REDIS_IP, redisCfg.connectIp(),
                        Keys.ENV_REDIS_PORT, String.valueOf(redisCfg.port()),
                        Keys.ENV_REDIS_DB, String.valueOf(redisCfg.dbId()),
                        "BRIDGE_CFG", bridgeJson.toString()
                ))
                .selectedMethod(RedstoneCloud.getConfig().startMethod()).build();

        server.initName(id);

        ServerCreateEvent event = RedstoneCloud.getInstance()
                .getEventManager()
                .callEvent(new ServerCreateEvent(server));

        if (event.isCancelled()) {
            log.info("Server creation cancelled by event for template: {}", template.getName());
            return null;
        }

        server.prepare();
        //wait for server.getStatus() to be PREPARED, then start
        add(server);
        //TODO: This might be in an extra thread later to not block the main thread
        while (server.getStatus() != ServerStatus.PREPARED) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interrupted while waiting for server to prepare", e);
            }
        }

        template.setRunningServers(template.getRunningServers() + 1);

        server.start();
        RedstoneCloud.getInstance()
                .getEventManager()
                .callEvent(new ServerStartEvent(server));

        log.info("Server {} created and scheduled to start", server.getName());
        return server;
    }

    private int generateRandomPort() {
        return ThreadLocalRandom.current().nextInt(MIN_PORT, MAX_PORT + 1);
    }

    /**
     * Stops all running servers.
     * Blocks until all servers have stopped or timeout occurs.
     *
     * @return true if all servers stopped successfully
     */
    public boolean stopAll() {
        if (servers.isEmpty()) {
            log.info("No servers to stop");
            return true;
        }

        log.info("Stopping all {} servers", servers.size());

        // Create snapshot to avoid concurrent modification
        List<Server> serverList = new ArrayList<>(servers.values());

        // Create futures for all stop operations
        List<CompletableFuture<Void>> stopFutures = serverList.stream()
                .map(server -> CompletableFuture.runAsync(() -> server.kill()))
                .collect(Collectors.toList());

        while(!servers.isEmpty()) {}

        try {
            // Wait for all servers to stop
            CompletableFuture.allOf(stopFutures.toArray(new CompletableFuture[0]))
                    .get(60, TimeUnit.SECONDS);

            log.info("All servers stopped successfully");
            return true;

        } catch (TimeoutException e) {
            log.error("Timeout while stopping servers - some servers may still be running");
            return false;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Error while stopping servers", e);
            return false;
        }
    }

    /**
     * Gets all servers using a specific template.
     *
     * @param template the template to filter by
     * @return array of matching servers
     */
    public Server[] getServersByTemplate(Template template) {
        if (template == null) {
            return new Server[0];
        }

        return servers.values().stream()
                .filter(server -> template.equals(server.getTemplate()))
                .toArray(Server[]::new);
    }

    /**
     * Gets all servers of a specific type.
     *
     * @param type the server type to filter by
     * @return array of matching servers
     */
    public Server[] getServersByType(ServerType type) {
        if (type == null) {
            return new Server[0];
        }

        return servers.values().stream()
                .filter(server -> type.equals(server.getType()))
                .toArray(Server[]::new);
    }

    /**
     * Finds the best servers for a template based on available slots.
     * Returns servers sorted by free slots (fewest free slots first).
     *
     * @param template the template to search for
     * @return array of best server results sorted by free slots
     */
    public BestServerResult[] getBestServer(Template template) {
        if (template == null) {
            return new BestServerResult[0];
        }

        return servers.values().stream()
                .filter(server ->
                        template.equals(server.getTemplate())
                                && server.getStatus() == ServerStatus.RUNNING
                                && server.getPlayers().size() < template.getMaxPlayers()
                )
                .map(server -> new BestServerResult(
                        server,
                        template.getMaxPlayers() - server.getPlayers().size()
                ))
                .sorted(Comparator.comparingInt(BestServerResult::freeSlots))
                .toArray(BestServerResult[]::new);
    }

    /**
     * Calculates total free slots across all running servers for a template.
     *
     * @param template the template to calculate for
     * @return total number of free slots
     */
    public int getTemplateFreeSlots(TemplateImpl template) {
        if (template == null) {
            return 0;
        }

        return servers.values().stream()
                .filter(server ->
                        template.equals(server.getTemplate())
                                && server.getStatus() == ServerStatus.RUNNING
                )
                .mapToInt(server -> template.getMaxPlayers() - server.getPlayers().size())
                .sum();
    }

    /**
     * Gets the total number of running servers.
     *
     * @return count of running servers
     */
    public int getRunningServerCount() {
        return (int) servers.values().stream()
                .filter(server -> server.getStatus() == ServerStatus.RUNNING)
                .count();
    }

    /**
     * Gets all currently managed servers.
     *
     * @return collection of all servers
     */
    public Collection<Server> getAllServers() {
        return new ArrayList<>(servers.values());
    }

    public List<Template> getTemplatesForNode(String nodeId) {
        List<Template> templates = new ArrayList<>();
        for (Template template : this.templates.values()) {
            if (!template.getNodes().isEmpty() && template.getNodes().contains(nodeId)) {
                templates.add(template);
            }
        }

        return templates;
    }

    /**
     * Result of finding the best server for load balancing.
     *
     * @param server    the server instance
     * @param freeSlots number of available player slots
     */
    public record BestServerResult(Server server, int freeSlots) {
    }
}
