package de.redstonecloud.cloud.plugin;

import de.redstonecloud.cloud.RedstoneCloud;
import de.redstonecloud.cloud.logger.Logger;
import it.unimi.dsi.fastutil.Pair;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectObjectImmutablePair;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.CustomClassLoaderConstructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class PluginManager {

    public static final Yaml yamlLoader;
    static {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        yamlLoader = new Yaml(new CustomClassLoaderConstructor(PluginManager.class.getClassLoader()), representer);
    }

    private final RedstoneCloud cloud;
    private final PluginLoader pluginLoader;

    protected final Object2ObjectMap<String, PluginClassLoader> pluginClassLoaders = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<String, Plugin> pluginMap = new Object2ObjectArrayMap<>();
    private final Object2ObjectMap<String, Class<?>> cachedClasses = new Object2ObjectArrayMap<>();

    private final List<Pair<PluginData, Path>> pluginsToLoad = new ObjectArrayList<>();

    public PluginManager(RedstoneCloud cloud) {
        this.cloud = cloud;
        this.pluginLoader = new PluginLoader(this);
        try {
            this.loadPluginsInside(Paths.get(RedstoneCloud.getWorkingDir() + "/plugins/"));
        } catch (IOException e) {
            Logger.getInstance().error("Error while filtering plugin files: " + e);
        }
    }

    private void loadPluginsInside(Path folderPath) throws IOException {
        Comparator<PluginData> comparator = (o1, o2) -> {
            if (o2.getName().equals(o1.getName())) {
                return 0;
            }
            if (o2.getDepends() == null) {
                return 1;
            }
            return o2.getDepends().contains(o1.getName()) ? -1 : 1;
        };

        Map<PluginData, Path> plugins = new TreeMap<>(comparator);
        try (Stream<Path> stream = Files.walk(folderPath)){
            stream.filter(Files::isRegularFile).filter(PluginLoader::isJarFile).forEach(jarPath -> {
                PluginData config = this.loadPluginConfig(jarPath);
                if (config != null) {
                    plugins.put(config, jarPath);
                }
            });
        }
        plugins.forEach(this::registerClassLoader);
    }

    private PluginData loadPluginConfig(Path path) {
        if (!Files.isRegularFile(path) || !PluginLoader.isJarFile(path)) {
            Logger.getInstance().warning("Cannot load plugin: Provided file is no jar file: " + path.getFileName());
            return null;
        }

        File pluginFile = path.toFile();
        if (!pluginFile.exists()) {
            return null;
        }
        return this.pluginLoader.loadPluginData(pluginFile, yamlLoader);
    }

    private PluginClassLoader registerClassLoader(PluginData config, Path path) {
        if (this.getPluginByName(config.getName()) != null) {
            Logger.getInstance().warning("Plugin is already loaded: " + config.getName());
            return null;
        }

        PluginClassLoader classLoader = this.pluginLoader.loadClassLoader(config, path.toFile());
        if (classLoader != null) {
            this.pluginClassLoaders.put(config.getName(), classLoader);
            this.pluginsToLoad.add(ObjectObjectImmutablePair.of(config, path));
            Logger.getInstance().debug("Loaded class loader from " + path.getFileName());
        }
        return classLoader;
    }

    public void loadAllPlugins() {
        for (Pair<PluginData, Path> pair : this.pluginsToLoad) {
            this.loadPlugin(pair.key(), pair.value());
        }
        this.pluginsToLoad.clear();
    }

    public Plugin loadPlugin(PluginData config, Path path) {
        File pluginFile = path.toFile();
        if (this.getPluginByName(config.getName()) != null) {
            Logger.getInstance().warning("Plugin is already loaded: " + config.getName());
            return null;
        }

        PluginClassLoader classLoader = this.pluginClassLoaders.get(config.getName());
        if (classLoader == null) {
            classLoader = this.registerClassLoader(config, path);
        }

        if (classLoader == null) {
            return null;
        }

        Plugin plugin = this.pluginLoader.loadPluginJAR(config, pluginFile, classLoader);
        if (plugin == null) {
            return null;
        }

        try {
            plugin.onLoad();
        } catch (Exception e) {
            Logger.getInstance().error("Failed to load plugin " + config.getName() + ": " + e);
            return null;
        }

        Logger.getInstance().info("Loaded plugin " + config.getName() + " successfully!");
        this.pluginMap.put(config.getName(), plugin);
        return plugin;
    }

    public void enableAllPlugins() {
        LinkedList<Plugin> failed = new LinkedList<>();

        for (Plugin plugin : this.pluginMap.values()) {
            if (!this.enablePlugin(plugin, null)) {
                failed.add(plugin);
            }
        }

        if (failed.isEmpty()) {
            return;
        }

        StringBuilder builder = new StringBuilder("§cFailed to load plugins: §e");
        while (failed.peek() != null) {
            Plugin plugin = failed.poll();
            builder.append(plugin.getName());
            if (failed.peek() != null) {
                builder.append(", ");
            }
        }
        Logger.getInstance().warning(builder.toString());
    }

    public boolean enablePlugin(Plugin plugin, String parent) {
        if (plugin.isEnabled()) return true;
        String pluginName = plugin.getName();

        if (plugin.getDescription().getDepends() != null) {
            for (String depend : plugin.getDescription().getDepends()) {
                if (depend.equals(parent)) {
                    Logger.getInstance().warning("§cCan not enable plugin " + pluginName + " circular dependency " + parent + "!");
                    return false;
                }

                Plugin dependPlugin = this.getPluginByName(depend);
                if (dependPlugin == null) {
                    Logger.getInstance().warning("§cCan not enable plugin " + pluginName + " missing dependency " + depend + "!");
                    return false;
                }

                if (!dependPlugin.isEnabled() && !this.enablePlugin(dependPlugin, pluginName)) {
                    return false;
                }
            }
        }

        try {
            plugin.setEnabled(true);
        } catch (Exception e) {
            Logger.getInstance().error(e.getMessage());
            return false;
        }
        return true;
    }

    public void disableAllPlugins() {
        for (Plugin plugin : this.pluginMap.values()) {
            Logger.getInstance().info("Disabling plugin " + plugin.getName() + "!");
            try {
                plugin.setEnabled(false);
            } catch (Exception e) {
                Logger.getInstance().error(e.getMessage());
            }
        }
    }

    public Class<?> getClassFromCache(String className) {
        Class<?> clazz = this.cachedClasses.get(className);
        if (clazz != null) {
            return clazz;
        }

        for (PluginClassLoader loader : this.pluginClassLoaders.values()) {
            try {
                if ((clazz = loader.findClass(className, false)) != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException e) {
                //ignore
            }
        }
        return null;
    }

    protected void cacheClass(String className, Class<?> clazz) {
        this.cachedClasses.putIfAbsent(className, clazz);
    }

    public Map<String, Plugin> getPluginMap() {
        return Collections.unmodifiableMap(this.pluginMap);
    }

    public Collection<Plugin> getPlugins() {
        return Collections.unmodifiableCollection(this.pluginMap.values());
    }

    public Collection<PluginClassLoader> getPluginClassLoaders() {
        return Collections.unmodifiableCollection(this.pluginClassLoaders.values());
    }

    public Plugin getPluginByName(String pluginName) {
        return this.pluginMap.getOrDefault(pluginName, null);
    }

    public RedstoneCloud getCloud() {
        return this.cloud;
    }
}