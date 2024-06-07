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

public class Member implements PostInit{

    @JsonProperty("name")
    public final String NAME;
    private Tribe TRIBE;
    private int deaths;
    private int respawnCooldown;
    private int kromers;
    private RespawnCooldown respawnCooldownTask;

    public Member(String playerName, Tribe tribe) {
        NAME = playerName;
        TRIBE = tribe;
        deaths = 0;
        respawnCooldown = 0;
        kromers = 0;
        Manager.registerMember(this);
        ItemManager.registerCoinItems(playerName);
        ItemManager.registerCoinRecipes(playerName);
    }

    @SuppressWarnings("unused")
    public Member(@JsonProperty("name") String playerName, @JsonProperty("deaths") int deaths, @JsonProperty("respawnCooldown") int respawnCooldown, @JsonProperty("kromers") int kromers) {
        NAME = playerName;
        this.deaths = deaths;
        this.respawnCooldown = respawnCooldown;
        this.kromers = kromers;
        Manager.registerMember(this);
        if (respawnCooldown > 0) {
            Manager.schedulePostInit(this);
        }
    }

    public void postInit() {
        ItemManager.registerCoinItems(NAME);
        respawnCooldownTask = new RespawnCooldown(NAME, respawnCooldown);
        respawnCooldownTask.runTaskTimer(Main.getPlugin(), 0L, 20L);
        Player p = Bukkit.getPlayerExact(NAME);
        if (p != null) {
            Manager.setDeathLocation(NAME, p.getLocation());
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

        boolean respawned = respawnAtAltarIfAvailable();
        if (respawned) {return;}

        Location deathLocation = Bukkit.getPlayerExact(NAME).getLocation();
        if (deathLocation.getWorld() == Bukkit.getWorld("Tribes_the_end") && deathLocation.getY() < 0) {
            deathLocation = Bukkit.getPlayerExact(NAME).getBedSpawnLocation();
            if (deathLocation == null) {
                deathLocation = Main.WORLD.getSpawnLocation();
            }
        }
        Manager.setDeathLocation(NAME, deathLocation);
        respawnCooldown = switch (deaths) {
            case 1 -> 2;
            case 2 -> 5;
            case 3 -> 12;
            case 4 -> 20;
            case 5 -> 30;
            default -> 300; // 5 timer
        };
        respawnCooldownTask = new RespawnCooldown(NAME, respawnCooldown);
        respawnCooldownTask.runTaskTimer(Main.getPlugin(), 0L, 20L);
        Manager.messagePlayers(String.format("Total deaths: %s%s%s, respawn time: %s%s",
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
        return Bukkit.getPlayerExact(NAME) != null;
    }

    public void playerJoined() {
        if (respawnCooldownTask == null) { // playeren lever og levde før de leava
            Bukkit.getPlayerExact(NAME).setGameMode(GameMode.SURVIVAL);
            return;
        }
        if (respawnCooldownTask.isCancelled()) { // playeren daua før de leava, men respawna mens de var borte
            respawnNaturally(Objects.requireNonNull(Bukkit.getPlayerExact(NAME)));
        } else { // playeren daua før de leava og er fortsatt dau når de jorner igjen
            boolean respawned = respawnAtAltarIfAvailable(); // respawner ved et alter hvis det er et ledig et. hvis ikke så kommer timer opp og man venter på at timeren går ned eller et alter blir ledig
            if (respawned) {return;}
            Bukkit.getPlayerExact(NAME).setGameMode(GameMode.SPECTATOR);
            respawnCooldownTask.playerJoined();
            if (Manager.getDeathLocation(NAME) == null) {
                Manager.setDeathLocation(NAME, Objects.requireNonNull(Bukkit.getPlayerExact(NAME)).getLocation());
            }
        }
    }

    public boolean respawnAtAltarIfAvailable() { // kalles fra andre steder. Er ikke sikkert at playeren respawner, kommer an på om det er alter tilgjengelige
        // forventes at playeren både er online og dau
        RevivalAltar altar = Manager.getAvailableAltar(NAME);
        if (altar != null) {
            Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
                Player p = Bukkit.getPlayerExact(NAME);
                respawnAtAltar(p, altar);
            }, 100L); // 100L = 5 seconds (20 ticks per second)
            return true;
        }
        return false;
    }

    public void respawnAtAltar(Player p ,RevivalAltar altar) { // calles direkte når et alter blir available og playeren kan respawnes
        if (respawnCooldownTask != null) {
            respawnCooldownTask.remove();
            respawnCooldownTask = null;
        }
        respawnCooldown = 0;
        Bukkit.broadcast(Component.text(NAME + " was revived", NamedTextColor.YELLOW)); // finn ut åssen man gjør at det blir gul tekst i cmden
        p.setGameMode(GameMode.SURVIVAL);
        altar.respawn(p);
    }

    public void respawnNaturally(Player p) {
        respawnCooldownTask = null;
        Bukkit.broadcast(Component.text(NAME + " respawned", NamedTextColor.YELLOW)); // finn ut åssen man gjør at det blir gul tekst i cmden
        p.setGameMode(GameMode.SURVIVAL);
        Location spawnPos = p.getBedSpawnLocation();
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
