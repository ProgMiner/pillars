package by.progminer.Pillars.Utility.Command

import org.bukkit.command.CommandExecutor
import org.bukkit.command.TabExecutor

open class CustomTabExecutor(
        child: TabExecutor = BaseTabExecutor(CommandExecutor { _, _, _, _ -> throw UnsupportedOperationException() })
): NodeTabExecutor(listOf(), child)