package by.progminer.Pillars

import by.progminer.Pillars.Command.PillarsCommand
import by.progminer.Pillars.Utility.MapContainer
import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.serialization.ConfigurationSerialization
import org.bukkit.plugin.java.JavaPlugin
import java.io.File

class Main: JavaPlugin() {
    companion object {
        const val MAPS_FILE = "maps.yml"
    }

    lateinit var pluginDir: File
        private set

    lateinit var configFile: FileConfiguration
        private set

    lateinit var mapContainer: MapContainer
        private set

    override fun onLoad() {
        pluginDir = dataFolder
        if (!pluginDir.exists()) {
            pluginDir.mkdirs()
        }

        configFile = config
        saveDefaultConfig()

        val mapsFile = File(pluginDir, MAPS_FILE)
        if (!mapsFile.exists()) {
            saveResource(MAPS_FILE, false)
        }

        mapContainer = MapContainer.fromFile(mapsFile)
    }

    override fun onEnable() {
        run {
            // Setting executor for game command
            val cmd = getCommand("pillars")
            val executor = PillarsCommand(this)
            cmd.tabCompleter = executor
            cmd.executor = executor
        }

        // Register serializable classes
        ConfigurationSerialization.registerClass(GameMap::class.java)
    }
}