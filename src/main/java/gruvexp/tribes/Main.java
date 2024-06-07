package gruvexp.tribes;

import gruvexp.tribes.commands.*;
import gruvexp.tribes.listeners.*;
import gruvexp.tribes.tasks.NetherEndCooldown;
import net.kyori.adventure.audience.Audience;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private static Main plugin;
    public static World WORLD;
    public static Audience AUDIENCE;
    public static final String testWorldName = "Tribes test server";
    public static final String worldName = "Tribes";
    public static final String VERSION = "2024.05.14";
    public static String dataPath;
    public static Player gruveXp;

    /**
     * <strong>Hoi</strong><br>
     * canoes are pretty ebic bdw
     * 2DO list:
     * Add dette innafor tribe objektet i json: {revival_altars:[{x:1, y:2, z:3, cooldown: 60}, {x:4, y:5, z:6, cooldown:-1},...]} // cooldown=-1 betyr at alteret ikke er aktivert
     */

    @Override
    public void onEnable() {
        // Plugin startup logic
        getServer().getPluginManager().registerEvents(new DeathListener(), this);
        getServer().getPluginManager().registerEvents(new JoinListener(), this);
        getServer().getPluginManager().registerEvents(new LeaveListener(), this);
        getServer().getPluginManager().registerEvents(new MoveListener(), this);
        getServer().getPluginManager().registerEvents(new PlayerHitPlayerListener(), this);
        getServer().getPluginManager().registerEvents(new ItemListener(), this);
        getServer().getPluginManager().registerEvents(new BlockInteractListener(), this);
        getServer().getPluginManager().registerEvents(new RightClickEntityListener(), this);
        getCommand("tribe").setExecutor(new TribeCommand());
        getCommand("tribe").setTabCompleter(new TribeTabCompletion());
        getCommand("spec").setExecutor(new SpectateCommand());
        getCommand("spec").setTabCompleter(new SpectateTabCompleter());
        getCommand("java").setExecutor(new JavaCommand());
        plugin = this;
        WORLD = Bukkit.getWorld(worldName);
        //AUDIENCE =
        dataPath = "C:\\Users\\gruve\\Desktop\\Server\\" + worldName + "\\plugin data\\tribes.json";
        if (WORLD == null) {
            WORLD = Bukkit.getWorld(testWorldName);
            Bukkit.getLogger().info("Cant load world \"" + worldName + "\", loading testworld instead");
            dataPath = "C:\\Users\\gruve\\Desktop\\Server\\" + testWorldName + "\\plugin data\\tribes.json";
        }
        Manager.loadData(); // laster inn json data
        ItemManager.registerCoinItems(); // registrerer coin items for alle members fra json fila
        Manager.postInit(); // initer objekter som krever at tribes variabelen er inita først
        ItemManager.registerAltar();
        Manager.pause();
        if (WORLD.getTime() < 41*24000) {
            new NetherEndCooldown().runTaskTimer(this, 0L, 24000L);
        }
        Bukkit.getLogger().info("Tribe Plugin v" + VERSION + " successfully loaded");
    }

    @Override
    public void onDisable() {
        Manager.saveData();
        // Plugin shutdown logic
    }

    public static Main getPlugin() {
        return plugin;
    }
}
