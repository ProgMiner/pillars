package by.progminer.Pillars

import org.bukkit.entity.Player

open class GameManager private constructor(private val games: MutableSet<Game>):
        Set<Game> by games {
    constructor(): this(mutableSetOf())

    private val players = mutableMapOf<Player, Game>()

    init {
        games.forEach {
            it.blocks.forEach { player, _ ->
                players[player] = it
            }
        }
    }

    operator fun get(player: Player): Game {
        if (!players.containsKey(player)) {
            throw IndexOutOfBoundsException("Player is not playing in game of this manager")
        }

        return players[player]!!
    }

    fun getPlayers(): Set<Player> =
            players.keys

    fun startGame(game: Game) {
        game.checkStarted()
        game.blocks.forEach { player, _ ->
            if (players.containsKey(player)) {
                throw IllegalArgumentException("One or more players of game already playing")
            }
        }

        games.add(game)
        game.blocks.forEach { player, _ ->
            players[player] = game
        }

        game.start()
    }

    fun stopGame(game: Game) {
        if (!game.ended) {
            game.stop()
            return
        }

        players.forEach { player, g ->
            if (g == game) {
                players.remove(player)
            }
        }
        games.remove(game)
    }
}