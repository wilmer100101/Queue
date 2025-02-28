package se.wilmer.serverQueue.queue;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import se.wilmer.serverQueue.ServerQueue;

import java.util.*;

public class QueueTransfer {
    private static final int MAX_RECONNECTING_ATTEMPTS = 3;

    private final ServerQueue plugin;
    private final Map<UUID, Integer> reconnectingPlayerAttempts = new HashMap<>();
    private final List<UUID> reconnectingPlayers = new ArrayList<>();

    public QueueTransfer(ServerQueue plugin) {
        this.plugin = plugin;
    }

    public void transferPlayer(Player player) {
        final UUID uuid = player.getUniqueId();
        final RegisteredServer targetServer = plugin.getTargetServer();

        reconnectingPlayerAttempts.merge(uuid, 1, Integer::sum);
        reconnectingPlayers.add(uuid);

        boolean success = player.createConnectionRequest(targetServer).connectWithIndication().join();
        if (success) {
            plugin.getLogger().info("Successfully transfer player: {}", player.getUsername());
            reconnectingPlayerAttempts.remove(uuid);
        } else {
            plugin.getLogger().warn("Failed to transfer player: {}", player.getUsername());
        }
        reconnectingPlayers.remove(uuid);
    }

    public void transferFailedPlayers() {
        final ProxyServer server = plugin.getServer();

        List<UUID> playersToTransfer = new ArrayList<>();
        List<UUID> playersToRemove = new ArrayList<>();
        reconnectingPlayerAttempts.forEach((uuid, attempts) -> {
            if (reconnectingPlayers.contains(uuid)) {
                return;
            }

            if (attempts > MAX_RECONNECTING_ATTEMPTS) {
                playersToRemove.add(uuid);
            }

            Optional<Player> optionalPlayer = server.getPlayer(uuid);
            if (optionalPlayer.isEmpty()) {
                reconnectingPlayers.remove(uuid);
                playersToRemove.add(uuid);
                return;
            }

            playersToTransfer.add(uuid);
        });

        playersToTransfer.forEach(uuid -> server.getPlayer(uuid).ifPresent(this::transferPlayer));

        playersToRemove.forEach(uuid -> {
            server.getPlayer(uuid).ifPresent(player -> player.disconnect(Component.text("We could not connect you to the target server. Please try again later.")));
            plugin.getLogger().warn("Failed to transfer player {}, after {} attempts", uuid, MAX_RECONNECTING_ATTEMPTS);

            reconnectingPlayerAttempts.remove(uuid);
        });
    }

}
