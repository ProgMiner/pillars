package by.progminer.Pillars

import org.bukkit.ChatColor
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable

class Game(
        private val main: Main,
        val options: Options,
        val blocks: Map<Player, Material>,
        val map: GameMap
): BukkitRunnable() {
    enum class State {
        LOBBY,
        HIDING_START,  HIDING,  HIDING_END,
        SEARCH_START,  SEARCH,
        PILLARS_START, PILLARS, PILLARS_END
    }

    data class Options(
            val gameDuration: Long = 300000,
            val startDuration: Long = 5000,
            val endDuration: Long = 300000,
            val blocksAmount: Int = 12
    )

    var state = State.LOBBY
        private set

    val collectedBlocks: MutableMap<Int, Player> = mutableMapOf()

    /**
     * Next timer endpoint
     */
    var timer = 0L
        private set

    var ended = false
        private set

    lateinit var manager: GameManager
        private set

    init {
        blocks.forEach { _, block ->
            if (!block.isBlock) {
                throw IllegalArgumentException("One or more blocks isn't a block")
            }
        }
    }

    fun start(manager: GameManager? = null, after: Long = 0L, period: Long = 1L) {
        checkStarted()
        if (manager != null) {
            this.manager = manager
        }

        blocks.keys.forEach {
            it.canPickupItems = false
        }

        // TODO Preparing before starting

        runTaskTimer(main, after, period)
        switchState(State.HIDING_START)
    }

    fun stop() {
        blocks.keys.forEach {
            it.canPickupItems = true
        }

        // TODO Preparing before stopping

        switchState(State.LOBBY)
    }

    fun checkStarted() {
        if (state != State.LOBBY) {
            throw IllegalStateException("Game already started")
        }
    }

    fun tick(): Boolean {
        if (ended) {
            return true
        }

        when (state) {
            State.HIDING_START -> switchStateIfTimerOut(State.HIDING)

            State.HIDING -> switchStateIfTimerOut(State.HIDING_END)

            State.HIDING_END -> switchStateIfEverybodyHasNotBlocks(State.SEARCH_START)

            State.SEARCH_START -> switchStateIfTimerOut(State.SEARCH)

            State.SEARCH -> switchStateIfTimerOut(State.PILLARS_START)

            State.PILLARS_START -> switchStateIfTimerOut(State.PILLARS)

            State.PILLARS -> switchStateIfEverybodyHasNotBlocks(State.PILLARS_END)

            State.PILLARS_END -> switchStateIfTimerOut(State.LOBBY)

            else -> {}
        }

        return ended
    }

    override fun run() {
        if (tick()) {
            cancel()

            if (this::manager.isInitialized) {
                manager.stopGame(this)
            }
        }
    }

    private fun switchState(newState: State) {
        when (newState) {
            State.HIDING_START, State.SEARCH_START -> {
                blocks.forEach { player, _ ->

                    // Teleporting players to start point
                    player.teleport(map.start)
                }

                // Setting timer for waiting
                timer = System.currentTimeMillis() + options.startDuration
            }

            State.HIDING -> {
                // TODO Giving an effects

                // TODO Move to settings
                // TODO Add adventure mode options
                val pickaxe = ItemStack(Material.DIAMOND_PICKAXE)

                blocks.forEach { player, block ->

                    // Hiding players
                    blocks.forEach { hidingPlayer, _ ->
                        hidingPlayer.hidePlayer(main, player)
                    }

                    // Giving blocks and pickaxe
                    player.inventory.addItem(ItemStack(block, options.blocksAmount), pickaxe)
                }

                // Setting timer for game
                timer = System.currentTimeMillis() + options.gameDuration
            }

            State.HIDING_END -> {
                blocks.keys.forEach {
                    it.sendMessage("${ChatColor.RED}Time is over!")
                }
            }

            State.SEARCH -> {

                // TODO Move to settings
                // TODO Add adventure mode options
                val pickaxe = ItemStack(Material.DIAMOND_PICKAXE)

                blocks.forEach { player, _ ->

                    // Giving pickaxe
                    player.inventory.addItem(pickaxe)
                }

                // Setting timer for game
                timer = System.currentTimeMillis() + options.gameDuration
            }

            State.PILLARS_START -> {
                blocks.forEach { player, _ ->

                    // Counting all collected blocks
                    var blocksCount = 0

                    blocks.forEach { _, block ->
                        @Suppress("LABEL_NAME_CLASH")
                        player.inventory.forEach {
                            if (it == null) {
                                return@forEach
                            }

                            if (it.type == block) {
                                blocksCount += it.amount

                                return@forEach
                            }
                        }
                    }

                    collectedBlocks[blocksCount] = player

                    // Teleporting players to pillars point
                    player.teleport(map.pillars)

                    // Showing players
                    blocks.forEach { showingPlayer, _ ->
                        showingPlayer.showPlayer(main, player)
                    }
                }

                // Setting timer for waiting
                timer = System.currentTimeMillis() + options.startDuration
            }

            State.PILLARS -> {

                // TODO Make blocks placeable for adventure mode
            }

            State.PILLARS_END -> {

                // Setting timer for ending
                timer = System.currentTimeMillis() + options.endDuration
            }

            State.LOBBY -> {
                blocks.forEach { player, _ ->

                    // Teleporting players to lobby
                    player.teleport(map.lobby)
                }

                ended = true
            }

            else -> {}
        }

        state = newState
    }

    private fun switchStateIfTimerOut(newState: State) {
        if (timer <= System.currentTimeMillis()) {
            switchState(newState)
        }
    }

    private fun isEverybodyHasNotBlocks(): Boolean {
        for ((player, _) in blocks) {
            for ((_, block) in blocks) {
                if (player.inventory.contains(block)) {
                    return false

                }
            }
        }

        return true
    }

    private fun switchStateIfEverybodyHasNotBlocks(newState: State) {
        if (isEverybodyHasNotBlocks()) {
            switchState(newState)
        }
    }
}