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
import java.util.UUID;
import java.util.stream.Collectors;

public class SpectateTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            Player p = (Player) sender;
            UUID playerID = p.getUniqueId();
            try {
                Tribe tribe = Manager.getMember(playerID).tribe();
                if (tribe == null) {
                    throw new IllegalArgumentException("You need to be in a tribe to use this command");
                }
                return Manager.getMemberIDs().stream()
                        .filter(id -> Bukkit.getPlayer(id) != null)
                        .map(id -> Manager.getMember(id).NAME)
                        .collect(Collectors.toList()); // returnerer kun online members
            } catch (IllegalArgumentException e) {
                return List.of(ChatColor.RED + e.getMessage());
            }
        }
        return List.of("");
    }
}
