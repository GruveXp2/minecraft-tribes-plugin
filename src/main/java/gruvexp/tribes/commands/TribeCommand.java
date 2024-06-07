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
                    HashMap<Tribe, Integer> tribeBalance = new HashMap<>();
                    for (Tribe tribe : Manager.getTribes()) {
                        tribeBalance.put(tribe, tribe.getCoinBalance());
                        totalCoins += tribe.getCoinBalance();
                    }
                    Component kromerStats = Component.text("Kromer distribution: ");
                    for (Map.Entry<Tribe, Integer> entry : tribeBalance.entrySet()) {
                        kromerStats = kromerStats.append(Component.text("|".repeat(entry.getValue() * LINES / totalCoins), NamedTextColor.NAMES.value(entry.getKey().COLOR.name().toLowerCase())));
                    }
                    sender.sendMessage(kromerStats);
                    for (Tribe tribe : Manager.getTribes()) {
                        sender.sendMessage(tribe.COLOR + tribe.displayName() + " tribe:");
                        for (String playerName : tribe.getMemberIDs()) {
                            TextComponent stats = Component.text(String.format("%-12s", playerName)); // adder mellomrom så han blir 12 bokstaver lang
                            stats = stats.append(Component.text(" - ")
                                    .append(tribe.isAlive(playerName) ? Component.text("ALIVE", NamedTextColor.GREEN) : Component.text("DEAD", NamedTextColor.RED)));
                            if (!tribe.isAlive(playerName)) {
                                stats = stats.append(Component.text(", Respawntime: ", NamedTextColor.WHITE))
                                        .append(Component.text(tribe.getMember(playerName).getRespawnCooldown()))
                                        .append(Component.text("min"));
                            }
                            stats = stats.append(Component.text(", Deaths: ", NamedTextColor.WHITE))
                                    .append(Component.text(tribe.getDeaths(playerName)));
                            stats = stats.append(Component.text(", Balance: "))
                                    .append(Component.text(tribe.getMember(playerName).getKromers() + "kr", NamedTextColor.GREEN));

                            sender.sendMessage(stats);
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
                    Tribe tribe = Manager.getTribe(tribeID);
                    tribe.addMember(playerName);
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
                    Member member = Manager.getMember(playerName);
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
                case "hacc" -> {
                    assert p != null;
                    checkAdmin(p);
                    String pName = p.getName();
                    int seconds = Integer.parseInt(args[1]);
                    //Manager.getMember(pName).hacc(seconds);
                }
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
