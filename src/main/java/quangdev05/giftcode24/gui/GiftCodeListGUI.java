package quangdev05.giftcode24.gui;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import quangdev05.giftcode24.manager.GiftCodeManager;
import quangdev05.giftcode24.model.GiftCode;

import java.util.*;

public class GiftCodeListGUI implements Listener {

    private static final int ITEMS_PER_PAGE = 45;
    private static final int PREV_BUTTON_SLOT = 45;
    private static final int NEXT_BUTTON_SLOT = 53;
    private static final int PAGE_INFO_SLOT = 49;

    private final GiftCodeManager manager;

    public GiftCodeListGUI(org.bukkit.plugin.Plugin plugin, GiftCodeManager manager) {
        this.manager = manager;
    }

    private ItemStack createNavigationButton(String displayName, Material material, String lore) {
        ItemStack button = new ItemStack(material);
        ItemMeta meta = button.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + displayName);
        List<String> lores = new ArrayList<>();
        lores.add(ChatColor.GRAY + lore);
        meta.setLore(lores);
        button.setItemMeta(meta);
        return button;
    }

    private ItemStack createGiftCodeItem(String code, GiftCode giftCode) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA + code);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.YELLOW + "Max Uses: " + ChatColor.WHITE + giftCode.getMaxUses());
        lore.add(ChatColor.YELLOW + "Expiry: " + ChatColor.WHITE + (giftCode.getExpiry().isEmpty() ? "None" : giftCode.getExpiry()));
        lore.add(ChatColor.YELLOW + "Enabled: " + ChatColor.WHITE + (giftCode.isEnabled() ? "Yes" : "No"));
        lore.add(ChatColor.YELLOW + "Player Max Uses: " + ChatColor.WHITE + giftCode.getPlayerMaxUses());
        lore.add(ChatColor.YELLOW + "Max Uses/IP: " + ChatColor.WHITE + giftCode.getMaxUsesPerIP());
        lore.add(ChatColor.YELLOW + "Required Playtime: " + ChatColor.WHITE + giftCode.getRequiredPlaytime() + " minutes");
        String perm = giftCode.getPermission();
        String displayPerm = (perm == null || perm.isBlank()) ? "None" : perm;
        lore.add(ChatColor.YELLOW + "Permission: " + ChatColor.WHITE + displayPerm);
        lore.add(ChatColor.YELLOW + "Used: " + ChatColor.WHITE + calculateUsedCount(code));
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private int calculateUsedCount(String code) {
        return manager.calculateUsedCount(code);
    }

    private ItemStack createPageInfoItem(int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD + "Page " + (currentPage + 1) + "/" + totalPages);
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Use the buttons to turn pages");
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    public void open(Player player, int page) {
        List<String> codeList = new ArrayList<>(manager.listGiftCodes());
        int totalCodes = codeList.size();

        int totalPages = (int) Math.ceil((double) totalCodes / ITEMS_PER_PAGE);
        if (totalPages <= 0) totalPages = 1;

        page = Math.max(0, Math.min(page, totalPages - 1));

        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.GOLD + "List of gift codes");

        int start = page * ITEMS_PER_PAGE;
        int end = Math.min(start + ITEMS_PER_PAGE, totalCodes);

        for (int i = start; i < end; i++) {
            String code = codeList.get(i);
            GiftCode gc = manager.get(code);
            inv.addItem(createGiftCodeItem(code, gc));
        }

        inv.setItem(PAGE_INFO_SLOT, createPageInfoItem(page, totalPages));

        if (totalPages > 1) {
            if (page > 0) {
                inv.setItem(PREV_BUTTON_SLOT, createNavigationButton("Previous Page", Material.ARROW, "Click to go back"));
            } else {
                inv.setItem(PREV_BUTTON_SLOT, null);
            }

            if (page < totalPages - 1) {
                inv.setItem(NEXT_BUTTON_SLOT, createNavigationButton("Next Page", Material.ARROW, "Click to go forward"));
            } else {
                inv.setItem(NEXT_BUTTON_SLOT, null);
            }
        } else {
            inv.setItem(PREV_BUTTON_SLOT, null);
            inv.setItem(NEXT_BUTTON_SLOT, null);
        }

        player.openInventory(inv);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.GOLD + "List of gift codes")) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();
        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || !clicked.hasItemMeta()) return;

        if (clicked.getType() == Material.PAPER) {
            player.sendMessage(ChatColor.YELLOW + "Click a navigation arrow to change page.");
        } else if (clicked.getType() == Material.ARROW) {
            int currentPage = 0;
            ItemStack pageInfo = event.getInventory().getItem(PAGE_INFO_SLOT);
            if (pageInfo != null && pageInfo.hasItemMeta()) {
                String displayName = pageInfo.getItemMeta().getDisplayName();
                if (displayName.contains("/")) {
                    try {
                        String[] parts = displayName.replace(ChatColor.GOLD + "Page ", "").split("/");
                        currentPage = Integer.parseInt(parts[0].trim()) - 1;
                    } catch (NumberFormatException ignored) {}
                }
            }
            int totalGiftCodes = manager.getAll().size();
            int totalPages = (int) Math.ceil((double) totalGiftCodes / ITEMS_PER_PAGE);
            if (totalPages <= 0) totalPages = 1;

            if (event.getSlot() == PREV_BUTTON_SLOT && event.getInventory().getItem(PREV_BUTTON_SLOT) != null && currentPage > 0) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                open(player, currentPage - 1);
            } else if (event.getSlot() == NEXT_BUTTON_SLOT && event.getInventory().getItem(NEXT_BUTTON_SLOT) != null && currentPage < totalPages - 1) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                open(player, currentPage + 1);
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTitle().equals(ChatColor.GOLD + "List of gift codes")) {
            event.setCancelled(true);
        }
    }
}