package quangdev05.giftcode24.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import quangdev05.giftcode24.manager.GiftCodeManager;

import java.util.ArrayList;
import java.util.List;

public class GiftItemEditorGUI implements Listener {

    private final JavaPlugin plugin;
    private final GiftCodeManager manager;

    public GiftItemEditorGUI(JavaPlugin plugin, GiftCodeManager manager) {
        this.plugin = plugin;
        this.manager = manager;
    }

    private String titleFor(String code) {
        return ChatColor.DARK_GREEN + "Edit items for " + code;
    }

    public void openNew(Player player, String code) {
        Inventory inv = Bukkit.createInventory(player, 54, titleFor(code));
        player.openInventory(inv);
    }

    public void openEdit(Player player, String code) {
        Inventory inv = Bukkit.createInventory(player, 54, titleFor(code));
        List<ItemStack> items = manager.getItemsForCode(code);
        if (items != null) {
            for (ItemStack it : items) {
                if (it == null || it.getType() == Material.AIR) continue;
                inv.addItem(it.clone());
            }
        }
        player.openInventory(inv);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        String title = e.getView().getTitle();
        if (!ChatColor.stripColor(title).startsWith("Edit items for ")) return;

        Player player = (Player) e.getPlayer();
        String code = ChatColor.stripColor(title).replace("Edit items for ", "");
        Inventory inv = e.getInventory();

        Bukkit.getRegionScheduler().run(plugin, player.getLocation(), task -> {
            List<ItemStack> items = new ArrayList<>();
            for (int i = 0; i < inv.getSize(); i++) {
                ItemStack it = inv.getItem(i);
                if (it == null || it.getType() == Material.AIR) continue;
                items.add(it.clone());
            }

            manager.setItemsForCode(code, items);
            player.sendMessage(ChatColor.GREEN + "Saved " + items.size() + " items for code "
                    + ChatColor.YELLOW + code + ChatColor.GREEN + ".");
        });
    }
}