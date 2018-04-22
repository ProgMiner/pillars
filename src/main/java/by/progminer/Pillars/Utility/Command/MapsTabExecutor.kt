package by.progminer.Pillars.Utility.Command

import by.progminer.Pillars.MapStorage
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

open class MapsTabExecutor(
        private val mapStorage: MapStorage,
        private val child: TabExecutor = BaseTabExecutor(CommandExecutor { _, _, _, _ -> throw UnsupportedOperationException() })
): NodeTabExecutor(listOf()) {

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): List<String> {
        commands = mapStorage.keys.associate { Pair(it, child) }
        return super.onTabComplete(sender, command, alias, argsArray)
    }
}