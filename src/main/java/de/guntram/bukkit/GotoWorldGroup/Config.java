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
    
    private static class WorldGroupData {
        public Map<String, WXYZ> destinations;
        public List<String> worlds;
    }

    private static Map<String, WorldGroupData> config;
    private static File configFile;
    private static File configDir;
    
    public static Set<String> getWorldGroups() {
        // System.out.println("getWorldGroups returns "+config.keySet());
        return config.keySet();
    }
    public static List<String> getWorldsInGroup(String worldgroup) {
        // System.out.println("getWorldsInGroup("+worldgroup+" returns "+config.get(worldgroup).worlds);
        return config.get(worldgroup).worlds;
    }
    public static String getGroupForWorld(String world) {
        for (String group: config.keySet()) {
            if (config.get(group).worlds.contains(world)) {
                // System.out.println("group for world "+world+" is "+group);
                return group;
            }
        }
        return null;
    }
    public static Map<String, Location> getDestinations(String worldgroup) {
        Map<String, WXYZ> savedMap=config.get(worldgroup).destinations;
        Map<String, Location> destinations=new HashMap<>();
        for (Map.Entry<String, WXYZ> entry: savedMap.entrySet()) {
            destinations.put(entry.getKey(), locationFromWxyz(entry.getValue()));
        }
        return destinations;
    }
    
    public static void setLastPlayerLocation(Player player, String worldgroup, Location loc) {
        // System.out.println("setting last location for "+player.getName()+" in "+worldgroup+" to "+loc);
        Map<String, WXYZ> playerConfig=getPlayerConfig(player);
        playerConfig.put(worldgroup, wxyzFromLocation(loc));
        savePlayerConfig(player, playerConfig);
    }
    
    public static Location getLastPlayerLocation(Player player, String worldgroup) {
        Map<String, WXYZ> playerConfig=getPlayerConfig(player);
        WXYZ wxyz = playerConfig.get(worldgroup);
        if (wxyz==null)
            return null;
        return locationFromWxyz(wxyz);
    }
    
    private static Map<String, WXYZ> getPlayerConfig(Player player) {
        Type type=new TypeToken<Map<String, WXYZ>>(){}.getType();
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
    
    private static void savePlayerConfig(Player player, Map<String, WXYZ> playerConfig) {
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
    
    static Location locationFromWxyz(WXYZ wxyz) {
        return new Location(Bukkit.getWorld(wxyz.world), wxyz.x, wxyz.y, wxyz.z);
    }
    
    static WXYZ wxyzFromLocation(Location loc) {
        return new WXYZ(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    public static void load(Main instance) {
        Type mapType = new TypeToken<Map<String, WorldGroupData>>(){}.getType();
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
            config=new HashMap<>();
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
