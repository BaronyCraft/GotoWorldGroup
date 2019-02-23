/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.guntram.bukkit.GotoWorldGroup;

import org.bukkit.Location;
import org.bukkit.World;

/**
 *
 * @author gbl
 */
public class LocationWithSafetyFlag extends Location {
    
    boolean isSafe;
    public LocationWithSafetyFlag(World world, double x, double y, double z) {
        super(world, x, y, z);
        isSafe=true;
    }
    
    boolean isSafe() {
        return isSafe;
    }
    
    void setSafe(boolean b) {
        isSafe=b;
    }
}
