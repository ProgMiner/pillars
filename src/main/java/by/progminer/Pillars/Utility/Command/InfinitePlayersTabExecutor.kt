package by.progminer.Pillars.Utility.Command

import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

open class InfinitePlayersTabExecutor(protected val server: Server, protected val limit: Long = 12): NodeTabExecutor(listOf()) {

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): List<String> {
        if (limit == 0L) {
            return listOf(argsArray.last())
        }

        commands = server.onlinePlayers.associate { Pair(it.name, InfinitePlayersTabExecutor(server, limit - 1)) }
        return super.onTabComplete(sender, command, alias, arrayOf(argsArray.last()))
    }
}