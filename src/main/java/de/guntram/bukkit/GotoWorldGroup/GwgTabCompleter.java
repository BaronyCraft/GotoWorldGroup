/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.GotoWorldGroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

/**
 *
 * @author gbl
 */
public class GwgTabCompleter implements TabCompleter {
    
    static List<String> subCommands = Arrays.asList(new String[]{"adddestination", "setunsafe"});
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {

        List<String> completions=new ArrayList<>();
        if (args.length == 1) {
            for (String subcmd: subCommands) {
                if (StringUtil.startsWithIgnoreCase(subcmd, args[0]) && sender.hasPermission("gotoworldgroup.gwg."+subcmd)) {
                    completions.add(subcmd);
                }
            }
        }
        else if (args.length == 2) {
            if (args[0].equals("setunsafe")) {
                StringUtil.copyPartialMatches(args[1], Config.getWorldGroups(), completions);
            }
            if (args[0].equals("adddestination") && args[1].length()==0) {
                completions.add("destinationname");
            }
        }
        else if (args.length == 3) {
            if (args[0].equals("adddestination")) {
                for (World world: Bukkit.getWorlds()) {
                    if (StringUtil.startsWithIgnoreCase(world.getName(), args[2])) {
                        completions.add(world.getName());
                    }
                }
                Collections.sort(completions);
            }
        }
        return completions;
    }
}
