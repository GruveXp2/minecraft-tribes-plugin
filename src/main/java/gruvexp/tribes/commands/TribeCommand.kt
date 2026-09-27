package gruvexp.tribes.commands

import gruvexp.tribes.Main
import gruvexp.tribes.Tribe
import gruvexp.tribes.Tribes
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

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

        if (args.isEmpty()) return Component.empty()
            .append(Component.text("Welcome to the Tribes server!\n", NamedTextColor.GREEN, TextDecoration.BOLD))
            .append(Component.text("- Run /tribe create to create your tribe\n"))
            .append(Component.text("- Run "))
            .append(Component.text("/tribe stats", NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/tribe stats")))
            .append(Component.text(" to see stats of all tribes\n"))
            .append(Component.text("- Run "))
            .append(Component.text("/tribe chunk", NamedTextColor.AQUA, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/tribe chunk")))
            .append(Component.text(" to see the chunks you have claimed, and to claim new chunks"))

        when (val oper = args[0]) {
            "stats" -> {
                val tribes = Tribes.getTribes()
                val totalCoins = tribes.sumOf { it.kromerBalance }
                val lines = 100 // # of '|' symbols
                return Component.text().apply {
                    it.append(Component.text("Kromer pool: "))
                    it.append(Component.text("${Tribes.kromerPool} kr", NamedTextColor.GREEN))
                    it.appendNewline()
                    it.append(Component.text("Kromer distribution: "))
                    if (totalCoins > 0) tribes.forEach { tribe ->
                        it.append(Component.text("|".repeat(tribe.kromerBalance * lines / totalCoins), tribe.color))
                    }
                    it.appendNewline()

                    tribes.forEach { tribe ->
                        it.append(tribe.displayName)
                        it.append(Component.text(" (${tribe.playerName}): "))
                        it.append(Component.text("${tribe.kromerBalance} kr", NamedTextColor.GREEN))
                        it.appendNewline()
                    }
                }.build()
            }

            "create", "init" -> { // create <color> <displayName>
                if (args.size == 2) return Component.text("you need to specify an id for your tribe\n", NamedTextColor.RED)
                    .append(Component.text("Usage: ", NamedTextColor.WHITE))
                    .append(Component.text("/tribe create <tribe-id> <color?> <display-name>", NamedTextColor.GREEN))

                if (p == null) return Component.text("tribes must be created ingame", NamedTextColor.YELLOW)

                val alreadyExistingTribe = Tribes.getTribe(p)
                if (alreadyExistingTribe != null) return Component.text("You already registered your tribe!\n", NamedTextColor.YELLOW)
                    .append(Component.text("(Your tribe is called ", NamedTextColor.WHITE)
                        .append(alreadyExistingTribe.displayName)
                        .append(Component.text(")")))

                val color = args.getOrNull(1)?.lowercase()?.let {
                    NamedTextColor.NAMES.value(it)
                } ?: NamedTextColor.WHITE
                if (args.size > 2) {
                    val displayName = args.drop(2).joinToString(" ")
                    Tribes.addTribe(p, Tribe(p, displayName, color))
                    Bukkit.broadcast(Component.text("New tribe created: $displayName", NamedTextColor.GREEN))
                } else {
                    val displayName = "${p.name}'s tribe"
                    Tribes.addTribe(p, Tribe(p, displayName, color))
                    Bukkit.broadcast(Component.text("New tribe created: $displayName", NamedTextColor.GREEN))
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
