/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.GotoWorldGroup;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.util.StringUtil;

/**
 *
 * @author gbl
 */
public class GotoTabCompleter implements TabCompleter {
    
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
