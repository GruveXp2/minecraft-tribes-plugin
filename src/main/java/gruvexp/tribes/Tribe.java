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
    private Map<UUID, Member> members = new HashMap<>();
    private final Map<Location, RevivalAltar> revivalAltars = new HashMap<>(); // <location, revivalAltar>
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

    public void addMember(Player p) {
        UUID playerID = p.getUniqueId();
        String playerName = p.getName();
        if (members.containsKey(playerID)) {
            throw new IllegalArgumentException(ChatColor.RED + "Player " + playerName + " is already in this tribe!");
        } else if (Manager.getMember(playerID) != null && Manager.getMember(playerID).tribe() != this) {
            throw new IllegalArgumentException(ChatColor.RED + "Player " + playerName + " is already in another tribe!");
        }
        members.put(playerID, new Member(playerName, this));
        Bukkit.broadcastMessage(String.format("%s%s %sjoined tribe %s%s", ChatColor.YELLOW, playerName, ChatColor.WHITE, COLOR, displayName));
        Manager.considerPauseToggle();
        Manager.handlePlayerJoin(p);
    }

    public void migrateMemberToThisTribe(Member member) {
        members.put(member.ID, member);
        Bukkit.broadcastMessage(String.format("%s%s %sswitched from tribe %s%s %sto %s%s", ChatColor.YELLOW, member.NAME, ChatColor.WHITE, member.tribe().COLOR, member.tribe().displayName, ChatColor.WHITE, COLOR, displayName));
        member.tribe().unregisterMember(member.ID);
        member.switchToTribe(this);
    }

    @JsonIgnore
    public Set<UUID> getMemberIDs() {
        return members.keySet();
    }

    @JsonIgnore
    public Collection<Member> getMembers() {return members.values();}

    public Member getMember(UUID playerID) {
        return members.get(playerID);
    }

    @SuppressWarnings("unused")
    @JsonProperty("members") @JsonInclude(JsonInclude.Include.NON_NULL)
    private Map<UUID, Member> getMembersJSON() {
        if (members.isEmpty()) {
            return null;
        }
        return members;
    }

    @SuppressWarnings("unused")
    @JsonProperty("members")
    private void setMembersJSON(HashSet<Member> members) {
        this.members = new HashMap<>(members.size());
        members.forEach(member -> {
            this.members.put(member.ID, member);
            member.registerTribe(this);
        });
        activeMembers = members.stream()
                .filter(member -> !member.isAlive())
                .collect(Collectors.toSet());
        //Manager.debugMessage("JSON setter: " + ID + " has " + activeMembers.size() + " active members");
    }

    public void removeMember(UUID playerID) {
        if (!members.containsKey(playerID)) {
            throw new IllegalArgumentException(ChatColor.YELLOW + "Nothing happened, that player wasnt in a tribe in the first place");
        }
        Manager.unRegisterMember(playerID); // fjerner at member er registrert hos manageren
        members.get(playerID).remove();
        members.remove(playerID);
        Bukkit.broadcastMessage(String.format("%s%s %sleft tribe %s%s", ChatColor.YELLOW, Bukkit.getOfflinePlayer(playerID).getName(), ChatColor.WHITE, COLOR, ID));
        Manager.handleMemberLeave(playerID);
    }

    public void unregisterMember(UUID playerID) { // used when u switch tribe
        if (!members.containsKey(playerID)) {
            throw new IllegalArgumentException(ChatColor.YELLOW + "Nothing happened, that player wasnt in a tribe in the first place");
        }
        members.remove(playerID);
    }

    public int getDeaths(UUID playerID) {
        if (!members.containsKey(playerID)){
            throw new IllegalArgumentException(ChatColor.RED + Bukkit.getOfflinePlayer(playerID).getName() + " is not a member of this tribe (" + ID + ")!");
        }
        return members.get(playerID).getDeaths();
    }

    public boolean isAlive(UUID playerID) {
        if (!members.containsKey(playerID)){
            throw new IllegalArgumentException(ChatColor.RED + Bukkit.getOfflinePlayer(playerID).getName() + " is not a member of this tribe (" + ID + ")!");
        }
        return members.get(playerID).isAlive();
    }

    public void death(UUID playerID) {
        for (Map.Entry<UUID, Member> memberEntry : members.entrySet()) {
            UUID playerID2 = memberEntry.getKey();
            Player p2 = Bukkit.getPlayer(playerID2);
            if (p2 == null) {continue;}
            if (p2.getGameMode() == GameMode.SPECTATOR) {continue;}
            Player p = Bukkit.getPlayer(playerID);
            assert p != null;
            p.teleport(p2);
            break;
        }
        members.get(playerID).die();
        Manager.considerCooldownReduction();
        Manager.handleDeath(playerID); // setter isSpectating til false
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
        if (revivalAltars.isEmpty()) {
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

    public void handleJoin(Player p) { // when someone comes online
        UUID playerID = p.getUniqueId();
        Member member = getMember(playerID);
        member.playerJoined();
        activeMembers.add(member);
        Manager.handlePlayerJoin(p);
        ItemManager.registerCoinRecipes(playerID);
    }

    public void handleLeaveActive(UUID playerID) { // when someone active goes offline or their respawn timer runs out
        Member member = getMember(playerID);
        if (member.isAlive()) {
            activeMembers.remove(member);
            Manager.considerPauseToggle();
            Manager.considerCooldownReduction();
        }
    }

    @JsonIgnore
    public boolean isActive() {
        return !activeMembers.isEmpty();
    }

    @JsonIgnore
    public boolean isAlive() { // hvis alle spillerene som er online eller har cooldown er daue,
        if (activeMembers.isEmpty()) {
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
