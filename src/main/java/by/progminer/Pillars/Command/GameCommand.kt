package by.progminer.Pillars.Command

import by.progminer.Pillars.Game
import by.progminer.Pillars.Main
import by.progminer.Pillars.Utility.Command.*
import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import java.util.*

@Suppress("unused")
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

                val games = if (players.isEmpty()) {
                    main.gameContainer
                } else {
                    main.gameContainer.filter {
                        for (player in players) {
                            if (player in it.players) {
                                players.remove(player)
                                return@filter true
                            }
                        }

                        return@filter false
                    }.toSet()
                }

                games.forEachIndexed { i, it ->
                    ret.add(it.blocks.keys.joinToString(prefix = "${i + 1}. Game at state ${it.state.name} still ${it.timer} ms with "))
                }

                sender.sendMessage(ret.toTypedArray())
                return true
            }
        },
        "start" to object: MapsTabExecutor(main.mapStorage, CustomTabExecutor(InfinitePlayersTabExecutor(main.server))) {

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
                        main.gameContainer.forEach {
                            if (player in it.players) {
                                sender.sendMessage("${ChatColor.RED}Player ${player.name} already plays")
                                return true
                            }
                        }

                        players.add(player)
                    } else {
                        badPlayers.add(it)
                    }
                }

                if (badPlayers.isNotEmpty()) {
                    sender.sendMessage(badPlayers.joinToString(prefix = "${ChatColor.RED}Players not found: "))
                }

                if (players.size < 2) {
                    sender.sendMessage("${ChatColor.RED}Too few players")
                    return true
                }

                players.forEach { player ->
                    player.sendMessage("Select your block now")

                    // TODO Make inventory configurable
                    val inventory = main.server.createInventory(player, InventoryType.PLAYER, "Select your block now")
                    inventory.contents = arrayOf(
                            ItemStack(Material.WHITE_GLAZED_TERRACOTTA),
                            ItemStack(Material.ORANGE_GLAZED_TERRACOTTA),
                            ItemStack(Material.MAGENTA_GLAZED_TERRACOTTA),
                            ItemStack(Material.LIGHT_BLUE_GLAZED_TERRACOTTA),
                            ItemStack(Material.YELLOW_GLAZED_TERRACOTTA),
                            ItemStack(Material.LIME_GLAZED_TERRACOTTA),
                            ItemStack(Material.PINK_GLAZED_TERRACOTTA),
                            ItemStack(Material.GRAY_GLAZED_TERRACOTTA),
                            ItemStack(Material.SILVER_GLAZED_TERRACOTTA),
                            ItemStack(Material.CYAN_GLAZED_TERRACOTTA),
                            ItemStack(Material.PURPLE_GLAZED_TERRACOTTA),
                            ItemStack(Material.BLUE_GLAZED_TERRACOTTA),
                            ItemStack(Material.BROWN_GLAZED_TERRACOTTA),
                            ItemStack(Material.GREEN_GLAZED_TERRACOTTA),
                            ItemStack(Material.RED_GLAZED_TERRACOTTA),
                            ItemStack(Material.BLACK_GLAZED_TERRACOTTA)
                    )

                    player.openInventory(inventory)
                }

                val game = Game(main, Game.Options(timer), main.mapStorage[mapName]!!)
                val listener = object: Listener {

                    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
                    fun onInventoryClick(event: InventoryClickEvent) {
                        if (event.whoClicked !in players) {
                            return
                        }

                        val block = event.currentItem.data

                        try {
                            game.joinPlayer(event.whoClicked as Player, block)
                            event.whoClicked.sendMessage("${ChatColor.GREEN}Your block is $block")
                            event.whoClicked.closeInventory()
                        } catch (e: IllegalArgumentException) {
                            event.whoClicked.sendMessage("${ChatColor.RED}${e.message}")
                        }

                        event.isCancelled = true
                    }

                    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
                    fun onInventoryClose(event: InventoryCloseEvent) {
                        if (event.player in players && event.player !in game.players) {
                            event.player.sendMessage("${ChatColor.RED}You have been leaved the game")
                            players.remove(event.player)
                        }
                    }

                    @EventHandler(priority = EventPriority.MONITOR)
                    fun onPlayerQuit(event: PlayerQuitEvent) {
                        if (event.player in players) {
                            players.remove(event.player)
                        }
                    }
                }

                main.server.pluginManager.registerEvents(listener, main)

                object: BukkitRunnable() {
                    override fun run() {
                        if (game.players.size < players.size) {
                            return
                        }

                        try {
                            game.start()
                            main.gameContainer.add(game)

                            cancel()
                            HandlerList.unregisterAll(listener)
                        } catch (e: UnsupportedOperationException) {
                            players.forEach {
                                it.sendMessage("${ChatColor.RED}${e.message}")
                            }

                            cancel()
                            HandlerList.unregisterAll(listener)
                            return
                        }

                        players.forEach {
                            it.inventory.contents = emptyArray()
                            it.sendMessage("Game starting")
                        }
                    }
                }.runTaskTimer(main, 0, 1)

                return true
            }
        },
        "stuck" to object: PlayersTabExecutor(main.server) {

            override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
                val args = cleanEmptyArgs(argsArray)

                try {
                    val (player, game) = parsePlayerArgument(main, sender, args)

                    player.teleport(game.map.start)

                    sender.sendMessage("Teleported to start")
                } catch (e: IllegalArgumentException) {
                    sender.sendMessage("${ChatColor.RED}${e.message}")
                }

                return true
            }
        },
        "skip" to object: PlayersTabExecutor(main.server) {

            override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
                val args = cleanEmptyArgs(argsArray)

                try {
                    val (_, game) = parsePlayerArgument(main, sender, args)

                    game.skipCurrentState()

                    sender.sendMessage("Game skipped to state ${game.state}")
                } catch (e: IllegalArgumentException) {
                    sender.sendMessage("${ChatColor.RED}${e.message}")
                }

                return true
            }
        },
        "stop" to object: PlayersTabExecutor(main.server) {

            override fun onCommand(sender: CommandSender, command: Command, alias: String, argsArray: Array<String>): Boolean {
                val args = cleanEmptyArgs(argsArray)

                try {
                    val (_, game) = parsePlayerArgument(main, sender, args)

                    game.stop()

                    sender.sendMessage("Game stopped")
                } catch (e: IllegalArgumentException) {
                    sender.sendMessage("${ChatColor.RED}${e.message}")
                }

                return true
            }
        }
)) {
    companion object {
        private fun parsePlayerArgument(main: Main, sender: CommandSender, args: Array<String>): Pair<Player, Game> {
            val player = if (args.isEmpty()) run {
                if (sender !is Player) {
                    throw IllegalArgumentException("Sender is not player")
                }

                return@run sender as Player
            } else {
                main.server.getPlayerExact(args[0]) ?: throw IllegalArgumentException("Player not found")
            }

            val game = run {
                for (game in main.gameContainer) {
                    if (player in game.players) {
                        return@run game
                    }
                }

                throw IllegalArgumentException("Game with the player ${player.name} not found")
            }

            return player to game
        }
    }
}