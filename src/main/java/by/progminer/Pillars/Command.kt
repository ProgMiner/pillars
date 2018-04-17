package by.progminer.Pillars

import org.bukkit.command.Command
import org.bukkit.command.TabExecutor
import org.bukkit.command.CommandSender
import org.bukkit.plugin.Plugin

class Command(private val plugin: Plugin): TabExecutor {

    override fun onCommand(sender: CommandSender?, command: Command?, label: String?, args: Array <out String> ?): Boolean {
        TODO()
    }

    override fun onTabComplete(sender: CommandSender?, command: Command?, alias: String?, args: Array <out String> ?): MutableList <String> {
        TODO()
    }
}