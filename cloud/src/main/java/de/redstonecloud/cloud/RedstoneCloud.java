package de.redstonecloud.cloud;

import de.pierreschwang.nettypacket.event.EventRegistry;
import de.redstonecloud.cloud.events.EventManager;
import de.redstonecloud.cloud.plugin.PluginManager;
import de.redstonecloud.cloud.server.ServerLogger;
import de.redstonecloud.cloud.broker.BrokerHandler;
import de.redstonecloud.cloud.commands.CommandManager;
import de.redstonecloud.cloud.console.Console;
import de.redstonecloud.cloud.scheduler.TaskScheduler;
import de.redstonecloud.cloud.scheduler.defaults.CheckTemplateTask;
import de.redstonecloud.cloud.server.ServerManager;
import lombok.Getter;
import lombok.Setter;
import de.redstonecloud.api.redis.broker.Broker;
import de.redstonecloud.api.redis.cache.Cache;
import netty.NettyHelper;
import netty.server.NettyServer;
import netty.server.handler.NettyEventHandler;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.ScheduledThreadPoolExecutor;


@Getter
public class RedstoneCloud {
    @Getter private static RedstoneCloud instance;
    @Getter public static String workingDir;
    @Getter public static Cache cache;
    @Getter public static boolean running = false;

    public static void main(String[] args) {
        workingDir = System.getProperty("user.dir");
        cache = new Cache();

        Broker broker = new Broker("cloud", "cloud");
        broker.listen("cloud", BrokerHandler::handle);

        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("log4j.skipJansi", "false");
        System.setProperty("Dterminal.jline", "true");
        System.setProperty("Dterminal.ansi", "true");
        System.setProperty("Djansi.passthrough", "true");

        RedstoneCloud cloud = new RedstoneCloud();

        Runtime.getRuntime().addShutdownHook(new Thread(cloud::stop));
    }

    private ConsoleThread consoleThread;
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

        this.scheduler = new TaskScheduler(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()));

        this.nettyServer = new NettyServer(NettyHelper.constructRegistry(), new EventRegistry());
        this.nettyServer.getEventRegistry().registerEvents(new NettyEventHandler(this.nettyServer));

        try {
            logFile = new BufferedWriter(new FileWriter("./logs/cloud.log", true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Cloud is starting up...");
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


        /*taskManager.runRepeatingTask(new TimerTask() {
            @Override
            public void run() {
                try {
                    logFile.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }, 1000, 1000);*/

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

        System.out.println("Cloud is shutting down.");

        try {
            Thread.sleep(200);
            System.out.println("Stopping all servers...");
            boolean a = this.serverManager.stopAll();
            System.out.println(a);
            if(a) System.out.println("Stopped all servers.");
            this.pluginManager.disableAllPlugins();
            this.eventManager.getThreadedExecutor().shutdown();
            System.out.println("Cloud successfully shut down.");
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
