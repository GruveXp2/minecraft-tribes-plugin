package gruvexp.tribes

import com.fasterxml.jackson.annotation.JsonProperty
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import java.util.*

class Tribe(
    @JsonProperty("playerId") playerId: String,
    @field:JsonProperty("color") @param:JsonProperty("color") var color: NamedTextColor,
    @JsonProperty("name") var name: String
) {
    @JsonProperty("id")
    val playerId: UUID = UUID.fromString(playerId)

    val displayName: Component
        get() = Component.text(name, color)

    var coinBalance: Int = 0
        private set

    fun addKromers(kromers: Int) {
        coinBalance += kromers
    }

    fun handleJoin(p: Player) { // when someone comes online
        val playerID = p.getUniqueId()
        ItemManager.registerCoinRecipes(playerID)
    }
}
