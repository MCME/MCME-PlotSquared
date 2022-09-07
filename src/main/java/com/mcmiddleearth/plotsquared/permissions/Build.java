package com.mcmiddleearth.plotsquared.permissions;

import org.bukkit.entity.Player;

public class Build {
    /**
     * Boolean for if a player has permission to build in a given location in a PlotSquared world.
     * @param player player trying to build
     * @param location location where player tries to build
     * @return Whether the player has permission to build there
     */
    public boolean hasPlotBuildPermission(Player player, org.bukkit.Location location) {
        com.plotsquared.core.location.Location plotSquaredLocation = com.plotsquared.core.location.Location.at(location.getWorld().toString(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (plotSquaredLocation.isPlotArea()) {
            if (player.hasPermission("mcmep2.build.everywhere")) return true;
            com.plotsquared.core.plot.Plot plot = plotSquaredLocation.getPlot();
            if (plot != null && (plot.isAdded(player.getUniqueId()) || player.hasPermission("mcmep2.build.all_plots"))) return true;
            else return false;
        }
        else return false;
    }
}
