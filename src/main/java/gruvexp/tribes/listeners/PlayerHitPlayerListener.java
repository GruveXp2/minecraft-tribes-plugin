package gruvexp.tribes.listeners;

import gruvexp.tribes.Manager;
import gruvexp.tribes.Member;
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
        Member pMember = Manager.getMember(p.getUniqueId());
        if (pMember == null) return;
        Player q = (Player) e.getDamager();
        Member qMember = Manager.getMember(q.getUniqueId());
        if (qMember == null) return;

        //hvis friendlyfire er av og spillerene er på samme tribe, så gjør de ikke damag
        if (!Manager.friendlyFire && qMember.tribe() == pMember.tribe()) {
            e.setCancelled(true);
        }
    }
}
