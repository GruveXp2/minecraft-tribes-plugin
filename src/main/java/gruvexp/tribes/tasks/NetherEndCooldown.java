package gruvexp.tribes.tasks;

import gruvexp.tribes.Main;
import gruvexp.tribes.Manager;
import org.bukkit.ChatColor;
import org.bukkit.scheduler.BukkitRunnable;

public class NetherEndCooldown extends BukkitRunnable {
    @Override
    public void run() {
        if (Main.WORLD.getTime() >= 24000*25) {
            if (Main.WORLD.getTime() <= 24000*26) {
                Manager.messagePlayers(ChatColor.BOLD +""+ ChatColor.GREEN + "THE NETHER HAS OPENED!");
                Manager.messagePlayers(ChatColor.YELLOW + "(Server restart necessary)");
            } else {
                if (Main.WORLD.getTime() >= 24000*50 && Main.WORLD.getTime() <= 24000*51) {
                    Manager.messagePlayers(ChatColor.BOLD +""+ ChatColor.GREEN + "THE END HAS OPENED!");
                    Manager.messagePlayers(ChatColor.YELLOW + "(Server restart necessary)");
                    cancel();
                }
            }
        }
    }
}
