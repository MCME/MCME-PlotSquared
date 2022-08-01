package com.mcmiddleearth.plotsquared;

import com.mcmiddleearth.plotsquared.listener.P2CommandListener;
import com.mcmiddleearth.plotsquared.plotflag.ReviewDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.mcmiddleearth.plotsquared.util.FlatFile;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.GlobalFlagContainer;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import me.gleeming.command.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

public final class MCMEP2 extends JavaPlugin {

    private static MCMEP2 instance;
    private static PlotAPI plotAPI;
    private static String plotWorld = "Test";

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

        new ReviewAPI();

        //data loading and making directories
        pluginDirectory = getDataFolder();
        if (!pluginDirectory.exists()) pluginDirectory.mkdir();
        reviewPlotDirectory = new File(pluginDirectory.toString() + File.separator + "ReviewPlotDirectory");
        if (!reviewPlotDirectory.exists()) reviewPlotDirectory.mkdir();

        //load all reviewplots
        for (File file : Objects.requireNonNull(reviewPlotDirectory.listFiles())) {

            getLogger().info(file.getName());
            ReviewPlot reviewPlot = FlatFile.readObjectFromFile(file);
            getLogger().info(file.getName());
            if(reviewPlot == null){
                file.delete();
            }
            else {
                if (reviewPlot.getReviewStatus() == ReviewPlot.ReviewStatus.ACCEPTED){
                    Plot plot = reviewPlot.getPlot();
                    plot.getFlag(ReviewDataFlag.class).addAll(reviewPlot.preparedReviewData());
                    reviewPlot.deleteReviewPlotData();
                    //set plot to done
                    long flagValue = System.currentTimeMillis() / 1000;
                    PlotFlag<?, ?> plotFlag = plot.getFlagContainer().getFlag(DoneFlag.class)
                            .createFlagInstance(Long.toString(flagValue));
                    plot.setFlag(plotFlag);
                    //set plot to ACCEPTED
                    plot.setFlag(ReviewStatusFlag.ACCEPTED_FLAG);
                }
                else{
                    ReviewAPI.addReviewPlot(reviewPlot.getId(), reviewPlot);
                }
            }
        }

        getLogger().info("after loading files");
        //initializes commands
        CommandHandler.registerCommands("com.mcmiddleearth.plotsquared.command", this);

    }
    @Override
    public void onDisable() {
        getLogger().info("onDisable is called!");
        GlobalFlagContainer.getInstance().removeFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
        PlotSquared.get().getEventDispatcher().unregisterListener(new P2CommandListener());

        for(ReviewParty i : ReviewAPI.getReviewParties().values()){
            i.stopReviewParty();
        }
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

