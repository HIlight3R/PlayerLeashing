package me.hilight3r.playerleashing

import org.bukkit.Bukkit
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team


object CollisionTeam {
    val board: Scoreboard
    val team: Team

    init {
        val manager = Bukkit.getScoreboardManager()
        val board = manager!!.newScoreboard
        val team = board.registerNewTeam("NoCollision")
        team.setOption(Team.Option.COLLISION_RULE, Team.OptionStatus.NEVER)
        this.team = team
        this.board = board
    }
}