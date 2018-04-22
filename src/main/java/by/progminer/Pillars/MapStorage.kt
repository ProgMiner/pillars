package by.progminer.Pillars

import org.bukkit.configuration.file.FileConfiguration
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

open class MapStorage(private val maps: MutableMap<String, GameMap>): Map<String, GameMap> by maps {
    companion object {
        fun fromFile(file: File): MapStorage {
            if (!file.exists()) {
                throw IllegalArgumentException("File must exists")
            }

            val maps = mutableMapOf<String, GameMap>()
            YamlConfiguration.loadConfiguration(file).getValues(false).forEach { name, map ->
                if (map !is GameMap) {
                    return@forEach
                }

                maps[name] = map
            }

            return MapStorage(maps)
        }
    }

    fun addMap(name: String, map: GameMap) {
        maps[name] = map
    }

    fun saveToFile(file: File) {
        val config = YamlConfiguration.loadConfiguration(file)

        maps.forEach { name, map ->
            config[name] = map
        }

        config.save(file)
    }
}