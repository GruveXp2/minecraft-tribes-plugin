package gruvexp.tribes.commands;

import gruvexp.tribes.Manager;
import gruvexp.tribes.Tribe;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class SpectateCommand implements CommandExecutor {

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        Player p = (Player) sender;
        try {
            if (args.length == 0) {
                throw new IllegalArgumentException("You need to specify the player you wanna spectate");
            }
            if (p.getGameMode() != GameMode.SPECTATOR) {
                throw new IllegalArgumentException("You must be in spectator mode to use this command");
            }
            String playerName = p.getName();
            Tribe tribe = Manager.getMember(playerName).tribe();
            if (tribe == null) {
                throw new IllegalArgumentException("You need to be in a tribe to spectate");
            }
            Player q = Bukkit.getPlayerExact(args[0]);
            if (q == null) {
                throw new IllegalArgumentException("The player you specified is either not online or doesnt exist");
            }
            String targetPlayerName = q.getName();
            Tribe otherTribe = Manager.getMember(targetPlayerName).tribe();
            if (tribe != otherTribe) {
                p.sendMessage("Sending spectate request to " + targetPlayerName);
                TextComponent message = new TextComponent(playerName + " has requested to spectate you [");

                TextComponent accept = new TextComponent(ChatColor.GREEN + "ACCEPT");
                accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/java accepttp " + playerName));
                message.addExtra(accept);

                message.addExtra(ChatColor.WHITE + " | ");  // Add a space between each option

                TextComponent decline = new TextComponent(ChatColor.RED + "DECLINE");
                decline.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/java declinetp " + playerName));
                message.addExtra(decline);
                message.addExtra(ChatColor.WHITE + "]");

                q.spigot().sendMessage(message);
                return true;
            }
            //p.setSpectatorTarget(q); // BØGGA FUNKER IKKE!
            // WORKAROUND:
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spectate " + targetPlayerName + " " + playerName); // p telporteres til q

            p.sendMessage("You are now spectating " + targetPlayerName);
            q.sendMessage(playerName + " is now spectating you");
        } catch (IllegalArgumentException e) {
            p.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }
}
