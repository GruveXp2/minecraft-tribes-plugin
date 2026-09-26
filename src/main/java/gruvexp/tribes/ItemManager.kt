package gruvexp.tribes

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType

object ItemManager {
    // recipes og items
    val playerCoins = mutableMapOf<Tribe, Map<Coin, ItemStack>>()
    val poolCoins = Coin.entries.associateWith { it.getItem(null) }
    private val tribesThatRegisteredRecipes = mutableSetOf<Tribe>()

    @JvmStatic
    fun registerCoinItems() { // registrerer coins for alle registrerte members
        Tribes.getTribes().forEach { registerCoinItems(it) }
    }

    fun getPoolCoins(coin: Coin, amount: Int): ItemStack {
        return poolCoins.getValue(coin).apply { this.amount = amount }
    }

    @JvmStatic
    fun toKromer(item: ItemStack): Int {
        val itemModel = item.itemMeta.customModelData
        return (Coin.toCoinType(itemModel)?.value ?: 0) * item.amount
    }

    fun toItems(kromer: Int, tribe: Tribe): List<ItemStack> { // owner = playerName
        var kromersLeft = kromer
        val coins = playerCoins[tribe] ?: error("Error when making coins: owner \"${tribe.name}\" isnt registered")
        val returnedCoins = mutableListOf<ItemStack>()
        Coin.entries.sortedBy { it.value }.forEach { coin ->
            val coinAmount = kromersLeft / coin.value
            if (coinAmount > 0) {
                kromersLeft -= coinAmount * coin.value
                returnedCoins.add(coins[coin]!!.apply { amount = coinAmount })
            }
        }
        return returnedCoins
    }

    @JvmStatic
    fun getHead(p: OfflinePlayer, deathMessage: Component): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD)
        val itemMeta = checkNotNull(item.getItemMeta() as SkullMeta)
        itemMeta.getPersistentDataContainer().set<String?, String?>(
            NamespacedKey(Main.getPlugin(), "uuid"),
            PersistentDataType.STRING,
            p.getUniqueId().toString()
        )
        itemMeta.setOwningPlayer(p)
        itemMeta.lore(listOf(deathMessage))
        item.setItemMeta(itemMeta)
        return item
    }

    fun registerCoinItems(tribe: Tribe) { // brukes kun når en ny player joiner
        if (playerCoins.containsKey(tribe)) return

        playerCoins[tribe] = Coin.entries
            .associateWith { it.getItem(tribe) }
    }

    fun registerCoinRecipes(tribe: Tribe) { // recipes with coins
        if (tribesThatRegisteredRecipes.contains(tribe)) {
            return
        } // returnerer hvis playeren allerede her registrert recipes

        val coins = playerCoins[tribe]!!

        val copperToIron =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), "${tribe.playerId}_CopperIronRecipe"), coins[Coin.IRON]!!)
        copperToIron.addIngredient(8, coins.get(Coin.COPPER)!!)
        Bukkit.addRecipe(copperToIron)

        //p.discoverRecipe(copperToIron.getKey());
        val ironToGold =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), "${tribe.playerId}_IronGoldRecipe"), coins[Coin.GOLD]!!)
        ironToGold.addIngredient(8, coins[Coin.IRON]!!)
        Bukkit.addRecipe(ironToGold)

        //p.discoverRecipe(ironToGold.getKey());
        val goldToDiamond =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), "${tribe.playerId}_GoldDiamondRecipe"), coins[Coin.DIAMOND]!!)
        goldToDiamond.addIngredient(8, coins[Coin.GOLD]!!)
        Bukkit.addRecipe(goldToDiamond)

        //p.discoverRecipe(goldToDiamond.getKey());
        val diamondToNetherite = ShapelessRecipe(
            NamespacedKey(Main.getPlugin(), "${tribe.playerId}_DiamondNetheriteRecipe"),
            coins.get(Coin.NETHERITE)!!
        )
        diamondToNetherite.addIngredient(8, coins[Coin.DIAMOND]!!)
        Bukkit.addRecipe(diamondToNetherite)

        //p.discoverRecipe(diamondToNetherite.getKey());
        coins[Coin.COPPER]!!.amount = 8
        val ironToCopper =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), "${tribe.playerId}_IronCopperRecipe"), coins[Coin.COPPER]!!)
        ironToCopper.addIngredient(1, coins[Coin.IRON]!!)
        Bukkit.addRecipe(ironToCopper)

        //p.discoverRecipe(ironToCopper.getKey());
        coins[Coin.IRON]!!.amount = 8
        val goldToIron =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), "${tribe.playerId}_GoldIronRecipe"), coins[Coin.IRON]!!)
        goldToIron.addIngredient(1, coins[Coin.GOLD]!!)
        Bukkit.addRecipe(goldToIron)

        //p.discoverRecipe(goldToIron.getKey());
        coins[Coin.GOLD]!!.amount = 8
        val diamondToGold =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), "${tribe.playerId}_DiamondGoldRecipe"), coins[Coin.GOLD]!!)
        diamondToGold.addIngredient(1, coins[Coin.DIAMOND]!!)
        Bukkit.addRecipe(diamondToGold)

        //p.discoverRecipe(diamondToGold.getKey());
        Bukkit.addRecipe(
            ShapelessRecipe(
                NamespacedKey(Main.getPlugin(), "${tribe.playerId}_NetheriteGoldRecipe"),
                coins[Coin.DIAMOND]!!.apply { amount = 8 }
            ).apply { addIngredient(1, coins[Coin.NETHERITE]!!) }
        )
        //p.discoverRecipe(netheriteToDiamond.getKey());
        tribesThatRegisteredRecipes.add(tribe)
    }
}
