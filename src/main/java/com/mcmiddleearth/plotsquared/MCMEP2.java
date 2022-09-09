package com.mcmiddleearth.plotsquared;

import com.mcmiddleearth.plotsquared.listener.P2CommandListener;
import com.mcmiddleearth.plotsquared.plotflag.ReviewRatingDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewTimeDataFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.mcmiddleearth.plotsquared.util.FileManagement;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.flag.GlobalFlagContainer;
import me.gleeming.command.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class MCMEP2 extends JavaPlugin {

    private static MCMEP2 instance;
    private static PlotAPI plotAPI;
    private static String plotWorld = "Plots";

    private static File pluginDirectory;
    private static File reviewPlotDirectory;

    @Override
    public void onEnable() {
        getLogger().info("onEnable is called!");

        instance = this;
        if (Bukkit.getPluginManager().getPlugin("PlotSquared") == null) {
            getLogger().info("No PlotSquared detected!");
        }


        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new com.mcmiddleearth.plotsquared.listener.PlayerListener(), this);

        plotAPI = new PlotAPI();
        PlotSquared.get().getEventDispatcher().registerListener(new P2CommandListener());
        GlobalFlagContainer.getInstance().addFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
        GlobalFlagContainer.getInstance().addFlag(ReviewRatingDataFlag.REVIEW_RATING_DATA_FLAG_NONE);
        GlobalFlagContainer.getInstance().addFlag(ReviewTimeDataFlag.REVIEW_TIME_DATA_FLAG_NONE);

        new ReviewAPI();

        //data loading and making directories
        pluginDirectory = getDataFolder();
        if (!pluginDirectory.exists()) pluginDirectory.mkdir();
        reviewPlotDirectory = new File(pluginDirectory, "ReviewPlotDirectory");
        if (!reviewPlotDirectory.exists()) reviewPlotDirectory.mkdir();
        getLogger().info("loading all files");
        //load all reviewplots
        for (File file : Objects.requireNonNull(reviewPlotDirectory.listFiles())) {
            ReviewPlot reviewPlot = FileManagement.readObjectFromFile(file);
            if(reviewPlot == null){
                file.delete();
            }
            else {
            ReviewAPI.addReviewPlot(reviewPlot.getPlotId(), reviewPlot);
            }
        }

        getLogger().info("all files are loaded");
        //initializes commands
        CommandHandler.registerCommands("com.mcmiddleearth.plotsquared.command", this);

    }
    @Override
    public void onDisable() {
        getLogger().info("onDisable is called!");
        PlotSquared.get().getEventDispatcher().unregisterListener(new P2CommandListener());
        for(ReviewParty i : ReviewAPI.getReviewParties().values()){
            i.stopReviewParty();
        }
    }

    /**
     * Boolean for if a player has permission to build in a given location in a PlotSquared world.
     * @param player player trying to build
     * @param location location where player tries to build
     * @return Whether the player has permission to build there
     */
    public boolean hasBuildPermission(Player player, org.bukkit.Location location) {
        com.plotsquared.core.location.Location plotSquaredLocation = com.plotsquared.core.location.Location.at(location.getWorld().toString(), location.getBlockX(), location.getBlockY(), location.getBlockZ());
        if (plotSquaredLocation.isPlotArea()) {
            if (player.hasPermission("mcmep2.build.everywhere")) return true;
            com.plotsquared.core.plot.Plot plot = plotSquaredLocation.getPlot();
            if (plot != null && (plot.isAdded(player.getUniqueId()) || player.hasPermission("mcmep2.build.all_plots"))) return true;
            else return false;
        }
        else return false;
    }

    public static MCMEP2 getInstance() {
        return instance;
    }
    public static PlotAPI getPlotAPI(){
        return plotAPI;
    }
    public static String getPlotWorld() { return plotWorld; }
    public static File getReviewPlotDirectory(){ return reviewPlotDirectory; }
}

