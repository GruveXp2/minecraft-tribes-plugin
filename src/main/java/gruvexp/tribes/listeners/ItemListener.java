package gruvexp.tribes.listeners;

import gruvexp.tribes.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
        Member pickupingMember = Manager.getMember(p.getUniqueId());
        considerOwnerChange(itemStack, pickupingMember);
        item.setItemStack(itemStack);
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
                Member member = Manager.getMember(p.getUniqueId());
                if (member == null) return;
                assert itemStack != null;
                considerOwnerChange(itemStack, member);
                e.setCurrentItem(itemStack); // oppdaterer itemet i eventen
            }
        } else if (e.getView().title().equals(RevivalAltar.NAME) && /* Man er inne på et alter */
                ((e.getCurrentItem() != null && (e.getCurrentItem().getType() == Material.FIREWORK_STAR || e.getCurrentItem().getType() == Material.PLAYER_HEAD)) || /* Man plasserte ned et item i inventoriet */
                (e.getCursor().getType() == Material.FIREWORK_STAR || e.getCursor().getType() == Material.PLAYER_HEAD))) { /* Man plukka opp et item cursoren */

            inventory = e.getView().getTopInventory();
            Location loc = inventory.getLocation();
            //Manager.debugMessage("inv loc: " + Utils.toString(loc));
            RevivalAltar altar = Manager.getAltar(loc);
            ItemStack clickedItem = e.getCurrentItem();

            if (clickedItem.getType() == Material.FIREWORK_STAR && clickedItem.getItemMeta().hasCustomModelData() && clickedItem.getItemMeta().getCustomModelData() == 77012) { // trykka på settings itemet
                e.setCancelled(true); // sånn at de ikke plukker den greia opp
                // method som collekter items og updater den ting greinga
                if (altar.isActivated()) {
                    altar.collectCoinsForCooldownSkipping(inventory);
                } else {
                    altar.collectItemsForActivation(inventory);
                }
            } else {
                Inventory finalInventory = inventory;
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> { // bøgger noen ganger, hvis man hovrer over settings itemet rett etterpå så endres han ikke
                    altar.updateInfo(finalInventory); // sånn at han skjekker etter inv eventen har skjedd
                }, 4);
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
        if (e.getView().title().equals(RevivalAltar.NAME) && (item.getType() == Material.FIREWORK_STAR || item.getType() == Material.PLAYER_HEAD)) {
            Inventory inventory = e.getInventory();
            //Manager.debugMessage("A");
            Location loc = inventory.getLocation();
            RevivalAltar altar = Manager.getAltar(loc);
            if (item.getType() == Material.FIREWORK_STAR && item.getItemMeta().hasCustomModelData() && item.getItemMeta().getCustomModelData() == 77012) { // trykka på settings itemet
                //Manager.debugMessage("A1");
                e.setCancelled(true); // sånn at de ikke plukker den greia opp
                // method som collekter items og updater den ting greinga
                if (altar.isActivated()) {
                    altar.collectCoinsForCooldownSkipping(inventory);
                } else {
                    altar.collectItemsForActivation(inventory);
                }
            } else {
                //Manager.debugMessage("A2");
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> { // bøgger noen ganger, hvis man hovrer over settings itemet rett etterpå så endres han ikke
                    altar.updateInfo(inventory); // sånn at han skjekker etter inv eventen har skjedd
                }, 1);
            }
        }
    }

    private void considerOwnerChange(ItemStack itemStack, Member pickupingMember) { // member er den som plukka itemet opp
        if (itemStack.getType() != Material.FIREWORK_STAR && itemStack.getType() != Material.PLAYER_HEAD) {return;} // hvis det ikke er en firework_star som brukes til coins eller player heads, returner
        ItemMeta meta = itemStack.getItemMeta();

        if (itemStack.getType() == Material.FIREWORK_STAR && meta.hasCustomModelData() && meta.getCustomModelData() >= 77000 && meta.getCustomModelData() < 77005) { // skjekker om itemet er en coin (customodeldata er mellom 77000 og 77004)
            List<Component> lore = meta.lore();
            if (lore != null) {
                String prevPlayerName = PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1)); // andre linje i loren er eieren av coinsene
                Member prevOwner = Manager.getMember(Bukkit.getOfflinePlayer(prevPlayerName).getUniqueId());
                int kromers = ItemManager.toKromer(itemStack);
                lore.set(lore.size() - 1, Component.text(pickupingMember.NAME).color(Manager.toTextColor(pickupingMember.tribe().COLOR)));
                meta.lore(lore);
                pickupingMember.addKromers(kromers); // adder kromers til playeren som plukka de opp
                prevOwner.addKromers(-kromers); // fjerner kromers til playeren som eide det fra før av
            }
        } else { // player head
            meta.getPersistentDataContainer().set(new NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING, pickupingMember.NAME);
        }
        itemStack.setItemMeta(meta);
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
                String playerName = PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1)); // andre linje i loren er eieren av coinsene
                Player p = Bukkit.getPlayer(playerName);

                if (p != null && p.isOnline()) { // hvis playeren er online så telporteres itemet til playeren, hvis ikke så bare blir itemet liggans
                    item.teleport(p);
                    item.setPickupDelay(0);
                }
            }
        } else if (type == Material.PLAYER_HEAD) { // player head
            Cancellable cancellable = (Cancellable) e;
            cancellable.setCancelled(true); // player heads kanke ødlegges uansett hva
            String owner = meta.getPersistentDataContainer().get(new NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING);
            if (owner == null) return;
            Player p = Bukkit.getPlayer(owner);
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
}
