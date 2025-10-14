package quangdev05.giftcode24;

import org.bstats.bukkit.Metrics;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import quangdev05.giftcode24.commands.GiftCodeAdminCommand;
import quangdev05.giftcode24.commands.RedeemCodeCommand;
import quangdev05.giftcode24.gui.GiftCodeListGUI;
import quangdev05.giftcode24.gui.GiftItemEditorGUI;
import quangdev05.giftcode24.manager.GiftCodeManager;
import quangdev05.giftcode24.storage.GiftCodesYml;
import quangdev05.giftcode24.storage.PlayerDataYml;
import quangdev05.giftcode24.storage.IpUsageYml;
import quangdev05.giftcode24.update.UpdateChecker;

import java.util.*;

public class GiftCode24 extends JavaPlugin implements Listener {

    private GiftCodesYml giftCodesYml;
    private PlayerDataYml playerDataYml;
    private IpUsageYml ipUsageYml;
    private GiftCodeManager giftCodeManager;
    private GiftCodeListGUI giftCodeListGUI;
    private GiftItemEditorGUI giftItemEditorGUI;
    private UpdateChecker updateChecker;

    private volatile String latestVersion;

    public String getLatestVersion() {
        return latestVersion;
    }

    public void setLatestVersion(String v) {
        this.latestVersion = v;
    }

    @Override
    public void onEnable() {
        saveDefaultConfig();

        // Storage
        this.giftCodesYml = new GiftCodesYml(this);
        this.playerDataYml = new PlayerDataYml(this);
        this.ipUsageYml = new IpUsageYml(this);

        // Manager
        this.giftCodeManager = new GiftCodeManager(this, giftCodesYml, playerDataYml, ipUsageYml);

        // CHẠY MIGRATION - THÊM ĐOẠN NÀY
        if (!getConfig().getBoolean("ip-migration-completed", false)) {
            getLogger().info("Starting IP usage data migration...");
            migrateIpUsageData();
        }

        // GUI
        this.giftCodeListGUI = new GiftCodeListGUI(this, giftCodeManager);
        this.giftItemEditorGUI = new GiftItemEditorGUI(this, giftCodeManager);
        getServer().getPluginManager().registerEvents(giftCodeListGUI, this);
        getServer().getPluginManager().registerEvents(giftItemEditorGUI, this);

        // Commands
        GiftCodeAdminCommand adminCmd = new GiftCodeAdminCommand(this, giftCodeManager, giftCodeListGUI);
        getCommand("giftcode").setExecutor(adminCmd);
        getCommand("giftcode").setTabCompleter(adminCmd);

        RedeemCodeCommand redeemCmd = new RedeemCodeCommand(this, giftCodeManager, playerDataYml);
        getCommand("code").setExecutor(redeemCmd);

        sendFancyMessage();

        // Update check (async)
        this.updateChecker = new UpdateChecker(this);
        getServer().getPluginManager().registerEvents(updateChecker, this);
        updateChecker.checkLatestReleaseAsync();

        // bStats (keep plugin id from legacy)
        int pluginId = 24198;
        Metrics metrics = new Metrics(this, pluginId);

        // Folia supported
        if (isFolia()) {
            getLogger().info("Running on Folia! Multithreading support enabled");
        }
    }

    private void migrateIpUsageData() {
        getLogger().info("Starting IP usage data migration from dataplayer.yml to ipusage.yml...");
        int totalMigrated = 0;

        // Duyệt qua tất cả gift codes
        for (String code : giftCodeManager.getAll().keySet()) {
            Map<String, Integer> ipCounts = new HashMap<>();

            // Duyệt qua toàn bộ player data để tính IP counts cho code này
            if (playerDataYml.getPlayersSection() != null) {
                for (String uuidStr : playerDataYml.getPlayersSection().getKeys(false)) {
                    // Lấy IP của player
                    String ip = playerDataYml.getPlayersSection().getString(uuidStr + ".ip");
                    if (ip == null || ip.isEmpty()) continue;

                    // Lấy danh sách codes đã dùng
                    List<String> usedCodes = playerDataYml.getPlayersSection().getStringList(uuidStr + ".usedCodes");

                    // Đếm số lần code này được dùng bởi IP này
                    int count = Collections.frequency(usedCodes, code);
                    if (count > 0) {
                        ipCounts.put(ip, ipCounts.getOrDefault(ip, 0) + count);
                        totalMigrated += count;
                    }
                }
            }

            // Cập nhật vào ipUsageYml
            for (Map.Entry<String, Integer> entry : ipCounts.entrySet()) {
                String ip = entry.getKey();
                int count = entry.getValue();
                // Set trực tiếp count
                for (int i = 0; i < count; i++) {
                    ipUsageYml.incrementIpUsage(code, ip);
                }
            }

            if (!ipCounts.isEmpty()) {
                getLogger().info("Migrated IP data for code " + code + ": " + ipCounts.size() + " IPs");
            }
        }

        // Lưu ipUsageYml
        ipUsageYml.saveAll();

        // Đánh dấu đã migration
        getConfig().set("ip-migration-completed", true);
        saveConfig();

        getLogger().info("IP usage migration completed! Migrated " + totalMigrated + " IP-code relationships.");
    }

    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    public IpUsageYml getIpUsageYml() {
        return ipUsageYml;
    }

    @Override
    public void onDisable() {
        // Đóng UpdateChecker
        if (updateChecker != null) {
            updateChecker.cancelTasks();
        }
        // Persist current codes to file
        if (giftCodesYml != null && giftCodeManager != null) {
            giftCodesYml.saveAll(giftCodeManager.getAll());
        }
        // Lưu IP usage data
        if (ipUsageYml != null) {
            ipUsageYml.saveAll();
        }
    }

    private void sendFancyMessage() {
        getLogger().info(" ");
        getLogger().info(" ██████╗ ██╗███████╗████████╗ ██████╗ ██████╗ ██████╗ ███████╗██████╗ ██╗  ██╗");
        getLogger().info("██╔════╝ ██║██╔════╝╚══██╔══╝██╔════╝██╔═══██╗██╔══██╗██╔════╝╚════██╗██║  ██║");
        getLogger().info("██║  ███╗██║█████╗     ██║   ██║     ██║   ██║██║  ██║█████╗   █████╔╝███████║");
        getLogger().info("██║   ██║██║██╔══╝     ██║   ██║     ██║   ██║██║  ██║██╔══╝  ██╔═══╝ ╚════██║");
        getLogger().info("╚██████╔╝██║██║        ██║   ╚██████╗╚██████╔╝██████╔╝███████╗███████╗     ██║");
        getLogger().info(" ╚═════╝ ╚═╝╚═╝        ╚═╝    ╚═════╝ ╚═════╝ ╚═════╝ ╚══════╝╚══════╝     ╚═╝");
        getLogger().info(" ");
        getLogger().info("  Author: QuangDev05");
        getLogger().info("  Current version: " + getDescription().getVersion());
    }

    public GiftCodeManager getGiftCodeManager() {
        return giftCodeManager;
    }

    public PlayerDataYml getPlayerDataYml() {
        return playerDataYml;
    }

    public GiftCodesYml getGiftCodesYml() {
        return giftCodesYml;
    }

    public GiftItemEditorGUI getGiftItemEditorGUI() {
        return giftItemEditorGUI;
    }
}