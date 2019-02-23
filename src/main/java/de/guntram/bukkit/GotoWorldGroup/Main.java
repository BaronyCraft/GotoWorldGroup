package de.guntram.bukkit.GotoWorldGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

public class Main extends JavaPlugin implements Listener, TabCompleter {
    
    public static Main instance;
    
    @Override 
    public void onEnable() {
        if (instance==null)
            instance=this;
        getServer().getPluginManager().registerEvents(this, this);
        getCommand("goto").setTabCompleter(this);
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
                if (destinations == null || ((targetLocation=destinations.get(args[1]))) == null) {
                    sender.sendMessage("Destination "+args[1]+" does not exist in world group "+args[0]);
                    return true;
                }
            } else {
                if (args[0].equals(Config.getGroupForWorld(((Player)sender).getLocation().getWorld().getName()))) {
                    sender.sendMessage("You are already in "+args[0]);
                    return true;
                }
                targetLocation = Config.getLastPlayerLocation((Player)sender, args[0]);
                if (targetLocation == null) {
                    String firstWorld = Config.getWorldsInGroup(args[0]).get(0);
                    targetLocation = getServer().getWorld(firstWorld).getSpawnLocation();
                }
            }
            ((Player)sender).teleport(targetLocation, PlayerTeleportEvent.TeleportCause.COMMAND);
            return true;
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
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        List<String> completions=new ArrayList<>();
        if (args.length == 1) {
            for (String group: Config.getWorldGroups()) {
                if (StringUtil.startsWithIgnoreCase(group, args[0]) && sender.hasPermission("gotoworldgroup.goto."+group)) {
                    completions.add(group);
                }
            }
        }
        else if (args.length == 2) {
            StringUtil.copyPartialMatches(args[1], Config.getDestinations(args[0]).keySet(), completions);
        }
        return completions;
    }
}
