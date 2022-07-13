package com.mcmiddleearth.plotsquared;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.mcmiddleearth.plotsquared.command.ReviewPartyStart;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.plot.flag.GlobalFlagContainer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import com.mcmiddleearth.plotsquared.plotflag.ReviewFlag;

import java.io.File;

public final class MCMEP2 extends JavaPlugin {

    private static MCMEP2 instance;
    private static PlotAPI plotAPI;

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
        //pm.registerEvents(new P2Listener(), this);
        plotAPI= new PlotAPI();
        GlobalFlagContainer.getInstance().addFlag(ReviewFlag.REVIEW_FALSE);
        //GlobalFlagContainer.getInstance().addFlag(ReviewFlag.LOCKED_FALSE);
        new ReviewAPI();


        //data loading
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
        getCommand("reviewParty").setExecutor(new ReviewPartyStart());
    }
    @Override
    public void onDisable() {
        getLogger().info("onDisable is called!");
    }

    public static MCMEP2 getInstance() {
        return instance;
    }
    public static PlotAPI getPlotAPI(){
        return plotAPI;
    }
    public static File getReviewPlotDirectory(){ return reviewPlotDirectory; }
    public static ObjectMapper getObjectMapper() { return objectMapper;}
}

