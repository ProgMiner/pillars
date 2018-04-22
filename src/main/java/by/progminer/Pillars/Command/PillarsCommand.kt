package by.progminer.Pillars.Command

import by.progminer.Pillars.Main
import by.progminer.Pillars.Utility.Command.BaseTabExecutor
import by.progminer.Pillars.Utility.Command.NodeTabExecutor
import org.bukkit.command.CommandExecutor

class PillarsCommand(private val main: Main): NodeTabExecutor(mapOf(
        "help" to HelpCommand(main),
        "game" to GameCommand(main),
        "map" to MapCommand(main),
        "reload" to BaseTabExecutor(CommandExecutor { _, _, _, _ ->
            val pluginLoader = main.pluginLoader

            pluginLoader.disablePlugin(main)
            main.onLoad()
            pluginLoader.enablePlugin(main)

            return@CommandExecutor true
        })
))