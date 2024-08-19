package gruvexp.tribes;

import gruvexp.tribes.commands.*;
import gruvexp.tribes.listeners.*;
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
    private static final int PORT = 25566; // Port used to communicate with the discord bot
    public static final String VERSION = "2024.08.19";
    public static String dataPath;
    public static Player gruveXp;

    /**
     * <strong>Hoi</strong><br>
     * Welcome to my java plugin
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
        getCommand("spec").setExecutor(new SpectateCommand());
        getCommand("spec").setTabCompleter(new SpectateTabCompleter());
        getCommand("java").setExecutor(new JavaCommand());
        plugin = this;
        WORLD = Bukkit.getWorld(worldName);
        dataPath = "C:\\Users\\gruve\\Desktop\\Server\\" + worldName + "\\plugin data\\tribes.json";
        if (WORLD == null) {
            WORLD = Bukkit.getWorld(testWorldName);
            getLogger().info("Cant load world \"" + worldName + "\", loading testworld instead");
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
        getLogger().info("Tribe Plugin v" + VERSION + " successfully loaded");
        new Thread(this::startSocketServer).start(); // Start the server in a new thread to avoid blocking the main thread
    }

    @Override
    public void onDisable() {
        Manager.saveData();
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
                    //getLogger().info("Received command: " + command);
                    if (command == null || command.trim().isEmpty()) return;
                    if (command.startsWith("@")) {
                        if (command.equals("@ping")) {
                            out.write("Tribes: " + Bukkit.getOnlinePlayers().size() + " online");
                            out.newLine();
                            out.flush();
                        }
                    } else { // a minecraft command

                        CountDownLatch latch = new CountDownLatch(1);

                        Bukkit.getScheduler().runTask(this, () -> { // Schedule the command execution on the main thread
                            try {
                                // Execute the command on the server console
                                ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
                                String result = executeCommand(console, command);

                                synchronized (out) { // Ensure safe access to the BufferedWriter
                                    try {
                                        // Send the result back to the client
                                        out.write(result);
                                        out.newLine();
                                        out.flush();
                                        //getLogger().info("The result of the command is: \n" + result + "\n======");
                                    } catch (IOException e) {
                                        getLogger().severe("Error sending result to client: " + e.getMessage());
                                    }
                                }
                            } finally {
                                latch.countDown(); // Signal that the task is complete
                            }
                        });

                        // Wait for the task to complete before closing the resources
                        try {
                            latch.await(1, TimeUnit.SECONDS); // if the server lags so much it takes over a second to run the command, then it will quit waiting
                        } catch (InterruptedException e) {
                            getLogger().severe("Waiting for task completion interrupted: " + e.getMessage());
                        }
                    }
                    //getLogger().warning("The socket will close now");
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
            // Redirect system output to capture command output
            System.setOut(new PrintStream(baos));

            // Execute the command
            Bukkit.dispatchCommand(console, command);

            // Restore original system output
            System.setOut(originalOut);

            // Return the captured output
            return baos.toString().trim();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error capturing command output: " + e.getMessage();
        }
    }
}
