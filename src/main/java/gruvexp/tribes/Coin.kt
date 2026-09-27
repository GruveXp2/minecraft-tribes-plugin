package gruvexp.tribes

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType

enum class Coin(val itemModel: String, val value: Int, val textColor: TextColor) {
    COPPER("copper_coin", 1,TextColor.color(216, 102, 67)),
    IRON("iron_coin", 8, NamedTextColor.WHITE),
    GOLD("gold_coin", 64, NamedTextColor.GOLD),
    DIAMOND("diamond_coin", 512, TextColor.color(95, 220, 205)),
    NETHERITE("netherite_coin", 4096, TextColor.color(195, 105, 90));

    private val item = ItemStack(Material.FIREWORK_STAR).apply { editMeta { meta ->
        val modelComponent = meta.customModelDataComponent
        modelComponent.strings = listOf(itemModel)
        meta.setCustomModelDataComponent(modelComponent)
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

        fun toCoinType(itemModel: String): Coin? {
            return entries.find { it.itemModel == itemModel }
        }

        fun isCoin(item: ItemStack): Boolean {
            val itemModel = item.itemMeta.customModelDataComponent.strings.first() ?: return false
            return Coin.toCoinType(itemModel) != null
        }

        fun toKromer(item: ItemStack): Int {
            val itemModel = item.itemMeta.customModelDataComponent.strings.first() ?: return 0
            return (Coin.toCoinType(itemModel)?.value ?: 0) * item.amount
        }
    }
}