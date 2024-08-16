package de.redstonecloud;

import de.redstonecloud.server.ServerLogger;
import de.redstonecloud.server.ServerManager;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import redis.cache.Cache;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class RedstoneCloud {
    public static String workingDir;
    public static Cache cache;

    public static void main(String[] args) {
        workingDir = System.getProperty("user.dir");
        cache = new Cache();


        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("log4j.skipJansi", "false");
        System.setProperty("Dterminal.jline", "true");
        System.setProperty("Dterminal.ansi", "true");
        System.setProperty("Djansi.passthrough", "true");


        RedstoneCloud cloud = new RedstoneCloud();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                cloud.stop();
            }
        });
    }

    @Getter
    public static RedstoneCloud instance;
    @Getter
    public ServerManager serverManager;
    private boolean stopped = false;
    @Setter
    @Getter
    public ServerLogger currentLogServer = null;
    @Getter
    public static boolean running = false;

    @Getter
    public BufferedWriter logFile;

    public RedstoneCloud() {
        instance = this;
        running = true;

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
        if(stopped) return;
        stopped = true;
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
