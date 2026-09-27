package gruvexp.tribes

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import net.kyori.adventure.text.Component
import org.bukkit.*
import org.bukkit.entity.Player
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.util.UUID

object Tribes {
    // A godclass that probably breaks every SOLID rule
    @JvmField
    var friendlyFire: Boolean = false
    private var tribes = HashMap<UUID, Tribe>()
    private var playerTribes = mutableMapOf<Player, Tribe>()
    private val tribeIds = mutableMapOf<String, Tribe>()
    @JvmStatic
    var kromerPool: Int = 0
        private set

    @JvmStatic
    fun addTribe(p: Player, tribe: Tribe) {
        tribes[tribe.playerId] = tribe
        playerTribes[p] = tribe
    }

    @JvmStatic
    fun tribeExists(tribeID: String?): Boolean {
        return tribeIds.containsKey(tribeID)
    }

    @JvmStatic
    fun getTribe(tribeId: String): Tribe? {
        return tribeIds[tribeId]
    }

    fun getTribe(p: Player): Tribe? {
        return playerTribes[p]
    }

    @JvmStatic
    fun getTribe(playerId: UUID): Tribe? {
        return tribes[playerId]
    }

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

}
