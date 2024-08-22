package gruvexp.tribes.listeners;

import gruvexp.tribes.Main;
import gruvexp.tribes.Manager;
import gruvexp.tribes.RevivalAltar;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

import java.util.Objects;
import java.util.UUID;

public class BlockInteractListener implements Listener { // RESPAWN ALTER DATA: koordinat (key), cooldown, heads, kromers, aura status

    @EventHandler
    public void onBlockInteract(PlayerInteractEvent e) { // sjekker om man plasserer ned respawn alters

        //Manager.debugMessage("0");
        if (e.getAction() != Action.RIGHT_CLICK_BLOCK) {return;}
        ItemStack stack = e.getItem();
        //Manager.debugMessage("1");
        if (stack == null) {return;}
        //Manager.debugMessage("2");
        if (stack.getType() != Material.FIREWORK_STAR) {return;}
        ItemMeta meta = stack.getItemMeta();
        //Manager.debugMessage("3");
        if (!meta.hasCustomModelData() || (meta.getCustomModelData() != 77010 && meta.getCustomModelData() != 77011)) {return;} // 77010 er (inaktiverte) respawn altere

        Block clickedBlock = e.getClickedBlock();
        BlockFace blockFace = e.getBlockFace();
        //Manager.debugMessage("4");
        if (clickedBlock == null) {return;}
        //Manager.debugMessage("5");
        if (clickedBlock.getType() == Material.HOPPER) {return;} // hindrer at man plasserer blokken 2 ganger

        Location loc = clickedBlock.getRelative(blockFace).getLocation();
        Block hopperBlock = loc.getBlock();
        //Manager.debugMessage("6");
        if (hopperBlock.getType() != Material.AIR) {return;}

        hopperBlock.setType(Material.HOPPER); // spawner inn hopperen

        BlockState state = hopperBlock.getState();
        //Manager.debugMessage("7");
        if (!(state instanceof Hopper)) {return;}
        Hopper hopper = (Hopper) state;
        hopper.customName(RevivalAltar.NAME);
        hopper.update();
        loc.add(0.5, 0.5, 0.5);

        // modellen for alteret vises som ItemDisplay
        ItemDisplay display = (ItemDisplay) Main.WORLD.spawnEntity(loc, EntityType.ITEM_DISPLAY);
        display.setItemStack(stack);
        Transformation transformation = display.getTransformation();
        display.setTransformation(new Transformation(transformation.getTranslation(), transformation.getLeftRotation(), new Vector3f(1.001f, 1.001f, 1.001f), transformation.getRightRotation()));


        stack.setAmount(stack.getAmount() - 1); // siden man plasserer ned blokken så må det tas vekk fra inventoriet


        // registrer alteret
        UUID playerID = e.getPlayer().getUniqueId();
        Location blockLoc = new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        RevivalAltar altar = new RevivalAltar(Manager.getMember(playerID).tribe(), blockLoc, meta.getCustomModelData() == 77011); // HUSK Å GJØR AT MAN KAN SETTE NED AKTIVERTE ALTERE
        altar.updateInfo(hopper.getInventory());
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        if (e.getBlock().getType() == Material.HOPPER) {
            Block block = e.getBlock();
            Hopper hopper = (Hopper) block.getState();
            if (!Objects.equals(hopper.customName(), Component.text("Altar of Revival"))) {return;}
            e.setDropItems(false); // gjør at alle items inkludert hopperen ikke dropper

            // dropper itemsene manuelt istedet sjekk med fler heads
            Location loc = block.getLocation();
            World world = block.getWorld();
            hopper.getInventory().forEach(item -> {
                if (item != null && !(item.getType() == Material.FIREWORK_STAR && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 77012)) {
                    world.dropItemNaturally(loc, item); // dropper ikke settings itemet
                }
            });
            for (Entity entity : world.getNearbyEntities(block.getLocation().toCenterLocation(), 0.5, 0.5, 0.5)) {
                if (entity instanceof ItemDisplay) {
                    entity.remove();
                }
            }
            RevivalAltar altar = Manager.getAltar(loc);
            altar.remove(); // fjerner alteret i systemet og dropper items som har blitt consuma
        } else if (e.getBlock().getType() == Material.PLAYER_HEAD) { // når man fjerner hode over et alter
            Location loc = e.getBlock().getLocation();
            loc.add(0, -1, 0); // flytter oss ned 1 blocc for å skjekke om det er et alter der
            RevivalAltar altar = Manager.getAltar(loc);
            if (altar == null) return;
            altar.selectPlayer(null);
        }
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) { // gjør at skulk shriekers placa av players kan spawne wardens
        if (e.getBlockPlaced().getType() == Material.SCULK_SHRIEKER) {
            Block block = e.getBlock();
            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), String.format("setblock %d %d %d sculk_shrieker[can_summon=true]", block.getX(), block.getY(), block.getZ()));
        } else if (e.getBlockPlaced().getType() == Material.PLAYER_HEAD) { // når man plasserer et hode på et alter
            Location loc = e.getBlockPlaced().getLocation();
            loc.add(0, -1, 0); // flytter oss ned 1 blocc for å skjekke om det er et alter der
            RevivalAltar altar = Manager.getAltar(loc);
            if (altar == null) return;
            Skull head = (Skull) e.getBlock().getState();
            OfflinePlayer p = head.getOwningPlayer();
            if (p == null) return;
            UUID owner = p.getUniqueId();
            altar.selectPlayer(owner);
        }
    }
}
