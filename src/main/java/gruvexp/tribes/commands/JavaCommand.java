package gruvexp.tribes.commands;

import gruvexp.tribes.ItemManager;
import gruvexp.tribes.Main;
import gruvexp.tribes.Manager;
import gruvexp.tribes.Member;
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
                case "accepttp" -> { // sender er den som trykka [YES/NO]
                    if (args.length == 1) {
                        throw new IllegalArgumentException("Server error: missing argument <player> (/java accepttp)");
                    }
                    String spectatingPlayerName = args[1];
                    Player q = Bukkit.getPlayerExact(spectatingPlayerName); // den som skal spectate
                    if (q == null) {
                        throw new IllegalArgumentException("This player is no longer online");
                    }
                    if (q.getGameMode() != GameMode.SPECTATOR) {
                        throw new IllegalArgumentException("This player can no longer spectate bc they respawned");
                    }
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spectate " + p.getName() + " " + spectatingPlayerName); // p telporteres til q
                    q.sendMessage("You are now spectating " + p.getName());
                    p.sendMessage(spectatingPlayerName + " is now spectating you");
                }
                case "declinetp" -> {
                    if (args.length == 1) {
                        throw new IllegalArgumentException("Server error: missing argument <player> (/java accepttp)");
                    }
                    String spectatingPlayerName = args[1];
                    Player q = Bukkit.getPlayerExact(spectatingPlayerName); // den som skal spectate
                    if (q == null || q.getGameMode() != GameMode.SPECTATOR) {return true;}
                    q.sendMessage(p.getName() + " declined your spectate request");
                }
                case "hack", "hacc" -> {
                    if (!(p.getName().equals("GruveXp") || p.getName().equals("ColinStorm") || sender instanceof ConsoleCommandSender) || Main.WORLD.getName().equals(Main.testWorldName)) {
                        throw new IllegalArgumentException("This command can only be used on test servers or by admins");
                    }
                    if (args.length == 1) {
                        throw new IllegalArgumentException("Error: missing argument <hack> (/java hack)");
                    }
                    String hack = args[1];
                    switch (hack) {
                        case "set_deaths" -> {
                            if (args.length == 2) {
                                throw new IllegalArgumentException("Error: missing argument <playerName> (/java hack set_deaths)");
                            }
                            String playerName = args[2];
                            if (args.length == 3) {
                                throw new IllegalArgumentException("Error: missing argument <deaths> (/java hack set_deaths <player name>)");
                            }
                            int deaths;
                            try {
                                deaths = Integer.parseInt(args[3]);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Error: argument <deaths> must be a number (/java hack set_deaths)");
                            }
                            UUID playerID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                            Manager.getMember(playerID).setDeaths(deaths); // setter deaths
                            Bukkit.broadcastMessage(String.format("%s%s hacked: set deaths of %s to %s", ChatColor.RED, p.getName(), playerName, deaths));
                        }
                        case "set_respawn_time" -> {
                            if (args.length == 2) {
                                throw new IllegalArgumentException("Error: missing argument <playerName> (/java hack set_respawn_time)");
                            }
                            String playerName = args[2];
                            UUID playerID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                            if (args.length == 3) {
                                throw new IllegalArgumentException("Error: missing argument <deaths> (/java hack set_respawn_time)");
                            }
                            int respawnTime;
                            try {
                                respawnTime = Integer.parseInt(args[3]);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("Error: argument <respawn_time> must be a number (/java hack set_respawn_time)");
                            }
                            Manager.getMember(playerID).haccRespawnCooldown(respawnTime); // setter respawncooldown i minutter
                            Bukkit.broadcastMessage(String.format("%s%s hacked: set respawncooldown of %s to %s", ChatColor.RED, p.getName(), playerName, respawnTime));
                        }
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
                            Manager.getMember(targetPlayerID).addKromers(320);
                        }
                        case "change_registered_balance" -> {
                            if (args.length < 4) {
                                throw new IllegalArgumentException("Error: not enough args! /java hack change_registered_balance <player> <amount>");
                            }
                            String targetPlayerName = args[2];
                            UUID playerID = Bukkit.getOfflinePlayer(targetPlayerName).getUniqueId();
                            Member member = Manager.getMember(playerID);
                            if (member == null) {
                                throw new IllegalArgumentException("That member doesnt exist!");
                            }
                            int Δkr;
                            try {
                                Δkr = Integer.parseInt(args[3]);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException("\"" + args[3] + "\" is not a number!");
                            }
                            member.addKromers(Δkr);
                            p.sendMessage(Component.text("Successfully changed registered kromer balance by " + Δkr, NamedTextColor.GRAY));
                        }
                        case "get_altar", "give_altar", "a" -> {
                            p.getInventory().addItem(ItemManager.REVIVAL_ALTAR);
                            Bukkit.broadcastMessage(String.format("%s%s hacked: gave themself an altar of revival", ChatColor.RED, p.getName()));
                        }
                        case "altars", "as" -> {
                            StringBuilder out = new StringBuilder("Current altars:\n");
                            for (String tribeID : Manager.getTribeIDs()) {
                                out.append("\n").append(tribeID).append(": ").append(Manager.getTribe(tribeID).getAltarInfo());
                            }
                            p.sendMessage(out.toString());
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
                            p.sendMessage("Your tribe has color: " + Manager.getMember(p.getUniqueId()).tribe().COLOR.toString() + "...");
                            p.sendMessage("Your tribe has color: " + Manager.getMember(p.getUniqueId()).tribe().COLOR.name() + "...");
                            p.sendMessage("Your tribe has color: " + Manager.getMember(p.getUniqueId()).tribe().COLOR + "...");
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
