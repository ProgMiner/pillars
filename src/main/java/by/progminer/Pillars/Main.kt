package by.progminer.Pillars

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Main: JavaPlugin() {
    companion object {
        const val MAPS_DIR = "maps"

        const val MAIN_CMD_NAME = "pillars"
    }

    lateinit var pluginDir: File
        private set

    lateinit var configFile: FileConfiguration
        private set

    override fun onLoad() {
        pluginDir = dataFolder
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
        }

        configFile = config
        saveDefaultConfig()
    }

    override fun onEnable() {
        run {
            // Setting executor for main command
            val cmd = getCommand(MAIN_CMD_NAME)
            val executor = MainCommand(this)
            cmd.tabCompleter = executor
            cmd.executor = executor
        }

        // Register serializable classes
        ConfigurationSerialization.registerClass(GameMap::class.java)
    }
}