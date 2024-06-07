package gruvexp.tribes.listeners;

import gruvexp.tribes.Manager;
import gruvexp.tribes.Member;
import gruvexp.tribes.Tribe;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class LeaveListener implements Listener {

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {
        String pName = e.getPlayer().getName();
        Member member = Manager.getMember(pName);
        if (member == null) {
            return; // Player is not a member of the game
        }
        Tribe tribe = member.tribe();
        if (tribe == null) {return;}
        tribe.handleLeaveActive(pName);
    }
}
