package quangdev05.giftcode24.database;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataRepository {

    private final JavaPlugin plugin;
    private final DatabaseManager databaseManager;

    private final Map<UUID, PlayerData> playerDataCache = new ConcurrentHashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 30000;

    public PlayerDataRepository(JavaPlugin plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }

    private void loadCacheIfNeeded() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate > CACHE_DURATION || playerDataCache.isEmpty()) {
            playerDataCache.clear();
            playerDataCache.putAll(loadAllPlayerDataFromDB());
            lastCacheUpdate = currentTime;
        }
    }

    private Map<UUID, PlayerData> loadAllPlayerDataFromDB() {
        Map<UUID, PlayerData> data = new ConcurrentHashMap<>();
        String sql = "SELECT * FROM player_data";

        try (Connection conn = databaseManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                UUID uuid = UUID.fromString(rs.getString("uuid"));
                String ip = rs.getString("ip");

                List<String> usedCodes = parseList(rs.getString("used_codes"));
                List<String> assignedCodes = parseList(rs.getString("assigned_codes"));

                data.put(uuid, new PlayerData(ip, usedCodes, assignedCodes));
            }
        } catch (SQLException e) {
            plugin.getLogger().warning("Error while loading player_data: " + e.getMessage());
        }
        return data;
    }

    private List<String> parseList(String str) {
        if (str == null || str.isEmpty()) return new ArrayList<>();
        return new ArrayList<>(Arrays.asList(str.split(",")));
    }

    public List<String> getUsedCodes(UUID uuid) {
        loadCacheIfNeeded();
        PlayerData data = playerDataCache.get(uuid);
        return data != null ? new ArrayList<>(data.getUsedCodes()) : new ArrayList<>();
    }

    public void addUsedCode(Player player, String code) {
        UUID uuid = player.getUniqueId();
        List<String> usedCodes = getUsedCodes(uuid);
        usedCodes.add(code);
        String playerIp = getPlayerIP(player);

        String sql = """
            INSERT INTO player_data (uuid, ip, used_codes)
            VALUES (?, ?, ?)
            ON DUPLICATE KEY UPDATE ip = VALUES(ip), used_codes = VALUES(used_codes)
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String usedCodesStr = String.join(",", usedCodes);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, playerIp);
            stmt.setString(3, usedCodesStr);
            stmt.executeUpdate();

            PlayerData cached = playerDataCache.computeIfAbsent(uuid, k -> new PlayerData(playerIp, new ArrayList<>(), new ArrayList<>()));
            cached.setIp(playerIp);
            cached.getUsedCodes().add(code);

        } catch (SQLException e) {
            plugin.getLogger().warning("Error when adding used code for " + uuid + ": " + e.getMessage());
        }
    }

    private String getPlayerIP(Player player) {
        try {
            if (player.getAddress() != null && player.getAddress().getAddress() != null)
                return player.getAddress().getAddress().getHostAddress();
        } catch (Exception ignored) {}
        return "";
    }

    public String getPlayerIP(UUID uuid) {
        loadCacheIfNeeded();
        PlayerData data = playerDataCache.get(uuid);
        return data != null ? data.getIp() : "";
    }

    public List<String> getAssignedCodes(UUID uuid) {
        loadCacheIfNeeded();
        PlayerData data = playerDataCache.get(uuid);
        return data != null ? new ArrayList<>(data.getAssignedCodes()) : new ArrayList<>();
    }

    public void addAssignedCode(UUID uuid, String code) {
        List<String> assignedCodes = getAssignedCodes(uuid);
        assignedCodes.add(code);

        String sql = """
            INSERT INTO player_data (uuid, assigned_codes)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE assigned_codes = VALUES(assigned_codes)
            """;

        try (Connection conn = databaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            String assignedCodesStr = String.join(",", assignedCodes);
            stmt.setString(1, uuid.toString());
            stmt.setString(2, assignedCodesStr);
            stmt.executeUpdate();

            PlayerData cached = playerDataCache.computeIfAbsent(uuid, k -> new PlayerData("", new ArrayList<>(), new ArrayList<>()));
            cached.getAssignedCodes().add(code);

        } catch (SQLException e) {
            plugin.getLogger().warning("Error when adding assigned code for " + uuid + ": " + e.getMessage());
        }
    }

    public ConfigurationSection getPlayersSection() {
        loadCacheIfNeeded();
        YamlConfiguration config = new YamlConfiguration();

        for (Map.Entry<UUID, PlayerData> entry : playerDataCache.entrySet()) {
            String base = "players." + entry.getKey();
            PlayerData data = entry.getValue();

            config.set(base + ".ip", data.getIp());
            config.set(base + ".usedCodes", new ArrayList<>(data.getUsedCodes()));
            config.set(base + ".assignedCodes", new ArrayList<>(data.getAssignedCodes()));
        }

        return config.getConfigurationSection("players");
    }

    public Map<UUID, PlayerData> getAllPlayerData() {
        loadCacheIfNeeded();
        return new ConcurrentHashMap<>(playerDataCache);
    }

    public static class PlayerData {
        private String ip;
        private final List<String> usedCodes;
        private final List<String> assignedCodes;

        public PlayerData(String ip, List<String> usedCodes, List<String> assignedCodes) {
            this.ip = ip != null ? ip : "";
            this.usedCodes = usedCodes != null ? new ArrayList<>(usedCodes) : new ArrayList<>();
            this.assignedCodes = assignedCodes != null ? new ArrayList<>(assignedCodes) : new ArrayList<>();
        }

        public String getIp() { return ip; }
        public void setIp(String ip) { this.ip = ip; }
        public List<String> getUsedCodes() { return usedCodes; }
        public List<String> getAssignedCodes() { return assignedCodes; }
    }
}
