package gruvexp.tribes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import gruvexp.tribes.tasks.AltarCooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Hopper;
import org.bukkit.block.Skull;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class RevivalAltar implements PostInit{ // RESPAWN ALTER DATA: koordinat (key), cooldown, heads, kromers, aura status

    private static final int ACTIVATION_COST_KROMER = 128;
    private static final int ACTIVATION_COST_HEADS = 4;
    private static final int COOLDOWN_TIME = 60; // minutter
    private static final int REDUCED_MINUTES_PER_COIN = 3;
    public static final Component NAME = Component.text("Altar of Revival"); // minutter
    @JsonIgnore
    public Tribe TRIBE;
    @JsonIgnore
    public final Location LOCATION;
    @JsonProperty("kromer")
    private int storedKromer;
    @JsonIgnore
    private int addedKromer;
    @JsonProperty("heads")
    private final HashMap<String, Integer> storedHeads; // <player, antall hoder>

    private int cooldown;
    private AltarCooldown cooldownTask;
    @JsonIgnore
    private String selectedPlayer = "NONE"; // playeren som skal spawne, er bestemt av hodet oppå. ved load så skal dette beregnes
    //private boolean auraActive = false; // skal brukes når aura blir adda
    @JsonIgnore
    private String coinOwner = "NONE"; // den som sist putta inn items vil ta eierskap over alle itemsene som blir sendt ut eller droppa når alteret mines eller veksler penger tilbake

    @JsonIgnore
    public RevivalAltar(Tribe tribe, Location loc, boolean activated) {
        LOCATION = loc;
        TRIBE = tribe;
        cooldown = activated ? COOLDOWN_TIME : -1; // uaktiverte altere får cooldown satt til -1, istedenfor å ha en egen bool å holde styr på for om han er aktivert eller ikke
        storedHeads = new HashMap<>();
        Block block = loc.getBlock();
        if (block.getType() != Material.HOPPER) {
            throw new IllegalArgumentException("Failed to register revival altar: no hopper block found");
        }
        Hopper hopper = (Hopper) block.getState();
        hopper.getInventory().setItem(4, getActivationItem(0, 0, false, false)); // adder settings item
        Manager.registerAltar(loc, this);
        tribe.registerAltar(loc, this);
        if (activated && cooldown > 0) { // bare start cooldown dersom alteret er aktivert og det er en cooldown
            startNewCooldown();
        }
    }

    @SuppressWarnings("unused")
    public RevivalAltar(@JsonProperty("x") int x, @JsonProperty("y") int y, @JsonProperty("z") int z, @JsonProperty("cooldown") int cooldown, @JsonProperty("heads") HashMap<String, Integer> storedHeads, @JsonProperty("kromer") int storedKromer) { // storedkromer og storedhead setter separat
        LOCATION = new Location(Main.WORLD, x, y, z);
        this.cooldown = cooldown;
        this.storedHeads = Objects.requireNonNullElseGet(storedHeads, HashMap::new);
        this.storedKromer = storedKromer;
        Manager.schedulePostInit(this);
        if (cooldown > 0) { // bare start cooldown dersom alteret er aktivert og det er en cooldown
            startNewCooldown();
        }
    }

    public void registerTribe(Tribe tribe) {
        TRIBE = tribe;
    }

    public void postInit() {
        Manager.registerAltar(LOCATION, this);
        TRIBE.registerAltar(LOCATION, this);
        // skjekker om det er hode oppå
        Block headBlock = LOCATION.clone().add(0, 1, 0).getBlock();
        if (headBlock.getType() == Material.PLAYER_HEAD) {
            //Manager.debugMessage("RevivalAltar at " + Utils.toString(LOCATION) + ": Head detected");
            Skull head = (Skull) headBlock.getState();
            if (!head.hasOwner()) {return;}
            selectPlayer(head.getOwningPlayer().getName());
        }
    }

    @JsonIgnore
    public boolean isActivated() {return cooldown != -1;}

    public void updateInfo(Inventory inventory) { // oppdaterer settings itemet og teller hoder og mynter i inventoriet
        ItemStack[] contents = inventory.getContents();
        int Δheads = 0;
        int Δkromer = 0;
        for (int i = 0; i < contents.length - 1; i++) { // slots 1-4
            ItemStack item = contents[i];
            if (item != null && item.getType() != Material.AIR) {
                if (item.getType() == Material.PLAYER_HEAD) {
                    Δheads += item.getAmount();
                } else if (item.getType() == Material.FIREWORK_STAR && item.getItemMeta().hasCustomModelData()) {
                    Δkromer += ItemManager.toKromer(item);
                }
            }
        }
        addedKromer = Δkromer;
        if (isActivated()) {
            inventory.setItem(4, getRespawnItem());
        } else {
            inventory.setItem(4, getActivationItem(Δheads + getStoredHeadCount(), Δkromer + storedKromer, Δheads > 0, Δkromer > 0));
        }
        //Manager.debugMessage(String.format("updateInfo(): heads: %dΔ%d, kromer_ %dΔ%d", getStoredHeadCount(), Δheads, storedKromer, Δkromer));
    }

    public void updateInfo() {
        Hopper altar = (Hopper) Main.WORLD.getBlockAt(LOCATION).getState();
        updateInfo(altar.getInventory());
    }

    public void selectPlayer(String playerName) {
        selectedPlayer = playerName;
        if (cooldown == 0 && !Objects.equals(playerName, "NONE")) {
            Manager.provideAltar(this);
        }
        updateInfo();
    }


    public void collectItemsForActivation(Inventory inventory){ // collecter heads og kromers, 2DO: Gjør at kolekta items blir registrert sånn at de dropper etterpå
        ItemStack[] contents = inventory.getContents();
        int totalStoredHeads = getStoredHeadCount();
        for (int i = 0; i < contents.length - 1; i++) { // slots 1-4
            ItemStack item = contents[i];
            if (item == null) continue;
            if (item.getType() == Material.PLAYER_HEAD && totalStoredHeads < ACTIVATION_COST_HEADS) { // skjekker om det er et player head og det trengs fler heads
                int itemHeadAmount = item.getAmount();
                SkullMeta head = (SkullMeta) item.getItemMeta();
                String playerName = head.getOwningPlayer().getName();
                int storedHeadCount = storedHeads.getOrDefault(playerName, 0);
                int Δheads = Math.min(ACTIVATION_COST_HEADS - totalStoredHeads, itemHeadAmount);
                // oppdaterer tall
                storedHeadCount += Δheads;
                storedHeads.put(playerName, storedHeadCount); // oppdaterer hashmapp med playerheads
                totalStoredHeads += Δheads;
                item.setAmount(itemHeadAmount - Δheads);
            } else {
                collectCoins(item);
            }
        }
        if (storedKromer >= ACTIVATION_COST_KROMER) {
            if (storedKromer > ACTIVATION_COST_KROMER) { // Hvis man la inn for me penger, sendes de tilbake
                int excess = storedKromer - ACTIVATION_COST_KROMER;
                //Manager.debugMessage("Excess coins will be given back: " + excess);
                returnExcessCoins(inventory, excess);
            }
            if (getStoredHeadCount() == ACTIVATION_COST_HEADS) {
                activate();
            }
        }
        inventory.setItem(4, getActivationItem(getStoredHeadCount(), storedKromer, false, false)); // oppdaterer info itemet
    }

    public void collectCoinsForCooldownSkipping(Inventory inventory) {
        ItemStack[] contents = inventory.getContents();
        for (int i = 0; i < contents.length - 1; i++) { // slots 1-4
            ItemStack item = contents[i];
            if (item == null) continue;
            collectCoins(item);
        }
        if (storedKromer == 0) return; // hvis noen trykka collect når cooldownen allerede var nede så er storedkromer 0 og ingenting skjer
        while (cooldown > 0 && storedKromer > 0) {
            cooldown -= Math.min(cooldown, 3);
            storedKromer -= 1;
        }
        if (cooldown == 0) {
            if (storedKromer > 0) {
                returnExcessCoins(inventory, storedKromer);
            }
            cooldownEnded();
            cooldownTask.cancel();
        }
        updateInfo();
    }

    private void collectCoins(ItemStack item) { // collecter coins fra en slot (man tar inn itemet i slotten)
        if (item == null || item.getType() != Material.FIREWORK_STAR || !item.getItemMeta().hasCustomModelData()) return;
        int itemKromerAmount = ItemManager.toKromer(item);
        if (itemKromerAmount == 0) return;
        item.setAmount(0); // alle coins blir plukka opp (de leveres tilbake igjen seinere om det er no til overs)
        List<Component> lore = item.lore();
        String coinOwner = PlainTextComponentSerializer.plainText().serialize(lore.get(lore.size() - 1));
        Manager.getMember(coinOwner).addKromers(-itemKromerAmount); // removes the coin from the user. the coins will be stored inside the altar, but wont go into the pool before the altar is fully activated
        this.coinOwner = coinOwner;
        storedKromer += itemKromerAmount;
    }

    private void returnExcessCoins(Inventory inventory, int amount) {
        for (ItemStack coin : ItemManager.toItems(amount, coinOwner)) {
            for (int i = 0; i < inventory.getSize(); i++) { // i er slotten i hopper inventoriet
                if (i == inventory.getSize() - 1) { // hvis det ikker er noe plass i heile inventoriet
                    Main.WORLD.dropItemNaturally(LOCATION.clone().add(0, 1, 0), coin); // spawner coinen oppå alteret istedenfor inni
                    break;
                } else if (inventory.getItem(i) == null) { // hvis det er plass i slotten
                    inventory.setItem(i, coin);
                    break;
                }
            }
        }
        storedKromer -= amount;
        Manager.getMember(coinOwner).addKromers(amount); // playeren får coins tilbake
    }

    private void activate() {
        cooldown = COOLDOWN_TIME; // alteret aktiveres
        //ta en method som starter cooldown
        storedHeads.clear();
        storedKromer = 0;
        for (Entity entity : Main.WORLD.getNearbyEntities(LOCATION.toCenterLocation(), 0.5, 0.5, 0.5)) {
            if (entity instanceof ItemDisplay) {
                ((ItemDisplay) entity).setItemStack(ItemManager.ACTIVATED_REVIVAL_ALTAR);
            }
        }
        Manager.addKromersToPool(ACTIVATION_COST_KROMER);
        Hopper altar = (Hopper) Main.WORLD.getBlockAt(LOCATION).getState();
        Inventory inventory = altar.getInventory();
        inventory.setItem(4, getRespawnItem());
        startNewCooldown();
    }

    public void respawn(Player p) { // respawner playeren (by force). Det må derfor testes før man caller denne funksjonen. Denne methoden er et alternativ til den innebygde respawn funksjonen i Member
        // fancy teleportering der man telporteres nøyaktig der hodet sto i riktig rotasjon
        Block headBlock = LOCATION.clone().add(0, 1, 0).getBlock();
        Skull skull = (Skull) headBlock.getState();
        BlockFace blockFace = skull.getRotation(); // idk åssen metode man skal bruke som alternativ til getRotation
        int modX = blockFace.getModX();
        int modZ = blockFace.getModZ();

        // Beregn yaw basert på modX og modZ som sammen er en vektor som forteller retning, der den ene er cos og den andre er sin
        double yaw = Math.toDegrees(Math.atan2(modX, -modZ));
        Location tpLoc = LOCATION.toCenterLocation();
        tpLoc.setYaw((float) yaw); // setter horisontal rotasjon (yaw) til samme retning som hodet, sånn at det ser ut som at hodet blei til playeren
        tpLoc.add(0, -0.35, 0); // sånn at hodet til playeren er på samme høyde som hode blokken
        tpLoc.setPitch(0); // hoder har alltid en pitch på 0
        p.teleport(tpLoc);
        //Manager.debugMessage(p.getName() + " was teleported to " + Utils.toString(tpLoc));
        p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 20, 1, false, false)); // man flyr opp fra alteret

        headBlock.setType(Material.AIR); // fjerner hodet på toppen

        // resetter alter variabler
        cooldown = 60;
        startNewCooldown();
        updateInfo();
        Manager.withdrawAltar(this);
        selectedPlayer = "NONE";
    }

    public void reduceCooldown() {
        cooldown -= 1;
        if (cooldown == 0) {
            cooldownEnded();
        }
        updateInfo();
    }

    private void cooldownEnded() {
        if (!Objects.equals(selectedPlayer, "NONE")) {
            Manager.provideAltar(this);
        }
    }

    public void remove() { // spawner itemsene som var inni og unregistrerer
        Manager.unRegisterAltar(LOCATION);
        TRIBE.unRegisterAltar(LOCATION);

        ItemStack altarItem = isActivated() ? ItemManager.ACTIVATED_REVIVAL_ALTAR : ItemManager.REVIVAL_ALTAR;
        Main.WORLD.dropItemNaturally(LOCATION, altarItem);
        if (!isActivated()) { // hvis alteret ikke er aktivert
            if (storedKromer > 0) {

                String playerName = TRIBE.getMemberIDs().toArray()[0].toString();
                for (ItemStack item : ItemManager.toItems(storedKromer, playerName)) {
                    Main.WORLD.dropItemNaturally(LOCATION, item);
                }
                TRIBE.getMember(playerName).addKromers(storedKromer); // coin ownered blir satt til en random player i triben
            }
            if (getStoredHeadCount() > 0) {
                for (Map.Entry<String, Integer> headEntry : storedHeads.entrySet()) {
                    OfflinePlayer p = Bukkit.getOfflinePlayer(headEntry.getKey());
                    ItemStack head = ItemManager.getHead(p, Component.text(headEntry.getKey() + " died to death"));
                    head.setAmount(headEntry.getValue());
                    Item headItem = Main.WORLD.dropItemNaturally(LOCATION, head);
                    headItem.setUnlimitedLifetime(true);
                }
            }
        } else if (cooldown > 0) {
            cooldownTask.cancel();
        }
    }

    private void startNewCooldown() {
        cooldownTask = new AltarCooldown(this, cooldown);
        cooldownTask.runTaskTimer(Main.getPlugin(), 0L, 1200L); // 1 gang i minuttet
    }

    public String getSelectedPlayer() {
        return selectedPlayer;
    }

    private ItemStack getActivationItem(int heads, int kromer, boolean addedHeads, boolean addedKromer) { // hoder og kromers som er putta inn. addedX gjør at x sin farge blir lysere for å vise at det er items i inventoriet som kan bli adda
        Component activateComp = Component.text("ACTIVATE");
        Component headsComp = Component.text(heads + "/" + ACTIVATION_COST_HEADS + " Heads");
        Component kromerComp = Component.text(kromer + "/" + ACTIVATION_COST_KROMER + " kr");

        headsComp = getComponent(heads, addedHeads, headsComp, ACTIVATION_COST_HEADS);

        kromerComp = getComponent(kromer, addedKromer, kromerComp, ACTIVATION_COST_KROMER);
        NamedTextColor activateColor;
        if (heads == ACTIVATION_COST_HEADS && kromer == ACTIVATION_COST_KROMER) {
            activateColor = NamedTextColor.GREEN;
        } else if(heads == 0 && kromer == 0) {
            activateColor = NamedTextColor.RED;
        } else {
            activateColor = addedHeads || addedKromer ? NamedTextColor.YELLOW : NamedTextColor.GOLD;
        }
        activateComp = activateComp.color(activateColor);

        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(activateComp);
        meta.lore(List.of(headsComp, kromerComp));
        meta.setCustomModelData(77012);
        item.setItemMeta(meta);
        return item;
    }

    @NotNull
    private Component getComponent(int amount, boolean added, Component comp, int activationCost) { // endrer på en components farge basert på hvor mye, om man adda noe, og hva aktiveringskostnaden er
        if (amount == 0) {
            comp = comp.color(NamedTextColor.RED);
        } else {
            NamedTextColor color = added ? NamedTextColor.YELLOW : NamedTextColor.GOLD;
            if (amount >= activationCost) {
                color = added ? NamedTextColor.GREEN : NamedTextColor.DARK_GREEN;
            }
            comp = comp.color(color);
        }
        return comp;
    }

    private ItemStack getRespawnItem() {
        // Player
        Component playerComp = Component.text("Player: " + selectedPlayer + ". ");
        Component playerInfo;

        if (!Objects.equals(selectedPlayer, "NONE")) {
            Member member = Manager.getMember(selectedPlayer);
            if (member != null) {
                if (!member.isAlive()) {
                    if (member.isOnline()) {
                        if (cooldown == 0) {
                            playerInfo = Component.text("Respawning in progress...", NamedTextColor.DARK_GREEN);
                        } else {
                            playerInfo = Component.text("This player will respawn once the cooldown goes down", NamedTextColor.YELLOW);
                        }
                    } else {
                        playerInfo = Component.text("This player will respawn once they join", NamedTextColor.YELLOW);
                    }
                } else {
                    playerInfo = Component.text("This player will respawn once they die", NamedTextColor.YELLOW);
                }
                playerComp = playerComp.color(NamedTextColor.GREEN);
            } else {
                playerInfo = Component.text("This player is not registered in a tribe", NamedTextColor.GOLD);
                playerComp = playerComp.color(NamedTextColor.RED);
            }
        } else {
            playerInfo = Component.text("Place a player head ontop", NamedTextColor.YELLOW);
            playerComp = playerComp.color(NamedTextColor.RED);
        }
        // Cooldown
        int reducedTime = addedKromer * REDUCED_MINUTES_PER_COIN;
        Component cooldownTitle = Component.text("Cooldown: ");
        Component cooldownBar = Component.text("|".repeat(COOLDOWN_TIME - cooldown), NamedTextColor.GREEN)
                .append(Component.text("|".repeat(Math.min(reducedTime, cooldown)), NamedTextColor.YELLOW))
                .append(Component.text("|".repeat(Math.max(cooldown - reducedTime, 0)), NamedTextColor.RED));
        Component cooldownTextAddedKromers = Component.text("");
        NamedTextColor cooldownTextColor = NamedTextColor.GOLD;
        NamedTextColor cooldownTitleColor = NamedTextColor.GOLD;
        if (addedKromer > 0) {
            cooldownTextAddedKromers = Component.text(" - " + Math.min(reducedTime, cooldown), addedKromer >= cooldown ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
            if (reducedTime >= cooldown) {
                cooldownTitleColor = NamedTextColor.GREEN;
                cooldownTextColor = NamedTextColor.YELLOW;
            } else {
                cooldownTitleColor = NamedTextColor.YELLOW;
            }
        }
        cooldownTitle = cooldownTitle.color(cooldownTitleColor);
        Component cooldownText;
        if (cooldown == 0) {
            cooldownText = Component.text(" READY", NamedTextColor.DARK_GREEN);
        } else {
            cooldownText = Component.text(" (" + cooldown, cooldownTextColor)
                    .append(cooldownTextAddedKromers)
                    .append(Component.text(" min left)", cooldownTextColor));
        }

        // Click event (Click to <do something>)
        Component clickText = Component.text("");
        Member member = Manager.getMember(selectedPlayer);
        if (member != null && !member.isAlive() && member.isOnline()) {
            if (reducedTime >= cooldown) {
                clickText = Component.text("CLICK TO RESPAWN NOW ", NamedTextColor.GREEN)
                        .append(Component.text("(" + cooldown + "kr)", NamedTextColor.YELLOW));
            } else {
                clickText = Component.text("Add kromers and click here to reduce the timer. (1kr > -" + REDUCED_MINUTES_PER_COIN + " min)");
            }
        }
        // item
        ItemStack item = new ItemStack(Material.FIREWORK_STAR);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Status", NamedTextColor.DARK_PURPLE));
        meta.lore(List.of(
                playerComp.append(playerInfo),
                cooldownTitle.append(cooldownBar).append(cooldownText),
                clickText
        )); // add mer i lista
        meta.setCustomModelData(77012);
        item.setItemMeta(meta);
        return item;
    }

    @SuppressWarnings("unused")
    @JsonProperty("x")
    private int getX() {return LOCATION.getBlockX();}

    @SuppressWarnings("unused")
    @JsonProperty("y")
    private int getY() {return LOCATION.getBlockY();}

    @SuppressWarnings("unused")
    @JsonProperty("z")
    private int getZ() {return LOCATION.getBlockZ();}

    @SuppressWarnings("unused")
    @JsonProperty("cooldown")
    private int getCooldown() {return cooldown;}

    @SuppressWarnings("unused")
    @JsonProperty("kromer") @JsonInclude(JsonInclude.Include.NON_DEFAULT) // ikke ta med hvis kromer = 0
    private int getStoredKromer() {
        return storedKromer;
    }

    @SuppressWarnings("unused")
    @JsonProperty("heads") @JsonInclude(JsonInclude.Include.NON_NULL)
    private HashMap<String, Integer> getHeads() {
        if (storedHeads.isEmpty()) {return null;}
        return storedHeads;
    }

    private int getStoredHeadCount() {
        return storedHeads.values().stream().mapToInt(Integer::intValue).sum();
    }
}
