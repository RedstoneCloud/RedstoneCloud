package de.redstonecloud;

import de.redstonecloud.broker.BrokerHandler;
import de.redstonecloud.scheduler.TaskScheduler;
import de.redstonecloud.server.ServerLogger;
import de.redstonecloud.server.ServerManager;
import lombok.Getter;
import lombok.Setter;
import redis.broker.Broker;
import redis.cache.Cache;

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

    @Setter protected ServerLogger currentLogServer = null;
    protected ServerManager serverManager;
    protected BufferedWriter logFile;

    protected boolean stopped = false;

    protected final TaskScheduler scheduler;


    public RedstoneCloud() {
        instance = this;
        running = true;

        this.scheduler = new TaskScheduler(new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors()));

        try {
            logFile = new BufferedWriter(new FileWriter("./logs/cloud.log", true));
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Cloud is starting up...");
        setup();
    }


    public void setup(){
        String[] dirs = {"./servers", "./templates", "./tmp", "./logs"};

        for(String dir : dirs) {
            File f = new File(dir);
            if(!f.exists()) {
                f.mkdir();
            }
        }

        /*try {
            this.communicationServer = new CommServer();
            this.communicationServer.start();
            Thread.sleep(1000);
        } catch (Exception e) {
            e.printStackTrace();
            Cloud.getLogger().error("Failed to start communication server with error: " + e.getMessage());
        }*/
        this.serverManager = ServerManager.getInstance();


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
    }

    public void stop() {
        if (this.stopped || !running) {
            return;
        }

        this.stopped = true;
        running = false;

        System.out.println("Cloud is shutting down.");

        try {
            Thread.sleep(200);
            System.out.println("Stopping all servers...");
            boolean a = this.serverManager.stopAll();
            System.out.println(a);
            if(a) System.out.println("Stopped all servers.");
            System.out.println("Cloud successfully shut down.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.exit(0);
    }

}
