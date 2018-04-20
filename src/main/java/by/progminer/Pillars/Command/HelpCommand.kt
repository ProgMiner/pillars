package by.progminer.Pillars.Command

import by.progminer.Pillars.Main
import by.progminer.Pillars.Utility.HelpDisplayer
import by.progminer.Pillars.Utility.IHelpDisplayer
import by.progminer.Pillars.Utility.NodeHelpDisplayer

class HelpCommand(private val main: Main): NodeHelpDisplayer("help", mapOf(
        "help" to HelpDisplayer("Displays help for the subcommands", "/pillars help [<subcommand>]"),
        "game" to NodeHelpDisplayer(HelpDisplayer("Manages games", "/pillars game [<action>]"), mapOf(
                "list" to HelpDisplayer("Displays the currently running games. If players are specified, looks for games with them", "/pillars game list [<players nicknames>]"),
                "start" to HelpDisplayer("Starts the game with the specified players and the card (\"--\" for a random card)", "/pillars game start <map name|\"--\"> <players nicknames>"),
                "stop" to HelpDisplayer("Stops the game with the specified player or sender", "/pillars game stop [<player nickname>]")
        )),
        "map" to NodeHelpDisplayer(HelpDisplayer("Manages the plugin map storage", "/pillars map [<action>]"), mapOf(
                "list" to HelpDisplayer("Displays all maps stored in the plugin map storage", "/pillars map list"),
                "info" to HelpDisplayer("Displays information of the specified map stored in the plugin map storage", "/pillars map info <map name>"),
                "init" to HelpDisplayer("Initializes a new map in the current world with the specified name and author, and saves it to the plugin map storage. If the author is not specified, the player nickname is used", "/pillars map init <map name> [<author>]"),
                "set" to HelpDisplayer("Sets the specified property of the specified map stored in the plugin map storage", "/pillars map set <map name> <property name> <new property value>"),
                "remove" to HelpDisplayer("Removes the specified map from the plugin map storage", "/pillars map remove <map name>")
        ))
))