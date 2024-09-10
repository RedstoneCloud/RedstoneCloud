package de.redstonecloud.cloud;

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
import redis.embedded.RedisServer;
import redis.embedded.RedisServerBuilder;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

    protected final TaskScheduler scheduler;
    protected NettyServer nettyServer;

    public RedstoneCloud() {
        instance = this;
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
        setup();
    }


    public void setup(){
        String[] dirs = {"./servers", "./templates", "./tmp", "./logs", "./plugins"};

        for(String dir : dirs) {
            File f = new File(dir);
            if(!f.exists()) {
                f.mkdir();
            }
        }

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
