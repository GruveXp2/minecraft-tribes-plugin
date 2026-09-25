package gruvexp.tribes.listeners;

import gruvexp.tribes.Tribe;
import gruvexp.tribes.Tribes;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerHitPlayerListener implements Listener {

    @EventHandler
    public void onPlayerhit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) {return;}
        // begge playersene må være registrert i en tribe
        Player p = (Player) e.getEntity();
        Tribe defenderTribe = Tribes.getTribe(p.getUniqueId());
        if (defenderTribe == null) return;
        Player q = (Player) e.getDamager();
        Tribe attackerTribe = Tribes.getTribe(q.getUniqueId());
        if (attackerTribe == null) return;

        //hvis friendlyfire er av og spillerene er på samme tribe, så gjør de ikke damag
        if (!Tribes.friendlyFire && true) { // TODO: der det står true, skal det heller sjekke om de er i en alianse
            e.setCancelled(true);
        }
    }
}
