package gruvexp.tribes.tasks;

import gruvexp.tribes.Main;
import gruvexp.tribes.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PauseIn1Min extends BukkitRunnable {

    int secondsLeft = 60;
    BossBar bar = Bukkit.createBossBar("Pausing timer", BarColor.YELLOW, BarStyle.SOLID);

    public PauseIn1Min() {
        bar.setProgress(1d);
        for (Player p : Bukkit.getOnlinePlayers()) {
            bar.addPlayer(p);
        }
        // offset sånn at teksten kommer etter <player> left the game
        Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> Bukkit.broadcastMessage(ChatColor.YELLOW + "Less than 2 tribes are active, game pauses in 1 minute"), 10L);
    }

    public void cancelPause() {
        bar.removeAll();
        cancel();
    }

    @Override
    public void run() {
        secondsLeft --;
        if (secondsLeft <= 0) {
            bar.removeAll();
            Manager.pause();
            cancel();
        }
        bar.setProgress((double) secondsLeft / 60);
    }
}
