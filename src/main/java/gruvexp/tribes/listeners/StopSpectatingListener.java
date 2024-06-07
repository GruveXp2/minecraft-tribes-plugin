package gruvexp.tribes.listeners;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class StopSpectatingListener implements Listener {
    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            Player player = event.getPlayer();
            Entity spectatedEntity = player.getSpectatorTarget();
            if (spectatedEntity != null) {
                // The player is currently spectating your entity
            } else {
                // The player stopped spectating your entity
            }
        }
    }
}
