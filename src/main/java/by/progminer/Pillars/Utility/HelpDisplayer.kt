package by.progminer.Pillars.Utility

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

open class HelpDisplayer(
        val description: String,
        val usage: String
): BaseTabExecutor(), IHelpDisplayer {

    open fun sendHelp(receiver: CommandSender) =
            receiver.sendMessage("Description: $description\nUsage: $usage")

    override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
        sendHelp(sender)

        return true
    }
}