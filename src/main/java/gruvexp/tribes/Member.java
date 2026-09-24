package gruvexp.tribes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import gruvexp.tribes.tasks.RespawnCooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

public class Member implements PostInit{

    @JsonProperty("name")
    public final String NAME;
    @JsonProperty("id")
    public final UUID ID;
    private Tribe TRIBE;
    private int deaths;
    private int respawnCooldown;
    private int kromers;
    private RespawnCooldown respawnCooldownTask;

    public Member(String playerName, Tribe tribe) {
        NAME = playerName;
        ID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        TRIBE = tribe;
        deaths = 0;
        respawnCooldown = 0;
        kromers = 0;
        Tribes.registerMember(this);
        ItemManager.registerCoinItems(ID);
        ItemManager.registerCoinRecipes(ID);
    }

    @SuppressWarnings("unused")
    public Member(@JsonProperty("id") String playerID, @JsonProperty("name") String playerName, @JsonProperty("deaths") int deaths, @JsonProperty("respawnCooldown") int respawnCooldown, @JsonProperty("kromers") int kromers) {
        NAME = playerName;
        ID = UUID.fromString(playerID);
        this.deaths = deaths;
        this.respawnCooldown = respawnCooldown;
        this.kromers = kromers;
        Tribes.registerMember(this);
        if (respawnCooldown > 0) {
            Tribes.schedulePostInit(this);
        }
    }

    public void postInit() {
        ItemManager.registerCoinItems(ID);
        respawnCooldownTask = new RespawnCooldown(ID, respawnCooldown);
        respawnCooldownTask.runTaskTimer(Main.getPlugin(), 0L, 20L);
        Player p = Bukkit.getPlayer(ID);
        if (p != null) {
            Tribes.setDeathLocation(ID, p.getLocation());
        }
    }

    public void registerTribe(Tribe tribe) {
        if (TRIBE == null) {
            TRIBE = tribe; // hindrer at man setter triben 2 ganger
        }
    }

    public void switchToTribe(Tribe tribe) { // /tribe switch_tribe LinusStorm netherlands
        TRIBE = tribe;
    }

    public Tribe tribe() {
        return TRIBE;
    }

    public int getDeaths() {
        return deaths;
    }

    public void setDeaths(int deaths) { // ONLY FOR HACKING
        this.deaths = deaths;
    }

    public void die() {
        deaths++;

        Player p = Bukkit.getPlayer(ID);
        Location deathLocation = p.getLocation();
        if (deathLocation.getWorld() == Bukkit.getWorld("Tribes_the_end") && deathLocation.getY() < 0) {
            deathLocation = p.getRespawnLocation();
            if (deathLocation == null) {
                deathLocation = Main.WORLD.getSpawnLocation();
            }
        }
        Tribes.setDeathLocation(ID, deathLocation);
        respawnCooldown = switch (deaths) {
            case 1 -> 2;
            case 2 -> 5;
            case 3 -> 12;
            case 4 -> 20;
            case 5 -> 30;
            default -> 300; // 5 timer
        };
        respawnCooldownTask = new RespawnCooldown(ID, respawnCooldown);
        respawnCooldownTask.runTaskTimer(Main.getPlugin(), 0L, 20L);
        Tribes.messagePlayers(String.format("Total deaths: %s%s%s, respawn time: %s%s",
                ChatColor.RED, deaths, ChatColor.WHITE, ChatColor.GOLD, respawnCooldown));
    }

    public void setRespawnCooldown(int minutes) {
        respawnCooldown = minutes;
    }

    public int getRespawnCooldown() {
        return respawnCooldown;
    }

    @JsonIgnore
    public RespawnCooldown getRespawnCooldownTask() {
        return respawnCooldownTask;
    }

    public void haccRespawnCooldown(int minutes) {
        respawnCooldown = minutes;
        respawnCooldownTask.haccMinutes(minutes);
    }

    public int getKromers() {
        return kromers;
    }

    public void addKromers(int kromers) { // Adding kromers (can be negative)
        this.kromers += kromers;
    }

    @JsonIgnore
    public boolean isAlive() {
        return respawnCooldown == 0;
    }

    @JsonIgnore
    public boolean isOnline() {
        return Bukkit.getPlayer(ID) != null;
    }

    public void respawnNaturally(Player p) {
        respawnCooldownTask = null;
        Bukkit.broadcast(Component.text(NAME + " respawned", NamedTextColor.YELLOW)); // finn ut åssen man gjør at det blir gul tekst i cmden
        p.setGameMode(GameMode.SURVIVAL);
        Location spawnPos = p.getRespawnLocation();
        if (spawnPos == null) {
            spawnPos = p.getWorld().getSpawnLocation();
        }
        p.teleport(spawnPos);
    }

    public void remove() { // når en player forlater triben
        if (respawnCooldownTask != null) {
            respawnCooldownTask.remove();
        }
    }
}
