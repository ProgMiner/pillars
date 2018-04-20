package by.progminer.Pillars.Utility

import by.progminer.Pillars.GameMap
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

class MapContainer(private val maps: Map<String, GameMap>): Map <String, GameMap> by maps {
    companion object {
        fun fromFile(file: File): MapContainer {
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

            return MapContainer(maps)
        }
    }
}