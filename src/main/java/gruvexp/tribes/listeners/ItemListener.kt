package gruvexp.tribes.listeners

import gruvexp.tribes.Coin
import gruvexp.tribes.ItemManager.toKromer
import gruvexp.tribes.Main
import gruvexp.tribes.Tribe
import gruvexp.tribes.Tribes.addKromersToPool
import gruvexp.tribes.Tribes.debugMessage
import gruvexp.tribes.Tribes.getTribe
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.block.ShulkerBox
import org.bukkit.entity.Entity
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityEvent
import org.bukkit.event.entity.EntityPickupItemEvent
import org.bukkit.event.entity.ItemDespawnEvent
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryDragEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.BlockStateMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.util.Vector
import java.util.UUID
import kotlin.math.cos
import kotlin.math.sin

class ItemListener : Listener {
    @EventHandler
    fun onDespawn(e: ItemDespawnEvent) {
        handleItemDestruction(e, e.entity)
    }

    @EventHandler
    fun onDestroyed(e: EntityDamageEvent) {
        if (e.entity !is Item) {
            return
        }
        handleItemDestruction(e, e.entity as Item)
    }

    @EventHandler
    fun onItemPickup(e: EntityPickupItemEvent) { // når man plukker opp item fra bakken
        val entity: Entity = e.entity
        if (entity !is Player) {
            return
        }
        val item = e.item
        val itemStack = item.itemStack
        val p = e.entity as Player
        val pickupingTribe = getTribe(p.uniqueId)
        if (pickupingTribe == null) {
            Bukkit.broadcast(Component.text("Error! Player ${p.name} tried to pick up a coin/head but arent registered in the tribe plugin. pls contact gruve"))
            return
        }
        val changedOwner = considerOwnerChange(itemStack, pickupingTribe)
        if (changedOwner) item.itemStack = itemStack
    }

    @EventHandler
    fun onInventoryClick(e: InventoryClickEvent) { // når man trykker i et inventory
        // Manager.debugMessage("O");
        val inventory = e.clickedInventory ?: return
        // gjør at items ikke despawner randomly

        if (inventory.type == InventoryType.CHEST) {
            // Manager.debugMessage("Å");
            // Check if the action was a pickup
            when(e.action) {
                InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_HALF, InventoryAction.PICKUP_ONE,
                InventoryAction.PICKUP_SOME, InventoryAction.MOVE_TO_OTHER_INVENTORY -> {
                    val itemStack = e.currentItem
                    val p = e.whoClicked as Player
                    val tribe = getTribe(p.uniqueId) ?: return
                    checkNotNull(itemStack)
                    considerOwnerChange(itemStack, tribe)
                    e.currentItem = itemStack // oppdaterer itemet i eventen
                }
                else -> {}
            }
        }
    }

    @EventHandler
    fun onInventoryDrag(e: InventoryDragEvent) {
        //Manager.debugMessage("*");
        var item = e.cursor
        if (item == null) {
            //Manager.debugMessage("Cursor is null");
            item = e.oldCursor
            //Manager.debugMessage(item.getType().toString());
        }
    }

    private fun considerOwnerChange(
        itemStack: ItemStack,
        pickupingTribe: Tribe
    ): Boolean { // member er den som plukka itemet opp
        if (itemStack.type != Material.FIREWORK_STAR && itemStack.type != Material.PLAYER_HEAD) {
            return false
        } // hvis det ikke er en firework_star som brukes til coins eller player heads, returner

        val meta = itemStack.itemMeta

        if (itemStack.type == Material.FIREWORK_STAR && meta.hasCustomModelData() && meta.customModelData >= 77000 && meta.customModelData < 77005) { // skjekker om itemet er en coin (customodeldata er mellom 77000 og 77004)
            val lore = meta.lore()
            if (lore != null) {
                val kromers = toKromer(itemStack)
                val prevPlayerIdStr = meta.persistentDataContainer
                    .get(NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING)
                if (prevPlayerIdStr == Coin.KROMER_POOL_ID) {
                    addKromersToPool(-kromers)
                    debugMessage("transfered " + kromers + " kromers: pool -> " + pickupingTribe.name)
                } else {
                    val prevPlayerId = UUID.fromString(prevPlayerIdStr)
                    val prevOwner = getTribe(prevPlayerId)
                    if (prevOwner == null) {
                        Bukkit.broadcast(Component.text("Error: failed to change coin owners (pls contact gruve)"))
                        return false
                    }
                    prevOwner.addKromers(-kromers) // fjerner kromers til playeren som eide det fra før av
                    debugMessage("transfered " + kromers + " kromers: " + prevOwner.name + " -> " + pickupingTribe.name)
                }
                lore[lore.size - 1] = pickupingTribe.displayName
                meta.persistentDataContainer.set(
                    NamespacedKey(Main.getPlugin(), "owner"),
                    PersistentDataType.STRING,
                    pickupingTribe.playerId.toString()
                )
                meta.lore(lore)
                pickupingTribe.addKromers(kromers) // adder kromers til playeren som plukka de opp //TODO: gjør at dette er 1 transaksjon!! ikke trekk fra og legg til på forskjellige steder
            }
        } else { // player head
            meta.persistentDataContainer.set(
                NamespacedKey(Main.getPlugin(), "owner"),
                PersistentDataType.STRING,
                pickupingTribe.playerId.toString()
            )
        }
        itemStack.itemMeta = meta
        return true
    }

    private fun handleItemDestruction(e: EntityEvent, item: Item) {
        val itemStack = item.itemStack
        val type = itemStack.type
        if (type != Material.FIREWORK_STAR && type != Material.PLAYER_HEAD && type != Material.SHULKER_BOX) {
            return
        }
        val meta = itemStack.itemMeta

        if (type == Material.FIREWORK_STAR && meta.hasCustomModelData()) { // sjekker om itemet er en coin (customModelData er større eller lik 77000)
            val lore = meta.lore()
            if (lore != null) {
                val cancellable = e as Cancellable
                cancellable.isCancelled = true // konverterer eventen til en cancellable sånn at men kan kanselere den
                //String playerName = PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1)); // andre linje i loren er eieren av coinsene
                val playerIDStr = meta.persistentDataContainer
                    .get(NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING)
                if (playerIDStr == null) return
                val playerID: UUID = getUUIDFromCoinItem(playerIDStr, lore)
                val p = Bukkit.getPlayer(playerID)

                if (p != null && p.isOnline) { // hvis playeren er online så telporteres itemet til playeren, hvis ikke så bare blir itemet liggans
                    item.teleport(p)
                    item.pickupDelay = 0
                }
            }
        } else if (type == Material.PLAYER_HEAD) { // player head
            val cancellable = e as Cancellable
            cancellable.isCancelled = true // player heads kanke ødlegges uansett hva
            val ownerIDStr = meta.persistentDataContainer
                .get(NamespacedKey(Main.getPlugin(), "owner"), PersistentDataType.STRING)
            if (ownerIDStr == null) return
            val ownerID = UUID.fromString(ownerIDStr)
            val p = Bukkit.getPlayer(ownerID)
            if (p != null && p.isOnline) { // hvis playeren er online så telporteres itemet til playeren, hvis ikke så bare blir itemet liggans
                item.teleport(p)
                item.pickupDelay = 0
            }
        } else { // shulker box
            val blockStateMeta = itemStack.itemMeta as BlockStateMeta
            val shulkerBox = blockStateMeta.blockState as ShulkerBox
            val inventory = shulkerBox.inventory
            val contents = inventory.contents
            val invSize = contents.size
            // spawner items som flyr ut i en ring i hver sin retning
            val step = 2 * Math.PI / invSize
            val loc = item.location
            Main.WORLD.playSound(loc, Sound.ENTITY_ITEM_FRAME_REMOVE_ITEM, 1.0f, 1.0f)
            for (i in 0..<invSize) {
                val x = cos(step * i)
                val z = sin(step * i)
                val contentItem = Main.WORLD.dropItemNaturally(loc, contents[i]!!)
                contentItem.velocity = Vector(0.1 * x, 0.1, 0.1 * z)
                contentItem.ticksLived = 11800 - i // 12min - 10s
            }
        }
    }

    companion object {
        fun getUUIDFromCoinItem(input: String?, lore: MutableList<Component>): UUID {
            val prevPlayerID: UUID
            if (input == null) { // Migrating to new system, from name system to uuid system
                var prevPlayerName = PlainTextComponentSerializer.plainText()
                    .serialize(lore[lore.size - 1]) // andre linje i loren er eieren av coinsene
                if (prevPlayerName == "Syntak") {
                    prevPlayerName = "_Syntak"
                } else if (prevPlayerName == "bossfight") {
                    prevPlayerName = "bossfight3"
                } else if (prevPlayerName == "yaringd") {
                    prevPlayerName = "legendaryyyyy"
                }
                val prevPlayer = Bukkit.getOfflinePlayer(prevPlayerName)
                prevPlayerID = prevPlayer.uniqueId
            } else { // this method will be the standard method
                prevPlayerID = UUID.fromString(input)
            }
            return prevPlayerID
        }
    }
}
