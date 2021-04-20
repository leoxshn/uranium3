package io.posidon.potassium.net

object Players {
    private var players = HashMap<Int, Player>()
    private var idsNyName = HashMap<String?, Int>()

    val isEmpty: Boolean get() = players.isEmpty()
    val ids: Set<Int> get() = players.keys
    val values: Collection<Player> get() = players.values


    fun getName(id: Int): String? = players[id]!!.playerName
    fun getId(name: String): Int? = idsNyName[name]!!


    fun add(player: Player) = set(player.id, player)

    operator fun get(id: Int): Player? = players[id]
    operator fun get(name: String): Player? = players[idsNyName[name]]

    operator fun set(id: Int, player: Player) {
        players[id] = player
        idsNyName[player.playerName] = id
    }

    fun remove(id: Int) = players.remove(id)
    fun remove(name: String) = players.remove(idsNyName[name])

    operator fun iterator(): Iterator<Player> = players.values.iterator()
    operator fun contains(player: Player) = player.id in players
}