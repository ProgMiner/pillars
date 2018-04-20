package by.progminer.Pillars.Utility

import org.bukkit.command.*

abstract class NodeTabExecutor(val commands: Map <String, TabExecutor>): TabExecutor {
    companion object {
        fun cleanEmptyArgs(args: Array<String>): Array<String> {
            val ret = mutableListOf<String>()

            for (i in 0 until args.size) {
                if (args[i].isBlank()) {
                    continue
                }

                for (j in i until args.size) {
                    ret.add(args[j])
                }

                break
            }

            return ret.toTypedArray()
        }
    }

    private val cmds = commands.keys.toList()

    constructor(commands: List<String>): this(commands.associate {
        Pair(it, BaseTabExecutor(CommandExecutor { _, _, _, _ -> throw UnsupportedOperationException() }))
    })

    override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
        val args = cleanEmptyArgs(argsArray)

        if (args.isEmpty()) {
            return false
        }

        val subcmdAlias = "$alias ${args[0]}"
        val subcmdArgs = args.slice(1 until args.size).toTypedArray()

        for ((cmd, completer) in commands) {
            if (args[0] == cmd) {
                return completer.onCommand(sender, command, subcmdAlias, subcmdArgs)
            }
        }

        return false
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): List<String> {
        val args = cleanEmptyArgs(argsArray)

        if (args.isEmpty()) {
            return cmds
        }

        val subcmdAlias = "$alias ${args[0]}"
        val subcmdArgs = args.slice(1 until args.size).toTypedArray()

        val similar = mutableSetOf<String>()

        for ((cmd, completer) in commands) {
            if (args[0] == cmd && args.size > 1) {
                return completer.onTabComplete(sender, command, subcmdAlias, subcmdArgs)
            }

            if (cmd.startsWith(args[0])) {
                similar.add(cmd)
            }
        }

        return similar.toList()
    }
}