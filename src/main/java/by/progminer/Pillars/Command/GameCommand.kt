package by.progminer.Pillars.Command

import by.progminer.Pillars.Game
import by.progminer.Pillars.Main
import by.progminer.Pillars.Utility.*
import by.progminer.Pillars.Utility.Command.InfinitePlayersTabExecutor
import by.progminer.Pillars.Utility.Command.MapsTabExecutor
import by.progminer.Pillars.Utility.Command.NodeTabExecutor
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

class GameCommand(private val main: Main): NodeTabExecutor(mapOf(
        "list" to object: InfinitePlayersTabExecutor(main.server) {

            override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
                val args = cleanEmptyArgs(argsArray)
                val ret = mutableSetOf<String>()

                val players = mutableSetOf<Player>()
                val badPlayers = mutableSetOf<String>()
                args.forEach {
                    val player = main.server.getPlayerExact(it)

                    if (player != null) {
                        players.add(player)
                    } else {
                        badPlayers.add(it)
                    }
                }

                if (badPlayers.isNotEmpty()) {
                    sender.sendMessage(badPlayers.joinToString(prefix = "${ChatColor.RED}Players not found: "))
                }

                if (players.isEmpty()) {
                    players.addAll(main.gameManager.getPlayers())
                }

                main.gameManager.forEach {
                    var hasPlayer = false
                    for (player in players) {
                        if (it.blocks.containsKey(player)) {
                            players.remove(player)
                            hasPlayer = true
                            break
                        }
                    }

                    if (!hasPlayer) {
                        return@forEach
                    }

                    val s = StringBuilder(it.state.name)
                            .append(" at ")
                            .append(it.map.start.toHumanReadable())
                            .append(" still ")
                            .append(it.timer - System.currentTimeMillis())
                            .append("ms: ")
                            .append(it.blocks.keys.joinToString())

                    ret.add(s.toString())
                }

                sender.sendMessage(ret.toTypedArray())
                return true
            }
        },
        "start" to object: MapsTabExecutor(main.mapStorage, InfinitePlayersTabExecutor(main.server)) {

            override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
                val args = cleanEmptyArgs(argsArray)

                if (args.size < 2) {
                    return false
                }

                var mapName = args[0]
                if (mapName == "--") {
                    if (main.mapStorage.isEmpty()) {
                        sender.sendMessage("${ChatColor.RED}No one map exists")
                        return true
                    }

                    mapName = main.mapStorage.keys.toList()[Random().nextInt(main.mapStorage.size)]
                } else if (!main.mapStorage.containsKey(mapName)) {
                    sender.sendMessage("${ChatColor.RED}Map \"$mapName\" is not exists")
                    return true
                }

                val timer: Long
                try {
                    timer = args[1].toLong() * 60000L
                } catch (ex: NumberFormatException) {
                    sender.sendMessage("${ChatColor.RED}\"${args[1]}\" is not a number")
                    return true
                }

                val players = mutableSetOf<Player>()
                val badPlayers = mutableSetOf<String>()
                args.slice(2 until args.size).forEach {
                    val player = main.server.getPlayerExact(it)

                    if (player != null) {
                        players.add(player)
                        player.sendMessage("Write name of your block now")
                    } else {
                        badPlayers.add(it)
                    }
                }

                if (badPlayers.isNotEmpty()) {
                    sender.sendMessage(badPlayers.joinToString(prefix = "${ChatColor.RED}Players not found: "))
                }

                if (players.size < 2) {
                    sender.sendMessage("${ChatColor.RED}Too few players")
                }

                val blocks = mutableMapOf<Player, Material>()
                main.server.pluginManager.registerEvents(object: Listener {
                    @EventHandler(ignoreCancelled = false)
                    fun onAsyncPlayerChat(event: AsyncPlayerChatEvent) {
                        if (event.player !in players) {
                            return
                        }

                        val block = Material.matchMaterial(event.message)
                        if (block == null) {
                            event.player.sendMessage("${ChatColor.RED}Write block ID or name, please")
                            return
                        }

                        if (!block.isBlock) {
                            event.player.sendMessage("${ChatColor.RED}\"$block\" is not a block (try to add \"block\")")
                            return
                        }

                        if (blocks.containsValue(block)) {
                            event.player.sendMessage("${ChatColor.RED}This block is already busy")
                            return
                        }

                        blocks[event.player] = block
                        event.player.sendMessage("${ChatColor.GREEN}Your block is $block")

                        if (blocks.size >= players.size) {
                            players.forEach {
                                it.sendMessage("Game starting")
                            }
                        }
                    }

                    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
                    fun onPlayerQuit(event: PlayerQuitEvent) {
                        if (event.player in players) {
                            players.remove(event.player)
                        }
                    }
                }, main)

                object: BukkitRunnable() {
                    override fun run() {
                        if (blocks.size < players.size) {
                            return
                        }

                        main.gameManager.startGame(Game(main, Game.Options(timer), blocks, main.mapStorage[mapName]!!))
                        cancel()
                    }
                }.runTaskTimer(main, 0, 1)

                return true
            }
        }
))