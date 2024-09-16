package de.redstonecloud.cloud.utils;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.config.CloudConfig;
import de.redstonecloud.cloud.logger.Logger;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class Utils {
    public static String[] dropFirstString(String[] input) {
        String[] anstring = new String[input.length - 1];
        System.arraycopy(input, 1, anstring, 0, input.length - 1);
        return anstring;
    }

    public static String readFileFromResources(String filename) throws IOException {
        // Use getResourceAsStream to read the file from the JAR or file system
        try (InputStream inputStream = RedstoneCloud.class.getClassLoader().getResourceAsStream(filename)) {
            if (inputStream == null) {
                throw new IllegalArgumentException("File not found! " + filename);
            }
            // Use a Scanner to read the InputStream as a String
            try (Scanner scanner = new Scanner(inputStream, StandardCharsets.UTF_8.name())) {
                return scanner.useDelimiter("\\A").next(); // Read entire file as a single string
            }
        }
    }

    public static URL getResourceFile(String filename) {
        return Utils.class.getClassLoader().getResource(filename);
    }

    public static void copyFolderFromCurrentJar(String folderInJar, File destDir) throws IOException, URISyntaxException {
        URL jarUrl = RedstoneCloud.class.getProtectionDomain().getCodeSource().getLocation();
        File jarFile = new File(jarUrl.toURI());

        if (jarFile.isFile()) {
            // If the application is running from a JAR file
            try (JarFile jar = new JarFile(jarFile)) {
                Enumeration<JarEntry> entries = jar.entries();

                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // Check if the entry is part of the folder you want to copy
                    if (entryName.startsWith(folderInJar)) {
                        File destFile = new File(destDir, entryName.substring(folderInJar.length()));
                        if (entry.isDirectory()) {
                            // Create the directory
                            FileUtils.forceMkdir(destFile);
                        } else {
                            // Copy the file
                            try (InputStream is = jar.getInputStream(entry)) {
                                FileUtils.copyInputStreamToFile(is, destFile);
                            }
                        }
                    }
                }
            }
        } else {
            throw new IllegalStateException("Not running from a JAR file");
        }
    }

    public static void setup() {
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
            l.setup("RC Setup", "§cThere is a redis update avaiable (Redis 7.2). Do you want to download it? Otherwise, outdated Redis 2.8 will be used. §3[y/n] §a(default: y)");
            result = input.nextLine();
            if(result.toLowerCase().contains("n")) downloadRedis = false;
            System.out.println();

             */

            //TODO: DOWNLOAD REDIS
        } else {
            //TODO: CUSTOM REDIS INSTANCE
        }

        l.setup("RC Setup", "§eSettings completed. Generating basic file structure...", true);
        RedstoneCloud.createBaseFolders();
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
