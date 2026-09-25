package gruvexp.tribes.listeners;

import gruvexp.tribes.*;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.Objects;

public class DeathListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Tribe tribe = Tribes.getTribe(p.getUniqueId());
        if (tribe == null) {
            return; // Player is not a member of the game
        }
        Location deathLocation = e.getEntity().getLocation();
        if (deathLocation.getWorld() == Bukkit.getWorld("Tribes_the_end") && deathLocation.getY() < 0) {
            deathLocation = p.getBedSpawnLocation();
            if (deathLocation == null) {
                deathLocation = Main.WORLD.getSpawnLocation();
            }
        }
        p.teleport(deathLocation);
        Bukkit.broadcastMessage(ChatColor.RED +  p.getName() + " ded");

        Item droppedItem = Main.WORLD.dropItemNaturally(deathLocation, ItemManager.getHead(p, Objects.requireNonNull(e.deathMessage())));
        droppedItem.setUnlimitedLifetime(true);
    }
}
