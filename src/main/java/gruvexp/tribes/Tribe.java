package gruvexp.tribes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class Tribe {

    @JsonProperty("id")
    public final String ID;
    @JsonProperty("color")
    public final ChatColor COLOR;
    @JsonProperty("displayName")
    private String displayName;
    private HashMap<String, Member> members = new HashMap<>();
    private final HashMap<Location, RevivalAltar> revivalAltars = new HashMap<>(); // <location, revivalAltar>
    private Set<Member> activeMembers = new HashSet<>(); // folk som er online eller har cooldown, som tels som at de er "online" og gjør at motstanderteamet kan spille

    public Tribe(@JsonProperty("id") String id, @JsonProperty("color") ChatColor color, @JsonProperty("displayName") String displayName) {
        ID = id;
        COLOR = color;
        if (displayName == null) {
            this.displayName = id;
        } else {
            this.displayName = displayName;
        }
    }

    public String displayName() {
        return displayName;
    }

    @JsonIgnore
    public int getCoinBalance() {
        int balance = 0;
        for (Member member : members.values()) {
            balance += member.getKromers();
        }
        return balance;
    }

    public void addMember(String playerName) {
        if (members.containsKey(playerName)) {
            throw new IllegalArgumentException(ChatColor.RED + "Player " + playerName + " is already in this tribe!");
        } else if (Manager.getMember(playerName) != null && Manager.getMember(playerName).tribe() != this) {
            throw new IllegalArgumentException(ChatColor.RED + "Player " + playerName + " is already in another tribe!");
        }
        members.put(playerName, new Member(playerName, this));
        Bukkit.broadcastMessage(String.format("%s%s %sjoined tribe %s%s", ChatColor.YELLOW, playerName, ChatColor.WHITE, COLOR, displayName));
        Manager.considerPauseToggle();
        Manager.handlePlayerJoin(playerName);
    }

    public void migrateMemberToThisTribe(Member member) {
        members.put(member.NAME, member);
        Bukkit.broadcastMessage(String.format("%s%s %sswitched from tribe %s%s %sto %s%s", ChatColor.YELLOW, member.NAME, ChatColor.WHITE, member.tribe().COLOR, member.tribe().displayName, ChatColor.WHITE, COLOR, displayName));
        member.tribe().unregisterMember(member.NAME);
        member.switchToTribe(this);
    }

    @JsonIgnore
    public Set<String> getMemberIDs() {
        return members.keySet();
    }

    public Member getMember(String playerName) {
        return members.get(playerName);
    }

    @SuppressWarnings("unused")
    @JsonProperty("members") @JsonInclude(JsonInclude.Include.NON_NULL)
    private HashMap<String, Member> getMembersJSON() {
        if (members.isEmpty()) {
            return null;
        }
        return members;
    }

    @SuppressWarnings("unused")
    @JsonProperty("members")
    private void setMembersJSON(HashMap<String, Member> members) {
        this.members = members;
        this.members.values().forEach(p -> p.registerTribe(this));
        activeMembers = members.values().stream()
                .filter(member -> !member.isAlive())
                .collect(Collectors.toSet());
        //Manager.debugMessage("JSON setter: " + ID + " has " + activeMembers.size() + " active members");
    }

    public void removeMember(String playerName) {
        if (!members.containsKey(playerName)) {
            throw new IllegalArgumentException(ChatColor.YELLOW + "Nothing happened, that player wasnt in a tribe in the first place");
        }
        Manager.unRegisterMember(playerName); // fjerner at member er registrert hos manageren
        members.get(playerName).remove();
        members.remove(playerName);
        Bukkit.broadcastMessage(String.format("%s%s %sleft tribe %s%s", ChatColor.YELLOW, playerName, ChatColor.WHITE, COLOR, ID));
        Manager.handleMemberLeave(playerName);
    }

    public void unregisterMember(String playerName) { // used when u switch tribe
        if (!members.containsKey(playerName)) {
            throw new IllegalArgumentException(ChatColor.YELLOW + "Nothing happened, that player wasnt in a tribe in the first place");
        }
        members.remove(playerName);
    }

    public int getDeaths(String playerName) {
        if (!members.containsKey(playerName)){
            throw new IllegalArgumentException(ChatColor.RED + playerName + " is not a member of this tribe!");
        }
        return members.get(playerName).getDeaths();
    }

    public boolean isAlive(String playerName) {
        if (!members.containsKey(playerName)){
            throw new IllegalArgumentException(ChatColor.RED + playerName + " is not a member of this tribe!");
        }
        return members.get(playerName).isAlive();
    }

    public void death(String playerName) {
        for (Map.Entry<String, Member> memberEntry : members.entrySet()) {
            String playerName2 = memberEntry.getKey();
            Player p2 = Bukkit.getPlayerExact(playerName2);
            if (p2 == null) {continue;}
            if (p2.getGameMode() == GameMode.SPECTATOR) {continue;}
            Player p = Bukkit.getPlayerExact(playerName);
            assert p != null;
            p.teleport(p2);
            break;
        }
        members.get(playerName).die();
        Manager.considerCooldownReduction();
        Manager.handleDeath(playerName); // setter isSpectating til false
    }

    public void registerAltar(Location loc, RevivalAltar altar) {
        revivalAltars.put(loc, altar);
    }

    public void unRegisterAltar(Location loc) {
        revivalAltars.remove(loc);
    }

    @SuppressWarnings("unused")
    @JsonProperty("revivalAltars") @JsonInclude(JsonInclude.Include.NON_NULL)
    private HashSet<RevivalAltar> getAltarsJSON() {
        if (revivalAltars.size() == 0) {
            return null;
        }
        return new HashSet<>(revivalAltars.values());
    }

    @SuppressWarnings("unused")
    @JsonProperty("revivalAltars")
    private void setAltarsJSON(ArrayList<RevivalAltar> altars) {
        for (RevivalAltar altar : altars) {
            revivalAltars.put(altar.LOCATION, altar);
            altar.registerTribe(this);
        }
    }

    @JsonIgnore
    public String getAltarInfo() { // DEBUG
        StringBuilder out = new StringBuilder("[");
        for (Map.Entry<Location, RevivalAltar> altarEntry : revivalAltars.entrySet()) {
            Location loc = altarEntry.getKey();
            out.append("\n{").append(loc.getX()).append(", ").append(loc.getY()).append(", ").append(loc.getZ()).append(", activated=").append(altarEntry.getValue().isActivated()).append("}, ");
        }
        out.append("]");
        return out.toString();
    }

    public void handleJoin(String playerName) { // when someone comes online
        Member member = getMember(playerName);
        member.playerJoined();
        activeMembers.add(member);
        Manager.handlePlayerJoin(playerName);
        ItemManager.registerCoinRecipes(playerName);
    }

    public void handleLeaveActive(String playerName) { // when someone active goes offline or their respawn timer runs out
        Member member = getMember(playerName);
        if (member.isAlive()) {
            activeMembers.remove(member);
            Manager.considerPauseToggle();
            Manager.considerCooldownReduction();
        }
    }

    @JsonIgnore
    public boolean isActive() {
        return activeMembers.size() != 0;
    }

    @JsonIgnore
    public boolean isAlive() { // hvis alle spillerene som er online eller har cooldown er daue,
        if (activeMembers.size() == 0) {
            return true;
        }
        for (Member member : activeMembers) {
            if (member.isAlive()) {
                return true;
            }
        }
        return false;
    }
}
