package by.progminer.Pillars

import org.bukkit.*
import org.bukkit.block.BlockState
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.inventory.ItemStack
import org.bukkit.entity.Player
import org.bukkit.entity.Firework
import org.bukkit.event.*
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.*
import org.bukkit.scheduler.BukkitRunnable
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@Suppress("unused", "MemberVisibilityCanBePrivate")
class Game(
        private val main: Main,
        val options: Options,
        val map: GameMap
): BukkitRunnable(), Listener {
    enum class State(
            val tick: Game.() -> Unit = {},
            val start: Game.() -> Unit = {},
            val rejoin: Game.(Player) -> Unit = {}
    ) {
        NOT_STARTED,

        HIDING_START({
            switchStateIfTimerOut(State.HIDING)
        }, {
            _players.forEach { player ->

                // Teleporting players to start point
                player.teleport(map.start)

                // TODO Make gamemode configurable
                player.gameMode = GameMode.SURVIVAL
                player.canPickupItems = false
                player.isCollidable = false
            }

            // Setting timer for waiting
            timer = options.startDuration
        }, {
            it.gameMode = GameMode.SURVIVAL
            it.canPickupItems = false
            it.isCollidable = false
        }),
        HIDING({
            run {
                for (player in _players) {
                    if (placedBlocks[player.name]?.size ?: 0 < options.blocksAmount) {
                        return@run
                    }
                }

                state = State.SEARCH_START
            }

            switchStateIfTimerOut(State.HIDING_END)
        }, {
            // TODO Giving an effects

            _players.forEach { player ->

                // Hiding players
                _players.forEach { hidingPlayer ->
                    hidingPlayer.hidePlayer(main, player)
                }

                // Giving blocks
                player.inventory.addItem(ItemStack(blocks[player.name]!!, options.blocksAmount))
            }

            // Setting timer for game
            timer = options.gameDuration
        }),
        HIDING_END({
            run {
                players.forEach {
                    if (placedBlocks[it.name]?.size ?: 0 < options.blocksAmount) {
                        return@run
                    }
                }

                state = State.SEARCH_START
            }
        }, {
            _players.forEach {
                it.sendMessage("${ChatColor.RED}Time is over!")
            }
        }),

        SEARCH_START({
            switchStateIfTimerOut(State.SEARCH)
        }, {
            _players.forEach { player ->

                // Teleporting players to start point
                player.teleport(map.start)
            }

            // Setting timer for waiting
            timer = options.startDuration
        }),
        SEARCH({
            switchStateIfTimerOut(State.PILLARS_START)
        }, {

            // Setting timer for game
            timer = options.gameDuration
        }),

        PILLARS_START({
            switchStateIfTimerOut(State.PILLARS)
        }, {
            _players.forEach { player ->

                // Teleporting players to pillars point
                player.teleport(map.pillars)

                // Showing players
                _players.forEach { showingPlayer ->
                    showingPlayer.showPlayer(main, player)
                }
            }

            topPlayers = run {
                val unsortedSets = mutableMapOf<Int, MutableSet<String>>()
                _collectedBlocks.forEach { player, count ->
                    if (count !in unsortedSets) {
                        unsortedSets[count] = mutableSetOf(player)
                    } else {
                        unsortedSets[count]!!.add(player)
                    }
                }

                val sets = unsortedSets.toSortedMap(Comparator { a, b -> a - b })

                val ret = mutableListOf<String>()
                sets.forEach { _, set ->
                    set.forEach {
                        ret.add(it)
                    }
                }

                return@run ret.toList()
            }

            // Setting timer for waiting
            timer = options.startDuration
        }),
        PILLARS({
            run {
                players.forEach {
                    if (placedBlocks[it.name]?.size ?: 0 != _collectedBlocks[it.name] ?: 0) {
                        return@run
                    }
                }

                state = State.PILLARS_END
            }
        }, {
            placedBlocks.clear()

            _players.forEach {
                it.sendMessage("${ChatColor.RED}Time is over!")
            }
        }),
        PILLARS_END({
            if (_players.isEmpty()) {
                state = State.ENDED
            }

            switchStateIfTimerOut(State.ENDED)
        }, {
            val chat = mutableListOf("Game finished! Top players is:")

            topPlayers.slice(0 until minOf(3, topPlayers.size)).forEachIndexed { i, it ->
                val player = main.server.getPlayerExact(it)!!

                chat.add("${i + 1}. ${ChatColor.UNDERLINE}${player.playerListName}${ChatColor.RESET} - ${_collectedBlocks[player.name]} blocks")

                val firework = player.world.spawn(player.location.add(.0, 2.0, .0), Firework::class.java)
                firework.fireworkMeta.addEffect(
                        FireworkEffect.builder()
                                .with(FireworkEffect.Type.STAR)
                                .withColor(Color.WHITE)
                                .build()
                )
                firework.fireworkMeta.power = 9 - i * 3
            }

            val chatArray = chat.toTypedArray()
            _players.forEach {
                it.sendMessage(chatArray)
            }

            // Setting timer for waiting
            timer = options.endDuration
        }),

        ENDED(start = {
            _players.forEach { player ->

                // Teleporting players to lobby
                player.teleport(map.lobby)

                player.canPickupItems = true
                player.isCollidable = true
            }

            blocksLog.forEach { location, block ->
                location.block.type = block.type
                location.block.data = block.data.data
            }

            bossBar.isVisible = false

            cancel()
            HandlerList.unregisterAll(this)
        })
    }

    data class Options(
            val gameDuration: Long = 300000,
            val startDuration: Long = 5000,
            val endDuration: Long = 300000,
            val blocksAmount: Int = 12
    )

    // Public variables

    val collectedBlocks: Map<String, Int>
        get() = _collectedBlocks.toMap()

    lateinit var topPlayers: List<String>

    val players: Set<Player>
        get() = _players.toSet()

    val blocks: Map<String, Material>
        get() = _blocks.toMap()

    val offlinePlayers: Set<String>
        get() = _offlinePlayers.toSet()

    var timer = 0L
        get() = field - System.currentTimeMillis()
        private set(value) {
            field = System.currentTimeMillis() + value
            timerFull = value
        }

    var state = State.NOT_STARTED
        private set(value) {
            if (value == State.NOT_STARTED) {
                return
            }

            value.start(this)

            field = value
        }

    // Private variables

    private var timerFull = 0L

    private var bossBar = main.server.createBossBar("Pillars", BarColor.WHITE, BarStyle.SOLID)

    private val placedBlocks = mutableMapOf<String, MutableSet<Location>>()

    private val blocksLog = mutableMapOf<Location, BlockState>()

    private val _collectedBlocks = mutableMapOf<String, Int>()

    private val _blocks = mutableMapOf<String, Material>()

    private val _offlinePlayers = mutableSetOf<String>()

    private val _players = mutableSetOf<Player>()

    // Public methods

    init {
        bossBar.isVisible = false
    }

    fun start() {
        _players.forEach {
            if (!it.isOnline) {
                _players.remove(it)
            }
        }

        if (_players.size < 2) {
            throw UnsupportedOperationException("The number of players is less than 2")
        }

        main.server.pluginManager.registerEvents(this, main)
        state = State.HIDING_START
        runTaskTimer(main, 0, 1)
    }

    fun skipCurrentState() {
        state = State.values()[state.ordinal + 1]
    }

    fun stop() {
        while (state != State.ENDED) {
            skipCurrentState()
        }
    }

    fun joinPlayer(player: Player, block: Material) {
        if (!player.isOnline) {
            throw IllegalArgumentException("Player is not online")
        }

        main.gameContainer.forEach {
            if (it._players.contains(player)) {
                throw IllegalArgumentException("Player already plays another game")
            }
        }

        if (!block.isBlock || !block.isSolid || block.hasGravity()) {
            throw IllegalArgumentException("Block must be solid non-falling block")
        }

        if (block in _blocks.values) {
            throw IllegalArgumentException("This block is already busy")
        }

        _players.add(player)
        bossBar.addPlayer(player)

        _blocks[player.name] = block
        _collectedBlocks[player.name] = 0
    }

    fun detachPlayer(player: Player) {
        _players.remove(player)
        bossBar.removePlayer(player)
        _offlinePlayers.remove(player.name)

        _blocks.remove(player.name)
        _collectedBlocks.remove(player.name)
    }

    fun rejoinPlayer(player: Player, force: Boolean = false) {
        if (player.name !in offlinePlayers && !force) {
            throw IllegalArgumentException("The player is not offline")
        }

        _offlinePlayers.remove(player.name)
        _players.add(player)

        bossBar.addPlayer(player)

        state.rejoin(this, player)
    }

    // Event handlers

    @EventHandler(priority = EventPriority.MONITOR)
    fun on(event: PlayerQuitEvent) {
        if (event.player !in _players) {
            return
        }

        _offlinePlayers.add(event.player.name)
        _players.remove(event.player)

        if (_players.size < 2) {
            _players.forEach {
                it.sendMessage("${ChatColor.RED}Too few players")
            }

            stop()
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: PlayerMoveEvent) {
        if (event.player !in players) {
            return
        }

        when (state) {
            State.HIDING_START, State.HIDING_END, State.SEARCH_START, State.PILLARS_START -> {}

            else ->
                return
        }

        if (
                event.from.blockX != event.to.blockX ||
                event.from.blockZ != event.to.blockZ
        ) {
            event.player.teleport(Location(
                    event.from.world,
                    event.from.x,
                    event.to.y,
                    event.from.z,
                    event.to.yaw,
                    event.to.pitch
            ))
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: BlockDamageEvent) {
        if (event.player !in players) {
            return
        }

        when (state) {
            State.HIDING, State.PILLARS ->
                if (event.block.location in placedBlocks[event.player.name] ?: emptySet<Location>()) {
                    placedBlocks[event.player.name]!!.remove(event.block.location)
                    event.player.inventory.addItem(ItemStack(event.block.type))
                    event.block.type = Material.AIR
                } else {
                    event.isCancelled = true
                }

            State.SEARCH -> {
                event.isCancelled = true

                if (event.block.type == _blocks[event.player.name]) {
                    return
                }

                for ((_, blocks) in placedBlocks) {
                    if (event.block.location in blocks) {
                        _collectedBlocks[event.player.name] = _collectedBlocks[event.player.name]!! + 1

                        event.player.inventory.addItem(ItemStack(event.block.type))
                        event.block.type = Material.AIR
                        event.isCancelled = false
                        break
                    }
                }
            }

            else ->
                onPlayerFreeze(event, event.player)
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: BlockPlaceEvent) {
        if (event.player !in players) {
            return
        }

        when (state) {
            State.HIDING, State.HIDING_END, State.SEARCH, State.PILLARS ->
                if (
                        state != State.PILLARS && event.block.type == _blocks[event.player.name] ||
                        state == State.PILLARS && event.block.type != _blocks[event.player.name]
                ) {
                    if (state == State.PILLARS && (
                            event.block.location.blockX != event.player.location.blockX ||
                            event.block.location.blockY != event.player.location.blockY - 1 ||
                            event.block.location.blockZ != event.player.location.blockZ
                    )) {
                        event.isCancelled = true
                        return
                    }

                    if (event.player.name !in placedBlocks.keys) {
                        placedBlocks[event.player.name] = mutableSetOf()
                    }

                    placedBlocks[event.player.name]!!.add(event.block.location)
                } else {
                    event.isCancelled = true
                }

            else ->
                onPlayerFreeze(event, event.player)
        }

        if (!event.isCancelled) {
            blocksLog[event.block.location] = event.blockReplacedState
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: BlockBreakEvent) {
        if (event.player !in _players) {
            return
        }

        blocksLog[event.block.location] = event.block.state

        event.isDropItems = false
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: PlayerDropItemEvent) {
        if (event.player !in _players) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: FoodLevelChangeEvent) {
        if (event.entity !in _players) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: InventoryOpenEvent) {
        if (event.player !in _players) {
            return
        }

        if (event.inventory.type != InventoryType.PLAYER) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: PlayerGameModeChangeEvent) {
        if (event.player !in _players) {
            return
        }

        // TODO Make gamemode configurable
        if (event.newGameMode != GameMode.SURVIVAL) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: PlayerInteractEvent) {
        if (event.player !in _players) {
            return
        }

        if (
                event.action == Action.LEFT_CLICK_BLOCK ||
                event.action == Action.RIGHT_CLICK_BLOCK
        ) {
            return
        }

        event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: EntityDamageEvent) {
        if (event.entity !in _players) {
            return
        }

        event.isCancelled = true
    }

    // Overrides

    override fun run() {
        state.tick(this)

        val timer = timer
        if (timer >= 0) {
            val localTime = LocalTime.ofNanoOfDay(timer * 1000000)

            // TODO Make format configurable
            val formatter = DateTimeFormatter.ofPattern("mm:ss/SSS")

            bossBar.progress = 1 - timer.toDouble() / timerFull
            bossBar.title = "${state.name} since ${localTime.format(formatter)}"

            bossBar.color = when (bossBar.progress) {
                in 0.0..0.35 ->
                    BarColor.GREEN

                in 0.35..0.8 ->
                    BarColor.YELLOW

                in 0.8..1.0 ->
                    BarColor.RED

                else ->
                    BarColor.WHITE
            }

            bossBar.isVisible = true
        } else {
            bossBar.isVisible = false
        }
    }

    // Private methods

    private fun <T> onPlayerFreeze(event: T) where
            T: Cancellable,
            T: PlayerEvent
    = onPlayerFreeze(event, event.player)


    private fun onPlayerFreeze(event: Cancellable, player: Player) {
        if (player !in _players) {
            return
        }

        when (state) {
            State.HIDING_START, State.HIDING_END, State.PILLARS_START, State.PILLARS_END, State.SEARCH_START ->
                event.isCancelled = true

            else -> {}
        }
    }

    private fun switchStateIfTimerOut(newState: State) {
        if (timer <= 0) {
            state = newState
        }
    }
}