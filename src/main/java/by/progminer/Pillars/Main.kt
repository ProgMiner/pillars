package by.progminer.Pillars

import org.bukkit.plugin.java.JavaPlugin

class Main: JavaPlugin() {
    companion object {
        const val MAIN_CMD_NAME = "pillars"
    }

    override fun onLoad() {
        val pluginDir = dataFolder
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
        }
    }

    override fun onEnable() {
        run {
            // Setting executor for main command
            val cmd = getCommand(MAIN_CMD_NAME)
            val executor = Command(this)
            cmd.tabCompleter = executor
            cmd.executor = executor
        }
    }
}