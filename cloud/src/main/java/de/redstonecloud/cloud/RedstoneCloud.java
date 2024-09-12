package de.redstonecloud.cloud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.pierreschwang.nettypacket.event.EventRegistry;
import de.redstonecloud.api.encryption.KeyManager;
import de.redstonecloud.api.encryption.cache.KeyCache;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.events.EventManager;
import de.redstonecloud.cloud.logger.Logger;
import de.redstonecloud.cloud.plugin.PluginManager;
import de.redstonecloud.cloud.scheduler.task.Task;
import de.redstonecloud.cloud.server.ServerLogger;
import de.redstonecloud.cloud.broker.BrokerHandler;
import de.redstonecloud.cloud.commands.CommandManager;
import de.redstonecloud.cloud.console.Console;
import de.redstonecloud.cloud.scheduler.TaskScheduler;
import de.redstonecloud.cloud.scheduler.defaults.CheckTemplateTask;
import de.redstonecloud.cloud.server.ServerManager;
import de.redstonecloud.cloud.utils.Translator;
import de.redstonecloud.cloud.utils.Utils;
import lombok.Getter;
import lombok.Setter;
import de.redstonecloud.api.redis.broker.Broker;
import de.redstonecloud.api.redis.cache.Cache;
import lombok.SneakyThrows;
import de.redstonecloud.api.netty.NettyHelper;
import de.redstonecloud.api.netty.server.NettyServer;
import de.redstonecloud.api.netty.server.handler.NettyEventHandler;
import org.apache.commons.io.FileUtils;
import redis.embedded.RedisServer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Scanner;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Getter
public class RedstoneCloud {
    @Getter
    private static RedstoneCloud instance;
    @Getter
    public static String workingDir;
    @Getter
    public static Cache cache;
    @Getter
    public static boolean running = false;
    private static RedisServer redisServer;

    @SneakyThrows
    public static void main(String[] args) {
        workingDir = System.getProperty("user.dir");

        if (!new File("./.cloud.setup").exists()) setup();

        try {
            redisServer = RedisServer.builder()
                    .port(CloudConfig.getCfg().get("redis_port").getAsInt())
                    .setting("bind 127.0.0.1")
                    .build();
            System.setProperty("redis.port", CloudConfig.getCfg().get("redis_port").getAsString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        redisServer.start();

        Thread.sleep(2000);

        cache = new Cache();

        try {
            System.out.println(Translator.translate("cloud.startup.redis"));
            Broker broker = new Broker("cloud", "cloud");
            broker.listen("cloud", BrokerHandler::handle);
        } catch (Exception e) {
            throw new RuntimeException("Cannot connect to Redis: " + e);
        }

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("log4j.skipJansi", "false");
        System.setProperty("Dterminal.jline", "true");
        System.setProperty("Dterminal.ansi", "true");
        System.setProperty("Djansi.passthrough", "true");

        RedstoneCloud cloud = new RedstoneCloud();

        Runtime.getRuntime().addShutdownHook(new Thread(cloud::stop));
    }

    private ConsoleThread consoleThread;
    @Getter
    protected static Logger logger;
    @Setter
    protected ServerLogger currentLogServer = null;
    protected ServerManager serverManager;
    protected CommandManager commandManager;
    protected Console console;
    protected BufferedWriter logFile;
    protected PluginManager pluginManager;
    protected EventManager eventManager;

    protected boolean stopped = false;

    protected TaskScheduler scheduler;
    protected NettyServer nettyServer;
    protected KeyCache keyCache;

    public RedstoneCloud() {
        instance = this;
        boot();
    }

    private static void setup() {
        Logger l = Logger.getInstance();
        Scanner input = new Scanner(System.in);
        String result;

        Gson gson = new Gson();

        boolean redis = true;
        int intRedisPort = 6379;
        boolean downloadRedis = true;

        l.setup("RC Setup", "§cRedstoneCloud comes with a built-in redis instance. Would you like to use it? §3[y/n] §a(default: y): ");
        result = input.nextLine();
        if (result.toLowerCase().contains("n")) redis = false;

        if (redis) {
            l.setup("RC Setup", "§cPlease provide a redis port you want to use. §3[number] §a(default: 6379): ");
            try {
                intRedisPort = input.nextInt();
            } catch (Exception e) {
                l.setup("RC Setup", "§eProvided invalid port, using default port.", true);
            }

            /*
            l.setup("RC Setup", "§cThere is a redis update avaiable (Redis 7.2). Do you want to download it? Otherwise outdated Redis 2.8 will be used. §3[y/n] §a(default: y)");
            result = input.nextLine();
            if(result.toLowerCase().contains("n")) downloadRedis = false;
            System.out.println();

             */

            //TODO: DOWNLOAD REDIS
        } else {
            //TODO: CUSTOM REDIS INSTANCE
        }

        l.setup("RC Setup", "§eSettings completed. Generating basic file structure...", true);
        createBaseFolders();
        l.setup("RC Setup", "§eBasic folders generated. Starting server config...", true);

        JsonObject supportedSoftware = null;

        try {
            supportedSoftware = gson.fromJson(Utils.readFileFromResources("supportedSoftware.json"), JsonObject.class);
        } catch (Exception e) {
            e.printStackTrace();
            l.setup("RC Setup", "§4Error while reading supportedSoftware.json, shutting down...", true);
            System.exit(0);
        }

        if (supportedSoftware == null) {
            l.setup("RC Setup", "§4Output of supportedSoftware.json is null, shutting down...", true);
            System.exit(0);
        }

        boolean setupProxy = true;
        boolean setupServer = true;

        l.setup("RC Setup", "§cWould you like to setup a proxy instance? §3[y/n] §a(default: y): ");
        result = input.nextLine();
        if (result.toLowerCase().contains("n")) setupProxy = false;

        if (setupProxy) {
            l.setup("RC Setup", "§cPlease select a proxy software you want to use §3" + supportedSoftware.get("proxy").getAsJsonArray().toString().replace("\"", "") + " ");
            result = input.nextLine();
            String finalResult = result.toUpperCase();
            if (supportedSoftware.get("proxy").getAsJsonArray().asList().stream().noneMatch(proxy -> proxy.getAsString().equalsIgnoreCase(finalResult))) {
                l.setup("RC Setup", "§eProxy software " + result + " is unknown.", true);
                System.exit(0);
            }
            l.setup("RC Setup", "§eGenerating structure for " + result + "...", true);
            try {
                JsonObject settings = gson.fromJson(Utils.readFileFromResources("templates/" + finalResult + "/settings.json"), JsonObject.class);
                FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + finalResult + "/template_cfg.json"), new File("./template_configs/Proxy.json"));
                FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + finalResult + "/type.json"), new File("./types/" + finalResult + ".json"));
                Utils.copyFolderFromCurrentJar("templates/" + finalResult + "/files", new File("./templates/Proxy/"));
                l.setup("RC Setup", "§eCopied important files, downloading software...", true);
                FileUtils.copyURLToFile(URI.create(Utils.readFileFromResources("templates/" + finalResult + "/download_url.txt")).toURL(), new File("./templates/Proxy/proxy.jar"));
                l.setup("RC Setup", "§eDownloaded software successfully.", true);

                l.setup("RC Setup", "§eInstalling CloudBridge on Proxy...", true);
                FileUtils.copyURLToFile(URI.create(Utils.readFileFromResources("templates/" + finalResult + "/download_url_bridge.txt")).toURL(), new File("./templates/Proxy/" + settings.get("pluginDir").getAsString() + "/CloudBridge.jar"));
                l.setup("RC Setup", "§eInstalled CloudBridge", true);

                l.setup("RC Setup", "§eProxy installed successfully. \n", true);
            } catch (Exception e) {
                e.printStackTrace();
                l.setup("RC Setup", "§4Cannot setup proxy, shutting down...", true);
                System.exit(0);
            }
        }

        l.setup("RC Setup", "§cWould you like to setup a server instance? §3[y/n] §a(default: y): ");
        result = input.nextLine();
        if (result.toLowerCase().contains("n")) setupServer = false;

        if (setupServer) {
            l.setup("RC Setup", "§cPlease select a server software you want to use §3" + supportedSoftware.get("server").getAsJsonArray().toString().replace("\"", "") + " ");
            result = input.nextLine();
            String finalResult = result.toUpperCase();
            if (supportedSoftware.get("server").getAsJsonArray().asList().stream().noneMatch(server -> server.getAsString().equalsIgnoreCase(finalResult))) {
                l.setup("RC Setup", "§eServer software " + result + " is unknown.", true);
                System.exit(0);
            }
            l.setup("RC Setup", "§eGenerating structure for " + result + "...", true);
            try {
                JsonObject settings = gson.fromJson(Utils.readFileFromResources("templates/" + finalResult + "/settings.json"), JsonObject.class);
                FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + finalResult + "/template_cfg.json"), new File("./template_configs/Lobby.json"));
                FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + finalResult + "/type.json"), new File("./types/" + finalResult + ".json"));
                Utils.copyFolderFromCurrentJar("templates/" + finalResult + "/files", new File("./templates/Lobby/"));
                l.setup("RC Setup", "§eCopied important files, downloading software...", true);
                FileUtils.copyURLToFile(URI.create(Utils.readFileFromResources("templates/" + finalResult + "/download_url.txt")).toURL(), new File("./templates/Lobby/server.jar"));
                l.setup("RC Setup", "§eDownloaded software successfully.", true);

                l.setup("RC Setup", "§eInstalling CloudBridge on Server...", true);
                FileUtils.copyURLToFile(URI.create(Utils.readFileFromResources("templates/" + finalResult + "/download_url_bridge.txt")).toURL(), new File("./templates/Lobby/" + settings.get("pluginDir").getAsString() + "/CloudBridge.jar"));
                l.setup("RC Setup", "§eInstalled CloudBridge", true);

                l.setup("RC Setup", "§eServer installed successfully. \n", true);
            } catch (Exception e) {
                e.printStackTrace();
                l.setup("RC Setup", "§4Cannot setup Server, shutting down...", true);
                System.exit(0);
            }

            l.setup("RC Setup", "§eCopying cloud setup files...", true);
            try {
                FileUtils.copyURLToFile(Utils.getResourceFile("cloud.json"), new File("./config.json"));
                FileUtils.copyURLToFile(Utils.getResourceFile("language.json"), new File("./language.json"));
            } catch (IOException e) {
                e.printStackTrace();
                l.setup("RC Setup", "§4Copying cloud files failed, shutting down...", true);
                System.exit(0);
            }
            l.setup("RC Setup", "§eCopied cloud files.", true);

            l.setup("RC Setup", "", true);
            l.setup("RC Setup", "", true);
            l.setup("RC Setup", "§eCloud setup completed.", true);
            l.setup("RC Setup", "====================", true);
            l.setup("RC Setup", "Built-in redis: " + redis, true);
            l.setup("RC Setup", "Built-in redis port: " + intRedisPort, true);
            l.setup("RC Setup", "Updated built-in redis: " + downloadRedis, true);
            l.setup("RC Setup", "Setup proxy: " + setupProxy, true);
            l.setup("RC Setup", "Setup server: " + setupServer, true);
            l.setup("RC Setup", "====================", true);

            try {
                JsonObject cfgFile = CloudConfig.getCfg();
                cfgFile.addProperty("redis_port", intRedisPort);

                Files.writeString(Paths.get(RedstoneCloud.workingDir + "/cloud.json"), cfgFile.toString());
                Files.writeString(Paths.get(RedstoneCloud.workingDir + "/.cloud.setup"), "Cloud is set up. Do not delete this file or the setup will start again.");

                CloudConfig.getCfg(true); // Reload config fur future uses
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            l.setup("RC Setup", "", true);
            l.setup("RC Setup", "§cPlease press Enter to start the cloud.", true);

            try {
                System.in.read(new byte[2]); // TODO: Handle
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createBaseFolders() {
        String[] dirs = {"./servers", "./templates", "./tmp", "./logs", "./plugins", "./template_configs", "./types"};

        for (String dir : dirs) {
            File f = new File(dir);
            if (!f.exists()) {
                f.mkdir();
            }
        }
    }

    public void boot() {
        running = true;
        logger = Logger.getInstance();

        this.scheduler = new TaskScheduler(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()));

        this.nettyServer = new NettyServer(NettyHelper.constructRegistry(), new EventRegistry());
        this.nettyServer.getEventRegistry().registerEvents(new NettyEventHandler(this.nettyServer));
        this.nettyServer.setPort(51123).bind();

        PublicKey publicKey = KeyManager.init();
        this.keyCache = new KeyCache();
        this.keyCache.addKey("cloud", publicKey);

        try {
            logFile = new BufferedWriter(new FileWriter("./logs/cloud.log", true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        logger.info(Translator.translate("cloud.startup"));


        createBaseFolders();

        this.serverManager = ServerManager.getInstance();
        this.commandManager = new CommandManager();
        commandManager.loadCommands();

        this.eventManager = new EventManager(this);


        this.pluginManager = new PluginManager(this);
        pluginManager.loadAllPlugins();


        this.scheduler.scheduleRepeatingTask(new Task() {
            @Override
            protected void onRun(long currentMillis) {
                try {
                    logFile.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, TimeUnit.MILLISECONDS, 1000);

        this.console = new Console(this);
        this.consoleThread = new ConsoleThread();
        this.consoleThread.start();


        this.scheduler.scheduleRepeatingTask(new CheckTemplateTask(), 3000L);

        this.pluginManager.enableAllPlugins();
    }

    public void stop() {
        if (this.stopped || !running) {
            return;
        }

        this.stopped = true;
        running = false;
        this.scheduler.stopScheduler();

        try {
            Thread.sleep(200);
            logger.info(Translator.translate("cloud.shutdown.started"));
            boolean a = this.serverManager.stopAll();
            if (a) logger.info(Translator.translate("cloud.shutdown.servers"));
            this.pluginManager.disableAllPlugins();
            logger.info(Translator.translate("cloud.shutdown.plugins"));
            this.eventManager.getThreadedExecutor().shutdown();
            logger.info(Translator.translate("cloud.shutdown.complete"));
            Broker.get().shutdown();
            redisServer.stop();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

    private class ConsoleThread extends Thread {
        public ConsoleThread() {
            super("Console Thread");
        }

        @Override
        public void run() {
            if (isRunning()) console.start();
        }
    }
}
