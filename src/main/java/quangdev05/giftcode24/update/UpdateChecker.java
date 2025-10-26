package quangdev05.giftcode24.update;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import quangdev05.giftcode24.GiftCode24;

import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class UpdateChecker implements Listener {
    private final JavaPlugin plugin;
    private final AsyncScheduler asyncScheduler = Bukkit.getAsyncScheduler();

    private final Set<UUID> notifiedOnce = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private boolean consoleReminderStarted = false;
    private ScheduledTask reminderTask;

    public UpdateChecker(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void checkLatestReleaseAsync() {
        boolean checkUpdate = plugin.getConfig().getBoolean("check-update", true);
        if (!checkUpdate) return;

        asyncScheduler.runNow(plugin, scheduledTask -> {
            try {
                String url = "https://api.github.com/repos/quangdev05/GiftCode24/releases/latest";
                HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (connection.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) response.append(line);
                    reader.close();
                    String json = response.toString();

                    String latest = null;
                    String key = "\"tag_name\":\"";
                    int idx = json.indexOf(key);
                    if (idx >= 0) {
                        int start = idx + key.length();
                        int end = json.indexOf('\"', start);
                        if (end > start) latest = json.substring(start, end);
                    }

                    if (latest != null && plugin instanceof GiftCode24 gc) {
                        gc.setLatestVersion(latest);

                        String current = plugin.getDescription().getVersion();
                        if (!latest.equals(current)) {
                            plugin.getLogger().info("Update available: v" + latest +
                                    " (current v" + current + ")");
                        }

                        startConsoleReminder();
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    private void startConsoleReminder() {
        if (consoleReminderStarted) return;
        consoleReminderStarted = true;

        final long PERIOD_MS = 9L * 60L * 1000L;

        reminderTask = asyncScheduler.runAtFixedRate(
                plugin,
                task -> {
                    if (!(plugin instanceof GiftCode24 gc)) return;
                    String latest = gc.getLatestVersion();
                    if (latest == null) return;

                    String current = plugin.getDescription().getVersion();
                    if (!latest.equalsIgnoreCase(current)) {
                        plugin.getLogger().info("Update available: v" + latest +
                                " (current v" + current + ")");
                    }
                },
                PERIOD_MS,
                PERIOD_MS,
                TimeUnit.MILLISECONDS
        );
    }

    @EventHandler
    public void onAdminJoin(PlayerJoinEvent e) {
        if (!(plugin instanceof GiftCode24 gc)) return;

        String latest = gc.getLatestVersion();
        if (latest == null) return;

        String current = plugin.getDescription().getVersion();
        if (latest.equalsIgnoreCase(current)) return;

        var p = e.getPlayer();
        if (!p.hasPermission("giftcode.admin")) return;
        if (!notifiedOnce.add(p.getUniqueId())) return;

        p.sendMessage(ChatColor.YELLOW + "[GiftCode24] A new version is available: "
                + ChatColor.GOLD + "v" + latest + ChatColor.YELLOW
                + " (current " + ChatColor.GRAY + "v" + current + ChatColor.YELLOW + ").");
    }

    public void cancelTasks() {
        if (reminderTask != null) {
            reminderTask.cancel();
            reminderTask = null;
        }
    }
}