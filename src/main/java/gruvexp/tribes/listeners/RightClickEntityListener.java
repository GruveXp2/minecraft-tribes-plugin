package gruvexp.tribes.listeners;

import gruvexp.tribes.Manager;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.inventory.ItemStack;

public class RightClickEntityListener implements Listener {

    @EventHandler
    public void onRightClick(PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getType() == EntityType.ARMOR_STAND) {
            Manager.debugMessage("Clicked on armor stand");
            ItemStack handItem = e.getPlayer().getInventory().getItemInMainHand();
            if (handItem.getType() == Material.STICK && handItem.getAmount() >= 2) {
                Manager.debugMessage("Player holds atleast 2 sticcs in inventory");
                ArmorStand armorStand = (ArmorStand) e.getRightClicked();
                if (armorStand.hasArms()) {return;}
                Manager.debugMessage("Adding arms to armor stand");
                armorStand.setArms(true);
                handItem.setAmount(handItem.getAmount() - 2);
            }
        }
    }

}
