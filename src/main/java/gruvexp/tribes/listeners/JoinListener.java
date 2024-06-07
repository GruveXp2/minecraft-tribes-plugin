package gruvexp.tribes.listeners;

import gruvexp.tribes.Main;
import gruvexp.tribes.Manager;
import gruvexp.tribes.Member;
import gruvexp.tribes.Tribe;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class JoinListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        p.setOp(Main.WORLD.getName().equals(Main.testWorldName));
        String pName = p.getName();
        Member member = Manager.getMember(pName);
        if (member == null) {
            p.setGameMode(GameMode.SURVIVAL); // fikser bøgg at playeren er i creative etter at kingdoms serveren var på rett før
            return; // Player is not a member of the game
        }
        Tribe tribe = member.tribe();
        if (tribe == null) {
            // Player is not in a tribe
            p.setGameMode(GameMode.SURVIVAL); // fikser bøgg at playeren er i creative etter at kingdoms serveren var på rett før
            return;
        }
        tribe.handleJoin(pName);
        // Grant 5 seconds of invulnerability
        p.setInvulnerable(true);
        // DEBUG
        if (p.getName().equals("GruveXp")) {
            Main.gruveXp = p;
            /*if (Main.WORLD.getName().equals(Main.testWorldName)) {

            }*/
        }

        // Schedule a task to remove invulnerability after 5 seconds
        new BukkitRunnable() {
            @Override
            public void run() {
                p.setInvulnerable(false);
            }
        }.runTaskLater(Main.getPlugin(), 100);
    }
}
