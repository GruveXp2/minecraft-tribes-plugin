package gruvexp.tribes.commands

import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class TribeTabCompletion : TabCompleter {
    override fun onTabComplete(sender: CommandSender, command: Command, label: String, args: Array<String>): MutableList<String> {
        if (args.size == 1) return mutableListOf("create", "stats")

        when (val oper = args[0]) {
            "create", "init" -> { // create <color> <displayName>
                if (args.size == 2) return NamedTextColor.NAMES.keys().map { it.lowercase() }.toMutableList()
                if (args.size == 3) return mutableListOf("<display name>")
                return mutableListOf()
            }

            "stats" -> {
                return mutableListOf()
            }

            else -> return mutableListOf("'$oper' is not a valid operation!")
        }
    }
}
