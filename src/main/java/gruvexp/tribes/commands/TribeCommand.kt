package gruvexp.tribes.commands

import gruvexp.tribes.Main
import gruvexp.tribes.Member
import gruvexp.tribes.Tribe
import gruvexp.tribes.Tribes
import gruvexp.tribes.Tribes.addTribe
import gruvexp.tribes.Tribes.getMember
import gruvexp.tribes.Tribes.getTribe
import gruvexp.tribes.Tribes.getTribes
import gruvexp.tribes.Tribes.isPaused
import gruvexp.tribes.Tribes.kromerPool
import gruvexp.tribes.Tribes.messagePlayers
import gruvexp.tribes.Tribes.pause
import gruvexp.tribes.Tribes.tribeExists
import gruvexp.tribes.Tribes.unPause
import net.kyori.adventure.text.Component
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
        var p: Player? = null
        if (sender is Player) {
            p = sender
        }

        try {
            require(args.size != 0) { "Not enough args!\nUsage: /tribe [create | join | switch | leave | stats | pause]" }
            val oper = args[0]
            when (oper) {
                "stats" -> {
                    var totalCoins = 0
                    val lines = 100 // hvor mange |
                    val tribeBalance = mutableMapOf<Tribe, Int>() // brukt for å beregne hvor mange kr hver tribe har i kromerDisctribution
                    for (tribe in getTribes()) {
                        tribeBalance[tribe] = tribe.getCoinBalance()
                        totalCoins += tribe.getCoinBalance()
                    }
                    var kromerDistribution: Component = Component.text("Kromer distribution: ")
                    for ((key, value) in tribeBalance) {
                        kromerDistribution = kromerDistribution.append(
                            Component.text(
                                "|".repeat(value * lines / totalCoins),
                                NamedTextColor.NAMES.value(key.COLOR.name.lowercase(Locale.getDefault()))
                            )
                        )
                    }
                    sender.sendMessage(kromerDistribution) // bar som viser fordelinga av kromers, fargelagt
                    sender.sendMessage(
                        Component.text("Kromer pool: ")
                            .append(Component.text("$kromerPool kr", NamedTextColor.GREEN))
                    ) // kromer pool
                    for (tribe in getTribes()) {
                        val balance = tribe.getMembers().stream().mapToInt { obj: Member? -> obj!!.getKromers() }.sum()
                        sender.sendMessage(tribe.COLOR.toString() + tribe.displayName() + " tribe: " + ChatColor.GREEN + balance + "kr ")
                        for (member in tribe.getMembers()) {
                            val playerID = member.ID
                            var playerStats = Component.text(
                                String.format(
                                    "%-12s",
                                    member.NAME
                                )
                            ) // adder mellomrom så han blir 12 bokstaver lang
                            playerStats = playerStats.append(Component.text(", Balance: "))
                                .append(Component.text(member.getKromers().toString() + " kr", NamedTextColor.GREEN))

                            sender.sendMessage(playerStats)
                        }
                    }
                }

                "add", "create" -> { // /create <tribeID> <color> <displayName>
                    require(args.size >= 3) { "Not enough args!" }
                    val tribeID: String? = args[1]
                    require(!tribeExists(tribeID)) { "Tribe already exists!" }
                    val color = args[2].uppercase(Locale.getDefault())
                    if (args.size > 3) {
                        val displayName = StringBuilder()
                        for (i in 3..<args.size) {
                            if (i > 3) {
                                displayName.append(" ")
                            }
                            displayName.append(args[i])
                        }
                        addTribe(Tribe(tribeID, ChatColor.valueOf(color), displayName.toString()))
                        Bukkit.broadcast(Component.text("New tribe created: " + displayName, NamedTextColor.GREEN))
                    } else {
                        addTribe(Tribe(tribeID, ChatColor.valueOf(color), tribeID))
                        Bukkit.broadcast(Component.text("New tribe created: " + tribeID, NamedTextColor.GREEN))
                    }
                }

                "join" -> { // /join <tribeID> <playerName>
                    require(args.size >= 3) { "Not enough args!" }
                    val tribeID: String? = args[1]
                    val playerName = args[2]
                    val joiningPlayer = Bukkit.getPlayer(playerName)
                    requireNotNull(joiningPlayer) { "No online player called \"" + playerName + "\" was found" }
                    val tribe = getTribe(tribeID)
                    tribe.addMember(joiningPlayer)
                    val q = Bukkit.getPlayerExact(playerName)
                    if (q != null) {
                        q.displayName(Component.text(q.getName(), NamedTextColor.NAMES.value(tribe.COLOR.toString())))
                    }
                }

                "kick", "leave" -> {
                    checkNotNull(p)
                    checkAdmin(p)
                    require(args.size >= 3) { "Not enough args!" }
                    val tribeID: String? = args[1]
                    val playerName = args[2]
                    val playerID = Bukkit.getOfflinePlayer(playerName).getUniqueId()
                    getTribe(tribeID).removeMember(playerID)
                }

                "switch" -> {
                    checkNotNull(p)
                    require(args.size >= 3) { "Not enough args!" }
                    val tribeID: String? = args[1]
                    val playerName = args[2]
                    val playerID = Bukkit.getOfflinePlayer(playerName).getUniqueId()
                    val member = getMember(playerID)
                    requireNotNull(member) { "That player wasnt in a tribe to begin with!" }
                    val tribe = getTribe(tribeID)
                    tribe.migrateMemberToThisTribe(member)
                    val q = Bukkit.getPlayerExact(playerName)
                    if (q != null) {
                        q.displayName(Component.text(q.getName(), NamedTextColor.NAMES.value(tribe.COLOR.toString())))
                    }
                }

                "pause" -> {
                    require(!isPaused) { "The game is already paused! Use /tribe unpause to unpause" }
                    pause()
                }

                "unpause" -> {
                    if (isPaused) {
                        unPause()
                    } else {
                        throw IllegalArgumentException("The game is already unpaused! Use /tribe pause to pause")
                    }
                }

                "toggle_friendly_fire" -> {
                    Tribes.friendlyFire = !Tribes.friendlyFire
                    checkNotNull(p)
                    var message = "[" + p.getName() + "]: Pvp between tribe members set to: "
                    message += if (Tribes.friendlyFire) ChatColor.GREEN.toString() + "ENABLED" else ChatColor.RED.toString() + "DISABLED"
                    messagePlayers(message)
                }

                "test" -> {
                    checkNotNull(p)
                    val entity = p.getSpectatorTarget()
                    val name = if (entity == null) "noone" else entity.getName()
                    p.sendMessage("Currently spectating: " + name)
                }

                "version" -> sender.sendMessage("Plugin was last updated " + Main.VERSION)
                else -> throw IllegalArgumentException(NamedTextColor.RED.toString() + "\"" + oper + "\" is not a valid operation!")
            }
        } catch (e: IllegalArgumentException) {
            sender.sendMessage(Component.text(e.message!!, NamedTextColor.RED))
        }
        return true
    }

    private fun checkAdmin(p: Player) {
        require(p.isOp()) { NamedTextColor.RED.toString() + "You dont have permission to run this command. If you have any queestions, please contact Colin or Gruve" }
    }
}
