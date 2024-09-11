package de.redstonecloud.cloud;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.pierreschwang.nettypacket.event.EventRegistry;
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
import netty.NettyHelper;
import netty.server.NettyServer;
import netty.server.handler.NettyEventHandler;
import org.apache.commons.io.FileUtils;
import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


@Getter
public class RedstoneCloud {
    @Getter private static RedstoneCloud instance;
    @Getter public static String workingDir;
    @Getter public static Cache cache;
    @Getter public static boolean running = false;
    private static RedisServer redisServer;

    @SneakyThrows
    public static void main(String[] args) {
        workingDir = System.getProperty("user.dir");

        if(!new File("./.cloud.setup").exists()) setup();

        try {
            redisServer = new RedisServer(CloudConfig.getCfg().get("redis_port").getAsInt());
            System.setProperty("redis.port", CloudConfig.getCfg().get("redis_port").getAsString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        redisServer.start();

        Thread.sleep(2000);

        cache = new Cache();

        try {
            System.out.println(Translator.translate("cloud.startup.redis"));
            Broker broker = new Broker("cloud", "cloud");
            broker.listen("cloud", BrokerHandler::handle);
        } catch(Exception e) {
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
    @Getter protected static Logger logger;
    @Setter protected ServerLogger currentLogServer = null;
    protected ServerManager serverManager;
    protected CommandManager commandManager;
    protected Console console;
    protected BufferedWriter logFile;
    protected PluginManager pluginManager;
    protected EventManager eventManager;

    protected boolean stopped = false;

    protected TaskScheduler scheduler;
    protected NettyServer nettyServer;

    public RedstoneCloud() {
        instance = this;
        boot();
    }

    private static void setup() {
        Scanner input = new Scanner(System.in);
        String result;

        Gson gson = new Gson();

        boolean redis = true;
        int intRedisPort = 6379;
        boolean downloadRedis = true;

        System.out.println("RedstoneCloud comes with a built-in redis instance. Would you like to use it? [y/n] (default: y)");
        result = input.nextLine();
        if(result.toLowerCase().contains("n")) redis = false;

        if(redis) {
            System.out.println("Please provide a redis port you want to use. [number] (default: 6379)");
            try {
                intRedisPort = input.nextInt();
            } catch(Exception e) {
                System.out.println("Provided invalid port, using default port.");
            }

            System.out.println("There is a redis update avaiable (Redis 7.2). Do you want to download it? Otherwise outdated Redis 2.8 will be used. [y/n] (default: y)");
            result = input.nextLine();
            if(result.toLowerCase().contains("n")) downloadRedis = false;

            //TODO: DOWNLOAD REDIS
        } else {
            //TODO: CUSTOM REDIS INSTANCE
        }

        System.out.println("Settings completed. Generating basic file structure...");
        createBaseFolders();
        System.out.println("Basic folders generated. Starting server config...");

        JsonObject supportedSoftware = null;

        try {
            supportedSoftware = gson.fromJson(Utils.readFileFromResources("supportedSoftware.json"), JsonObject.class);
        } catch(Exception e) {
            e.printStackTrace();
            System.out.println("Error while reading supportedSoftware.json, shutting down...");
            System.exit(0);
        }

        if(supportedSoftware == null) {
            System.out.println("Output of supportedSoftware.json is null, shutting down...");
            System.exit(0);
        }

        boolean setupProxy = true;
        boolean setupServer = true;

        System.out.println("Would you like to setup a proxy instance? [y/n] (default: y)");
        result = input.nextLine();
        if(result.toLowerCase().contains("n")) setupProxy = false;

        if(setupProxy) {
            System.out.println("Please select a proxy software you want to use " + supportedSoftware.get("proxy").getAsJsonArray().toString().replace("\"", ""));
            result = input.nextLine();
            if(!supportedSoftware.get("proxy").getAsJsonArray().contains(new JsonParser().parse(result.toUpperCase()))) {
                System.out.println("Proxy software " + result + " is unknown.");
                System.exit(0);
            }
            System.out.println("Generating structure for " + result + "...");
            try {
                JsonObject settings = gson.fromJson(Utils.readFileFromResources("templates/" + result.toUpperCase() + "/settings.json"), JsonObject.class);
                FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + result.toUpperCase() + "/template_cfg.json"), new File("./template_configs/Proxy.json"));
                FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + result.toUpperCase() + "/type.json"), new File("./types/" + result.toUpperCase() + ".json"));
                Utils.copyFolderFromCurrentJar("templates/" + result.toUpperCase() + "/files", new File("./templates/Proxy/"));
                System.out.println("Copied important files, downloading software...");
                FileUtils.copyURLToFile(URI.create(Utils.readFileFromResources("templates/" + result.toUpperCase() + "/download_url.txt")).toURL(), new File("./templates/Proxy/proxy.jar"));
                System.out.println("Downloaded software successfully.");

                System.out.println("Installing CloudBridge on Proxy...");
                FileUtils.copyURLToFile(URI.create(Utils.readFileFromResources("templates/" + result.toUpperCase() + "/download_url_bridge.txt")).toURL(), new File("./templates/Proxy/" + settings.get("pluginDir").getAsString() + "/CloudBridge.jar"));
                System.out.println("Installed CloudBridge");

                System.out.println("Proxy installed successfully. \n\n\n");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Cannot setup proxy, shutting down...");
                System.exit(0);
            }
        }

        System.out.println("Would you like to setup a server instance? [y/n] (default: y)");
        result = input.nextLine();
        if(result.toLowerCase().contains("n")) setupServer = false;

        if(setupServer) {
            System.out.println("Please select a server software you want to use " + supportedSoftware.get("server").getAsJsonArray().toString().replace("\"", ""));
            result = input.nextLine();
            if(!supportedSoftware.get("server").getAsJsonArray().contains(new JsonParser().parse(result.toUpperCase()))) {
                System.out.println("Server software " + result + " is unknown.");
                System.exit(0);
            }
            System.out.println("Generating structure for " + result + "...");
            try {
                JsonObject settings = gson.fromJson(Utils.readFileFromResources("templates/" + result.toUpperCase() + "/settings.json"), JsonObject.class);
                FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + result.toUpperCase() + "/template_cfg.json"), new File("./template_configs/Lobby.json"));
                FileUtils.copyURLToFile(Utils.getResourceFile("templates/" + result.toUpperCase() + "/type.json"), new File("./types/" + result.toUpperCase() + ".json"));
                Utils.copyFolderFromCurrentJar("templates/" + result.toUpperCase() + "/files", new File("./templates/Lobby/"));
                System.out.println("Copied important files, downloading software...");
                FileUtils.copyURLToFile(URI.create(Utils.readFileFromResources("templates/" + result.toUpperCase() + "/download_url.txt")).toURL(), new File("./templates/Lobby/server.jar"));
                System.out.println("Downloaded software successfully.");

                System.out.println("Installing CloudBridge on Server...");
                FileUtils.copyURLToFile(URI.create(Utils.readFileFromResources("templates/" + result.toUpperCase() + "/download_url_bridge.txt")).toURL(), new File("./templates/Lobby/" + settings.get("pluginDir").getAsString() + "/CloudBridge.jar"));
                System.out.println("Installed CloudBridge");

                System.out.println("Server installed successfully. \n\n\n");
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Cannot setup Server, shutting down...");
                System.exit(0);
            }

            System.out.println("Copying cloud setup files...");
            try {
                FileUtils.copyURLToFile(Utils.getResourceFile("cloud.json"), new File("./cloud.json"));
                FileUtils.copyURLToFile(Utils.getResourceFile("language.json"), new File("./language.json"));
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Copying cloud files failed, shutting down...");
                System.exit(0);
            }
            System.out.println("Copied cloud files.");

            System.out.println("\n\n");
            System.out.println("Cloud setup completed.");
            System.out.println("====================");
            System.out.println("Built-in redis: " + redis);
            System.out.println("Built-in redis port: " + intRedisPort);
            System.out.println("Updated built-in redis: " + downloadRedis);
            System.out.println("Setup proxy: " + setupProxy);
            System.out.println("Setup server: " + setupServer);
            System.out.println("====================");

            System.out.println();
            System.out.println("Please press Enter to start the cloud.");
            input.next();
        }
    }

    private static void createBaseFolders() {
        String[] dirs = {"./servers", "./templates", "./tmp", "./logs", "./plugins", "./template_configs", "./types"};

        for(String dir : dirs) {
            File f = new File(dir);
            if(!f.exists()) {
                f.mkdir();
            }
        }
    }

    public void boot(){
        running = true;
        logger = Logger.getInstance();

        this.scheduler = new TaskScheduler(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()));

        this.nettyServer = new NettyServer(NettyHelper.constructRegistry(), new EventRegistry());
        this.nettyServer.getEventRegistry().registerEvents(new NettyEventHandler(this.nettyServer));

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
            if(a) logger.info(Translator.translate("cloud.shutdown.servers"));
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
            if(isRunning()) console.start();
        }
    }
}
