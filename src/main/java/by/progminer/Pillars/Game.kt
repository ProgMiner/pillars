package by.progminer.Pillars

import org.bukkit.Material
import org.bukkit.block.Block
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.Plugin

class Game(
        val plugin: Plugin,
        val options: Options,
        val blocks: Map<Player, Block>,
        val map: GameMap
) {
    enum class State {
        LOBBY,
        HIDING_START,  HIDING,  HIDING_END,
        SEARCH_START,  SEARCH,
        PILLARS_START, PILLARS, PILLARS_END
    }

    data class Options(
            val gameDuration: Long,
            val startDuration: Long,
            val endDuration: Long,
            val blocksAmount: Int
    )

    var state = State.LOBBY
        private set

    val collectedBlocks: MutableMap<Int, Player> = mutableMapOf()

    /**
     * Next timer endpoint
     */
    var timer = 0L
        private set

    fun start() {
        // TODO Preparing before starting

        switchState(State.HIDING_START)
    }

    fun tick() {
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
                        hidingPlayer.hidePlayer(plugin, player)
                    }

                    // Giving blocks and pickaxe
                    player.inventory.addItem(ItemStack(block.type, options.blocksAmount), pickaxe)
                }

                // Setting timer for game
                timer = System.currentTimeMillis() + options.gameDuration
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
                        player.inventory.forEach {
                            if (it.type == block.type) {
                                blocksCount += it.amount

                                @Suppress("LABEL_NAME_CLASH")
                                return@forEach
                            }
                        }
                    }

                    collectedBlocks[blocksCount] = player

                    // Teleporting players to pillars point
                    player.teleport(map.pillars)

                    // Showing players
                    blocks.forEach { showingPlayer, _ ->
                        showingPlayer.showPlayer(plugin, player)
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

            State.LOBBY ->
                blocks.forEach { player, _ ->

                    // Teleporting players to lobby
                    player.teleport(map.lobby)
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
        var hasBlocks = false

        for (player in blocks) {
            if (player.key.inventory.contains(player.value.type)) {
                hasBlocks = true
                break
            }
        }

        return hasBlocks
    }

    private fun switchStateIfEverybodyHasNotBlocks(newState: State) {
        if (isEverybodyHasNotBlocks()) {
            switchState(newState)
        }
    }
}