package de.redstonecloud.cloud.utils;

import de.redstonecloud.cloud.RedstoneCloud;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
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
}
