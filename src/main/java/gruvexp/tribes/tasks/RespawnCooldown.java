package gruvexp.tribes.tasks;

import gruvexp.tribes.Tribes;
import gruvexp.tribes.Member;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class RespawnCooldown extends BukkitRunnable {

    UUID playerID;
    Player p;
    int secondsLeft;
    final int MAX;
    Member member;
    BossBar bar = Bukkit.createBossBar(ChatColor.RED + "RespawnTimer", BarColor.RED, BarStyle.SOLID);

    public RespawnCooldown(UUID playerID, int minutes) {
        this.playerID = playerID;
        member = Tribes.getMember(playerID);
        p = Bukkit.getPlayer(playerID);
        if (p != null) {
            bar.addPlayer(p);
        }
        MAX = minutes * 60;
        secondsLeft = MAX + 1; // kommer til å gå ned med 1 minutt med en gang timeren starter
        bar.setProgress(1d);
    }

    public void playerJoined() {
        p = Bukkit.getPlayer(playerID);
        assert p != null;
        bar.addPlayer(p);
        bar.setVisible(true);
    }

    public void reduceCooldown() {
        secondsLeft -= 10;
        if (secondsLeft < 1) {
            secondsLeft = 1;
            run();
            Tribes.stopCooldownReduction();
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
        if (Tribes.INSTANCE.isPaused()) {return;}
        secondsLeft--;
        if (secondsLeft % 60 == 0) {
            // minutter i tribe cooldown left reduseres med 1, hvis det blir 0 så respawner man
            member.setRespawnCooldown((int) Math.ceil(secondsLeft / 60.0));
            if (secondsLeft <= 0) {
                bar.removeAll();
                if (p != null && p.isOnline()) {
                    //Bukkit.getLogger().info("the timer ran out, online");
                    member.respawnNaturally(p);
                } else {
                    //Bukkit.getLogger().info("the timer ran out, offline");
                Bukkit.broadcastMessage(ChatColor.YELLOW + member.NAME + "'s respawn cooldown ran out");
                    Tribes.getMember(playerID).tribe().handleLeaveActive(playerID);
                }
                cancel();
            }
        }
        bar.setProgress((double) secondsLeft / MAX);
    }
}
