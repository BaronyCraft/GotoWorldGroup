package de.guntram.bukkit.GotoWorldGroup;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {
    
    public static Main instance;
    
    @Override 
    public void onEnable() {
        if (instance==null)
            instance=this;
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("goto").setTabCompleter(new GotoTabCompleter());
        getCommand("gwg").setTabCompleter(new GwgTabCompleter());
        Config.load(instance);
    }
    
    @Override
    public void onDisable() {
        Config.save();
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String commandName=command.getName();
        Location targetLocation;
        if (commandName.equals("goto")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("Can only be used by players");
                return true;
            }
            if (args.length < 1) {
                return false;           // bukkit will display usage info
            }
            if (!(Config.getWorldGroups().contains(args[0]))) {
                sender.sendMessage("That group doesn't exist");
                return true;
            }
            if (!(sender.hasPermission("gotoworldgroup.goto."+args[0]))) {
                sender.sendMessage("You cannot go there");
                return true;
            }

            if (args.length > 1) {
                Map<String, Location> destinations = Config.getDestinations(args[0]);
                if (destinations==null) {
                    sender.sendMessage("No destinations defined in "+args[0]);
                    return true;
                }
                if (((targetLocation=destinations.get(args[1]))) == null) {
                    for (String key: destinations.keySet()) {
                        if (key.equalsIgnoreCase(args[1])) {
                            targetLocation=destinations.get(key);
                            break;
                        }
                    }
                    if (targetLocation==null) {
                        sender.sendMessage("Destination "+args[1]+" does not exist in world group "+args[0]);
                        return true;
                    }
                }
            } else {
                if (args[0].equals(Config.getGroupForWorld(((Player)sender).getLocation().getWorld().getName()))) {
                    sender.sendMessage("You are already in "+args[0]);
                    return true;
                }
                targetLocation = Config.getLastPlayerLocation((Player)sender, args[0]);
                if (targetLocation == null || !(((LocationWithSafetyFlag)targetLocation).isSafe())) {
                    String firstWorld = Config.getWorldsInGroup(args[0]).get(0);
                    if (targetLocation != null) {
                        warnAboutUnsafeWorld(sender);
                    }
                    targetLocation = getServer().getWorld(firstWorld).getSpawnLocation();
                }
            }
            ((Player)sender).teleport(targetLocation, PlayerTeleportEvent.TeleportCause.COMMAND);
            return true;
        }
        if (commandName.equals("gwg")) {
            if (args.length<1)
                return false;
            if (!(sender.hasPermission("gotoworldgroup.gwg."+args[0]))) {
                sender.sendMessage("You cannot do that");
                return true;
            }
            if (args.length==2 && args[0].equals("setunsafe")) {
                Config.setWorldGroupResetTimestamp(args[1], System.currentTimeMillis());
                return true;
            }
            if (args.length==6 && args[0].equals("adddestination")) {
                String name=args[1], worldName=args[2];
                World world=Bukkit.getWorld(worldName);
                if (world==null) {
                    sender.sendMessage("World "+worldName+" doesn't exist");
                    return true;
                }
                int x, y, z;
                try {
                    x=Integer.parseInt(args[3]);
                    y=Integer.parseInt(args[4]);
                    z=Integer.parseInt(args[5]);
                } catch (NumberFormatException ex) {
                    sender.sendMessage("all three coordinates must be integers");
                    return true;
                }
                Config.addDestination(null, name, new Location(world, x, y, z));
                return true;
            }
        }
        return false;
    }
    

    @EventHandler
    public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
        Location oldPos=event.getFrom();
        Location newPos=event.getTo();
        
        String oldWorldGroup=Config.getGroupForWorld(oldPos.getWorld().getName());
        String newWorldGroup=Config.getGroupForWorld(newPos.getWorld().getName());
        if (!(oldWorldGroup.equals(newWorldGroup))) {
            Config.setLastPlayerLocation(event.getPlayer(), oldWorldGroup, oldPos);
        }
    }
    
    @EventHandler
    public void onPlayerQuitEvent(PlayerQuitEvent event) {
        Player player=event.getPlayer();
        String groupName=Config.getGroupForWorld(player.getLocation().getWorld().getName());
        Config.setLastPlayerLocation(player, groupName, player.getLocation());
    }

    /**
     * When a player joins, and the world group they're in has been reset since
     * they last logged out in that world, move them to a safe position, unless
     * another plugin has already taken care of this. This handler registers 
     * itself on priority LOWEST to give other plugins a chance to act first.
     * 
     * @param event The player join event sent from Bukkit
     */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoinEvent(PlayerJoinEvent event) {
        Player player=event.getPlayer();
        String groupName=Config.getGroupForWorld(player.getLocation().getWorld().getName());
        LocationWithSafetyFlag lastPlayerLocation = Config.getLastPlayerLocation(player, groupName);
        if (lastPlayerLocation != null && !lastPlayerLocation.isSafe()) {
            warnAboutUnsafeWorld(player);
            player.teleport(player.getLocation().getWorld().getSpawnLocation());
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerEnterBedEvent(PlayerBedEnterEvent event) {
        Player player=event.getPlayer();
        Location loc = event.getBed().getLocation();
        String groupName=Config.getGroupForWorld(loc.getWorld().getName());
        Config.setLastPlayerLocation(player, "BED-"+groupName, loc);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerRespawnEvent(PlayerRespawnEvent event) {
        Player player=event.getPlayer();
        System.out.println("Player location is "+player.getLocation());
        String groupName=Config.getGroupForWorld(player.getLocation().getWorld().getName());
        LocationWithSafetyFlag newLoc = Config.getLastPlayerLocation(player, "BED-"+groupName, groupName);
        if (newLoc!=null && newLoc.isSafe()) {
            System.out.println("set spawn location "+newLoc+", event.isbed="+event.isBedSpawn());
            event.setRespawnLocation(newLoc);
        } else {
            System.out.println("Sending player to spawn as their bed isn't safe");
            event.setRespawnLocation(player.getLocation().getWorld().getSpawnLocation());
        }
    }

    private void warnAboutUnsafeWorld(CommandSender sender) {
        sender.sendMessage("§4The world you tried to enter was changed and your last position isn't safe anymore. You have been sent to spawn.");
    }
}
