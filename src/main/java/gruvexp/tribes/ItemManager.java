package gruvexp.tribes;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class ItemManager { // recipes og items

    public static final HashMap<UUID, HashMap<String, ItemStack>> playerCoins = new HashMap<>();
    public static ItemStack REVIVAL_ALTAR;
    public static ItemStack ACTIVATED_REVIVAL_ALTAR;
    private static final HashSet<String> playersThatHasRegisteredRecipes = new HashSet<>();

    public static void registerCoinItems() { // registrerer coins for alle registrerte members
        Collection<Tribe> tribes = Manager.getTribes();
        for (Tribe tribe : tribes) {
            for (UUID playerID : tribe.getMemberIDs()) {
                registerCoinItems(playerID);
            }
        }
    }

    public static void registerAltar() {
        registerAltarItems();
        registerAltarRecipe();
    }
    
    public static ItemStack getStarterItems(UUID playerID) {
        ItemStack GOLD_COIN = new ItemStack(Material.FIREWORK_STAR);

        ItemMeta goldMeta = GOLD_COIN.getItemMeta();
        goldMeta.displayName(Component.text("Gold Coin").color(NamedTextColor.GOLD));
        String playerName = Manager.getMember(playerID).NAME;
        goldMeta.lore(List.of(Component.text("64 Kromer"), Component.text(playerName).color(Manager.toTextColor(Manager.getMember(playerID).tribe().COLOR))));
        goldMeta.setCustomModelData(77002);
        GOLD_COIN.setItemMeta(goldMeta);
        GOLD_COIN.setAmount(5);
        return GOLD_COIN;
    }

    public static int toKromer(ItemStack item) {
        int modelID = item.getItemMeta().getCustomModelData();
        return switch (modelID) {
            case 77000 -> 1;
            case 77001 -> 8;
            case 77002 -> 64;
            case 77003 -> 512;
            case 77004 -> 4096;
            default -> 0;
        } * item.getAmount();
    }

    public static ArrayList<ItemStack> toItems(int kromer, UUID ownerID) {// owner = playerName
        if (!playerCoins.containsKey(ownerID)) {
            throw new IllegalArgumentException("Error when making coins: owner \"" + Manager.getMember(ownerID).NAME + "\" is not registered");
        }
        ArrayList<ItemStack> coins = new ArrayList<>();
        int netheriteCoins = kromer / 4096;
        if (netheriteCoins > 0) {
            kromer -= netheriteCoins * 4096;
            playerCoins.get(ownerID).get("netherite").setAmount(netheriteCoins);
            coins.add(playerCoins.get(ownerID).get("netherite"));
        }
        int diamondCoins = kromer / 512;
        if (diamondCoins > 0) {
            kromer -= diamondCoins * 512;
            playerCoins.get(ownerID).get("diamond").setAmount(diamondCoins);
            coins.add(playerCoins.get(ownerID).get("diamond"));
        }
        int goldCoins = kromer / 64;
        if (goldCoins > 0) {
            kromer -= goldCoins * 64;
            playerCoins.get(ownerID).get("gold").setAmount(goldCoins);
            coins.add(playerCoins.get(ownerID).get("gold"));
        }
        int ironCoins = kromer / 8;
        if (ironCoins > 0) {
            kromer -= ironCoins * 8;
            playerCoins.get(ownerID).get("iron").setAmount(ironCoins);
            coins.add(playerCoins.get(ownerID).get("iron"));
        }
        if (kromer > 0) {
            playerCoins.get(ownerID).get("copper").setAmount(kromer);
            coins.add(playerCoins.get(ownerID).get("copper"));
        }
        return coins;
    }

    public static ItemStack getHead(OfflinePlayer p, Component deathMessage) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta itemMeta = (SkullMeta) item.getItemMeta();
        assert itemMeta != null;
        itemMeta.getPersistentDataContainer().set(new NamespacedKey(Main.getPlugin(), "uuid"), PersistentDataType.STRING, p.getUniqueId().toString());
        itemMeta.setOwningPlayer(p);
        itemMeta.lore(List.of(deathMessage));
        item.setItemMeta(itemMeta);
        return item;
    }

    public static void registerCoinItems(UUID playerID) { // brukes kun publically når en ny player joiner
        if (playerCoins.containsKey(playerID)) {return;} // hvis playeren allerede he registrert coin items
        Member member = Manager.getMember(playerID);

        Component ownerLore = Component.text(member.NAME).color(Manager.toTextColor(member.tribe().COLOR));

        ItemStack COPPER_COIN = new ItemStack(Material.FIREWORK_STAR);
        ItemStack IRON_COIN = new ItemStack(Material.FIREWORK_STAR);
        ItemStack GOLD_COIN = new ItemStack(Material.FIREWORK_STAR);
        ItemStack DIAMOND_COIN = new ItemStack(Material.FIREWORK_STAR);
        ItemStack NETHERITE_COIN = new ItemStack(Material.FIREWORK_STAR);

        ItemMeta copperMeta = COPPER_COIN.getItemMeta();
        copperMeta.displayName(Component.text("Copper Coin").color(TextColor.color(216, 102, 67)));
        copperMeta.lore(List.of(ownerLore));
        copperMeta.setCustomModelData(77000);
        COPPER_COIN.setItemMeta(copperMeta);

        ItemMeta ironMeta = IRON_COIN.getItemMeta();
        ironMeta.displayName(Component.text("Iron Coin"));
        ironMeta.lore(List.of(Component.text("8 Kromer"), ownerLore));
        ironMeta.setCustomModelData(77001);
        IRON_COIN.setItemMeta(ironMeta);

        ItemMeta goldMeta = GOLD_COIN.getItemMeta();
        goldMeta.displayName(Component.text("Gold Coin").color(NamedTextColor.GOLD));
        goldMeta.lore(List.of(Component.text("64 Kromer"), ownerLore));
        goldMeta.setCustomModelData(77002);
        GOLD_COIN.setItemMeta(goldMeta);

        ItemMeta diamondMeta = DIAMOND_COIN.getItemMeta();
        diamondMeta.displayName(Component.text("Diamond Coin").color(TextColor.color(95,220,205)));
        diamondMeta.lore(List.of(Component.text("512 Kromer"), ownerLore));
        diamondMeta.setCustomModelData(77003);
        DIAMOND_COIN.setItemMeta(diamondMeta);

        ItemMeta netheriteMeta = NETHERITE_COIN.getItemMeta();
        netheriteMeta.displayName(Component.text("Netherite Coin").color(TextColor.color(195, 105, 90)));
        netheriteMeta.lore(List.of(Component.text("4096 Kromer"), ownerLore));
        netheriteMeta.setCustomModelData(77004);
        NETHERITE_COIN.setItemMeta(netheriteMeta);

        HashMap<String, ItemStack> coins = new HashMap<>();
        coins.put("copper", COPPER_COIN);
        coins.put("iron", IRON_COIN);
        coins.put("gold", GOLD_COIN);
        coins.put("diamond", DIAMOND_COIN);
        coins.put("netherite", NETHERITE_COIN);
        playerCoins.put(playerID, coins);
    }

    private static void registerAltarItems() {
        REVIVAL_ALTAR = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta altarMeta = REVIVAL_ALTAR.getItemMeta();
        altarMeta.displayName(Component.text("Altar of Revival", NamedTextColor.LIGHT_PURPLE));
        altarMeta.lore(List.of(Component.text("not activated")));
        altarMeta.setCustomModelData(77010);
        REVIVAL_ALTAR.setItemMeta(altarMeta);

        ACTIVATED_REVIVAL_ALTAR = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta activeAltarMeta = ACTIVATED_REVIVAL_ALTAR.getItemMeta();
        activeAltarMeta.displayName(Component.text("Altar of Revival", NamedTextColor.LIGHT_PURPLE));
        activeAltarMeta.lore(List.of(Component.text("activated")));
        activeAltarMeta.setCustomModelData(77011);
        ACTIVATED_REVIVAL_ALTAR.setItemMeta(activeAltarMeta);
    }

    public static void registerCoinRecipes(UUID playerID) { // recipes with coins

        if (playersThatHasRegisteredRecipes.contains(playerID)) {return;} // returnerer hvis playeren allerede her registrert recipes

        Player p = Bukkit.getPlayer(playerID);
        String playerName = Manager.getMember(playerID).NAME;

        assert p != null;

        HashMap<String, ItemStack> coins = playerCoins.get(playerID);

        ShapelessRecipe copperToIron = new ShapelessRecipe(new NamespacedKey(Main.getPlugin(), playerName + "_CopperIronRecipe"), coins.get("iron"));
        copperToIron.addIngredient(8, coins.get("copper"));
        Bukkit.addRecipe(copperToIron);
        //p.discoverRecipe(copperToIron.getKey());

        ShapelessRecipe ironToGold = new ShapelessRecipe(new NamespacedKey(Main.getPlugin(), playerName + "_IronGoldRecipe"), coins.get("gold"));
        ironToGold.addIngredient(8, coins.get("iron"));
        Bukkit.addRecipe(ironToGold);
        //p.discoverRecipe(ironToGold.getKey());

        ShapelessRecipe goldToDiamond = new ShapelessRecipe(new NamespacedKey(Main.getPlugin(), playerName + "_GoldDiamondRecipe"), coins.get("diamond"));
        goldToDiamond.addIngredient(8, coins.get("gold"));
        Bukkit.addRecipe(goldToDiamond);
        //p.discoverRecipe(goldToDiamond.getKey());

        ShapelessRecipe diamondToNetherite = new ShapelessRecipe(new NamespacedKey(Main.getPlugin(), playerName + "_DiamondNetheriteRecipe"), coins.get("netherite"));
        diamondToNetherite.addIngredient(8, coins.get("diamond"));
        Bukkit.addRecipe(diamondToNetherite);
        //p.discoverRecipe(diamondToNetherite.getKey());

        coins.get("copper").setAmount(8);
        ShapelessRecipe ironToCopper = new ShapelessRecipe(new NamespacedKey(Main.getPlugin(), playerName + "_IronCopperRecipe"), coins.get("copper"));
        ironToCopper.addIngredient(1, coins.get("iron"));
        Bukkit.addRecipe(ironToCopper);
        //p.discoverRecipe(ironToCopper.getKey());

        coins.get("iron").setAmount(8);
        ShapelessRecipe goldToIron = new ShapelessRecipe(new NamespacedKey(Main.getPlugin(), playerName + "_GoldIronRecipe"), coins.get("iron"));
        goldToIron.addIngredient(1, coins.get("gold"));
        Bukkit.addRecipe(goldToIron);
        //p.discoverRecipe(goldToIron.getKey());

        coins.get("gold").setAmount(8);
        ShapelessRecipe diamondToGold = new ShapelessRecipe(new NamespacedKey(Main.getPlugin(), playerName + "_DiamondGoldRecipe"), coins.get("gold"));
        diamondToGold.addIngredient(1, coins.get("diamond"));
        Bukkit.addRecipe(diamondToGold);
        //p.discoverRecipe(diamondToGold.getKey());

        coins.get("diamond").setAmount(8);
        ShapelessRecipe netheriteToDiamond = new ShapelessRecipe(new NamespacedKey(Main.getPlugin(), playerName + "_NetheriteGoldRecipe"), coins.get("diamond"));
        netheriteToDiamond.addIngredient(1, coins.get("netherite"));
        Bukkit.addRecipe(netheriteToDiamond);
        //p.discoverRecipe(netheriteToDiamond.getKey());
        playersThatHasRegisteredRecipes.add(playerName);
    }

    private static void registerAltarRecipe() { // recipes without coins
        // create a NamespacedKey for your recipe
        NamespacedKey altarrecipe = new NamespacedKey(Main.getPlugin(), "altar_of_revival");

        ShapedRecipe revivalAltar = new ShapedRecipe(altarrecipe, REVIVAL_ALTAR);

        revivalAltar.shape(" T ", "WCW", "SBS");

        revivalAltar.setIngredient('T', Material.TOTEM_OF_UNDYING);
        revivalAltar.setIngredient('W', Material.LIGHT_BLUE_CARPET);
        revivalAltar.setIngredient('C', Material.RECOVERY_COMPASS);
        revivalAltar.setIngredient('S', Material.SMOOTH_STONE);
        revivalAltar.setIngredient('B', Material.BEACON);
        Bukkit.addRecipe(revivalAltar);
    }
}
