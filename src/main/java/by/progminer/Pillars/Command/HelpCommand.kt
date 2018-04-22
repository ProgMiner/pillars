package by.progminer.Pillars.Command

import by.progminer.Pillars.Main
import by.progminer.Pillars.Utility.Command.HelpDisplayer
import by.progminer.Pillars.Utility.Command.NodeHelpDisplayer

class HelpCommand(private val main: Main): NodeHelpDisplayer("help", mapOf(
        "help" to HelpDisplayer("Displays help for the subcommands", "/pillars help [<subcommand>]"),
        "game" to NodeHelpDisplayer(HelpDisplayer("Manages games", "/pillars game [<action>]"), mapOf(
                "list" to HelpDisplayer("Displays the currently running games. If players are specified, looks for games with them", "/pillars game list [<players>]"),
                "start" to HelpDisplayer("Starts the game with the specified players and the card (\"--\" for a random card)", "/pillars game start <map name|\"--\"> <timer> <players>"),
                "stop" to HelpDisplayer("Stops the game with the specified player or sender", "/pillars game stop [<player>]")
        )),
        "map" to NodeHelpDisplayer(HelpDisplayer("Manages the plugin map storage", "/pillars map [<action>]"), mapOf(
                "list" to HelpDisplayer("Displays all maps stored in the plugin map storage", "/pillars map list"),
                "info" to HelpDisplayer("Displays information of the specified map stored in the plugin map storage", "/pillars map info <map name>"),
                "init" to HelpDisplayer("Initializes a new map at player location with the specified name and author, and saves it to the plugin map storage. If the author is not specified, the player nickname is used", "/pillars map init <map name> [<author>]")
        )),
        "reload" to HelpDisplayer("Reloads plugin", "/pillars reload")
))