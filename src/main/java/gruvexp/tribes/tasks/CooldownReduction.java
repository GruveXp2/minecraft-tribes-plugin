package gruvexp.tribes.tasks;

import gruvexp.tribes.Tribes;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;

public class CooldownReduction extends BukkitRunnable {

    private final HashSet<RespawnCooldown> respawnCooldowns;

    public CooldownReduction(HashSet<RespawnCooldown> respawnCooldowns) {
        this.respawnCooldowns = respawnCooldowns;
    }

    @Override
    public void run() {
        for (RespawnCooldown respawnCooldown : respawnCooldowns) {
            respawnCooldown.reduceCooldown();
        }
        if (!Tribes.INSTANCE.isReducingCooldowns()) {
            cancel();
        }
    }
}
