package by.progminer.Pillars.Utility

import org.bukkit.command.Command
import org.bukkit.command.CommandSender

open class NodeHelpDisplayer protected constructor(
        val children: Map <String, IHelpDisplayer>
): NodeTabExecutor(children), IHelpDisplayer {

    private var default: IHelpDisplayer? = null

    constructor(default: String, children: Map<String, IHelpDisplayer>): this(children) {
        this.default = children[default]
    }

    constructor(default: IHelpDisplayer, children: Map<String, IHelpDisplayer>): this(children) {
        this.default = default
    }

    override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
        val args = cleanEmptyArgs(argsArray)

        if (args.isEmpty()) {
            return default?.onCommand(sender, command, alias, args.slice(1 until args.size).toTypedArray()) ?: false
        }

        return super.onCommand(sender, command, alias, argsArray)
    }
}