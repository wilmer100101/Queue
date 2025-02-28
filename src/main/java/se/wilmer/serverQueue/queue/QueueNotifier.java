package se.wilmer.serverQueue.queue;

import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Nullable;
import se.wilmer.serverQueue.ServerQueue;

import java.util.Queue;
import java.util.UUID;

public class QueueNotifier {
    private final ServerQueue plugin;
    private final QueueManager queueManager;

    public QueueNotifier(ServerQueue plugin, QueueManager queueManager) {
        this.plugin = plugin;
        this.queueManager = queueManager;
    }

    public void notifyPlayerPositions() {
        final ProxyServer proxyServer = plugin.getServer();

        final Queue<UUID> queue = queueManager.getQueue();
        int queueSize = queue.size();
        int position = 0;
        for (UUID uuid : queue) {
            position++;

            final int currentPosition = position;
            proxyServer.getPlayer(uuid).ifPresent(player -> player.sendActionBar(Component.text("You are in place " + currentPosition + " in the queue").color(generatePositionColor(currentPosition, queueSize))));
        }
    }

    public static TextColor generatePositionColor(int number, int amount) {
        double position = (double) number / amount;
        position = Math.max(0, Math.min(1, position));
        int red = (int) (255 * position);
        int green = 255 - red;
        return TextColor.fromHexString(String.format("#%02X%02X00", red, green));
    }
}
