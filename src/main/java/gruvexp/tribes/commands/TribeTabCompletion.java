package gruvexp.tribes.commands;

import gruvexp.tribes.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TribeTabCompletion implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        if (args.length == 1) {
            return List.of("create", "join", "switch", "leave", "stats", "status", "unpause");
        }
        try {
            String oper = args[0];
            switch (oper) {
                case "create", "add" -> { // /add <tribeID> <color> <displayName>
                    if (args.length == 2) {
                        return List.of("<tribe id>");
                    }
                    if (args.length == 3) { // liste over farger man kan bruke
                        return Arrays.stream(ChatColor.values())
                                .map(Enum::name)
                                .map(String::toLowerCase)
                                .collect(Collectors.toList());
                    }
                    if (args.length == 4) {
                        return List.of("<display name>");
                    }
                }
                case "join" -> {
                    if (args.length == 2) {
                        return new ArrayList<>(Manager.getTribeIDs());
                    }
                    if (args.length == 3) {
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toCollection(ArrayList::new));
                    }
                }
                case "switch" -> {
                    if (args.length == 2) {
                        return new ArrayList<>(Manager.getTribeIDs());
                    }
                    if (args.length == 3) {
                        return Bukkit.getOnlinePlayers().stream()
                                .map(Player::getName)
                                .collect(Collectors.toCollection(ArrayList::new));
                    }
                }
                case "leave", "kick" -> {
                    if (args.length == 2) {
                        return new ArrayList<>(Manager.getTribeIDs());
                    }
                    String tribeID = args[1];
                    if (args.length == 3) {
                        return Manager.getTribe(tribeID).getMembers()
                            .stream()
                            .map(member -> member.NAME)
                            .collect(Collectors.toCollection(ArrayList::new));
                    }
                }
                case "stats" -> {
                    return new ArrayList<>(0);
                }
                default -> throw new IllegalArgumentException(ChatColor.RED + "\"" + oper + "\" is not a valid operation!");
            }
        } catch (IllegalArgumentException e) {
            return List.of(e.getMessage());
        }
        return new ArrayList<>(0);
    }
}
