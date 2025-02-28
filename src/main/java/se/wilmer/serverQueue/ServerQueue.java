package se.wilmer.serverQueue;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.slf4j.Logger;
import se.wilmer.serverQueue.listener.QueueListener;
import se.wilmer.serverQueue.queue.QueueManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Plugin(id = "serverqueue", name = "ServerQueue", version = BuildConstants.VERSION)
public class ServerQueue {
    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private RegisteredServer targetServer;
    private RegisteredServer queueServer;

    @Inject
    public ServerQueue(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        if (!saveDefaultConfig()) {
            return;
        }

        Toml config = new Toml().read(getConfigFile());
        Optional<RegisteredServer> optionalTargetServer = this.server.getServer(config.getString("target-server.server", "main"));
        if (optionalTargetServer.isEmpty()) {
            logger.error("Target server could not be found!");
            return;
        } else {
            targetServer = optionalTargetServer.get();
        }

        Optional<RegisteredServer> optionalQueueServer = this.server.getServer(config.getString("queue.server", "queue"));
        if (optionalQueueServer.isEmpty()) {
            logger.error("Queue server could not be found!");
            return;
        } else {
            queueServer = optionalQueueServer.get();
        }

        QueueManager queueManager = new QueueManager(this, config);
        queueManager.startQueue();

        server.getEventManager().register(this, new QueueListener(this, queueManager));
    }

    public ProxyServer getServer() {
        return server;
    }

    public RegisteredServer getTargetServer() {
        return targetServer;
    }

    public RegisteredServer getQueueServer() {
        return queueServer;
    }

    public File getConfigFile() {
        return new File(dataDirectory.toFile(), "config.toml");
    }

    public boolean saveDefaultConfig() {
        File directory = dataDirectory.toFile();

        if (!directory.exists() && !directory.mkdirs()) {
            logger.error("Could not create file {}", directory.getPath());
            return false;
        }
        File configFile = getConfigFile();

        if (!configFile.exists()) {
            try (InputStream inputStream = getClass().getResourceAsStream("/config.toml")) {
                if (inputStream == null) {
                    logger.error("Could not read config.toml from plugin resources");
                    return false;
                }
                Files.copy(inputStream, configFile.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public Logger getLogger() {
        return logger;
    }
}
