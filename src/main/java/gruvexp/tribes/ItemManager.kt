package gruvexp.tribes

import gruvexp.tribes.Tribes.getTribe
import gruvexp.tribes.Tribes.getTribes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.OfflinePlayer
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.ShapelessRecipe
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType
import java.util.*
import java.util.List

object ItemManager {
    // recipes og items
    val playerCoins = mutableMapOf<UUID, Map<String, ItemStack>>()
    private val playersThatHasRegisteredRecipes = HashSet<UUID?>()

    @JvmStatic
    fun registerCoinItems() { // registrerer coins for alle registrerte members
        val tribes = getTribes()
        for (tribe in tribes) {
            registerCoinItems(tribe.playerId)
        }
    }

    @JvmStatic
    fun getStarterItems(playerId: UUID): ItemStack { // gir 4 gold kromer til en player
        val goldCoin = ItemStack(Material.FIREWORK_STAR)

        val goldMeta = goldCoin.getItemMeta()
        goldMeta.displayName(Component.text("Gold Coin").color(NamedTextColor.GOLD))
        val playerName = getTribe(playerId)!!.playerName
        goldMeta.lore(
            List.of<TextComponent>(
                Component.text("64 Kromer"),
                Component.text(playerName).color(getTribe(playerId)!!.color)
            )
        )
        goldMeta.setCustomModelData(77002)
        goldCoin.setAmount(5)
        return goldCoin
    }

    @JvmStatic
    fun toKromer(item: ItemStack): Int {
        val modelID = item.getItemMeta().getCustomModelData()
        return when (modelID) {
            77000 -> 1
            77001 -> 8
            77002 -> 64
            77003 -> 512
            77004 -> 4096
            else -> 0
        } * item.getAmount()
    }

    fun toItems(kromer: Int, ownerID: UUID): ArrayList<ItemStack?> { // owner = playerName
        var kromer = kromer
        require(playerCoins.containsKey(ownerID)) { "Error when making coins: owner \"" + getTribe(ownerID)!!.name + "\" is not registered" }
        val coins = ArrayList<ItemStack?>()
        val netheriteCoins = kromer / 4096
        if (netheriteCoins > 0) {
            kromer -= netheriteCoins * 4096
            playerCoins.get(ownerID)!!.get("netherite")!!.setAmount(netheriteCoins)
            coins.add(playerCoins.get(ownerID)!!.get("netherite"))
        }
        val diamondCoins = kromer / 512
        if (diamondCoins > 0) {
            kromer -= diamondCoins * 512
            playerCoins.get(ownerID)!!.get("diamond")!!.setAmount(diamondCoins)
            coins.add(playerCoins.get(ownerID)!!.get("diamond"))
        }
        val goldCoins = kromer / 64
        if (goldCoins > 0) {
            kromer -= goldCoins * 64
            playerCoins.get(ownerID)!!.get("gold")!!.setAmount(goldCoins)
            coins.add(playerCoins.get(ownerID)!!.get("gold"))
        }
        val ironCoins = kromer / 8
        if (ironCoins > 0) {
            kromer -= ironCoins * 8
            playerCoins.get(ownerID)!!.get("iron")!!.setAmount(ironCoins)
            coins.add(playerCoins.get(ownerID)!!.get("iron"))
        }
        if (kromer > 0) {
            playerCoins.get(ownerID)!!.get("copper")!!.setAmount(kromer)
            coins.add(playerCoins.get(ownerID)!!.get("copper"))
        }
        return coins
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
        itemMeta.lore(List.of<Component?>(deathMessage))
        item.setItemMeta(itemMeta)
        return item
    }

    fun registerCoinItems(playerId: UUID) { // brukes kun publically når en ny player joiner
        if (playerCoins.containsKey(playerId)) {
            return
        } // hvis playeren allerede he registrert coin items

        val tribe = getTribe(playerId)

        val ownerLore: Component = Component.text(tribe!!.name).color(tribe.color)

        val COPPER_COIN = ItemStack(Material.FIREWORK_STAR)
        val IRON_COIN = ItemStack(Material.FIREWORK_STAR)
        val GOLD_COIN = ItemStack(Material.FIREWORK_STAR)
        val DIAMOND_COIN = ItemStack(Material.FIREWORK_STAR)
        val NETHERITE_COIN = ItemStack(Material.FIREWORK_STAR)

        val copperMeta = COPPER_COIN.getItemMeta()
        copperMeta.displayName(Component.text("Copper Coin").color(TextColor.color(216, 102, 67)))
        copperMeta.lore(List.of<Component?>(ownerLore))
        copperMeta.setCustomModelData(77000)
        COPPER_COIN.setItemMeta(copperMeta)

        val ironMeta = IRON_COIN.getItemMeta()
        ironMeta.displayName(Component.text("Iron Coin"))
        ironMeta.lore(List.of<Component?>(Component.text("8 Kromer"), ownerLore))
        ironMeta.setCustomModelData(77001)
        IRON_COIN.setItemMeta(ironMeta)

        val goldMeta = GOLD_COIN.getItemMeta()
        goldMeta.displayName(Component.text("Gold Coin").color(NamedTextColor.GOLD))
        goldMeta.lore(List.of<Component?>(Component.text("64 Kromer"), ownerLore))
        goldMeta.setCustomModelData(77002)
        GOLD_COIN.setItemMeta(goldMeta)

        val diamondMeta = DIAMOND_COIN.getItemMeta()
        diamondMeta.displayName(Component.text("Diamond Coin").color(TextColor.color(95, 220, 205)))
        diamondMeta.lore(List.of<Component?>(Component.text("512 Kromer"), ownerLore))
        diamondMeta.setCustomModelData(77003)
        DIAMOND_COIN.setItemMeta(diamondMeta)

        val netheriteMeta = NETHERITE_COIN.getItemMeta()
        netheriteMeta.displayName(Component.text("Netherite Coin").color(TextColor.color(195, 105, 90)))
        netheriteMeta.lore(List.of<Component?>(Component.text("4096 Kromer"), ownerLore))
        netheriteMeta.setCustomModelData(77004)
        NETHERITE_COIN.setItemMeta(netheriteMeta)

        val coins = mapOf<String, ItemStack>(
            "copper" to COPPER_COIN,
            "iron" to IRON_COIN,
            "gold" to GOLD_COIN,
            "diamond" to DIAMOND_COIN,
            "netherite" to NETHERITE_COIN,
        )

        for (coin in coins.values) {
            val meta = coin.getItemMeta()
            meta.getPersistentDataContainer().set<String?, String?>(
                NamespacedKey(Main.getPlugin(), "owner"),
                PersistentDataType.STRING,
                playerId.toString()
            )
            coin.setItemMeta(meta)
        }

        playerCoins[playerId] = coins
    }

    fun registerCoinRecipes(playerId: UUID) { // recipes with coins

        if (playersThatHasRegisteredRecipes.contains(playerId)) {
            return
        } // returnerer hvis playeren allerede her registrert recipes


        val p = Bukkit.getPlayer(playerId)
        val playerName = getTribe(playerId)!!.playerName

        checkNotNull(p)

        val coins = playerCoins[playerId]!!

        val copperToIron =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), playerName + "_CopperIronRecipe"), coins.get("iron")!!)
        copperToIron.addIngredient(8, coins.get("copper")!!)
        Bukkit.addRecipe(copperToIron)

        //p.discoverRecipe(copperToIron.getKey());
        val ironToGold =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), playerName + "_IronGoldRecipe"), coins.get("gold")!!)
        ironToGold.addIngredient(8, coins.get("iron")!!)
        Bukkit.addRecipe(ironToGold)

        //p.discoverRecipe(ironToGold.getKey());
        val goldToDiamond =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), playerName + "_GoldDiamondRecipe"), coins.get("diamond")!!)
        goldToDiamond.addIngredient(8, coins.get("gold")!!)
        Bukkit.addRecipe(goldToDiamond)

        //p.discoverRecipe(goldToDiamond.getKey());
        val diamondToNetherite = ShapelessRecipe(
            NamespacedKey(Main.getPlugin(), playerName + "_DiamondNetheriteRecipe"),
            coins.get("netherite")!!
        )
        diamondToNetherite.addIngredient(8, coins.get("diamond")!!)
        Bukkit.addRecipe(diamondToNetherite)

        //p.discoverRecipe(diamondToNetherite.getKey());
        coins.get("copper")!!.setAmount(8)
        val ironToCopper =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), playerName + "_IronCopperRecipe"), coins.get("copper")!!)
        ironToCopper.addIngredient(1, coins.get("iron")!!)
        Bukkit.addRecipe(ironToCopper)

        //p.discoverRecipe(ironToCopper.getKey());
        coins.get("iron")!!.setAmount(8)
        val goldToIron =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), playerName + "_GoldIronRecipe"), coins.get("iron")!!)
        goldToIron.addIngredient(1, coins.get("gold")!!)
        Bukkit.addRecipe(goldToIron)

        //p.discoverRecipe(goldToIron.getKey());
        coins.get("gold")!!.setAmount(8)
        val diamondToGold =
            ShapelessRecipe(NamespacedKey(Main.getPlugin(), playerName + "_DiamondGoldRecipe"), coins.get("gold")!!)
        diamondToGold.addIngredient(1, coins.get("diamond")!!)
        Bukkit.addRecipe(diamondToGold)

        //p.discoverRecipe(diamondToGold.getKey());
        coins.get("diamond")!!.setAmount(8)
        val netheriteToDiamond = ShapelessRecipe(
            NamespacedKey(Main.getPlugin(), playerName + "_NetheriteGoldRecipe"),
            coins.get("diamond")!!
        )
        netheriteToDiamond.addIngredient(1, coins.get("netherite")!!)
        Bukkit.addRecipe(netheriteToDiamond)
        //p.discoverRecipe(netheriteToDiamond.getKey());
        playersThatHasRegisteredRecipes.add(playerId)
    }
}
