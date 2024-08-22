package gruvexp.tribes.listeners;

import gruvexp.tribes.Main;
import gruvexp.tribes.Manager;
import gruvexp.tribes.Member;
import gruvexp.tribes.Tribe;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.UUID;

public class MoveListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent e) {
        Player p = e.getPlayer();
        float yaw = e.getTo().getYaw();
        float pitch = e.getTo().getPitch();
        pauseMovement(p, yaw, pitch);
        spectatorMovement(p);
    }

    private void pauseMovement(Player p, float yaw, float pitch) {
        if (!Manager.isPaused()) {return;}
        UUID playerID = p.getUniqueId();
        Member member = Manager.getMember(playerID);
        if (member == null) {return;}
        Tribe tribe = member.tribe();
        if (tribe == null) {return;}
        Location loc = Manager.getPauseLocation(playerID);
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        p.teleport(loc);
    }

    private void spectatorMovement(Player p) {
        if (p.getGameMode() != GameMode.SPECTATOR) {return;}
        UUID playerID = p.getUniqueId();
        if (Manager.getMember(playerID).tribe() == null) {return;}
        if (p.getSpectatorTarget() != null) {return;}
        if (Main.WORLD.getName().equals(Main.testWorldName)) {return;}
        Location deathLoc = Manager.getDeathLocation(playerID);
        if (deathLoc == null) {
            deathLoc = p.getLastDeathLocation();
            Manager.setDeathLocation(p.getUniqueId(), deathLoc);
            if (deathLoc == null) {return;}
        }
        Location pLoc = p.getLocation();
        if (deathLoc.getX() == pLoc.getX() && deathLoc.getZ() == pLoc.getZ() && deathLoc.getY() == pLoc.getY()) {return;}
        p.teleport(deathLoc); // telporterer spilleren tilbake til der høn daua hvis høn har bevegd seg
    }
}
