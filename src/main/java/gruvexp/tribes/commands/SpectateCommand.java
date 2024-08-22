package gruvexp.tribes.commands;

import gruvexp.tribes.Manager;
import gruvexp.tribes.Tribe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

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
            Tribe tribe = Manager.getMember(p.getUniqueId()).tribe();
            if (tribe == null) {
                throw new IllegalArgumentException("You need to be in a tribe to spectate");
            }
            Player q = Bukkit.getPlayerExact(args[0]);
            if (q == null) {
                throw new IllegalArgumentException("The player you specified is either not online or doesnt exist");
            }
            UUID targetPlayerID = q.getUniqueId();
            Tribe otherTribe = Manager.getMember(targetPlayerID).tribe();
            if (tribe != otherTribe) {
                p.sendMessage("Sending spectate request to " + targetPlayerID);

                TextComponent message = getTpReqMessage(p.getName());

                q.spigot().sendMessage((BaseComponent) message);
                return true;
            }
            //p.setSpectatorTarget(q); // BØGGA FUNKER IKKE!
            // WORKAROUND:
            String playerName = p.getName();
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "spectate " + targetPlayerID + " " + playerName); // p telporteres til q

            p.sendMessage("You are now spectating " + targetPlayerID);
            q.sendMessage(playerName + " is now spectating you");
        } catch (IllegalArgumentException e) {
            p.sendMessage(ChatColor.RED + e.getMessage());
        }
        return true;
    }

    @NotNull
    private static TextComponent getTpReqMessage(String playerName) {
        TextComponent accept = Component.text("ACCEPT", NamedTextColor.GREEN)
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/java accepttp " + playerName));
        TextComponent decline = Component.text("DECLINE", NamedTextColor.RED)
                .clickEvent(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "/java declinetp " + playerName));

        return Component.text(playerName + " has requested to spectate you [")
                .append(accept)
                .append(Component.text(" | ")) // Add a space between each option
                .append(decline)
                .append(Component.text(" | "));
    }
}
