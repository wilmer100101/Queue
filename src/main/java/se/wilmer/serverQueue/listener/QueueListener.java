package se.wilmer.serverQueue.listener;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.player.ServerConnectedEvent;
import se.wilmer.serverQueue.ServerQueue;
import se.wilmer.serverQueue.queue.QueueManager;

public class QueueListener {
    private final ServerQueue plugin;
    private final QueueManager queueManager;

    public QueueListener(ServerQueue plugin, QueueManager queueManager) {
        this.plugin = plugin;
        this.queueManager = queueManager;
    }

    @Subscribe
    public void onServerConnected(ServerConnectedEvent event) {
        if (!event.getServer().getServerInfo().getAddress().equals(plugin.getQueueServer().getServerInfo().getAddress())) {
            return;
        }

        queueManager.addToQueue(event.getPlayer());
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        queueManager.removeFromQueue(event.getPlayer().getUniqueId());
    }
}
