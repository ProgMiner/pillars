package by.progminer.Pillars

import org.bukkit.Location
import org.bukkit.configuration.serialization.ConfigurationSerializable

// TODO Add more points
// TODO Make builder

data class GameMap(
        val lobby: Location,
        val start: Location,
        val pillars: Location,
        val author: String
): ConfigurationSerializable {
    constructor(lobby: Location, start: Location, author: String):
            this(lobby, start, start, author)

    constructor(map: Map<String, Any>): this(
            map["lobby"] as Location,
            map["start"] as Location,
            map["pillars"] as Location,
            map["author"] as String
    )

    override fun serialize(): MutableMap<String, Any> {
        return mutableMapOf(
                "lobby" to lobby,
                "start" to start,
                "pillars" to pillars,
                "author" to author
        )
    }
}