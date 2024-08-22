package gruvexp.tribes;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gruvexp.tribes.tasks.CooldownReduction;
import gruvexp.tribes.tasks.PauseIn1Min;
import gruvexp.tribes.tasks.RespawnCooldown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public final class Manager {

    public static boolean friendlyFire = false;
    private static final HashMap<UUID, Member> members = new HashMap<>(); // liste over alle members uavhengig av tribe
    private static final HashMap<UUID, Location> playerPauseCoords = new HashMap<>();
    private static final HashMap<UUID, Location> playerDeathCoords = new HashMap<>();
    private static final HashMap<UUID, Boolean> playerSpectatingStatus = new HashMap<>();
    private static final HashMap<Location, RevivalAltar> revivalAltars = new HashMap<>(); // har refrences til revivalalterene. Alt gjøres fra alter objektet
    private static final HashMap<UUID, HashSet<RevivalAltar>> availableAltars = new HashMap<>(); // hver player har et sett med altere som er klare til å respawne playeren om den dauer
    private static final HashSet<PostInit> postInitObjects= new HashSet<>();
    private static final BossBar pauseBar = Bukkit.createBossBar("Game Paused", BarColor.YELLOW, BarStyle.SOLID);
    private static HashMap<String, Tribe> tribes = new HashMap<>();
    private static boolean paused = false; // alle står stille osv. begynner som false og settes til true rett etter serveren har starta
    private static boolean reducingCooldowns = false;
    private static PauseIn1Min pauseCooldown;
    private static int kromerPool;

    public static boolean isPaused() {
        return paused;
    }

    public static void pause() {
        if (paused) {return;}
        paused = true;
        pauseCooldown = null;
        Bukkit.getServerTickManager().setFrozen(true);
        //Main.WORLD.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        //Main.WORLD.setGameRule(GameRule.DO_FIRE_TICK, false);
        //Main.WORLD.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        Bukkit.broadcast(Component.text("The game is now paused. Wait for someone from another tribe to join", NamedTextColor.GOLD));
        pauseBar.setVisible(true);
        for (UUID playerID : members.keySet()) {
            Player p = Bukkit.getPlayer(playerID);
            if (p == null) {
                continue;
            }
            playerPauseCoords.put(playerID, p.getLocation());
        }
    }

    public static void unPause() {
        if (!paused) {return;}
        paused = false;
        Bukkit.getServerTickManager().setFrozen(false);
        /*Main.WORLD.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        Main.WORLD.setGameRule(GameRule.DO_FIRE_TICK, true);
        Main.WORLD.setGameRule(GameRule.RANDOM_TICK_SPEED, 3);*/
        messagePlayers(Component.text("The game is now unpaused. Have fun!").color(NamedTextColor.GREEN));
        Bukkit.getLogger().info("\u001B[32m" + "Game unpaused" + "\u001B[0m");
        pauseBar.setVisible(false);
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        for (Player p : onlinePlayers) {
            p.setInvulnerable(true);
        }
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> {
            for (Player p : onlinePlayers) { // hvis noen leaver med en gang så mister de fortsatt invulnerability
                p.setInvulnerable(false);
            }
        }, 100L);
    }

    public static void setPauseLocation(UUID playerID, Location loc) {
        playerPauseCoords.put(playerID, loc);
    }

    public static Location getPauseLocation(UUID playerID) {
        return playerPauseCoords.get(playerID);
    }

    public static void setDeathLocation(UUID playerID, Location loc) {
        playerDeathCoords.put(playerID, loc);
    }

    public static Location getDeathLocation(UUID playerID) {
        return playerDeathCoords.get(playerID);
    }

    public static void schedulePostInit(PostInit object) {
        postInitObjects.add(object);
    }

    public static void postInit() {
        for (PostInit object : postInitObjects) {
            object.postInit();
        }
        pause();
        considerCooldownReduction();
        for (Tribe tribe : tribes.values()) {
            for (UUID playerID : tribe.getMemberIDs()) {
                if (!tribe.getMember(playerID).isAlive()) {
                    playerSpectatingStatus.put(playerID, false); // alle som er daue og joiner serveren spawner på dødsstedet
                }
            }
        }
    }

    public static void addTribe(Tribe tribe) {
        tribes.put(tribe.ID, tribe);
    }

    public static boolean tribeExists(String tribeID) {
        return tribes.containsKey(tribeID);
    }

    public static Tribe getTribe(String tribeID) {
        Tribe tribe = tribes.get(tribeID);
        if (tribe == null) {
            throw new IllegalArgumentException("The tribe \"" + tribeID + "\" doesnt exist!");
        }
        return tribe;
    }

    public static void registerAltar(Location loc, RevivalAltar altar) {
        revivalAltars.put(loc, altar);
    }

    public static RevivalAltar getAltar(Location loc) {
        return revivalAltars.get(loc);
    }

    public static void unRegisterAltar(Location loc) {
        revivalAltars.remove(loc);
    }

    public static void provideAltar(RevivalAltar altar) { // gjør at det er available for spawning
        UUID selectedPlayerID = altar.getSelectedPlayerID();
        availableAltars.computeIfAbsent(selectedPlayerID, k -> new HashSet<>());
        availableAltars.get(selectedPlayerID).add(altar);
        //debugMessage("RevivalAltar at " + Utils.toString(altar.LOCATION) + " now available to spawn " + selectedPlayer);

        if (availableAltars.get(selectedPlayerID).size() == 1) { // hvis ingen altere var ledige før og dette er det første som ble ledig, skjekk om selectedPlayer er dau og venter på et tilgjengelig alter, hvis det så er jo dette alteret ledig og da spawner vi playeren
            Member member = getMember(selectedPlayerID);
            if (member != null && !member.isAlive() && member.isOnline()) {
                Player p = Bukkit.getPlayer(selectedPlayerID);
                member.respawnAtAltar(p, altar);
            }
        }
    }

    public static void withdrawAltar(RevivalAltar altar) {
        UUID selectedPlayerID = altar.getSelectedPlayerID();
        availableAltars.get(selectedPlayerID).remove(altar);
        //debugMessage("RevivalAltar at " + Utils.toString(altar.LOCATION) + " no longer available");
    }

    public static RevivalAltar getAvailableAltar(UUID playerID) { // returnerer et random alter som er klar til å spawne inn playeren
        HashSet<RevivalAltar> altarSet = availableAltars.get(playerID);
        if (altarSet == null) {return null;}
        ArrayList<RevivalAltar> altars = new ArrayList<>(altarSet);
        if (altars.isEmpty()) return null;
        return altars.getFirst();
    }

    public static Set<String> getTribeIDs() {
        return tribes.keySet();
    }

    public static Collection<Tribe> getTribes() {
        return tribes.values();
    }

    public static Set<UUID> getMemberIDs() {
        return members.keySet();
    }

    public static void registerMember(Member member) {
        members.put(member.ID, member);
    }

    public static Member getMember(UUID playerID) {
        return members.get(playerID);
    }

    public static void unRegisterMember(String playerName) {
        members.remove(playerName);
    }

    public static void handleDeath(UUID playerName) {
        playerSpectatingStatus.put(playerName, false);
    }

    public static void messagePlayers(String message) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message));
    }

    public static void messagePlayers(Component message) {
        Bukkit.getOnlinePlayers().forEach(p -> p.sendMessage(message));
    }

    public static void debugMessage(String message) { // used 4 debugging stuff by printing it in the chat
        messagePlayers(ChatColor.GRAY + "[DEBUG]: " + message);
        Bukkit.getLogger().info("[DEBUG]: " + message);
    }

    public static void considerPauseToggle() {
        if (Main.WORLD.getName().equals(Main.testWorldName)) {// TEST DEBUG SKAL FJERNES ETTERPÅ!!! <=============
            unPause();
            debugMessage("Server unpaused bc its a testing server");
            return;
        }
        //debugMessage("considering to toggle pause");
        int activeTribes = 0;
        for (Tribe tribe : tribes.values()) {
            //debugMessage(tribe.ID + " status: " + (tribe.isActive() ? "active" : "inactive"));
            if (tribe.isActive()) {
                activeTribes++;
            }
        }
        boolean active = activeTribes >= 2;
        // etter aktivheten er regna ut, så utføres
        if (active) {
            if (pauseCooldown != null) {
                pauseCooldown.cancelPause();
                Bukkit.broadcast(Component.text("Someone joined, pausing cancelled", NamedTextColor.GREEN));
                pauseCooldown = null;
            } else {
                unPause();
            }
        } else {
            if (activeTribes == 0) {
                // close server in 5 seconds
                Bukkit.broadcast(Component.text("No more players online, server will shutdown in 5 seconds", NamedTextColor.RED));
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(), Bukkit::shutdown, 100L);
            }
            if (paused) {return;}
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                pause(); // hvis den siste playeren leava så er dekke no vits å starte en pause timer
                return;
            }
            if (pauseCooldown != null) {return;} // hvis det allerede er en cooldown, ikke start en ny en
            pauseCooldown = new PauseIn1Min();
            pauseCooldown.runTaskTimer(Main.getPlugin(), 0, 20);
        }
    }

    public static void considerCooldownReduction() { // hvis alle er daue så reduserers cooldownen til den første spawner
        //debugMessage("considering cooldown reduction");
        for (Tribe tribe : tribes.values()) {
            if (tribe.isAlive()) {
                return; // skal kun redusere cooldown hvis absolutt alle aktive spillere er daue
            }
        }
        //start cooldownreduksjon
        //debugMessage("starting cooldown reduction");
        reducingCooldowns = true;
        HashSet<RespawnCooldown> respawnCooldowns = new HashSet<>();
        for (Tribe tribe : tribes.values()) {
            for (UUID memberID : tribe.getMemberIDs()) {
                RespawnCooldown respawnCooldown = tribe.getMember(memberID).getRespawnCooldownTask();
                if (respawnCooldown != null) {
                    respawnCooldowns.add(respawnCooldown); // adder respawnCooldowns til en liste, og alle i listen vil få cooldownen redusert
                }
            }
        }
        Bukkit.broadcast(Component.text("No players alive, cooldown timers will be reduced", NamedTextColor.GREEN));
        new CooldownReduction(respawnCooldowns).runTaskTimer(Main.getPlugin(), 0, 1);
    }

    public static void stopCooldownReduction() {
        reducingCooldowns = false;
    }

    public static boolean isReducingCooldowns() {
        return reducingCooldowns;
    }

    public static void handlePlayerJoin(Player p) { // når en spiller leaver serveren
        pauseBar.addPlayer(p);
        considerPauseToggle();
        if (paused) {
            setPauseLocation(p.getUniqueId(), p.getLocation());
        }
    }

    public static void handleMemberLeave(String playerName) { // når en player leaver triben
        Player p = Bukkit.getPlayerExact(playerName);
        if (p == null) {return;}
        pauseBar.removePlayer(p);
    }

    public static void addKromersToPool(int kromer) {
        kromerPool += kromer;
    }

    public static int getKromerPool() {
        return kromerPool;
    }

    public static void saveData() {
        if (tribes.isEmpty()) {
            return;
        }
        // {kromerPool: 69, tribes: {}}
        HashMap<String, Object> data = new HashMap<>(2);
        data.put("kromerPool", kromerPool);
        data.put("friendlyFire", friendlyFire);
        data.put("tribes", tribes);

        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(data);
            FileWriter fileWriter = new FileWriter(Main.dataPath);
            fileWriter.write(json);
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadData() {
        // setter gamerules
        Main.WORLD.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        Main.WORLD.setGameRule(GameRule.DO_TILE_DROPS, true);
        Main.WORLD.setGameRule(GameRule.DO_MOB_LOOT, true);
        Main.WORLD.setGameRule(GameRule.DO_MOB_SPAWNING, true);
        Main.WORLD.setGameRule(GameRule.DO_ENTITY_DROPS, true);
        Main.WORLD.setGameRule(GameRule.FALL_DAMAGE, true);
        Main.WORLD.setGameRule(GameRule.RANDOM_TICK_SPEED, 3);
        Main.WORLD.setDifficulty(Difficulty.HARD);
        ObjectMapper mapper = new ObjectMapper();
        try {
            File file = new File(Main.dataPath);
            JsonData jsonData = mapper.readValue(file, new TypeReference<>() { // JsonData er wrapper class, som wrapper alle variablene i en class så går jackson fortere og lettere
            });
            kromerPool = jsonData.getKromerPool();
            friendlyFire = jsonData.friendlyFire();
            tribes = jsonData.getTribes();
            Bukkit.getLogger().info("Successfully loaded " + tribes.size() + " tribes");
        } catch (IOException e) {
            Bukkit.getLogger().warning("Error occurred while loading JSON data:");
            e.printStackTrace();
        }
    }

    public static NamedTextColor toTextColor(ChatColor chatColor) {
        return switch (chatColor) {
            case BLACK, ITALIC, UNDERLINE, STRIKETHROUGH, BOLD -> NamedTextColor.BLACK;
            case DARK_BLUE -> NamedTextColor.DARK_BLUE;
            case DARK_GREEN -> NamedTextColor.DARK_GREEN;
            case DARK_AQUA -> NamedTextColor.DARK_AQUA;
            case DARK_RED -> NamedTextColor.DARK_RED;
            case DARK_PURPLE, MAGIC -> NamedTextColor.DARK_PURPLE;
            case GOLD -> NamedTextColor.GOLD;
            case GRAY -> NamedTextColor.GRAY;
            case DARK_GRAY -> NamedTextColor.DARK_GRAY;
            case BLUE -> NamedTextColor.BLUE;
            case GREEN -> NamedTextColor.GREEN;
            case AQUA -> NamedTextColor.AQUA;
            case RED -> NamedTextColor.RED;
            case LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE;
            case YELLOW -> NamedTextColor.YELLOW;
            case WHITE, RESET -> NamedTextColor.WHITE;
        };
    }
}
