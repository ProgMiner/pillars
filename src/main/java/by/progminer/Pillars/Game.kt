package by.progminer.Pillars

import org.bukkit.*
import org.bukkit.boss.BarColor
import org.bukkit.boss.BarStyle
import org.bukkit.entity.Player
import org.bukkit.entity.Firework
import org.bukkit.event.*
import org.bukkit.event.block.Action
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.block.BlockDamageEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.inventory.InventoryType
import org.bukkit.event.player.*
import org.bukkit.material.MaterialData
import org.bukkit.scheduler.BukkitRunnable
import java.time.LocalTime
import java.time.format.DateTimeFormatter
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

                player.gameMode = GameMode.SURVIVAL
                player.canPickupItems = false
                player.isCollidable = false
            }

            // Showing the bossbar
            bossBar.isVisible = true

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
                player.inventory.addItem(blocks[player.name]!!.toItemStack(options.blocksAmount))
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

                // TODO Taking all effects from player

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

                val sets = unsortedSets.toSortedMap(Comparator { a, b -> b - a })

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
                location.block.type = block.itemType
                location.block.state.data = block
                location.block.state.update(true, true)
            }

            cancel()
            HandlerList.unregisterAll(this)

            bossBar.isVisible = false
        })
    }

    data class Options(
            val gameDuration: Long = 300000, // duration of main game states,   default - 5 min
            val startDuration: Long = 5000,  // delay between main game states, default - 5 sec
            val endDuration: Long = 60000,   // duration of game ending,        default - 1 min
            val blocksAmount: Int = 12       // amount of blocks,               default - 12 blocks
    )

    // Public variables

    val collectedBlocks: Map<String, Int>
        get() = _collectedBlocks.toMap()

    lateinit var topPlayers: List<String>

    val players: Set<Player>
        get() = _players.toSet()

    val blocks: Map<String, MaterialData>
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

    private val blocksLog = mutableMapOf<Location, MaterialData>()

    private val _collectedBlocks = mutableMapOf<String, Int>()

    private val _blocks = mutableMapOf<String, MaterialData>()

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

    fun joinPlayer(player: Player, block: MaterialData) {
        if (!player.isOnline) {
            throw IllegalArgumentException("Player is not online")
        }

        main.gameContainer.forEach {
            if (it._players.contains(player)) {
                throw IllegalArgumentException("Player already plays another game")
            }
        }

        if (!block.itemType.isBlock || !block.itemType.isSolid || block.itemType.hasGravity()) {
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

        if (state !in arrayOf(State.HIDING_START, State.HIDING_END, State.SEARCH_START, State.PILLARS_START)) {
            return
        }

        if (state == State.HIDING && placedBlocks[event.player.name]?.size ?: 0 < options.blocksAmount) {
            return
        }

        if (
                event.from.blockX != event.to.blockX ||
                event.from.blockZ != event.to.blockZ
        ) {
            event.isCancelled = true
        }
    }

    // TODO Make blocks breaking like naturally

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: BlockDamageEvent) {
        if (event.player !in players) {
            return
        }

        when (state) {
            State.HIDING ->
                if (event.block.location in placedBlocks[event.player.name] ?: emptySet<Location>()) {
                    placedBlocks[event.player.name]!!.remove(event.block.location)
                } else {
                    event.isCancelled = true
                }

            State.SEARCH -> {
                event.isCancelled = true

                for ((player, blocks) in placedBlocks) {
                    if (player == event.player.name) {
                        continue
                    }

                    if (event.block.location in blocks) {
                        _collectedBlocks[event.player.name] = _collectedBlocks[event.player.name]!! + 1

                        event.isCancelled = false
                        break
                    }
                }
            }

            State.PILLARS ->
                event.isCancelled = true

            else ->
                onPlayerFreeze(event, event.player)
        }

        if (!event.isCancelled) {
            event.player.inventory.addItem(event.block.state.data.toItemStack(1))
            event.block.type = Material.AIR

            blocksLog[event.block.location] = event.block.state.data
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: BlockPlaceEvent) {
        if (event.player !in players) {
            return
        }

        when (state) {
            State.HIDING, State.HIDING_END, State.SEARCH -> {
                if (event.block.type != _blocks[event.player.name]?.itemType) {
                    event.isCancelled = true
                    return
                }

                if ((state == State.HIDING) && (placedBlocks[event.player.name]?.size ?: 0 >= options.blocksAmount)) {
                    event.isCancelled = true
                    return
                }
            }

            State.PILLARS ->
                if (event.block.type != _blocks[event.player.name]?.itemType) {
                    // TODO Fix pillars building mechanism

                    // Blocks in pillars can be placed only under the player
                    if (
                            event.block.x != event.player.location.blockX ||
                            event.block.y >= event.player.location.blockY ||
                            event.block.z != event.player.location.blockZ
                    ) {
                        event.isCancelled = true
                        return
                    }

                    // Blocks in pillars can be placed only on previous block of this pillar
                    val prevBlock = placedBlocks[event.player.name]?.firstOrNull() ?: event.player.location.add(.0, -1.0, .0)
                    if (
                            event.block.x != prevBlock.blockX ||
                            event.block.z != prevBlock.blockZ
                    ) {
                        event.isCancelled = true
                        return
                    }
                } else {
                    event.isCancelled = true
                }

            else ->
                onPlayerFreeze(event, event.player)
        }

        if (!event.isCancelled) {
            if (event.player.name !in placedBlocks.keys) {
                placedBlocks[event.player.name] = mutableSetOf()
            }

            placedBlocks[event.player.name]!!.add(event.block.location)

            blocksLog[event.block.location] = event.blockReplacedState.data
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: BlockBreakEvent) {
        if (event.player in _players) {
            event.isDropItems = false
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: PlayerDropItemEvent) {
        if (event.player in _players) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: FoodLevelChangeEvent) {
        if (event.entity in _players) {
            event.isCancelled = true
        }
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
            event.isCancelled = event.newGameMode != GameMode.SURVIVAL
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
            // TODO Add more cases
            return
        }

        event.isCancelled = true
    }

    // TODO Fix pictures breaking

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: EntityDamageEvent) {
        if (event.entity in _players) {
            event.isCancelled = true
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    fun on(event: EntityDamageByEntityEvent) {
        if (event.damager in _players) {
            event.isCancelled = true
        }
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
        } else {
            bossBar.progress = 1.0
            bossBar.title = state.name

            bossBar.color = BarColor.WHITE
        }
    }

    // Private methods

    // TODO Remove onPlayerFreeze

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