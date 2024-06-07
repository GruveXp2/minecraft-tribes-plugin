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
        String pName = p.getName();
        Member member = Manager.getMember(pName);
        if (member == null) {return;}
        Tribe tribe = member.tribe();
        if (tribe == null) {return;}
        Location loc = Manager.getPauseLocation(pName);
        loc.setYaw(yaw);
        loc.setPitch(pitch);
        p.teleport(loc);
    }

    private void spectatorMovement(Player p) {
        if (p.getGameMode() != GameMode.SPECTATOR) {return;}
        String pName = p.getName();
        if (Manager.getMember(pName).tribe() == null) {return;}
        if (p.getSpectatorTarget() != null) {return;}
        if (Main.WORLD.getName().equals(Main.testWorldName)) {return;}
        Location deathLoc = Manager.getDeathLocation(pName);
        if (deathLoc == null) {
            deathLoc = p.getLastDeathLocation();
            Manager.setDeathLocation(p.getName(), deathLoc);
            if (deathLoc == null) {return;}
        }
        Location pLoc = p.getLocation();
        if (deathLoc.getX() == pLoc.getX() && deathLoc.getZ() == pLoc.getZ() && deathLoc.getY() == pLoc.getY()) {return;}
        p.teleport(deathLoc); // telporterer spilleren tilbake til der høn daua hvis høn har bevegd seg
    }
}
