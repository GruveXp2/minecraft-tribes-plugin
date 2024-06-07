package gruvexp.tribes.tasks;

import gruvexp.tribes.Manager;
import gruvexp.tribes.Member;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RespawnCooldown extends BukkitRunnable {

    String playerName;
    Player p;
    int secondsLeft;
    final int MAX;
    Member MEMBER;
    BossBar bar = Bukkit.createBossBar(ChatColor.RED + "RespawnTimer", BarColor.RED, BarStyle.SOLID);

    public RespawnCooldown(String playerName, int minutes) {
        this.playerName = playerName;
        MEMBER = Manager.getMember(playerName);
        if (Bukkit.getPlayerExact(playerName) != null) {
            p = Bukkit.getPlayerExact(playerName);
            assert p != null;
            bar.addPlayer(p);
        }
        MAX = minutes * 60;
        secondsLeft = MAX + 1; // kommer til å gå ned med 1 minutt med en gang timeren starter
        bar.setProgress(1d);
    }

    public void playerJoined() {
        p = Bukkit.getPlayerExact(playerName);
        assert p != null;
        bar.addPlayer(p);
        bar.setVisible(true);
    }

    public void reduceCooldown() {
        secondsLeft -= 10;
        if (secondsLeft < 1) {
            secondsLeft = 1;
            run();
            Manager.stopCooldownReduction();
        }
    }

    public void haccMinutes(int minutes) {
        secondsLeft = minutes*60;
        if (secondsLeft < 1) {
            secondsLeft = 1;
        }
    }

    public void remove() { // når en player forlater triben eller man respawner ved et alter
        bar.removeAll();
        cancel();
    }

    @Override
    public void run() {
        if (Manager.isPaused()) {return;}
        secondsLeft--;
        if (secondsLeft % 60 == 0) {
            // minutter i tribe cooldown left reduseres med 1, hvis det blir 0 så respawner man
            MEMBER.setRespawnCooldown((int) Math.ceil(secondsLeft / 60.0));
            if (secondsLeft <= 0) {
                bar.removeAll();
                if (p != null && p.isOnline()) {
                    //Bukkit.getLogger().info("the timer ran out, online");
                    MEMBER.respawnNaturally(p);
                } else {
                    //Bukkit.getLogger().info("the timer ran out, offline");
                    Bukkit.broadcastMessage(ChatColor.YELLOW + playerName + "'s respawn cooldown ran out");
                    Manager.getMember(playerName).tribe().handleLeaveActive(playerName);
                }
                cancel();
            }
        }
        bar.setProgress((double) secondsLeft / MAX);
    }
}
