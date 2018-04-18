package by.progminer.Pillars.Utility

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

abstract class AbstractTabCompleter(val commands: Map <String, TabCompleter>): TabCompleter {
    companion object {
        fun cleanEmptyArgs(args: Array<String>): Array<String> {
            val ret = mutableSetOf<String>()
            args.forEach {
                if (it.isNotBlank()) {
                    ret.add(it)
                }
            }

            return ret.toTypedArray()
        }

        fun emptyTabCompleter() =
                TabCompleter { _, _, _, _ -> emptyList() }
    }

    private val cmds = commands.keys.toList()

    constructor(commands: List<String>):
            this(commands.associate <String, String, TabCompleter> { Pair(it, emptyTabCompleter()) })

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): List<String> {
        val args = cleanEmptyArgs(argsArray)

        if (args.isEmpty()) {
            return cmds
        }

        val subcmdAlias = "$alias ${args[0]}"
        val subcmdArgs = args.slice(1 until args.size).toTypedArray()

        for ((cmd, completer) in commands) {
            if (args[0] == cmd) {
                return completer.onTabComplete(sender, command, subcmdAlias, subcmdArgs)
            }

            if (cmd.startsWith(args[0])) {
                return listOf(cmd)
            }
        }

        return emptyList()
    }
}