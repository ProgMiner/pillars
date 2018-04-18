package by.progminer.Pillars

import by.progminer.Pillars.Utility.AbstractTabCompleter
import org.bukkit.command.Command
import org.bukkit.command.TabExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

class MainCommand(private val plugin: Plugin): AbstractTabCompleter(mapOf(
        "map" to MapCommand(),
        "game" to emptyTabCompleter()
)), TabExecutor {
    private class MapCommand: AbstractTabCompleter(listOf("list", "init")), TabExecutor {

        override fun onCommand(sender: CommandSender, command: Command, label: String, argsArray: Array <String>): Boolean {
            val args = cleanEmptyArgs(argsArray)

            TODO()
        }
    }

    override fun onCommand(sender: CommandSender, command: Command, label: String, argsArray: Array <String>): Boolean {
        val args = cleanEmptyArgs(argsArray)

        if (args.isEmpty()) {
            return false
        }

        val subcmdLabel = "$label ${args[0]}"

        return when (args[0]) {
            "map" -> MapCommand().onCommand(sender, command, subcmdLabel, args)
            "game" -> TODO()
            else -> false
        }
    }
}