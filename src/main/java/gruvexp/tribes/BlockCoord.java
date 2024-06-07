package gruvexp.tribes;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;

public class BlockCoord {

    protected int x;
    protected int y;
    protected int z;


    public BlockCoord(@JsonProperty("x") int x, @JsonProperty("y") int y, @JsonProperty("z") int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public BlockCoord(String x, String y, String z) {
        try {
            this.x = Integer.parseInt(x);
            this.y = Integer.parseInt(y);
            this.z = Integer.parseInt(z);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ChatColor.RED + "" + x + " " + y + " " + z + " is not a valid position!");
        }
    }

    public BlockCoord(double x, double y, double z) {
        try {
            this.x = (int) x;
            this.y = (int) y;
            this.z = (int) z;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(ChatColor.RED + "" + x + " " + y + " " + z + " is not a valid position!");
        }
    }

    public BlockCoord(Location location) {
        this.x = location.getBlockX();
        this.y = location.getBlockY();
        this.z = location.getBlockZ();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public String toString() {
        return String.format("%s %s %s", x, y, z);
    }

    public Location toLocation(World world) {
        return new Location(world, x + 0.5, y, z + 0.5);
    }
}