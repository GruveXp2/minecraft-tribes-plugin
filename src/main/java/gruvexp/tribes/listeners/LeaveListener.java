package gruvexp.tribes.listeners;

import gruvexp.tribes.Manager;
import gruvexp.tribes.Member;
import gruvexp.tribes.Tribe;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class LeaveListener implements Listener {

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        UUID playerID = e.getPlayer().getUniqueId();
        Member member = Manager.getMember(playerID);
        if (member == null) {
            return; // Player is not a member of the game
        }
        Tribe tribe = member.tribe();
        if (tribe == null) {return;}
        tribe.handleLeaveActive(playerID);
    }
}
