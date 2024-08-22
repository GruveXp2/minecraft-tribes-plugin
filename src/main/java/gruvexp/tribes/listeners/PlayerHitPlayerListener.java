package gruvexp.tribes.listeners;

import gruvexp.tribes.Manager;
import gruvexp.tribes.Member;
import gruvexp.tribes.Tribe;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class PlayerHitPlayerListener implements Listener {

    @EventHandler
    public void onPlayerhit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player) || !(e.getEntity() instanceof Player)) {return;}
        Player p = (Player) e.getEntity();
        Member pMember = Manager.getMember(p.getUniqueId());
        if (pMember == null) return;
        Player q = (Player) e.getDamager();
        Member qMember = Manager.getMember(q.getUniqueId());
        if (qMember == null) return;
        if (!Manager.friendlyFire && qMember.tribe() == pMember.tribe()) {
            e.setCancelled(true);
        }
    }
}
