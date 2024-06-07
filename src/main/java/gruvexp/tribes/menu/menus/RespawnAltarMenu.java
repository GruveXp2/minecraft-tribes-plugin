package gruvexp.tribes.menu.menus;

import gruvexp.tribes.menu.Menu;
import org.bukkit.event.inventory.InventoryClickEvent;

public class RespawnAltarMenu extends Menu {


    public RespawnAltarMenu() {
        super(false);
    }

    @Override
    public String getMenuName() {
        return "Insert 5 player heads";
    }

    @Override
    public int getSlots() {
        return 5;
    }

    @Override
    public void handleMenu(InventoryClickEvent e) {

    }

    @Override
    public void setMenuItems() {

    }

    @Override
    public void callInternalFunction(int i) {

    }
}
