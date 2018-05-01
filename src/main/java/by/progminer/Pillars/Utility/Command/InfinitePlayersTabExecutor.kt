package by.progminer.Pillars.Utility.Command

import org.bukkit.Server
import org.bukkit.command.Command
import org.bukkit.command.CommandSender

open class InfinitePlayersTabExecutor(protected val server: Server, protected val limit: Long = 12): NodeTabExecutor(listOf()) {

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): List<String> {
        val args = cleanEmptyArgs(argsArray)

        if (limit == 0L) {
            return if (args.isEmpty()) {
                emptyList()
            } else {
                listOf(args.first())
            }
        }

        commands = if (args.isEmpty()) {
            server.onlinePlayers.associate { it.name to InfinitePlayersTabExecutor(server, limit - 1) }
        } else {
            mapOf(args.first() to InfinitePlayersTabExecutor(server, limit - 1))
        }

        return super.onTabComplete(sender, command, alias, argsArray)
    }
}