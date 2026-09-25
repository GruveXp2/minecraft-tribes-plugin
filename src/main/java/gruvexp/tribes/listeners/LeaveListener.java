package gruvexp.tribes.listeners;

import gruvexp.tribes.Tribes;
import gruvexp.tribes.Tribe;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class LeaveListener implements Listener {

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        UUID playerID = e.getPlayer().getUniqueId();
        Tribe tribe = Tribes.getTribe(playerID);
        if (tribe == null) {return;} // Player is not a member of the game
        // will probably be used soon
    }
}
