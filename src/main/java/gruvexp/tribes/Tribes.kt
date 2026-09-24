package gruvexp.tribes

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import gruvexp.tribes.tasks.CooldownReduction
import gruvexp.tribes.tasks.PauseIn1Min
import gruvexp.tribes.tasks.RespawnCooldown
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

object Tribes {
    // A godclass that probably breaks every SOLID rule
    @kotlin.jvm.JvmField
    var friendlyFire: Boolean = false
    private val members = HashMap<UUID, Member?>() // liste over alle members uavhengig av tribe
    private val playerPauseCoords = HashMap<UUID?, Location?>()
    private val playerDeathCoords = HashMap<UUID?, Location?>()
    private val playerSpectatingStatus = HashMap<UUID?, Boolean?>()
    private val revivalAltars =
        HashMap<Location?, RevivalAltar?>() // har refrences til revivalalterene. Alt gjøres fra alter objektet
    private val availableAltars =
        HashMap<UUID?, HashSet<RevivalAltar?>?>() // hver player har et sett med altere som er klare til å respawne playeren om den dauer
    private val postInitObjects = HashSet<PostInit>()
    private val pauseBar = Bukkit.createBossBar("Game Paused", BarColor.YELLOW, BarStyle.SOLID)
    private var tribes = HashMap<String?, Tribe>()
    var isPaused: Boolean =
        false // alle står stille osv. begynner som false og settes til true rett etter serveren har starta
        private set
    var isReducingCooldowns: Boolean = false
        private set
    private var pauseCooldown: PauseIn1Min? = null
    @kotlin.jvm.JvmStatic
    var kromerPool: Int = 0
        private set

    @kotlin.jvm.JvmStatic
    fun pause() {
        if (isPaused) {
            return
        }
        isPaused = true
        pauseCooldown = null
        Bukkit.getServerTickManager().setFrozen(true)
        //Main.WORLD.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
        //Main.WORLD.setGameRule(GameRule.DO_FIRE_TICK, false);
        //Main.WORLD.setGameRule(GameRule.RANDOM_TICK_SPEED, 0);
        Bukkit.broadcast(
            Component.text(
                "The game is now paused. Wait for someone from another tribe to join",
                NamedTextColor.GOLD
            )
        )
        pauseBar.setVisible(true)
        for (playerID in members.keys) {
            val p = Bukkit.getPlayer(playerID)
            if (p == null) {
                continue
            }
            playerPauseCoords.put(playerID, p.getLocation())
        }
    }

    @kotlin.jvm.JvmStatic
    fun unPause() {
        if (!isPaused) {
            return
        }
        isPaused = false
        Bukkit.getServerTickManager().setFrozen(false)
        /*Main.WORLD.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, true);
        Main.WORLD.setGameRule(GameRule.DO_FIRE_TICK, true);
        Main.WORLD.setGameRule(GameRule.RANDOM_TICK_SPEED, 3);*/
        messagePlayers(Component.text("The game is now unpaused. Have fun!").color(NamedTextColor.GREEN))
        Bukkit.getLogger().info("\u001B[32m" + "Game unpaused" + "\u001B[0m")
        pauseBar.setVisible(false)
        val onlinePlayers = Bukkit.getOnlinePlayers()
        for (p in onlinePlayers) {
            p.setInvulnerable(true)
        }
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), Runnable {
            for (p in onlinePlayers) { // hvis noen leaver med en gang så mister de fortsatt invulnerability
                p.setInvulnerable(false)
            }
        }, 100L)
    }

    fun setPauseLocation(playerID: UUID?, loc: Location?) {
        playerPauseCoords.put(playerID, loc)
    }

    @kotlin.jvm.JvmStatic
    fun getPauseLocation(playerID: UUID?): Location? {
        return playerPauseCoords.get(playerID)
    }

    @kotlin.jvm.JvmStatic
    fun setDeathLocation(playerID: UUID?, loc: Location?) {
        playerDeathCoords.put(playerID, loc)
    }

    @kotlin.jvm.JvmStatic
    fun getDeathLocation(playerID: UUID?): Location? {
        return playerDeathCoords.get(playerID)
    }

    @kotlin.jvm.JvmStatic
    fun schedulePostInit(`object`: PostInit?) {
        postInitObjects.add(`object`!!)
    }

    @kotlin.jvm.JvmStatic
    fun postInit() {
        for (`object` in postInitObjects) {
            `object`.postInit()
        }
        pause()
        considerCooldownReduction()
        for (tribe in tribes.values) {
            for (playerID in tribe.getMemberIDs()) {
                if (!tribe.getMember(playerID).isAlive()) {
                    playerSpectatingStatus.put(
                        playerID,
                        false
                    ) // alle som er daue og joiner serveren spawner på dødsstedet
                }
            }
        }
    }

    @kotlin.jvm.JvmStatic
    fun addTribe(tribe: Tribe) {
        tribes.put(tribe.ID, tribe)
    }

    @kotlin.jvm.JvmStatic
    fun tribeExists(tribeID: String?): Boolean {
        return tribes.containsKey(tribeID)
    }

    @kotlin.jvm.JvmStatic
    fun getTribe(tribeID: String?): Tribe {
        val tribe: Tribe = tribes.get(tribeID)!!
        requireNotNull(tribe) { "The tribe \"" + tribeID + "\" doesnt exist!" }
        return tribe
    }

    @kotlin.jvm.JvmStatic
    fun registerAltar(loc: Location?, altar: RevivalAltar?) {
        revivalAltars.put(loc, altar)
    }

    @kotlin.jvm.JvmStatic
    fun getAltar(loc: Location?): RevivalAltar? {
        return revivalAltars.get(loc)
    }

    @kotlin.jvm.JvmStatic
    fun unRegisterAltar(loc: Location?) {
        revivalAltars.remove(loc)
    }

    @kotlin.jvm.JvmStatic
    fun provideAltar(altar: RevivalAltar) { // gjør at det er available for spawning
        val selectedPlayerID = altar.getSelectedPlayerID()
        availableAltars.computeIfAbsent(selectedPlayerID) { k: UUID? -> HashSet<RevivalAltar?>() }
        availableAltars.get(selectedPlayerID)!!.add(altar)

        //debugMessage("RevivalAltar at " + Utils.toString(altar.LOCATION) + " now available to spawn " + selectedPlayer);
        if (availableAltars.get(selectedPlayerID)!!.size == 1) { // hvis ingen altere var ledige før og dette er det første som ble ledig, skjekk om selectedPlayer er dau og venter på et tilgjengelig alter, hvis det så er jo dette alteret ledig og da spawner vi playeren
            val member = getMember(selectedPlayerID)
            if (member != null && !member.isAlive() && member.isOnline()) {
                val p = Bukkit.getPlayer(selectedPlayerID)
                member.respawnAtAltar(p, altar)
            }
        }
    }

    @kotlin.jvm.JvmStatic
    fun withdrawAltar(altar: RevivalAltar) {
        val selectedPlayerID = altar.getSelectedPlayerID()
        availableAltars.get(selectedPlayerID)!!.remove(altar)
        //debugMessage("RevivalAltar at " + Utils.toString(altar.LOCATION) + " no longer available");
    }

    @kotlin.jvm.JvmStatic
    fun getAvailableAltar(playerID: UUID?): RevivalAltar? { // returnerer et random alter som er klar til å spawne inn playeren
        val altarSet = availableAltars.get(playerID)
        if (altarSet == null) {
            return null
        }
        val altars = ArrayList<RevivalAltar?>(altarSet)
        if (altars.isEmpty()) return null
        return altars.getFirst()
    }

    @kotlin.jvm.JvmStatic
    val tribeIDs: MutableSet<String?>
        get() = tribes.keys

    @kotlin.jvm.JvmStatic
    fun getTribes(): MutableCollection<Tribe> {
        return tribes.values
    }

    @kotlin.jvm.JvmStatic
    val memberIDs: MutableSet<UUID>
        get() = members.keys

    @kotlin.jvm.JvmStatic
    fun registerMember(member: Member) {
        members.put(member.ID, member)
    }

    @kotlin.jvm.JvmStatic
    fun getMember(playerID: UUID?): Member? {
        return members.get(playerID!!)
    }

    @kotlin.jvm.JvmStatic
    fun unRegisterMember(playerID: UUID?) {
        members.remove(playerID!!)
    }

    @kotlin.jvm.JvmStatic
    fun handleDeath(playerID: UUID?) {
        playerSpectatingStatus.put(playerID, false)
    }

    @kotlin.jvm.JvmStatic
    fun messagePlayers(message: String) {
        Bukkit.getOnlinePlayers().forEach { p: Player -> p.sendMessage(message) }
    }

    fun messagePlayers(message: Component) {
        Bukkit.getOnlinePlayers().forEach { p: Player -> p.sendMessage(message) }
    }

    @kotlin.jvm.JvmStatic
    fun debugMessage(message: String?) { // used 4 debugging stuff by printing it in the chat
        messagePlayers(ChatColor.GRAY.toString() + "[DEBUG]: " + message)
        Bukkit.getLogger().info("[DEBUG]: " + message)
    }

    @kotlin.jvm.JvmStatic
    fun considerPauseToggle() {
        if (Main.WORLD.getName() == Main.testWorldName) { // TEST DEBUG SKAL FJERNES ETTERPÅ!!! <=============
            unPause()
            debugMessage("Server unpaused bc its a testing server")
            return
        }
        //debugMessage("considering to toggle pause");
        var activeTribes = 0
        for (tribe in tribes.values) {
            //debugMessage(tribe.ID + " status: " + (tribe.isActive() ? "active" : "inactive"));
            if (tribe.isActive()) {
                activeTribes++
            }
        }
        val active = activeTribes >= 2
        // etter aktivheten er regna ut, så utføres
        if (active) {
            if (pauseCooldown != null) {
                pauseCooldown!!.cancelPause()
                Bukkit.broadcast(Component.text("Someone joined, pausing cancelled", NamedTextColor.GREEN))
                pauseCooldown = null
            } else {
                unPause()
            }
        } else {
            if (activeTribes == 0) {
                // close server in 5 seconds
                Bukkit.broadcast(
                    Component.text(
                        "No more players online, server will shutdown in 5 seconds",
                        NamedTextColor.RED
                    )
                )
                Bukkit.getScheduler().runTaskLater(Main.getPlugin(), Runnable { Bukkit.shutdown() }, 100L)
            }
            if (isPaused) {
                return
            }
            if (Bukkit.getOnlinePlayers().isEmpty()) {
                pause() // hvis den siste playeren leava så er dekke no vits å starte en pause timer
                return
            }
            if (pauseCooldown != null) {
                return
            } // hvis det allerede er en cooldown, ikke start en ny en

            pauseCooldown = PauseIn1Min()
            pauseCooldown!!.runTaskTimer(Main.getPlugin(), 0, 20)
        }
    }

    @kotlin.jvm.JvmStatic
    fun considerCooldownReduction() { // hvis alle er daue så reduserers cooldownen til den første spawner
        //debugMessage("considering cooldown reduction");
        for (tribe in tribes.values) {
            if (tribe.isAlive()) {
                return  // skal kun redusere cooldown hvis absolutt alle aktive spillere er daue
            }
        }
        //start cooldownreduksjon
        //debugMessage("starting cooldown reduction");
        isReducingCooldowns = true
        val respawnCooldowns = HashSet<RespawnCooldown?>()
        for (tribe in tribes.values) {
            for (memberID in tribe.getMemberIDs()) {
                val respawnCooldown = tribe.getMember(memberID).getRespawnCooldownTask()
                if (respawnCooldown != null) {
                    respawnCooldowns.add(respawnCooldown) // adder respawnCooldowns til en liste, og alle i listen vil få cooldownen redusert
                }
            }
        }
        Bukkit.broadcast(Component.text("No players alive, cooldown timers will be reduced", NamedTextColor.GREEN))
        CooldownReduction(respawnCooldowns).runTaskTimer(Main.getPlugin(), 0, 1)
    }

    @kotlin.jvm.JvmStatic
    fun stopCooldownReduction() {
        isReducingCooldowns = false
    }

    @kotlin.jvm.JvmStatic
    fun handlePlayerJoin(p: Player) {
        pauseBar.addPlayer(p)
        if (isPaused) {
            setPauseLocation(p.getUniqueId(), p.getLocation())
        }
    }

    @kotlin.jvm.JvmStatic
    fun handleMemberLeave(playerID: UUID) { // når en player leaver triben
        val p = Bukkit.getPlayer(playerID)
        if (p == null) {
            return
        }
        pauseBar.removePlayer(p)
    }

    @kotlin.jvm.JvmStatic
    fun addKromersToPool(kromer: Int) {
        kromerPool += kromer
    }

    @kotlin.jvm.JvmStatic
    fun saveData() {
        if (tribes.isEmpty()) {
            return
        }
        // {kromerPool: 69, tribes: {}}
        val data = HashMap<String?, Any?>(2)
        data.put("kromerPool", kromerPool)
        data.put("friendlyFire", friendlyFire)
        data.put("tribes", tribes)

        val mapper = ObjectMapper()
        try {
            val json = mapper.writeValueAsString(data)
            val fileWriter = FileWriter(Main.dataPath)
            fileWriter.write(json)
            fileWriter.close()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    @kotlin.jvm.JvmStatic
    fun loadData() {
        // setter gamerules
        Main.WORLD.setGameRule<Boolean?>(GameRule.DO_DAYLIGHT_CYCLE, true)
        Main.WORLD.setGameRule<Boolean?>(GameRule.DO_TILE_DROPS, true)
        Main.WORLD.setGameRule<Boolean?>(GameRule.DO_MOB_LOOT, true)
        Main.WORLD.setGameRule<Boolean?>(GameRule.DO_MOB_SPAWNING, true)
        Main.WORLD.setGameRule<Boolean?>(GameRule.DO_ENTITY_DROPS, true)
        Main.WORLD.setGameRule<Boolean?>(GameRule.FALL_DAMAGE, true)
        Main.WORLD.setGameRule<Int?>(GameRule.RANDOM_TICK_SPEED, 3)
        Main.WORLD.setDifficulty(Difficulty.HARD)
        val mapper = ObjectMapper()
        try {
            val file = File(Main.dataPath)
            val jsonData = mapper.readValue<JsonData>(file, object : TypeReference<JsonData?>() {
                // JsonData er wrapper class, som wrapper alle variablene i en class så går jackson fortere og lettere
            })
            kromerPool = jsonData.getKromerPool()
            friendlyFire = jsonData.friendlyFire()
            tribes = jsonData.getTribes()
            Bukkit.getLogger().info("Successfully loaded " + tribes.size + " tribes")
        } catch (e: IOException) {
            Bukkit.getLogger().warning("Error occurred while loading JSON data:")
            e.printStackTrace()
        }
    }

    @kotlin.jvm.JvmStatic
    fun toTextColor(chatColor: ChatColor): NamedTextColor {
        return when (chatColor) {
            ChatColor.BLACK, ChatColor.ITALIC, ChatColor.UNDERLINE, ChatColor.STRIKETHROUGH, ChatColor.BOLD -> NamedTextColor.BLACK
            ChatColor.DARK_BLUE -> NamedTextColor.DARK_BLUE
            ChatColor.DARK_GREEN -> NamedTextColor.DARK_GREEN
            ChatColor.DARK_AQUA -> NamedTextColor.DARK_AQUA
            ChatColor.DARK_RED -> NamedTextColor.DARK_RED
            ChatColor.DARK_PURPLE, ChatColor.MAGIC -> NamedTextColor.DARK_PURPLE
            ChatColor.GOLD -> NamedTextColor.GOLD
            ChatColor.GRAY -> NamedTextColor.GRAY
            ChatColor.DARK_GRAY -> NamedTextColor.DARK_GRAY
            ChatColor.BLUE -> NamedTextColor.BLUE
            ChatColor.GREEN -> NamedTextColor.GREEN
            ChatColor.AQUA -> NamedTextColor.AQUA
            ChatColor.RED -> NamedTextColor.RED
            ChatColor.LIGHT_PURPLE -> NamedTextColor.LIGHT_PURPLE
            ChatColor.YELLOW -> NamedTextColor.YELLOW
            ChatColor.WHITE, ChatColor.RESET -> NamedTextColor.WHITE
        }
    }
}
