package de.guntram.bukkit.GotoWorldGroup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

/*
  structure:
  town:
    town
      spawn:
        x: 0
        y: 65
        z: 0
    adminworld
  kingdom:
    world
    world_nether
    world_the_end
  resource:
    waste
      center
      north
      south
    waste_nether
    waste_the_end
 */

public class Config {
    

    public static class WXYZ {
        public String world;
        public int x, y, z;
        public WXYZ(String w, int x, int y, int z) {
            this.world=w;
            this.x=x;
            this.y=y;
            this.z=z;
        }
    }
    
    public static class WXYZT extends WXYZ {
        public long time;
        public WXYZT(String w, int x, int y, int z, long t) {
            super(w, x, y, z);
            time = t;
        }
    }

    private static class WorldGroupData {
        public Map<String, WXYZ> destinations;
        public List<String> worlds;
        public long resetSavedLocationsOlderThan;
        
        WorldGroupData() {
            destinations = new HashMap<>();
            worlds = new ArrayList<>();
            resetSavedLocationsOlderThan = 0;
        }
    }

    private static class KeyCheckHashMap<T extends Object, U extends Object> extends HashMap<T, U> {
        @Override
        public U get(Object key) {
            U value=super.get(key);
            if (value == null) {
                throw new IllegalArgumentException("bad argument: "+key.toString());
            }
            return value;
        }
    }

    private static Map<String, WorldGroupData> config;
    private static File configFile;
    private static File configDir;

    /**
     * Gets all defined world group names.
     * @return defined names set
     */    
    public static Set<String> getWorldGroups() {
        // System.out.println("getWorldGroups returns "+config.keySet());
        return config.keySet();
    }

    /**
     * Gets a list of worlds within a group. The first of them is the one that 
     * players spawn into when they haven't visited that group yet.
     * @param worldgroup The name of the worldgroup to get info for
     * @return the list of worlds
     */
    public static List<String> getWorldsInGroup(String worldgroup) {
        // System.out.println("getWorldsInGroup("+worldgroup+" returns "+config.get(worldgroup).worlds);
        return config.get(worldgroup).worlds;
    }

    /**
     * Gets the name of the group a particular world is in.
     * @param world the world name to get the group for
     * @return the name of the world group, or null if the world isn't in any group
     */
    public static String getGroupForWorld(String world) {
        for (String group: config.keySet()) {
            if (config.get(group).worlds.contains(world)) {
                // System.out.println("group for world "+world+" is "+group);
                return group;
            }
        }
        return null;
    }
    
    /**
     * Add an empty group
     * @param groupName the name of the group
     */
    public static void addWorldGroup(String groupName) {
        if (config.get(groupName) == null) {
            config.put(groupName, new WorldGroupData());
            save();
        }
    }

    /**
     * Add a world to a group, removing it from all others
     * @param worldName the world to add
     * @param groupName the group to add the world to
     */
    public static void addWorldToGroup(String worldName, String groupName) {
        addWorldGroup(groupName);
        for (WorldGroupData wgd: config.values()) {
            wgd.worlds.remove(worldName);
        }
        WorldGroupData wgd = config.get(groupName);
        wgd.worlds.add(worldName);
        save();
    }

    /**
     * Remove a world from a group, without adding it anywhere else
     * @param worldName the world to remove
     * @param groupName the group to remove the world from
     */
    public static void removeWorldFromGroup(String worldName, String groupName) {
        config.get(groupName).worlds.remove(worldName);
        save();
    }

    /**
     * Remove a world from all groups, for example, after it was deleted
     * @param worldName the world to remove
     */
    public static void removeWorldFromAllGroups(String worldName) {
        for (WorldGroupData wgd: config.values()) {
            wgd.worlds.remove(worldName);
        }
        save();
    }

    /**
     * Set the time when a world was reset, which means players that have
     * saved their location in that world can't safely use that location 
     * anymore.
     * @param groupName
     * @param timestamp 
     */
    public static void setWorldGroupResetTimestamp(String groupName, long timestamp) {
        config.get(groupName).resetSavedLocationsOlderThan = timestamp;
        save();
    }
    
    /**
     * Find the timestamp when the world group was reset last.
     * @param groupName the group for which the time needs to be found
     * @return the reset timestamp for that world
     */
    public static long getWorldGroupResetTimestamp(String groupName) {
        return config.get(groupName).resetSavedLocationsOlderThan;
    }

    /**
     * Get all named destinations within a world
     * @param worldgroup
     * @return a Map that contains the location name as keys, and the location as entries
     */
    public static Map<String, Location> getDestinations(String worldgroup) {
        Map<String, WXYZ> savedMap=config.get(worldgroup).destinations;
        Map<String, Location> destinations=new HashMap<>();
        for (Map.Entry<String, WXYZ> entry: savedMap.entrySet()) {
            destinations.put(entry.getKey(), locationFromWxyz(entry.getValue()));
        }
        return destinations;
    }

    /**
     * Add a new destination to a world group
     * @param worldgroup the world group that has the destination. Can be null,
     * in this case, the group is inferred from the world in location.
     * @param destName the name of the destination
     * @param loc the location of the destination
     */
    public static void addDestination(String worldgroup, String destName, Location loc) {
        if (worldgroup==null || worldgroup.isEmpty()) {
            worldgroup=getGroupForWorld(loc.getWorld().getName());
        }
        config.get(worldgroup).destinations.put(destName, wxyztFromLocation(loc));
        save();
    }

    /**
     * Removes a destination from a world group
     * @param worldgroup the world group that has the destination
     * @param destName the name of the destination
     */
    public static void removeDestination(String worldgroup, String destName) {
        config.get(worldgroup).destinations.remove(destName);
        save();
    }

    /**
     * Returns the last location a player had in a certain world group. This is
     * the location the player should teleport to when porting to that group.
     * 
     * If the player has visited the world before, and the world has since
     * been reset, the safety flag is set to false. In general, that means the
     * location should not be teleported to.
     * 
     * @param player the player to get the location for
     * @param worldgroup the world group in which to search for the location
     * @return the last saved location for the player, or null if the player
     * has never visited that world.
     */
    public static LocationWithSafetyFlag getLastPlayerLocation(Player player, String worldgroup) {
        Map<String, WXYZT> playerConfig=getPlayerConfig(player);
        WXYZT wxyzt = playerConfig.get(worldgroup);
        if (wxyzt==null)
            return null;
        LocationWithSafetyFlag result=locationFromWxyz(wxyzt);
        long reset=config.get(worldgroup).resetSavedLocationsOlderThan;
        if (reset > wxyzt.time)
            result.setSafe(false);
        return result;
    }

    /**
     * Returns the timestamp when a player has last left a worldgroup
     * @param player
     * @param worldgroup
     * @return timestamp
     */
    public static long getLastPlayerTimestamp(Player player, String worldgroup) {
        Map<String, WXYZT> playerConfig=getPlayerConfig(player);
        return playerConfig.get(worldgroup).time;
    }

    private static Map<String, WXYZT> getPlayerConfig(Player player) {
        Type type=new TypeToken<Map<String, WXYZT>>(){}.getType();
        try(JsonReader reader=new JsonReader(new FileReader(getPlayerConfigFile(player)))) {
            Gson gson=new Gson();
            return gson.fromJson(reader, type);
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
        }
        return new HashMap<>();
    }
    
    public static void setLastPlayerLocation(Player player, String worldgroup, Location loc) {
        // System.out.println("setting last location for "+player.getName()+" in "+worldgroup+" to "+loc);
        Map<String, WXYZT> playerConfig=getPlayerConfig(player);
        playerConfig.put(worldgroup, wxyztFromLocation(loc));
        savePlayerConfig(player, playerConfig);
    }

    private static void savePlayerConfig(Player player, Map<String, WXYZT> playerConfig) {
        try (FileWriter writer = new FileWriter(getPlayerConfigFile(player))) {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            gson.toJson(playerConfig, writer);
        } catch (IOException ex) {
            System.err.println("Trying to save player file:");
            ex.printStackTrace(System.err);
        }
    }
    
    public static File getPlayerConfigFile(Player player) {
        return new File(configDir, player.getUniqueId().toString()+".json");
    }
    
    static LocationWithSafetyFlag locationFromWxyz(WXYZ wxyz) {
        return new LocationWithSafetyFlag(Bukkit.getWorld(wxyz.world), wxyz.x, wxyz.y, wxyz.z);
    }
    
    static WXYZT wxyztFromLocation(Location loc) {
        return new WXYZT(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), System.currentTimeMillis());
    }

    public static void load(Main instance) {
        Type mapType = new TypeToken<KeyCheckHashMap<String, WorldGroupData>>(){}.getType();
        configDir=instance.getDataFolder();
        configFile=new File(instance.getDataFolder(), "config.json");
        try (JsonReader reader=new JsonReader(new FileReader(configFile))) {
            Gson gson=new Gson();
            config=gson.fromJson(reader, mapType);
            for (WorldGroupData val: config.values()) {
                if (val.destinations==null)
                    val.destinations=new HashMap<>();
                if (val.worlds==null)
                    val.worlds=new ArrayList<>();
            }
        } catch (IOException | JsonSyntaxException ex) {
            System.err.println(ex.getClass().getName()+" when reading config; creating a fresh one");
            config=new KeyCheckHashMap<>();
            WorldGroupData wgdata=new WorldGroupData();
            wgdata.destinations=new HashMap<>();
            wgdata.worlds=new ArrayList<>();
            for (World world: instance.getServer().getWorlds()) {
                Location loc=world.getSpawnLocation();
                wgdata.destinations.put(world.getName()+"-spawn", 
                        new WXYZ(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
                wgdata.worlds.add(world.getName());
            }
            config.put("allworlds", wgdata);
            instance.getDataFolder().mkdirs();
            // Try to stash the old file before we write a good one
            configFile.renameTo(new File(instance.getDataFolder(), "config.malformed_json."+System.currentTimeMillis()));
            save();
        }
    }
    
    public static void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            Gson gson = new GsonBuilder()
                    .setPrettyPrinting()
                    .create();
            gson.toJson(config, writer);
        } catch (IOException ex) {
            System.err.println("Trying to save config file "+configFile.getAbsolutePath()+":");
            ex.printStackTrace(System.err);
        }
    }
}
