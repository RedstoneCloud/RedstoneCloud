package de.redstonecloud.cloud;

import de.pierreschwang.nettypacket.event.EventRegistry;
import de.redstonecloud.api.encryption.KeyManager;
import de.redstonecloud.api.encryption.cache.KeyCache;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.events.EventManager;
import de.redstonecloud.cloud.logger.Logger;
import de.redstonecloud.cloud.netty.CommHandler;
import de.redstonecloud.cloud.netty.PlayerHandler;
import de.redstonecloud.cloud.netty.ServerHandler;
import de.redstonecloud.cloud.netty.TemplateHandler;
import de.redstonecloud.cloud.player.CloudPlayer;
import de.redstonecloud.cloud.player.PlayerManager;
import de.redstonecloud.cloud.plugin.PluginManager;
import de.redstonecloud.cloud.scheduler.task.Task;
import de.redstonecloud.cloud.server.ServerLogger;
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
    private static boolean usingIntRedis;

    @Getter private static Broker broker;

    @SneakyThrows
    public static void main(String[] args) {
        workingDir = System.getProperty("user.dir");

        if (!new File("./.cloud.setup").exists()) Utils.setup();

        usingIntRedis = CloudConfig.getCfg().get("custom_redis").getAsBoolean();
        System.setProperty("redis.port", CloudConfig.getCfg().get("redis_port").getAsString());
        System.setProperty("redis.bind", CloudConfig.getCfg().get("redis_bind").getAsString());

        if(usingIntRedis) {
            try {
                redisServer = RedisServer.builder()
                        .port(CloudConfig.getCfg().get("redis_port").getAsInt())
                        .setting("bind " + CloudConfig.getCfg().get("redis_bind").getAsString())
                        .build();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            redisServer.start();
        }

        Thread.sleep(2000);

        cache = new Cache();

        try {
            System.out.println(Translator.translate("cloud.startup.redis"));
            broker = new Broker("cloud", "cloud");
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
    protected PlayerManager playerManager;
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


    public static void createBaseFolders() {
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
        this.nettyServer.getEventRegistry().registerEvents(new CommHandler(this.nettyServer));
        this.nettyServer.getEventRegistry().registerEvents(new TemplateHandler(this.nettyServer));
        this.nettyServer.getEventRegistry().registerEvents(new PlayerHandler(this.nettyServer));
        this.nettyServer.getEventRegistry().registerEvents(new ServerHandler(this.nettyServer));
        this.nettyServer.setPort(CloudConfig.getCfg().get("netty_port").getAsInt()).bind();

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

        this.playerManager = new PlayerManager();
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
        this.nettyServer.shutdown();

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
            if(usingIntRedis) redisServer.stop();
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
