package gruvexp.tribes.listeners;

import gruvexp.tribes.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.*;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.inventory.*;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.UUID;

public class ItemListener implements Listener {

    @EventHandler
    public void onDespawn(ItemDespawnEvent e) {
        handleItemDestruction(e, e.getEntity());
    }

    @EventHandler
    public void onDestroyed(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Item)) {return;}
        handleItemDestruction(e, (Item) e.getEntity());
    }

    @EventHandler
    public void onItemPickup(EntityPickupItemEvent e) { // når man plukker opp item fra bakken
        Entity entity = e.getEntity();
        if (!(entity instanceof Player)) {
            return;
        }
        Item item = e.getItem();
        ItemStack itemStack = item.getItemStack();
        Player p = (Player) e.getEntity();
        Tribe pickupingTribe = Tribes.getTribe(p.getUniqueId());
        if (pickupingTribe == null) {
            Bukkit.broadcast(Component.text("Error! Player " + p.getName() + " tried to pick up a coin/head but arent registered in the tribe plugin. pls contact gruve"));
            return;
        }
        boolean changedOwner = considerOwnerChange(itemStack, pickupingTribe);
        if (changedOwner) item.setItemStack(itemStack);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) { // når man trykker i et inventory
        // Manager.debugMessage("O");
        Inventory inventory = e.getClickedInventory();
        if (inventory == null) {return;} // gjør at items ikke despawner randomly
        if (inventory.getType() == InventoryType.CHEST) {
            // Manager.debugMessage("Å");
            // Check if the action was a pickup
            if (e.getAction() == InventoryAction.PICKUP_ALL || e.getAction() == InventoryAction.PICKUP_HALF || e.getAction() == InventoryAction.PICKUP_ONE || e.getAction() == InventoryAction.PICKUP_SOME || e.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                ItemStack itemStack = e.getCurrentItem();
                Player p = (Player) e.getWhoClicked();
                Tribe tribe = Tribes.getTribe(p.getUniqueId());
                if (tribe == null) return;
                assert itemStack != null;
                considerOwnerChange(itemStack, tribe);
                e.setCurrentItem(itemStack); // oppdaterer itemet i eventen
            }
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent e) {
        //Manager.debugMessage("*");
        ItemStack item = e.getCursor();
        if (item == null) {
            //Manager.debugMessage("Cursor is null");
            item = e.getOldCursor();
            //Manager.debugMessage(item.getType().toString());
        }
    }

    private boolean considerOwnerChange(ItemStack itemStack, Tribe pickupingTribe) { // member er den som plukka itemet opp
        if (itemStack.getType() != Material.FIREWORK_STAR && itemStack.getType() != Material.PLAYER_HEAD) {return false;} // hvis det ikke er en firework_star som brukes til coins eller player heads, returner
        ItemMeta meta = itemStack.getItemMeta();

        if (itemStack.getType() == Material.FIREWORK_STAR && meta.hasCustomModelData() && meta.getCustomModelData() >= 77000 && meta.getCustomModelData() < 77005) { // skjekker om itemet er en coin (customodeldata er mellom 77000 og 77004)
            List<Component> lore = meta.lore();
            if (lore != null) {
                String prevPlayerIDstr = meta.getPersistentDataContainer().get(new NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING);
                UUID prevPlayerID = getUUIDFromCoinItem(prevPlayerIDstr, lore);
                Tribe prevOwner = Tribes.getTribe(prevPlayerID);
                if (prevOwner == null) {
                    Bukkit.broadcast(Component.text("Error: failed to change coin owners (pls contact gruve)"));
                    return false;
                }
                int kromers = ItemManager.toKromer(itemStack);
                lore.set(lore.size() - 1, pickupingTribe.getDisplayName());
                meta.getPersistentDataContainer().set(new NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING, pickupingTribe.getPlayerId().toString());
                meta.lore(lore);
                pickupingTribe.addKromers(kromers); // adder kromers til playeren som plukka de opp
                prevOwner.addKromers(-kromers); // fjerner kromers til playeren som eide det fra før av
            }
        } else { // player head
            meta.getPersistentDataContainer().set(new NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING, pickupingTribe.getPlayerId().toString());
        }
        itemStack.setItemMeta(meta);
        return true;
    }

    private void handleItemDestruction(EntityEvent e, Item item) {
        ItemStack itemStack = item.getItemStack();
        Material type = itemStack.getType();
        if (type != Material.FIREWORK_STAR && type != Material.PLAYER_HEAD && type != Material.SHULKER_BOX) {return;}
        ItemMeta meta = itemStack.getItemMeta();

        if (type == Material.FIREWORK_STAR && meta.hasCustomModelData() && meta.getCustomModelData() >= 77000) { // sjekker om itemet er en coin (customModelData er større eller lik 77000)
            List<Component> lore = meta.lore();
            if (lore != null) {
                Cancellable cancellable = (Cancellable) e;
                cancellable.setCancelled(true); // konverterer eventen til en cancellable sånn at men kan kanselere den
                //String playerName = PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1)); // andre linje i loren er eieren av coinsene
                String playerIDStr = meta.getPersistentDataContainer().get(new NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING);
                if (playerIDStr == null) return;
                UUID playerID = getUUIDFromCoinItem(playerIDStr, lore);
                Player p = Bukkit.getPlayer(playerID);

                if (p != null && p.isOnline()) { // hvis playeren er online så telporteres itemet til playeren, hvis ikke så bare blir itemet liggans
                    item.teleport(p);
                    item.setPickupDelay(0);
                }
            }
        } else if (type == Material.PLAYER_HEAD) { // player head
            Cancellable cancellable = (Cancellable) e;
            cancellable.setCancelled(true); // player heads kanke ødlegges uansett hva
            String ownerIDStr = meta.getPersistentDataContainer().get(new NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING);
            if (ownerIDStr == null) return;
            UUID ownerID = UUID.fromString(ownerIDStr);
            Player p = Bukkit.getPlayer(ownerID);
            if (p != null && p.isOnline()) { // hvis playeren er online så telporteres itemet til playeren, hvis ikke så bare blir itemet liggans
                item.teleport(p);
                item.setPickupDelay(0);
            }
        } else { // shulker box
            BlockStateMeta blockStateMeta = (BlockStateMeta) itemStack.getItemMeta();
            ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
            Inventory inventory = shulkerBox.getInventory();
            ItemStack[] contents = inventory.getContents();
            int invSize = contents.length;
            // spawner items som flyr ut i en ring i hver sin retning
            double step = 2*Math.PI / invSize;
            Location loc = item.getLocation();
            Main.WORLD.playSound(loc, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f);
            for (int i = 0; i < invSize; i++) {
                double x = Math.cos(step * i);
                double z = Math.sin(step * i);
                Item contentItem = Main.WORLD.dropItemNaturally(loc, contents[i]);
                contentItem.setVelocity(new Vector(0.1 * x, 0.1, 0.1 * z));
                contentItem.setTicksLived(11800 - i); // 12min - 10s
            }
        }
    }

    public static UUID getUUIDFromCoinItem(String input, List<Component> lore) {
        UUID prevPlayerID;
        if (input == null) { // Migrating to new system, from name system to uuid system
            String prevPlayerName = PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1)); // andre linje i loren er eieren av coinsene
            if (prevPlayerName.equals("Syntak")) {
                prevPlayerName = "_Syntak";
            } else if (prevPlayerName.equals("bossfight")) {
                prevPlayerName = "bossfight3";
            } else if (prevPlayerName.equals("yaringd")) {
                prevPlayerName = "legendaryyyyy";
            }
            OfflinePlayer prevPlayer = Bukkit.getOfflinePlayer(prevPlayerName);
            prevPlayerID = prevPlayer.getUniqueId();
        } else { // this method will be the standard method
            prevPlayerID = UUID.fromString(input);
        }
        return prevPlayerID;
    }
}
