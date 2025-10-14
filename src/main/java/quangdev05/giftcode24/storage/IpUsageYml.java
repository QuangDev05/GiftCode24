package quangdev05.giftcode24.storage;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import quangdev05.giftcode24.GiftCode24;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class IpUsageYml {

    private GiftCode24 plugin;
    private File file;
    private FileConfiguration config;
    private Map<String, Map<String, Integer>> ipUsageCache; // code -> (ip -> count)

    public IpUsageYml(GiftCode24 plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "ipusage.yml");
        this.config = YamlConfiguration.loadConfiguration(file);
        this.ipUsageCache = new HashMap<>();
        loadAll();
    }

    public void loadAll() {
        ipUsageCache.clear();
        for (String code : config.getKeys(false)) {
            Map<String, Integer> ipCounts = new HashMap<>();
            for (String ip : config.getConfigurationSection(code).getKeys(false)) {
                ipCounts.put(ip, config.getInt(code + "." + ip));
            }
            ipUsageCache.put(code, ipCounts);
        }
    }

    public void saveAll() {
        // Clear file trước khi save mới
        for (String key : config.getKeys(false)) {
            config.set(key, null);
        }

        for (Map.Entry<String, Map<String, Integer>> codeEntry : ipUsageCache.entrySet()) {
            String code = codeEntry.getKey();
            for (Map.Entry<String, Integer> ipEntry : codeEntry.getValue().entrySet()) {
                config.set(code + "." + ipEntry.getKey(), ipEntry.getValue());
            }
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Could not save ipusage.yml");
        }
    }

    public int getIpUsageCount(String code, String ip) {
        Map<String, Integer> ipCounts = ipUsageCache.get(code);
        if (ipCounts == null) return 0;
        return ipCounts.getOrDefault(ip, 0);
    }

    public void incrementIpUsage(String code, String ip) {
        Map<String, Integer> ipCounts = ipUsageCache.computeIfAbsent(code, k -> new HashMap<>());
        ipCounts.put(ip, ipCounts.getOrDefault(ip, 0) + 1);
    }

    public void reload() {
        loadAll();
    }
}