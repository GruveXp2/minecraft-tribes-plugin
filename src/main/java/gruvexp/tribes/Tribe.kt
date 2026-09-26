package gruvexp.tribes

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import java.util.UUID

class Tribe(
    @JsonProperty("playerId") playerId: String,
    @JsonProperty("color") colorHex: String,
    @JsonProperty("name") var name: String,
    @JsonProperty("playerName") var playerName: String
) {
    @JsonProperty("playerId")
    val playerId: UUID = UUID.fromString(playerId)

    @JsonIgnore
    var color: TextColor = requireNotNull(TextColor.fromHexString(colorHex)) { "Invalid tribe color: $colorHex" }

    @get:JsonProperty("color")
    private val colorHex: String
        get() = color.asHexString()

    @get:JsonIgnore
    val displayName: Component
        get() = Component.text(name, color)

    @get:JsonIgnore
    val commandName: String
        get() = name.lowercase().replace(" ", "_")

    var kromerBalance: Int = 0
        private set

    constructor(p: Player, name: String, color: NamedTextColor) : this(p.uniqueId.toString(), color.asHexString(), name, p.name)

    fun addKromers(kromers: Int) {
        kromerBalance += kromers
    }

    fun handleJoin(p: Player) { // when someone comes online
        playerName = p.name
        ItemManager.registerCoinRecipes(this)
    }
}
