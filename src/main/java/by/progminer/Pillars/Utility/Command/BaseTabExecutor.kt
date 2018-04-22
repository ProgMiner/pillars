package by.progminer.Pillars.Utility.Command

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabExecutor

open class BaseTabExecutor(executor: CommandExecutor): TabExecutor, CommandExecutor by executor {
    override fun onTabComplete(p0: CommandSender?, p1: Command?, p2: String?, p3: Array<String>) =
            emptyList <String> ()

    constructor(): this(CommandExecutor { _, _, _, _ -> true })
}