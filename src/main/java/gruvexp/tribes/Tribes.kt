package gruvexp.tribes

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.*
import org.bukkit.entity.Player
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.*

object Tribes {
    // A godclass that probably breaks every SOLID rule
    @JvmField
    var friendlyFire: Boolean = false
    private val playerDeathCoords = HashMap<UUID?, Location?>()
    private var tribes = HashMap<String?, Tribe>()
    private var playerTribes = mutableMapOf<Player, Tribe>()
    @JvmStatic
    var kromerPool: Int = 0
        private set

    @JvmStatic
    fun setDeathLocation(playerID: UUID?, loc: Location?) {
        playerDeathCoords.put(playerID, loc)
    }

    @JvmStatic
    fun getDeathLocation(playerID: UUID?): Location? {
        return playerDeathCoords.get(playerID)
    }

    @JvmStatic
    fun addTribe(p: Player, tribe: Tribe) {
        tribes[tribe.playerId.toString()] = tribe
        playerTribes[p] = tribe
    }

    @JvmStatic
    fun tribeExists(tribeID: String?): Boolean {
        return tribes.containsKey(tribeID)
    }

    @JvmStatic
    fun getTribe(tribeID: String?): Tribe {
        val tribe: Tribe = tribes.get(tribeID)!!
        requireNotNull(tribe) { "The tribe \"" + tribeID + "\" doesnt exist!" }
        return tribe
    }

    fun getTribe(p: Player): Tribe? {
        return playerTribes[p]
    }

    fun getTribe(p: Player): Tribe? {
        return playerTribes[p]
    }

    @JvmStatic
    val tribeIDs: MutableSet<String?>
        get() = tribes.keys

    @JvmStatic
    fun getTribes(): MutableCollection<Tribe> {
        return tribes.values
    }

    @JvmStatic
    fun messagePlayers(message: String) {
        Bukkit.getOnlinePlayers().forEach { p: Player -> p.sendMessage(message) }
    }

    fun messagePlayers(message: Component) {
        Bukkit.getOnlinePlayers().forEach { p: Player -> p.sendMessage(message) }
    }

    @JvmStatic
    fun debugMessage(message: String?) { // used 4 debugging stuff by printing it in the chat
        messagePlayers(ChatColor.GRAY.toString() + "[DEBUG]: " + message)
        Bukkit.getLogger().info("[DEBUG]: " + message)
    }

    @JvmStatic
    fun addKromersToPool(kromer: Int) {
        kromerPool += kromer
    }

    @JvmStatic
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

    @JvmStatic
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

    @JvmStatic
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
