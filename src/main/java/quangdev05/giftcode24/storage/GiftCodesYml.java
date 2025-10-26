package quangdev05.giftcode24.storage;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.inventory.ItemStack;
import quangdev05.giftcode24.model.GiftCode;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GiftCodesYml {
    private final JavaPlugin plugin;
    private File file;
    private FileConfiguration cfg;

    public GiftCodesYml(JavaPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "giftcode.yml");
        if (!file.exists()) {
            file.getParentFile().mkdirs();
            plugin.saveResource("giftcode.yml", false);
        }
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public void reload() {
        this.cfg = YamlConfiguration.loadConfiguration(file);
    }

    public Map<String, GiftCode> loadAll() {
        Map<String, GiftCode> map = new LinkedHashMap<>();
        for (String key : new ArrayList<>(cfg.getKeys(false))) {
            Object messageObj = cfg.get(key + ".message");
            List<String> commands = cfg.getStringList(key + ".commands");
            int maxUses = cfg.getInt(key + ".max-uses");
            String expiry = cfg.getString(key + ".expiry");
            boolean enabled = cfg.getBoolean(key + ".enabled");
            int playerMaxUses = cfg.getInt(key + ".player-max-uses");
            int maxUsesPerIP = cfg.getInt(key + ".player-max-uses-perip");
            int requiredPlaytime = cfg.getInt(key + ".required-playtime");

            List<?> rawItems = cfg.getList(key + ".items");
            List<ItemStack> itemRewards = new ArrayList<>();
            if (rawItems != null) {
                for (Object o : rawItems) if (o instanceof ItemStack) itemRewards.add((ItemStack) o);
            }

            GiftCode giftCode = new GiftCode(commands, messageObj, maxUses, expiry, enabled,
                    playerMaxUses, maxUsesPerIP, requiredPlaytime, itemRewards);

            giftCode.setPermission(cfg.getString(key + ".permission", ""));

            if (cfg.isConfigurationSection(key + ".ip-usage-counts")) {
                ConfigurationSection section = cfg.getConfigurationSection(key + ".ip-usage-counts");
                for (String ip : section.getKeys(false)) {
                    int usage = section.getInt(ip);
                    giftCode.ipUsageCounts.put(ip, usage);
                }
            }
            map.put(key, giftCode);
        }
        return map;
    }

    public void saveAll(Map<String, GiftCode> map) {
        for (String oldKey : new java.util.HashSet<>(cfg.getKeys(false))) {
            cfg.set(oldKey, null);
        }

        for (Map.Entry<String, GiftCode> entry : map.entrySet()) {
            String key = entry.getKey();
            GiftCode gc = entry.getValue();

            cfg.set(key + ".commands", gc.getCommands());
            cfg.set(key + ".message", gc.getMessages());
            cfg.set(key + ".max-uses", gc.getMaxUses());
            cfg.set(key + ".expiry", gc.getExpiry());
            cfg.set(key + ".enabled", gc.isEnabled());
            cfg.set(key + ".player-max-uses", gc.getPlayerMaxUses());
            cfg.set(key + ".player-max-uses-perip", gc.getMaxUsesPerIP());
            cfg.set(key + ".required-playtime", gc.getRequiredPlaytime());
            cfg.set(key + ".permission", gc.getPermission());

            cfg.set(key + ".items", gc.getItemRewards());

            cfg.set(key + ".ip-usage-counts", null);
            for (Map.Entry<String, Integer> ipEntry : gc.ipUsageCounts.entrySet()) {
                cfg.set(key + ".ip-usage-counts." + ipEntry.getKey(), ipEntry.getValue());
            }
        }

        try {
            cfg.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getConfig() { return cfg; }
}
