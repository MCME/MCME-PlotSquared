package com.mcmiddleearth.plotsquared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.GlobalFlagContainer;
import me.gleeming.command.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public final class MCMEP2 extends JavaPlugin {

    private static MCMEP2 instance;
    private static PlotAPI plotAPI;
    private static String plotWorld = "world";
    private static PlotArea plotArea =  PlotSquared.get().getPlotAreaManager().getPlotAreaByString(plotWorld);

    private static File pluginDirectory;
    private static File reviewPlotDirectory;
    private static ObjectMapper objectMapper;
    @Override
    public void onEnable() {
        getLogger().info("onEnable is called!");

        instance = this;
        if (Bukkit.getPluginManager().getPlugin("PlotSquared") == null) {
            getLogger().info("No PlotSquared detected!");
        }
        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new com.mcmiddleearth.plotsquared.listener.PlayerListener(), this);

        plotAPI= new PlotAPI();
        GlobalFlagContainer.getInstance().addFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);

        new ReviewAPI();

        //data loading and making directories
        pluginDirectory = getDataFolder();
        if (!pluginDirectory.exists()){
            pluginDirectory.mkdir();
        }
        reviewPlotDirectory = new File(pluginDirectory + File.separator + "ReviewPlotDirectory");
        if (!reviewPlotDirectory.exists()){
            reviewPlotDirectory.mkdir();
        }
        //initiate objectMapper for YAML files
        objectMapper = new ObjectMapper(new YAMLFactory());

        CommandHandler.registerCommands("com.mcmiddleearth.plotsquared.command", this);

    }
    @Override
    public void onDisable() {
        getLogger().info("onDisable is called!");

        for(ReviewParty i : ReviewAPI.getReviewParties().values()){
            i.stopParty();
        }
    }

    public static MCMEP2 getInstance() {
        return instance;
    }
    public static PlotAPI getPlotAPI(){
        return plotAPI;
    }
    public static String getPlotWorld() { return plotWorld; }
    public static PlotArea getPlotArea() { return plotArea; }
    public static File getReviewPlotDirectory(){ return reviewPlotDirectory; }
    public static ObjectMapper getObjectMapper() { return objectMapper;}
}

