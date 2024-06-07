package gruvexp.tribes.listeners;

import gruvexp.tribes.*;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Objects;

public class DeathListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Player p = e.getEntity();
        Member member = Manager.getMember(p.getName());
        if (member == null) {
            return; // Player is not a member of the game
        }
        Tribe tribe = member.tribe();
        if (tribe == null) {return;}
        p.setGameMode(GameMode.SPECTATOR);
        Location deathLocation = e.getEntity().getLocation();
        if (deathLocation.getWorld() == Bukkit.getWorld("Tribes_the_end") && deathLocation.getY() < 0) {
            deathLocation = p.getBedSpawnLocation();
            if (deathLocation == null) {
                deathLocation = Main.WORLD.getSpawnLocation();
            }
        }
        p.teleport(deathLocation);
        String playerName = p.getName();
        Bukkit.broadcastMessage(ChatColor.RED +  playerName + " ded");
        tribe.death(playerName);

        Item droppedItem = Main.WORLD.dropItemNaturally(deathLocation, ItemManager.getHead(p, Objects.requireNonNull(e.deathMessage())));
        droppedItem.setUnlimitedLifetime(true);
    }
}
