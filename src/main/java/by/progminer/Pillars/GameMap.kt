package by.progminer.Pillars

import org.bukkit.Location
import org.bukkit.configuration.serialization.ConfigurationSerializable

data class GameMap(
        val lobby: Location,
        val start: Location,
        val pillars: Location
): ConfigurationSerializable {
    constructor(lobby: Location, start: Location):
            this(lobby, start, start)

    constructor(map: Map<String, Any>): this(
            map["lobby"] as Location,
            map["start"] as Location,
            map["pillars"] as Location
    )

    override fun serialize(): MutableMap<String, Any> {
        return mutableMapOf(
                "lobby" to lobby,
                "start" to start,
                "pillars" to pillars
        )
    }
}