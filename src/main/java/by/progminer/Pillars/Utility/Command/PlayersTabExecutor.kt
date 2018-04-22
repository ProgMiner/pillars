package by.progminer.Pillars.Utility.Command

import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

open class PlayersTabExecutor(
        private val server: Server,
        private val child: TabExecutor = BaseTabExecutor(CommandExecutor { _, _, _, _ -> throw UnsupportedOperationException() })
): NodeTabExecutor(listOf()) {

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): List<String> {
        commands = server.onlinePlayers.associate { Pair(it.name, child) }
        return super.onTabComplete(sender, command, alias, argsArray)
    }
}