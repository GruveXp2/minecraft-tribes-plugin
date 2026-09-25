package gruvexp.tribes.commands

import gruvexp.tribes.Main
import gruvexp.tribes.Tribe
import gruvexp.tribes.Tribes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import java.util.*

class TribeCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val message = runCommandAndMessage(sender, args)
        sender.sendMessage(message)
        return true
    }

    private fun runCommandAndMessage(sender: CommandSender, args: Array<String>): TextComponent {
        var p: Player? = null
        if (sender is Player) {
            p = sender
        }

        when (val oper = args[0]) {
            "stats" -> {
                var totalCoins = 0
                val lines = 100 // hvor mange |
                val tribeBalance = mutableMapOf<Tribe, Int>() // brukt for å beregne hvor mange kr hver tribe har i kromerDisctribution
                for (tribe in Tribes.getTribes()) {
                    tribeBalance[tribe] = tribe.coinBalance
                    totalCoins += tribe.coinBalance
                }
                return Component.text("Kromer distribution: ").apply {
                    tribeBalance.forEach { (tribe, balance) ->
                        append(Component.text("|".repeat(balance * lines / totalCoins), tribe.color))
                    }

                    append(Component.text("Kromer pool: "))
                    append(Component.text("${Tribes.kromerPool} kr", NamedTextColor.GREEN))
                    appendNewline()

                    Tribes.getTribes().forEach {
                        val balance = it.coinBalance
                        append(it.displayName)
                        append(Component.text(" (${it.playerId}): "))
                        append(Component.text("$balance kr", NamedTextColor.GREEN))
                        appendNewline()
                    }
                }
            }

            "create", "init" -> { // create <tribeID> <color> <displayName>
                if (args.size == 2) return Component.text("you need to specify an id for your tribe\n", NamedTextColor.RED)
                    .append(Component.text("Usage: ", NamedTextColor.WHITE))
                    .append(Component.text("/tribe create <tribe-id>", NamedTextColor.GREEN))

                val tribeId = args[1]
                if (p == null) return Component.text("tribes must be created ingame", NamedTextColor.YELLOW)

                val alreadyExistingTribe = Tribes.getTribe(p)
                if (alreadyExistingTribe != null) return Component.text("You already registered your tribe!\n", NamedTextColor.YELLOW)
                    .append(Component.text("(Your tribe is called ", NamedTextColor.WHITE)
                        .append(alreadyExistingTribe.displayName)
                        .append(Component.text(")")))

                val color = args.getOrNull(2)?.lowercase()?.let {
                    NamedTextColor.NAMES.value(it)
                } ?: NamedTextColor.WHITE
                if (args.size > 3) {
                    val displayName = args.drop(3).joinToString(" ")
                    Tribes.addTribe(p, Tribe(tribeId, color, displayName))
                    Bukkit.broadcast(Component.text("New tribe created: $displayName", NamedTextColor.GREEN))
                } else {
                    Tribes.addTribe(p, Tribe(tribeId, color, tribeId))
                    Bukkit.broadcast(Component.text("New tribe created: $tribeId", NamedTextColor.GREEN))
                }
                p.displayName(Component.text(p.name, color))
                return Component.empty()
            }

            "toggle_friendly_fire" -> {
                Tribes.friendlyFire = !Tribes.friendlyFire
                checkNotNull(p)
                var message = "[${p.name}]: Pvp between tribe member set to: "
                message += if (Tribes.friendlyFire) ChatColor.GREEN.toString() + "ENABLED" else ChatColor.RED.toString() + "DISABLED"
                Tribes.messagePlayers(message)
                return Component.empty()
            }

            "version" -> return Component.text("Plugin was last updated " + Main.VERSION)
            else -> return Component.text("'$oper' is not a valid operation!", NamedTextColor.RED)
        }
    }

    private fun checkAdmin(p: Player) {
        require(p.isOp) { NamedTextColor.RED.toString() + "You dont have permission to run this command. If you have any queestions, please contact Colin or Gruve" }
    }
}
