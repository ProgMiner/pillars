package by.progminer.Pillars

import org.bukkit.Location

class Map(
        var lobby: Location,
        var start: Location,
        var pillars: Location
) {

    constructor(lobby: Location, start: Location):
            this(lobby, start, start)

    constructor(lobby: Location):
            this(lobby, lobby)
}