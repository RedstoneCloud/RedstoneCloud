package de.redstonecloud.cloud.plugin;


import lombok.extern.log4j.Log4j2;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

@Log4j2
public class PluginLoader {

    private final PluginManager pluginManager;

    public PluginLoader(PluginManager pluginManager) {
        this.pluginManager = pluginManager;
    }

    protected static boolean isJarFile(Path file) {
        return file.getFileName().toString().endsWith(".jar");
    }

    protected PluginClassLoader loadClassLoader(PluginData pluginConfig, File pluginJar) {
        try {
            return new PluginClassLoader(this.pluginManager, this.getClass().getClassLoader(), pluginJar);
        } catch (MalformedURLException e) {
            log.error("Error while creating class loader(plugin={})", pluginConfig.getName());
        }
        return null;
    }

    protected Plugin loadPluginJAR(PluginData pluginConfig, File pluginJar, PluginClassLoader loader) {
        try {
            Class<?> mainClass = loader.loadClass(pluginConfig.getMain());
            if (!Plugin.class.isAssignableFrom(mainClass)) {
                return null;
            }

            Class<? extends Plugin> castedMain = mainClass.asSubclass(Plugin.class);
            Plugin plugin = castedMain.getDeclaredConstructor().newInstance();
            plugin.init(pluginConfig, this.pluginManager.getCloud(), pluginJar);
            return plugin;
        } catch (Exception e) {
            log.error("Error while loading plugin main class(main={}, plugin={})", pluginConfig.getMain(), pluginConfig.getName(), e);
        }
        return null;
    }

    protected PluginData loadPluginData(File file, Yaml yaml) {
        try (JarFile pluginJar = new JarFile(file)) {
            JarEntry configEntry = pluginJar.getJarEntry("redstonecloud.yml");
            if (configEntry == null) {
                configEntry = pluginJar.getJarEntry("plugin.yml");
            }

            if (configEntry == null) {
                log.warn("Jar file " + file.getName() + " doesnt contain a redstonecloud.yml or plugin.yml!");
                return null;
            }

            try (InputStream fileStream = pluginJar.getInputStream(configEntry)) {
                PluginData pluginConfig = yaml.loadAs(fileStream, PluginData.class);
                if (pluginConfig.getMain() != null && pluginConfig.getName() != null) {
                    // Valid plugin.yml, main and name set
                    return pluginConfig;
                }
            }
            log.warn("Invalid plugin.yml for " + file.getName() + ": main and/or name property missing");
        } catch (Exception e) {
            log.error("Can not load plugin files in " + file.getPath(), e);
        }
        return null;
    }
}
