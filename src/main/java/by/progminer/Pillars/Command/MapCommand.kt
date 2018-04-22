package by.progminer.Pillars.Command

import by.progminer.Pillars.GameMap
import by.progminer.Pillars.Main
import by.progminer.Pillars.Utility.Command.BaseTabExecutor
import by.progminer.Pillars.Utility.Command.MapsTabExecutor
import by.progminer.Pillars.Utility.Command.NodeTabExecutor
import by.progminer.Pillars.Utility.toHumanReadable
import org.bukkit.Location
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class MapCommand(private val main: Main): NodeTabExecutor(mutableMapOf(
        "list" to BaseTabExecutor(CommandExecutor { sender, _, _, _ ->
            sender.sendMessage(main.mapStorage.keys.toTypedArray())
            return@CommandExecutor true
        }),
        "info" to object: MapsTabExecutor(main.mapStorage) {

            override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
                val args = cleanEmptyArgs(argsArray)

                if (args.isEmpty()) {
                    return false
                }

                val mapName = args[0]
                if (!main.mapStorage.containsKey(mapName)) {
                    sender.sendMessage("Map \"$mapName\" is not exists")
                    return true
                }

                val ret = mutableListOf<String>()
                main.mapStorage[mapName]!!.serialize().forEach { name, value ->
                    if (value is Location) {
                        ret.add("$name: ${value.toHumanReadable()}.")
                    } else {
                        ret.add("$name: $value.")
                    }
                }

                sender.sendMessage(ret.toTypedArray())
                return true
            }
        },
        "init" to BaseTabExecutor(CommandExecutor { sender, _, _, argsArray ->
            if (sender !is Player) {
                sender.sendMessage("This command available only for players")
                return@CommandExecutor true
            }

            val args = cleanEmptyArgs(argsArray)

            if (args.isEmpty()) {
                return@CommandExecutor false
            }

            val mapName = args[0]
            val author = if (args.size < 2) {
                sender.name
            } else {
                args[1]
            }

            if (main.mapStorage.containsKey(mapName)) {
                sender.sendMessage("Map \"$mapName\" is already exists")
                return@CommandExecutor true
            }

            val point = sender.location
            main.mapStorage.addMap(mapName, GameMap(point, point, author))

            sender.sendMessage(arrayOf(
                    "Map \"$mapName\" successfully initialized!",
                    """Now you can finish it by direct maps file editing"""
            ))
            return@CommandExecutor true
        })
))