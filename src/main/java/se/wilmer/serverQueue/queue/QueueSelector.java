package se.wilmer.serverQueue.queue;

import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.ServerPing;
import se.wilmer.serverQueue.ServerQueue;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public class QueueSelector {
    private final ServerQueue plugin;
    private final QueueManager queueManager;

    public QueueSelector(ServerQueue plugin, QueueManager queueManager) {
        this.plugin = plugin;
        this.queueManager = queueManager;
    }

    public void selectQueuedPlayers() {
        getEmptyPlayerSlots().thenAccept(amount -> {
            final ProxyServer server = plugin.getServer();

            for (int i = 0; i < amount; i++) {
                final UUID uuid = queueManager.getQueue().poll();
                if (uuid == null) {
                    break;
                }

                server.getPlayer(uuid).ifPresent(player -> queueManager.getQueueTransfer().transferPlayer(player));
            }
        });
    }

    private CompletableFuture<Long> getEmptyPlayerSlots() {
        return plugin.getTargetServer().ping()
                .thenApply(pingedServer -> {
                    final Optional<ServerPing.Players> optionalPlayers = pingedServer.getPlayers();
                    if (optionalPlayers.isPresent()) {
                        ServerPing.Players players = optionalPlayers.get();
                        return queueManager.getMaxPlayers() - players.getOnline();
                    }
                    return 0L;
                })
                .exceptionally(exception -> {
                    plugin.getLogger().warn("Failed to fetch the target servers empty slots", exception);
                    return 0L;
                });
    }
}
