package by.progminer.Pillars.Utility.Command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

open class CustomTabExecutor(
        protected val child: TabExecutor = BaseTabExecutor(CommandExecutor { _, _, _, _ -> throw UnsupportedOperationException() })
): NodeTabExecutor(listOf(), child) {

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): List<String> {
        val args = cleanEmptyArgs(argsArray)

        if (args.isEmpty()) {
            return emptyList()
        }

        commands = mapOf(args.first() to child)
        return super.onTabComplete(sender, command, alias, argsArray)
    }
}