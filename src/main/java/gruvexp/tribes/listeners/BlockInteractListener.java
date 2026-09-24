package gruvexp.tribes.listeners;

import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;

public class BlockInteractListener implements Listener { // RESPAWN ALTER DATA: koordinat (key), cooldown, heads, kromers, aura status

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) { // gjør at skulk shriekers placa av players kan spawne wardens
        if (e.getBlockPlaced().getType() == Material.SCULK_SHRIEKER) {
            Block block = e.getBlock();
            Bukkit.dispatchCommand(Bukkit.getServer().getConsoleSender(), String.format("setblock %d %d %d sculk_shrieker[can_summon=true]", block.getX(), block.getY(), block.getZ()));
        }
    }
}
