package se.wilmer.serverQueue.queue;

import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.proxy.Player;
import se.wilmer.serverQueue.ServerQueue;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class QueueManager {
    private final ServerQueue plugin;
    private final QueueNotifier queueNotifier;
    private final QueueTransfer queueTransfer;
    private final QueueSelector queueSelector;
    private final Queue<UUID> queue = new LinkedList<>();
    private final long maxPlayers;
    private final String skipQueuePermission;
    private final long queueUpdateIntervalSeconds;

    public QueueManager(ServerQueue plugin, Toml config) {
        this.plugin = plugin;

        queueNotifier = new QueueNotifier(plugin, this);
        queueTransfer = new QueueTransfer(plugin);
        queueSelector = new QueueSelector(plugin, this);

        maxPlayers = config.getLong("target-server.max-players", 20L);
        skipQueuePermission = config.getString("queue.skip-permission", "queue.skip");
        queueUpdateIntervalSeconds = config.getLong("queue.update-interval-seconds", 10L);
    }

    public void startQueue() {
        plugin.getServer().getScheduler().buildTask(plugin, () -> {
            queueSelector.selectQueuedPlayers();
            queueTransfer.transferFailedPlayers();
            queueNotifier.notifyPlayerPositions();
        }).repeat(queueUpdateIntervalSeconds, TimeUnit.SECONDS).schedule();
    }

    public void addToQueue(Player player) {
        if (player.hasPermission(skipQueuePermission)) {
            queueTransfer.transferPlayer(player);
            return;
        }

        queue.add(player.getUniqueId());
    }

    public void removeFromQueue(UUID uuid) {
        queue.remove(uuid);
    }

    public Queue<UUID> getQueue() {
        return queue;
    }

    public long getMaxPlayers() {
        return maxPlayers;
    }

    public QueueTransfer getQueueTransfer() {
        return queueTransfer;
    }
}
