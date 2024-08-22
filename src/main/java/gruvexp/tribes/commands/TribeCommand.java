package gruvexp.tribes.commands;

import gruvexp.tribes.Main;
import gruvexp.tribes.Manager;
import gruvexp.tribes.Member;
import gruvexp.tribes.Tribe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TribeCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        Player p = null;
        if (sender instanceof Player) {
            p = (Player) sender;
        }

        try {
            if (args.length == 0) {
                throw new IllegalArgumentException("Not enough args!\nUsage: /tribe [create | join | switch | leave | stats | pause]");
            }
            String oper = args[0];
            switch (oper) {
                case "stats" -> {
                    int totalCoins = 0;
                    int LINES = 100; // hvor mange |
                    HashMap<Tribe, Integer> tribeBalance = new HashMap<>(); // brukt for å beregne hvor mange kr hver tribe har i kromerDisctribution
                    for (Tribe tribe : Manager.getTribes()) {
                        tribeBalance.put(tribe, tribe.getCoinBalance());
                        totalCoins += tribe.getCoinBalance();
                    }
                    Component kromerDistribution = Component.text("Kromer distribution: ");
                    for (Map.Entry<Tribe, Integer> entry : tribeBalance.entrySet()) {
                        kromerDistribution = kromerDistribution.append(Component.text("|".repeat(entry.getValue() * LINES / totalCoins), NamedTextColor.NAMES.value(entry.getKey().COLOR.name().toLowerCase())));
                    }
                    sender.sendMessage(kromerDistribution); // bar som viser fordelinga av kromers, fargelagt
                    sender.sendMessage(Component.text("Kromer pool: ").append(Component.text(Manager.getKromerPool() + " kr", NamedTextColor.GREEN))); // kromer pool
                    for (Tribe tribe : Manager.getTribes()) {
                        sender.sendMessage(tribe.COLOR + tribe.displayName() + " tribe:");
                        for (Member member : tribe.getMembers()) {
                            String playerName = member.NAME;
                            TextComponent playerStats = Component.text(String.format("%-12s", playerName)); // adder mellomrom så han blir 12 bokstaver lang
                            playerStats = playerStats.append(Component.text(" - ")
                                    .append(tribe.isAlive(playerName) ? Component.text("ALIVE", NamedTextColor.GREEN) : Component.text("DEAD", NamedTextColor.RED)));
                            if (!tribe.isAlive(playerName)) {
                                playerStats = playerStats.append(Component.text(", Respawntime: ", NamedTextColor.WHITE))
                                        .append(Component.text(member.getRespawnCooldown()))
                                        .append(Component.text("min"));
                            }
                            playerStats = playerStats.append(Component.text(", Deaths: ", NamedTextColor.WHITE))
                                    .append(Component.text(tribe.getDeaths(playerName)));
                            playerStats = playerStats.append(Component.text(", Balance: "))
                                    .append(Component.text(member.getKromers() + " kr", NamedTextColor.GREEN));

                            sender.sendMessage(playerStats);
                        }
                    }
                }
                case "add", "create" -> { // /create <tribeID> <color> <displayName>
                    if (args.length < 3) {
                        throw new IllegalArgumentException("Not enough args!");
                    }
                    String tribeID = args[1];
                    if (Manager.tribeExists(tribeID)) {
                        throw new IllegalArgumentException("Tribe already exists!");
                    }
                    String color = args[2].toUpperCase();
                    if (args.length > 3) {
                        StringBuilder displayName = new StringBuilder();
                        for (int i = 3; i < args.length; i++) {
                            if (i > 3) {displayName.append(" ");}
                            displayName.append(args[i]);
                        }
                        Manager.addTribe(new Tribe(tribeID, ChatColor.valueOf(color), displayName.toString()));
                        Bukkit.broadcast(Component.text("New tribe created: " + displayName, NamedTextColor.GREEN));
                    } else {
                        Manager.addTribe(new Tribe(tribeID, ChatColor.valueOf(color), tribeID));
                        Bukkit.broadcast(Component.text("New tribe created: " + tribeID, NamedTextColor.GREEN));
                    }
                }
                case "join" -> { // /join <tribeID> <playerName>
                    if (args.length < 3) {
                        throw new IllegalArgumentException("Not enough args!");
                    }
                    String tribeID = args[1];
                    String playerName = args[2];
                    Player joiningPlayer = Bukkit.getPlayer(playerName);
                    if (joiningPlayer == null) {
                        throw new IllegalArgumentException("No online player called \"" + playerName + "\" was found");
                    }
                    Tribe tribe = Manager.getTribe(tribeID);
                    tribe.addMember(joiningPlayer);
                    Player q = Bukkit.getPlayerExact(playerName);
                    if (q != null) {
                        q.displayName(Component.text(q.getName(), NamedTextColor.NAMES.value(tribe.COLOR.toString())));
                    }
                }
                case "kick", "leave" -> {
                    assert p != null;
                    checkAdmin(p);
                    if (args.length < 3) {
                        throw new IllegalArgumentException("Not enough args!");
                    }
                    String tribeID = args[1];
                    String playerName = args[2];
                    Manager.getTribe(tribeID).removeMember(playerName);
                }
                case "switch" -> {
                    assert p != null;
                    if (args.length < 3) {
                        throw new IllegalArgumentException("Not enough args!");
                    }
                    String tribeID = args[1];
                    String playerName = args[2];
                    UUID playerID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
                    Member member = Manager.getMember(playerID);
                    if (member == null) {
                        throw new IllegalArgumentException("That player wasnt in a tribe to begin with!");
                    }
                    Tribe tribe = Manager.getTribe(tribeID);
                    tribe.migrateMemberToThisTribe(member);
                    Player q = Bukkit.getPlayerExact(playerName);
                    if (q != null) {
                        q.displayName(Component.text(q.getName(), NamedTextColor.NAMES.value(tribe.COLOR.toString())));
                    }
                }
                case "pause" -> {
                    if (Manager.isPaused()) {
                        throw new IllegalArgumentException("The game is already paused! Use /tribe unpause to unpause");
                    } else {
                        Manager.pause();
                    }
                }
                case "unpause" -> {
                    if (Manager.isPaused()) {
                        Manager.unPause();
                    } else {
                        throw new IllegalArgumentException("The game is already unpaused! Use /tribe pause to pause");
                    }
                }
                case "toggle_friendly_fire" -> {
                    Manager.friendlyFire = !Manager.friendlyFire;
                    assert p != null;
                    String message = "[" + p.getName() + "]: Pvp between tribe members set to: ";
                    message += Manager.friendlyFire ? ChatColor.GREEN + "ENABLED" : ChatColor.RED + "DISABLED";
                    Manager.messagePlayers(message);
                }
                case "test" -> {
                    assert p != null;
                    Entity entity = p.getSpectatorTarget();
                    String name = entity == null ? "noone" : entity.getName();
                    p.sendMessage("Currently spectating: " + name);
                }
                /*case "hacc" -> {
                    assert p != null;
                    checkAdmin(p);
                    String pName = p.getName();
                    int seconds = Integer.parseInt(args[1]);
                    //Manager.getMember(pName).hacc(seconds);
                }*/
                case "version" -> sender.sendMessage("Plugin was last updated " + Main.VERSION);
                default -> throw new IllegalArgumentException(NamedTextColor.RED + "\"" + oper + "\" is not a valid operation!");
            }
        } catch (IllegalArgumentException e) {
            sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED));
        }
        return true;
    }
    private void checkAdmin(Player p) {
        if (!p.isOp()) {
            throw new IllegalArgumentException(NamedTextColor.RED + "You dont have permission to run this command. If you have any queestions, please contact Colin or Gruve");
        }
    }
}
