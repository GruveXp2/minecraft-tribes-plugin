package gruvexp.tribes.commands

import gruvexp.tribes.ItemManager.getStarterItems
import gruvexp.tribes.Main
import gruvexp.tribes.Tribes.getTribe
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player

class JavaCommand : CommandExecutor {
    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<String>): Boolean {
        val p = sender as Player // /java accepttp <spectating player>

        try {
            require(args.isNotEmpty()) { "Server error: missing argument <oper> (/java)" }
            val oper = args[0]
            when (oper) {
                "hack", "hacc" -> {
                    require(!(!(p.name == "GruveXp" || p.name == "ColinStorm" || sender is ConsoleCommandSender) || Main.WORLD.name == Main.testWorldName)) { "Admin abuse can only be performed by admins" }
                    require(args.size != 1) { "Error: missing argument <hack> (/java hack)" }
                    val hack = args[1]
                    when (hack) {
                        "starter_coins", "coins", "get_coins" -> {
                            require(args.size != 2) { "Error: missing argument <player> (/java hack starter_coins)" }
                            val targetPlayerName = args[2]
                            val targetPlayer = Bukkit.getPlayer(targetPlayerName)
                            requireNotNull(targetPlayer) { "Error: Player \"$targetPlayerName\" is not online!" }
                            val gruveXp = Bukkit.getPlayer("GruveXp")
                            requireNotNull(gruveXp) { "Bruhh gwuve not online :(" }
                            val targetPlayerID = targetPlayer.uniqueId
                            targetPlayer.inventory.addItem(getStarterItems(targetPlayerID))
                            getTribe(targetPlayerID)!!.addKromers(320)
                        }

                        "change_registered_balance" -> {
                            require(args.size >= 4) { "Error: not enough args! /java hack change_registered_balance <player> <amount>" }
                            val targetPlayerName = args[2]
                            val playerID = Bukkit.getOfflinePlayer(targetPlayerName).uniqueId
                            val tribe = getTribe(playerID)
                            requireNotNull(tribe) { "That member doesnt exist!" }
                            val Δkr: Int
                            try {
                                Δkr = args[3].toInt()
                            } catch (e: NumberFormatException) {
                                throw IllegalArgumentException("\"${args[3]}\" is not a number!")
                            }
                            tribe.addKromers(Δkr)
                            p.sendMessage(
                                Component.text(
                                    "Successfully changed registered kromer balance by $Δkr",
                                    NamedTextColor.GRAY
                                )
                            )
                        }

                        "sb" -> {
                            val itemStack = p.inventory.itemInMainHand
                            val item = Main.WORLD.dropItemNaturally(p.location.add(0.0, -2.0, 0.0), itemStack)
                            item.ticksLived = 11950
                        }

                        "test" -> {
                            require(args.size != 2) { "Error: missing argument <color> (/java hack test)" }
                            val color = args[2]
                            sender.sendMessage(Component.text(color, NamedTextColor.NAMES.value(color)))
                            p.sendMessage("Your tribe has color: ${getTribe(p.uniqueId)!!.color}...")
                        }

                        else -> throw IllegalArgumentException("Error: wrong argument <hack> (/java hack)")
                    }
                }
                else -> throw IllegalArgumentException("Server error: wrong argument <oper> (/java)")
            }
        } catch (e: IllegalArgumentException) {
            p.sendMessage(ChatColor.RED.toString() + e.message)
        }
        return true
    }
}
