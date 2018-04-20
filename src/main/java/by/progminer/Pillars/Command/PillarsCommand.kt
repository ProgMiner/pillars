package by.progminer.Pillars.Command

import by.progminer.Pillars.Main
import by.progminer.Pillars.Utility.BaseTabExecutor
import by.progminer.Pillars.Utility.NodeTabExecutor

class PillarsCommand(private val main: Main): NodeTabExecutor(mapOf(
        "help" to HelpCommand(main),
        "game" to GameCommand(main),
        "map" to BaseTabExecutor()
))