package me.hilight3r.playerleashing

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.entity.EntityType
import org.bukkit.entity.LeashHitch
import org.bukkit.entity.Player
import org.bukkit.entity.Slime
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.hanging.HangingPlaceEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import org.bukkit.metadata.FixedMetadataValue
import org.bukkit.scheduler.BukkitRunnable


class PlayerLeashingListener : Listener {
    private val leashed = mutableListOf<String>()
    private val plugin = PlayerLeashing.instance

    @EventHandler
    fun onLeashEvent(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (player.hasPermission("playerleashing.use")) {
            val slot = event.hand
            if (slot != EquipmentSlot.HAND || event.rightClicked !is Player) return

            val leashedPlayer = event.rightClicked as Player
            if (leashedPlayer.uniqueId.toString() in leashed || player.inventory.itemInMainHand.type != Material.LEAD) {
                unleashPlayer(leashedPlayer, player)
                return
            }
            leashed += leashedPlayer.uniqueId.toString()

            if (player.gameMode != GameMode.CREATIVE) {
                --player.inventory.itemInMainHand.amount
            }

            spawnSlimeTo(leashedPlayer)

            object : BukkitRunnable() {
                override fun run() {
                    if (leashedPlayer.uniqueId.toString() !in this@PlayerLeashingListener.leashed) {
                        cancel()
                    }
                    if (player.location.distanceSquared(leashedPlayer.location) > 10.0) {
                        leashedPlayer.velocity =
                            player.location.toVector().subtract(leashedPlayer.location.toVector()).multiply(0.05)
                    }
                }
            }.runTaskTimer(plugin, 0L, 0L)
        }
    }


    @EventHandler
    fun onPlayerMoveEvent(event: PlayerMoveEvent) {
        if (event.player.uniqueId.toString() in leashed) {
            val leashedPlayer = event.player
            for (entity in leashedPlayer.getNearbyEntities(5.0, 5.0, 5.0)) {
                if (entity is Slime && entity.hasMetadata(leashedPlayer.uniqueId.toString())) {
                    entity.teleport(leashedPlayer.location.add(0.0, 1.0, 0.0))
                }
            }
        }
    }

    @EventHandler
    fun onPlayerDeathEvent(event: PlayerDeathEvent) = unleashAllRelated(event.entity)

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) = unleashAllRelated(event.player)

    @EventHandler
    fun onHangingPlaceEvent(event: HangingPlaceEvent) {
        if (event.entity !is LeashHitch) return
        val leash = event.entity as LeashHitch
        for (entity in leash.getNearbyEntities(7.0, 7.0, 7.0)) {
            if (entity.uniqueId.toString() in leashed) {
                event.isCancelled = true
            }
        }
    }

    private fun unleashPlayer(leashedPlayer: Player, leashHolder: Player) {
        for (entity in leashedPlayer.getNearbyEntities(1.0, 1.0, 1.0)) {
            if (entity is Slime && entity.hasMetadata(leashedPlayer.uniqueId.toString())) {
                entity.setLeashHolder(null)
                CollisionTeam.team.removeEntry(entity.uniqueId.toString())
                entity.remove()
                leashed -= leashedPlayer.uniqueId.toString()
                if (leashHolder.gameMode != GameMode.CREATIVE) {
                    leashHolder.inventory.addItem(*arrayOf(ItemStack(Material.LEAD)))
                }
            }
        }
    }

    private fun unleashAllRelated(player: Player) {
        if (leashed.contains(player.uniqueId.toString())) {
            for (otherPlayer in Bukkit.getOnlinePlayers()) {
                if (otherPlayer.hasMetadata(player.uniqueId.toString())) {
                    unleashPlayer(player, otherPlayer)
                }
            }
        }
    }

    private fun spawnSlimeTo(player: Player) {
        val slime = player.world.spawnEntity(
            player.location.add(0.0, 1.0, 0.0), EntityType.SLIME
        ) as Slime

        slime.apply {
            size = 0
            setAI(false)
            setGravity(false)
            setLeashHolder(player)
            isInvulnerable = true
            isCollidable = false
            isInvisible = true
            isSilent = true
            setMetadata(player.uniqueId.toString(), FixedMetadataValue(plugin, "NoCollision"))
        }

        CollisionTeam.team.addEntry(slime.uniqueId.toString())
        player.scoreboard = CollisionTeam.board
        player.setMetadata(player.uniqueId.toString(), FixedMetadataValue(plugin, "Holder"))
    }
}