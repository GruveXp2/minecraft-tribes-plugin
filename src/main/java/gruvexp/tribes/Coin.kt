package gruvexp.tribes

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

enum class Coin(val itemModel: Int, val value: Int, val textColor: TextColor) {
    COPPER(77000, 1,TextColor.color(216, 102, 67)),
    IRON(77001, 8, NamedTextColor.WHITE),
    GOLD(77002, 64, NamedTextColor.GOLD),
    DIAMOND(77003, 512, TextColor.color(95, 220, 205)),
    NETHERITE(77004, 4096, TextColor.color(195, 105, 90));

    private val item = ItemStack(Material.FIREWORK_STAR).apply { editMeta { meta ->
        meta.setCustomModelData(itemModel)
        meta.displayName(Component.text("${name.let { it.replaceFirstChar{ c -> c.uppercase() } }} Coin", textColor))
    } }

    fun getItem(tribe: Tribe?): ItemStack {
        return item.clone().apply {
            lore(
                (lore() ?: mutableListOf()).apply { add(tribe?.displayName ?: Component.text("Pool", NamedTextColor.DARK_GRAY)) }
            )
            editMeta {
                it.persistentDataContainer.set(
                    NamespacedKey(Main.getPlugin(), "owner"),
                    PersistentDataType.STRING,
                    tribe?.playerId?.toString() ?: KROMER_POOL_ID
                )
            }
        }
    }

    companion object {
        const val KROMER_POOL_ID = "kromer_pool"

        fun toCoinType(itemModel: Int): Coin? {
            return entries.find { it.itemModel == itemModel }
        }
    }
}