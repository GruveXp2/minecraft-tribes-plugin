package gruvexp.tribes.tasks;

import gruvexp.tribes.Manager;
import gruvexp.tribes.RevivalAltar;
import org.bukkit.scheduler.BukkitRunnable;

public class AltarCooldown extends BukkitRunnable { // skal runnes 1 gang i minuttet

    RevivalAltar ALTAR;
    int minutesLeft;

    public AltarCooldown(RevivalAltar altar, int minutes) {
        ALTAR = altar;
        minutesLeft = minutes;
    }

    public void reduceCooldown(int kromer) {
        minutesLeft -= 2 * kromer;
        if (minutesLeft < 1) {
            minutesLeft = 1;
            run();
        }
    }

    public void haccMinutes(int minutes) {
        minutesLeft = minutes;
        if (minutesLeft < 1) {
            minutesLeft = 1;
        }
    }

    public void remove() { // når en player forlater triben
        cancel();
    }

    @Override
    public void run() {
        if (Manager.isPaused()) {return;}
        minutesLeft--;
        // minutter i tribe cooldown left reduseres med 1, hvis det blir 0 så respawner man
        ALTAR.reduceCooldown(); // reduserer med 1 minutt. handler hva som skjer om det er 0min igjen.
        if (minutesLeft <= 0) {
            cancel();
        }
    }

}
