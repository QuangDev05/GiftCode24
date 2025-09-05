package quangdev05.giftcode24.util;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * SchedulerCompat: chạy được trên Spigot/Paper/Folia/Arclight mà không cần import Paper/Folia.
 * - runAtPlayer: ưu tiên EntityScheduler của Folia nếu có; fallback Bukkit scheduler sync.
 * - runConsole: ưu tiên GlobalRegionScheduler nếu có; fallback Bukkit scheduler sync.
 * - runAsync: luôn chạy async bằng Bukkit scheduler.
 */
public final class SchedulerCompat {
    private SchedulerCompat() {}

    public static void runAtPlayer(JavaPlugin plugin, Player player, Runnable job) {
        // Thử dùng Folia EntityScheduler qua reflection
        try {
            Method getSch = player.getClass().getMethod("getScheduler");
            Object entityScheduler = getSch.invoke(player);
            if (entityScheduler != null) {
                // Tìm method run(Plugin, Consumer, Object?) và gọi
                Method run = null;
                for (Method m : entityScheduler.getClass().getMethods()) {
                    if (!m.getName().equals("run")) continue;
                    Class<?>[] p = m.getParameterTypes();
                    if (p.length == 3 && JavaPlugin.class.isAssignableFrom(p[0]) && Consumer.class.isAssignableFrom(p[1])) {
                        run = m;
                        break;
                    }
                }
                if (run != null) {
                    Consumer<Object> consumer = (ignored) -> job.run();
                    run.invoke(entityScheduler, plugin, consumer, null);
                    return;
                }
            }
        } catch (Throwable ignore) {
            // Bỏ qua, fallback dưới
        }
        // Fallback: Bukkit sync
        Bukkit.getScheduler().runTask(plugin, job);
    }

    public static void runConsole(JavaPlugin plugin, Runnable job) {
        // Thử dùng GlobalRegionScheduler qua reflection (Folia/Paper)
        try {
            Method mGetGRS = Bukkit.class.getMethod("getGlobalRegionScheduler");
            Object grs = mGetGRS.invoke(null);
            if (grs != null) {
                // Tìm method execute(Plugin, Consumer) hoặc run(Plugin, Consumer)
                for (Method m : grs.getClass().getMethods()) {
                    if ((m.getName().equals("execute") || m.getName().equals("run"))
                        && m.getParameterCount() == 2
                        && JavaPlugin.class.isAssignableFrom(m.getParameterTypes()[0])
                        && Consumer.class.isAssignableFrom(m.getParameterTypes()[1])) {
                        Consumer<Object> consumer = (ignored) -> job.run();
                        m.invoke(grs, plugin, consumer);
                        return;
                    }
                }
            }
        } catch (Throwable ignore) {
            // bỏ qua
        }
        // Fallback: Bukkit sync
        Bukkit.getScheduler().runTask(plugin, job);
    }

    public static void runAsync(JavaPlugin plugin, Runnable job) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, job);
    }
}
