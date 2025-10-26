package quangdev05.giftcode24.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.command.TabCompleter;

import quangdev05.giftcode24.GiftCode24;
import quangdev05.giftcode24.gui.GiftCodeListGUI;
import quangdev05.giftcode24.manager.GiftCodeManager;
import quangdev05.giftcode24.model.GiftCode;

import java.util.*;
import java.util.stream.Collectors;

public class GiftCodeAdminCommand implements CommandExecutor, TabCompleter {

    private final GiftCode24 plugin;
    private final GiftCodeManager manager;
    private final GiftCodeListGUI gui;

    public GiftCodeAdminCommand(GiftCode24 plugin, GiftCodeManager manager, GiftCodeListGUI gui) {
        this.plugin = plugin;
        this.manager = manager;
        this.gui = gui;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            sender.sendMessage(ChatColor.GOLD + "Command List");
            sender.sendMessage(ChatColor.GREEN + " /gc help - Show command list");
            sender.sendMessage(ChatColor.GREEN + " /gc create <code> - Create a gift code");
            sender.sendMessage(ChatColor.GREEN + " /gc create <code> -g - Create and open GUI to set item rewards");
            sender.sendMessage(ChatColor.GREEN + " /gc create <base> -r [amount] - Generate random codes (default 10)");
            sender.sendMessage(ChatColor.GREEN + " /gc create <base> -r [amount] -c <template> - Random codes using <template>'s");
            sender.sendMessage(ChatColor.GREEN + " /gc guie <code> - Open item GUI editor for a code");
            sender.sendMessage(ChatColor.GREEN + " /gc setperm <code> <permission|none> - Set/clear required permission for a code");
            sender.sendMessage(ChatColor.GREEN + " /gc del <code> - Delete a gift code");
            sender.sendMessage(ChatColor.GREEN + " /gc reload - Reload the plugin");
            sender.sendMessage(ChatColor.GREEN + " /gc enable <code> - Enable a gift code");
            sender.sendMessage(ChatColor.GREEN + " /gc disable <code> - Disable a gift code");
            sender.sendMessage(ChatColor.GREEN + " /gc list - List all gift codes");
            sender.sendMessage(ChatColor.GREEN + " /gc assign <code> <player> - Assign a gift code to a player");
            sender.sendMessage("");
            sender.sendMessage(ChatColor.GOLD + "Information:");
            sender.sendMessage(ChatColor.YELLOW + " Author: QuangDev05");
            sender.sendMessage(ChatColor.YELLOW + " Current version: " + plugin.getDescription().getVersion());
            String latest = plugin.getLatestVersion();
            sender.sendMessage(ChatColor.YELLOW + " Latest version: " + (latest != null ? latest : "Cannot be checked"));
            return true;
        }

        if (!sender.hasPermission("giftcode.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                plugin.reloadConfig();
                manager.reloadFromDisk();
                sender.sendMessage(ChatColor.GREEN + "GiftCode24 reloaded.");
                break;

            case "create":
                if (args.length >= 2) {
                    String baseOrCode = args[1];

                    boolean flagRandom = false;
                    int amount = 10;
                    String template = null;
                    boolean openGui = false;

                    for (int i = 2; i < args.length; i++) {
                        String a = args[i];
                        if ("-r".equalsIgnoreCase(a) || "random".equalsIgnoreCase(a)) {
                            flagRandom = true;
                            if (i + 1 < args.length && !args[i + 1].startsWith("-")) {
                                try { amount = Math.max(1, Math.min(1000, Integer.parseInt(args[i + 1]))); i++; }
                                catch (NumberFormatException ignored) {}
                            }
                        } else if ("-c".equalsIgnoreCase(a)) {
                            if (i + 1 < args.length) { template = args[i + 1]; i++; }
                        } else if ("-g".equalsIgnoreCase(a)) {
                            openGui = true;
                        }
                    }

                    if (flagRandom) {
                        if (template != null) {
                            int made = manager.createRandomGiftCodesFromTemplate(baseOrCode, amount, template);
                            if (made < 0) {
                                sender.sendMessage(ChatColor.RED + "Template \"" + ChatColor.YELLOW + template + ChatColor.RED + "\" does not exist.");
                            } else {
                                sender.sendMessage(ChatColor.GREEN + "Generated " + ChatColor.YELLOW + made + ChatColor.GREEN
                                        + " codes from template \"" + ChatColor.YELLOW + template + ChatColor.GREEN
                                        + "\" with base \"" + ChatColor.YELLOW + baseOrCode + ChatColor.GREEN + "\"");
                            }
                        } else {
                            int made = manager.createRandomGiftCodes(baseOrCode, amount);
                            sender.sendMessage(ChatColor.GREEN + "Generated " + ChatColor.YELLOW + made + ChatColor.GREEN
                                    + " gift codes with base \"" + ChatColor.YELLOW + baseOrCode + ChatColor.GREEN + "\"");
                        }
                        break;
                    }

                    if (manager.exists(baseOrCode)) {
                        sender.sendMessage(ChatColor.RED + "The gift code \"" + ChatColor.YELLOW + baseOrCode + ChatColor.RED + "\" already exists. Please create a different code.");
                        break;
                    }

                    if (openGui) {
                        manager.createGiftCode(baseOrCode,
                                java.util.Collections.emptyList(),
                                java.util.Collections.singletonList("You have received items!"),
                                99, "2029-12-31T23:59:59", true, 1, 1, 8);
                        sender.sendMessage(ChatColor.GREEN + "Gift code \"" + ChatColor.YELLOW + baseOrCode + ChatColor.GREEN + "\" created. Opening item editor...");
                        if (sender instanceof org.bukkit.entity.Player) {
                            ((quangdev05.giftcode24.GiftCode24)plugin).getGiftItemEditorGUI().openNew((org.bukkit.entity.Player) sender, baseOrCode);
                        } else {
                            sender.sendMessage(ChatColor.RED + "Only players can open the GUI.");
                        }
                    } else {
                        manager.createGiftCode(baseOrCode,
                                java.util.Collections.singletonList("give %player% diamond 1"),
                                java.util.Collections.singletonList("You have received 1 diamond!"),
                                99, "2029-12-31T23:59:59", true, 1, 1, 8);
                        sender.sendMessage(ChatColor.GREEN + "Gift code \"" + ChatColor.YELLOW + baseOrCode + ChatColor.GREEN + "\" has been created successfully!");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /giftcode create <code> [-g]");
                    sender.sendMessage(ChatColor.RED + "Usage: /giftcode create <base> -r [amount] [-c <template>]");
                }
                break;

            case "guie":
                if (args.length == 2) {
                    String codeName = args[1];
                    if (!manager.exists(codeName)) {
                        sender.sendMessage(ChatColor.RED + "The gift code \"" + ChatColor.YELLOW + codeName + ChatColor.RED + "\" does not exist.");
                        break;
                    }
                    if (!(sender instanceof Player)) {
                        sender.sendMessage(ChatColor.RED + "Only players can open the GUI.");
                        break;
                    }
                    ((quangdev05.giftcode24.GiftCode24)plugin).getGiftItemEditorGUI().openEdit((Player) sender, codeName);
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /giftcode guie <code>");
                }
                break;

            case "setperm":
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /giftcode setperm <code> <permission|none>");
                    break;
                }
            {
                String code = args[1];
                quangdev05.giftcode24.model.GiftCode gc = manager.getGiftCode(code);
                if (gc == null) {
                    sender.sendMessage(ChatColor.RED + "The gift code \"" + ChatColor.YELLOW + code + ChatColor.RED + "\" does not exist.");
                    break;
                }
                String perm = args[2];
                if ("none".equalsIgnoreCase(perm) || "null".equalsIgnoreCase(perm) || "-".equals(perm)) perm = "";
                gc.setPermission(perm);
                manager.save();
                sender.sendMessage(ChatColor.GREEN + "Permission for " + ChatColor.YELLOW + code + ChatColor.GREEN + " set to "
                        + (perm.isEmpty() ? ChatColor.AQUA + "none" : ChatColor.AQUA + perm) + ChatColor.GREEN + ".");
            }
            break;

            case "del":
            case "delete":
                if (args.length == 2) {
                    if (!manager.exists(args[1])) {
                        sender.sendMessage(ChatColor.RED + "The gift code \"" + ChatColor.YELLOW + args[1] + ChatColor.RED + "\" does not exist.");
                    } else {
                        manager.deleteGiftCode(args[1]);
                        sender.sendMessage(ChatColor.GREEN + "Gift code \"" + ChatColor.YELLOW + args[1] + ChatColor.GREEN + "\" has been deleted.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /giftcode del <code>");
                }
                break;

            case "enable":
                if (args.length == 2) {
                    if (!manager.exists(args[1])) {
                        sender.sendMessage(ChatColor.RED + "The gift code \"" + ChatColor.YELLOW + args[1] + ChatColor.RED + "\" does not exist.");
                    } else {
                        manager.get(args[1]).setEnabled(true);
                        manager.save();
                        sender.sendMessage(ChatColor.GREEN + "Gift code \"" + ChatColor.YELLOW + args[1] + ChatColor.GREEN + "\" has been enabled.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /giftcode enable <code>");
                }
                break;

            case "disable":
                if (args.length == 2) {
                    if (!manager.exists(args[1])) {
                        sender.sendMessage(ChatColor.RED + "The gift code \"" + ChatColor.YELLOW + args[1] + ChatColor.RED + "\" does not exist.");
                    } else {
                        manager.get(args[1]).setEnabled(false);
                        manager.save();
                        sender.sendMessage(ChatColor.GREEN + "Gift code \"" + ChatColor.YELLOW + args[1] + ChatColor.GREEN + "\" has been disabled.");
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + "Usage: /giftcode disable <code>");
                }
                break;

            case "list":
                if (sender instanceof Player) {
                    gui.open((Player) sender, 0);
                } else {
                    java.util.List<String> codes = manager.listGiftCodes();
                    sender.sendMessage(ChatColor.GOLD + "Gift code list (" + codes.size() + "):");

                    for (String code : codes) {
                        GiftCode gc = manager.get(code);
                        if (gc == null) continue;

                        int used = manager.calculateUsedCount(code);
                        String expiry = (gc.getExpiry() == null || gc.getExpiry().isEmpty()) ? "∞" : gc.getExpiry();
                        String status = gc.isEnabled() ? (ChatColor.GREEN + "ENABLED") : (ChatColor.RED + "DISABLED");

                        sender.sendMessage(
                                ChatColor.YELLOW + code
                                        + ChatColor.GRAY + " | qty: " + ChatColor.AQUA + gc.getMaxUses()
                                        + ChatColor.GRAY + " | used: " + ChatColor.AQUA + used
                                        + ChatColor.GRAY + " | expiry: " + ChatColor.AQUA + expiry
                                        + ChatColor.GRAY + " | status: " + status
                        );
                    }
                }
                break;

            case "assign":
                if (args.length != 3) {
                    sender.sendMessage(ChatColor.RED + "Usage: /giftcode assign <code> <player>");
                    break;
                }
                String codeName = args[1];
                String targetName = args[2];

                if (!manager.exists(codeName)) {
                    sender.sendMessage(ChatColor.RED + "The gift code \"" + ChatColor.YELLOW + codeName + ChatColor.RED + "\" does not exist!");
                    break;
                }

                org.bukkit.entity.Player target = plugin.getServer().getPlayerExact(targetName);
                if (target == null) {
                    sender.sendMessage(ChatColor.RED + "Player not found or offline.");
                    break;
                }

                manager.assignGiftCodeToPlayer(sender, codeName, target);
                break;

            default:
                sender.sendMessage(ChatColor.RED + "Unknown command: " + ChatColor.YELLOW + args[0]);
                sender.sendMessage(ChatColor.GOLD + "Use " + ChatColor.YELLOW + "/giftcode help" + ChatColor.GOLD + " for command list");
                break;
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        java.util.function.BiFunction<List<String>, String, List<String>> pick = (opts, pref) -> {
            String p = pref == null ? "" : pref.toLowerCase();
            return opts.stream().filter(s -> s.toLowerCase().startsWith(p)).collect(Collectors.toList());
        };

        List<String> subs = Arrays.asList("help","create","del","reload","enable","disable","list","assign","setperm","guie");

        if (args.length == 1) {
            return pick.apply(subs, args[0]);
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "create": {
                if (args.length == 2) {
                    return Collections.emptyList();
                }
                if (args.length >= 3) {
                    List<String> flags = new ArrayList<>();
                    if (!Arrays.asList(args).contains("-r")) flags.add("-r");
                    if (!Arrays.asList(args).contains("-c")) flags.add("-c");
                    if (!Arrays.asList(args).contains("-g")) flags.add("-g");
                    if ("-r".equalsIgnoreCase(args[2])) {
                        if (args.length == 3) return Arrays.asList("10","20","50","100");
                        if (args.length == 4 && ("-c".equalsIgnoreCase(args[3]) || "-g".equalsIgnoreCase(args[3])))
                            return Collections.emptyList();
                    }
                    if ("-c".equalsIgnoreCase(args[2])) {
                        if (args.length == 3 || (args.length == 5 && "-c".equalsIgnoreCase(args[4]))) {
                            return pick.apply(manager.listGiftCodes(), args[args.length-1]);
                        }
                    }
                    return pick.apply(flags, args[args.length-1]);
                }
                return Collections.emptyList();
            }
            case "del":
            case "enable":
            case "disable":
            case "guie": {
                if (args.length == 2) {
                    return pick.apply(manager.listGiftCodes(), args[1]);
                }
                return Collections.emptyList();
            }
            case "assign": {
                if (args.length == 2) {
                    return pick.apply(manager.listGiftCodes(), args[1]);
                } else if (args.length == 3) {
                    List<String> names = Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
                    return pick.apply(names, args[2]);
                }
                return Collections.emptyList();
            }
            case "setperm": {
                if (args.length == 2) {
                    return pick.apply(manager.listGiftCodes(), args[1]);
                } else if (args.length == 3) {
                    return pick.apply(Arrays.asList("none", "giftcode24.use."), args[2]);
                }
                return Collections.emptyList();
            }
            case "list":
            case "reload":
            default:
                return Collections.emptyList();
        }
    }
}
