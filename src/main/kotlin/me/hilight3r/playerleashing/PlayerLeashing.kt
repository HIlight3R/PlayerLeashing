package me.hilight3r.playerleashing

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin

@Suppress("unused")
class PlayerLeashing : JavaPlugin() {
    companion object {
        lateinit var instance: PlayerLeashing
    }
    override fun onEnable() {
        instance = this
        Bukkit.getPluginManager().registerEvents(PlayerLeashingListener(), this)
    }
}