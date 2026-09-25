package gruvexp.tribes

import com.fasterxml.jackson.annotation.JsonProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.util.*

class Tribe(
    @JsonProperty("playerId") playerId: String,
    @field:JsonProperty("color") @param:JsonProperty("color") var color: NamedTextColor,
    @JsonProperty("name") var name: String,
    @JsonProperty("name") var playerName: String
) {
    @JsonProperty("playerId")
    val playerId: UUID = UUID.fromString(playerId)

    val displayName: Component
        get() = Component.text(name, color)

    val commandName: String
        get() = name.lowercase().replace(" ", "_")

    var kromerBalance: Int = 0
        private set

    constructor(p: Player, name: String, color: NamedTextColor) : this(p.uniqueId.toString(), color, name, p.name)

    fun addKromers(kromers: Int) {
        kromerBalance += kromers
    }

    fun handleJoin(p: Player) { // when someone comes online
        playerName = p.name
        ItemManager.registerCoinRecipes(p.uniqueId)
    }
}
