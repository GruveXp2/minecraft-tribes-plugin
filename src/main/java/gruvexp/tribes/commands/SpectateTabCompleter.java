package gruvexp.tribes.commands;

import gruvexp.tribes.Manager;
import gruvexp.tribes.Tribe;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class SpectateTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            Player p = (Player) sender;
            String playerName = p.getName();
            try {
                Tribe tribe = Manager.getMember(playerName).tribe();
                if (tribe == null) {
                    throw new IllegalArgumentException("You need to be in a tribe to use this command");
                }
                return Manager.getMemberIDs().stream().filter(name -> Bukkit.getPlayerExact(name) != null).collect(Collectors.toList()); // returnerer kun online members
            } catch (IllegalArgumentException e) {
                return List.of(ChatColor.RED + e.getMessage());
            }
        }
        return List.of("");
    }
}
