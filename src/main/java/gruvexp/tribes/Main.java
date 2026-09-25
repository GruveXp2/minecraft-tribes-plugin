package gruvexp.tribes;

import gruvexp.tribes.commands.*;
import gruvexp.tribes.listeners.*;
import gruvexp.tribes.secrets.Secrets;
import gruvexp.tribes.tasks.NetherEndCooldown;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public final class Main extends JavaPlugin {

    private static Main plugin;
    public static World WORLD;
    public static final String testWorldName = "Tribes test server";
    public static final String worldName = "Tribes";
    private static final int PORT = 25566; // port used to communicate with the discord bot
    public static final String VERSION = "2024.08.19";
    public static String dataPath;
    public static Player gruveXp;

    /**
     * <strong>Hoi</strong><br>
     * Welcome to my java plugin, this plugin is used for my SPM
     * <p>It has custom blocks (like altar of revival), a coin currency system, many tribes with multiple players in them, and more</p>
     * */

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
        getCommand("java").setExecutor(new JavaCommand());
        plugin = this;
        WORLD = Bukkit.getWorld(worldName);
        dataPath = Secrets.SERVER_PATH + worldName + "\\plugin data\\tribes.json";
        if (WORLD == null) {
            WORLD = Bukkit.getWorld(testWorldName);
            getLogger().info("Cant load world \"" + worldName + "\", loading testworld instead");
            dataPath = Secrets.SERVER_PATH + testWorldName + "\\plugin data\\tribes.json";
        }
        Tribes.loadData(); // loading json data
        ItemManager.registerCoinItems(); // register coin items for all members from the json file
        if (WORLD.getTime() < 41*24000) {
            new NetherEndCooldown().runTaskTimer(this, 0L, 24000L);
        }
        getLogger().info("Tribe Plugin v" + VERSION + " successfully loaded");
        new Thread(this::startSocketServer).start(); // start the server in a new thread to avoid blocking the main thread
    }

    @Override
    public void onDisable() {
        Tribes.saveData();
        // Plugin shutdown logic
    }

    public static Main getPlugin() {
        return plugin;
    }

    private void startSocketServer() {
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            getLogger().info("Server listening on port " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                    BufferedWriter out = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()))) {

                    String command = in.readLine();
                    if (command == null || command.trim().isEmpty()) return;
                    if (command.startsWith("@")) { // a request to the plugin to perform something
                        if (command.equals("@ping")) { // the discord bot pinged the plugin
                            out.write("Tribes: " + Bukkit.getOnlinePlayers().size() + " online"); // return info about the server (how many players online etc)
                            out.newLine();
                            out.flush();
                        }
                    } else { // a minecraft command
                        CountDownLatch latch = new CountDownLatch(1);
                        Bukkit.getScheduler().runTask(this, () -> { // schedule the command execution on the main thread
                            try {
                                // execute the command on the server console
                                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                                String result = executeCommand(console, command);

                                synchronized (out) {
                                    try {
                                        // send the result back to the dc bot
                                        out.write(result);
                                        out.newLine();
                                        out.flush();
                                    } catch (IOException e) {
                                        getLogger().severe("Error sending result to client: " + e.getMessage());
                                    }
                                }
                            } finally {
                                latch.countDown(); // signal that the task is complete
                            }
                        });

                        try {
                            latch.await(1, TimeUnit.SECONDS); // if the server lags so much it takes over a second to run the command, then it will quit waiting
                        } catch (InterruptedException e) {
                            getLogger().severe("Waiting for task completion interrupted: " + e.getMessage());
                        }
                    }
                } catch (IOException e) {
                    getLogger().severe("Error handling client: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            getLogger().severe("Could not listen on port " + PORT);
            e.printStackTrace();
        }
    }

    private String executeCommand(ConsoleCommandSender console, String command) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;

        try {
            // redirect system output to capture command result (so if someone did a syntax error, they will know about it)
            System.setOut(new PrintStream(baos));

            // run the command
            Bukkit.dispatchCommand(console, command);
            Bukkit.getLogger().info(baos.toString().trim());

            // restore original system output
            System.setOut(originalOut);

            // return the output (the output of the command)
            return baos.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error capturing command output: " + e.getMessage();
        }
    }
}
