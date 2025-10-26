package quangdev05.giftcode24.model;

import java.util.*;
import org.bukkit.inventory.ItemStack;

public class GiftCode {
    public Map<String, Integer> ipUsageCounts = new HashMap<>();

    private int maxUsesPerIP;
    private List<String> commands;
    private List<String> messages;
    private int maxUses;
    private String expiry;
    private boolean enabled;
    private int playerMaxUses;
    private int requiredPlaytime;
    private String permission;
    private List<org.bukkit.inventory.ItemStack> itemRewards;

    public GiftCode(List<String> commands, Object messageObj, int maxUses, String expiry, boolean enabled,
                    int playerMaxUses, int maxUsesPerIP, int requiredPlaytime,
                    List<org.bukkit.inventory.ItemStack> itemRewards) {
        this.commands = commands != null ? new ArrayList<>(commands) : new ArrayList<>();
        this.messages = new ArrayList<>();
        if (messageObj instanceof List) for (Object o : (List<?>) messageObj) this.messages.add(String.valueOf(o));
        else if (messageObj != null) this.messages.add(String.valueOf(messageObj));

        this.maxUses = maxUses;
        this.expiry = expiry != null ? expiry : "";
        this.enabled = enabled;
        this.playerMaxUses = playerMaxUses;
        this.maxUsesPerIP = maxUsesPerIP;
        this.requiredPlaytime = requiredPlaytime;
        this.permission = "";
        this.itemRewards = itemRewards != null ? new ArrayList<>(itemRewards) : new ArrayList<>();
    }

    public List<String> getCommands() { return commands; }
    public void setCommands(List<String> commands) { this.commands = commands; }

    public List<String> getMessages() { return messages; }
    public void setMessage(List<String> messages) { this.messages = messages; }

    public int getMaxUses() { return maxUses; }
    public void setMaxUses(int maxUses) { this.maxUses = maxUses; }

    public String getExpiry() { return expiry; }
    public void setExpiry(String expiry) { this.expiry = expiry; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public int getPlayerMaxUses() { return playerMaxUses; }
    public void setPlayerMaxUses(int playerMaxUses) { this.playerMaxUses = playerMaxUses; }

    public int getMaxUsesPerIP() { return maxUsesPerIP; }
    public void setMaxUsesPerIP(int maxUsesPerIP) { this.maxUsesPerIP = maxUsesPerIP; }

    public int getRequiredPlaytime() { return requiredPlaytime; }

    public String getPermission() { return permission; }
    public void setPermission(String permission) { this.permission = (permission != null) ? permission : ""; }

    public List<ItemStack> getItemRewards() { return itemRewards; }
    public void setItemRewards(List<ItemStack> items) { this.itemRewards = items != null ? new ArrayList<>(items) : new ArrayList<>(); }
}