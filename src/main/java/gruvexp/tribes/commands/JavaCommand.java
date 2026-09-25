package gruvexp.tribes.commands;

import gruvexp.tribes.ItemManager;
import gruvexp.tribes.Main;
import gruvexp.tribes.Tribe;
import gruvexp.tribes.Tribes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class JavaCommand implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player p = (Player) sender; // /java accepttp <spectating player>

        try {
            if (args.length == 0) {
                throw new IllegalArgumentException("Server error: missing argument <oper> (/java)");
            }
            String oper = args[0];
            switch (oper) {
                case "hack", "hacc" -> {
                    if (!(p.getName().equals("GruveXp") || p.getName().equals("ColinStorm") || sender instanceof ConsoleCommandSender) || Main.WORLD.getName().equals(Main.testWorldName)) {
                        throw new IllegalArgumentException("Admin abuse can only be performed by admins");
                    }
                    if (args.length == 1) {
                        throw new IllegalArgumentException("Error: missing argument <hack> (/java hack)");
                    }
                    String hack = args[1];
                    switch (hack) {
                        case "starter_coins", "coins", "get_coins" -> {
                            if (args.length == 2) {
                                throw new IllegalArgumentException("Error: missing argument <player> (/java hack starter_coins)");
                            }
                            String targetPlayerName = args[2];
                            Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
                            if (targetPlayer == null) {
                                throw new IllegalArgumentException("Error: Player \"" + targetPlayerName + "\" is not online!");
                            }
                            Player gruveXp = Bukkit.getPlayer("GruveXp");
                            if (gruveXp == null) {
                                throw new IllegalArgumentException("Bruhh gwuve not online :(");
                            }
                            UUID targetPlayerID = targetPlayer.getUniqueId();
                            targetPlayer.getInventory().addItem(ItemManager.getStarterItems(targetPlayerID));
                            Tribes.getTribe(targetPlayerID).addKromers(320);
                        }
                        case "change_registered_balance" -> {
                            if (args.length < 4) {
                                throw new IllegalArgumentException("Error: not enough args! /java hack change_registered_balance <player> <amount>");
                            }
                            String targetPlayerName = args[2];
                            UUID playerID = Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
                            Tribe tribe = Tribes.getTribe(playerID);
                            if (tribe == null) {
                                throw new IllegalArgumentException("That member doesnt exist!");
                            }
                            int Δkr;
                            try {
                                Δkr = Integer.parseInt(args[3]);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("\"" + args[3] + "\" is not a number!");
                            }
                            tribe.addKromers(Δkr);
                            p.sendMessage(Component.text("Successfully changed registered kromer balance by " + Δkr, NamedTextColor.GRAY));
                        }
                        case "sb" -> {
                            ItemStack itemStack = p.getInventory().getItemInMainHand();
                            Item item = Main.WORLD.dropItemNaturally(p.getLocation().add(0, -2, 0), itemStack);
                            item.setTicksLived(11950);
                        }
                        case "test" -> {
                            if (args.length == 2) {
                                throw new IllegalArgumentException("Error: missing argument <color> (/java hack test)");
                            }
                            String color = args[2];
                            sender.sendMessage(Component.text(color, NamedTextColor.NAMES.value(color)));
                            p.sendMessage("Your tribe has color: " + Tribes.getTribe(p.getUniqueId()).getColor().toString() + "...");
                            p.sendMessage("Your tribe has color: " + Tribes.getTribe(p.getUniqueId()).getColor() + "...");
                        }
                        default -> throw new IllegalArgumentException("Error: wrong argument <hack> (/java hack)");
                    }
                }
                default -> throw new IllegalArgumentException("Server error: wrong argument <oper> (/java)");
            }
        } catch (IllegalArgumentException e) {
            p.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }
}
