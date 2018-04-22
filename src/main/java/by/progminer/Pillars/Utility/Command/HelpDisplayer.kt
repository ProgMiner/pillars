package by.progminer.Pillars.Utility.Command

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

open class HelpDisplayer(
        val description: String,
        val usage: String
): BaseTabExecutor(), IHelpDisplayer {

    open fun sendHelp(receiver: CommandSender) =
            receiver.sendMessage(arrayOf(
                    "Description: $description",
                    "Usage: $usage"
            ))

    override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
        sendHelp(sender)

        return true
    }
}