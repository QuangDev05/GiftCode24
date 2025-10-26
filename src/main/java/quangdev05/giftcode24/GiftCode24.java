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
import quangdev05.giftcode24.update.UpdateChecker;
import quangdev05.giftcode24.database.DatabaseManager;
import quangdev05.giftcode24.database.PlayerDataRepository;

public class GiftCode24 extends JavaPlugin implements Listener {

    private GiftCodesYml giftCodesYml;
    private PlayerDataRepository playerDataRepository;
    private GiftCodeManager giftCodeManager;
    private GiftCodeListGUI giftCodeListGUI;
    private GiftItemEditorGUI giftItemEditorGUI;
    private UpdateChecker updateChecker;
    private DatabaseManager databaseManager;

    private volatile String latestVersion;
    public String getLatestVersion() { return latestVersion; }
    public void setLatestVersion(String v) { this.latestVersion = v; }

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.databaseManager = new DatabaseManager(this);
        this.databaseManager.initialize();

        this.giftCodesYml = new GiftCodesYml(this);
        this.playerDataRepository = new PlayerDataRepository(this, databaseManager);

        this.giftCodeManager = new GiftCodeManager(this, giftCodesYml, playerDataRepository);

        this.giftCodeListGUI = new GiftCodeListGUI(this, giftCodeManager);
        this.giftItemEditorGUI = new GiftItemEditorGUI(this, giftCodeManager);
        getServer().getPluginManager().registerEvents(giftCodeListGUI, this);
        getServer().getPluginManager().registerEvents(giftItemEditorGUI, this);


        GiftCodeAdminCommand adminCmd = new GiftCodeAdminCommand(this, giftCodeManager, giftCodeListGUI);
        getCommand("giftcode").setExecutor(adminCmd);
        getCommand("giftcode").setTabCompleter(adminCmd);

        RedeemCodeCommand redeemCmd = new RedeemCodeCommand(this, giftCodeManager, playerDataRepository);
        getCommand("code").setExecutor(redeemCmd);

        sendFancyMessage();

        this.updateChecker = new UpdateChecker(this);
        getServer().getPluginManager().registerEvents(updateChecker, this);
        updateChecker.checkLatestReleaseAsync();

        int pluginId = 24198;
        Metrics metrics = new Metrics(this, pluginId);

        if (isFolia()) {
            getLogger().info("Running on Folia! Multithreading support enabled");
        }
    }
    private boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        updateChecker.cancelTasks();
        giftCodesYml.saveAll(giftCodeManager.getAll());
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

    public PlayerDataRepository getPlayerDataRepository() { return playerDataRepository; }

    public GiftCodesYml getGiftCodesYml() {
        return giftCodesYml;
    }

    public GiftItemEditorGUI getGiftItemEditorGUI() { return giftItemEditorGUI; }
}
