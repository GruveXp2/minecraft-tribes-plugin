package gruvexp.tribes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

public class Member implements PostInit{

    @JsonProperty("name")
    public final String NAME;
    @JsonProperty("id")
    public final UUID ID;
    private Tribe TRIBE;
    private int deaths;
    private int kromers;

    public Member(String playerName, Tribe tribe) {
        NAME = playerName;
        ID = Bukkit.getOfflinePlayer(playerName).getUniqueId();
        TRIBE = tribe;
        deaths = 0;
        kromers = 0;
        Tribes.registerMember(this);
        ItemManager.registerCoinItems(ID);
        ItemManager.registerCoinRecipes(ID);
    }

    @SuppressWarnings("unused")
    public Member(@JsonProperty("id") String playerID, @JsonProperty("name") String playerName, @JsonProperty("deaths") int deaths, @JsonProperty("kromers") int kromers) {
        NAME = playerName;
        ID = UUID.fromString(playerID);
        this.deaths = deaths;
        this.kromers = kromers;
        Tribes.registerMember(this);
    }

    public void postInit() {
        ItemManager.registerCoinItems(ID);
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
    }

    public int getKromers() {
        return kromers;
    }

    public void addKromers(int kromers) { // Adding kromers (can be negative)
        this.kromers += kromers;
    }

    @JsonIgnore
    public boolean isOnline() {
        return Bukkit.getPlayer(ID) != null;
    }
}
