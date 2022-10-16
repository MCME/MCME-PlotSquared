package com.mcmiddleearth.plotsquared;

import com.mcmiddleearth.plotsquared.plotflag.ReviewRatingDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewTimeDataFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.mcmiddleearth.plotsquared.util.FileManagement;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotArea;
import com.plotsquared.core.plot.flag.GlobalFlagContainer;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.util.Permissions;
import lombok.NonNull;
import me.gleeming.command.CommandHandler;
import net.megavex.scoreboardlibrary.ScoreboardLibraryImplementation;
import net.megavex.scoreboardlibrary.api.ScoreboardManager;
import net.megavex.scoreboardlibrary.exception.ScoreboardLibraryLoadException;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;

import static com.mcmiddleearth.plotsquared.review.ReviewPlayer.Template.templateOf;

public final class MCMEP2 extends JavaPlugin {

    private static MCMEP2 instance;
    private static PlotAPI plotAPI;
    private static ScoreboardManager scoreboardManager;
    private static final String plotWorld = "yaa";

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
        new ReviewAPI();
        GlobalFlagContainer.getInstance().addFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
        GlobalFlagContainer.getInstance().addFlag(ReviewRatingDataFlag.REVIEW_RATING_DATA_FLAG_NONE);
        GlobalFlagContainer.getInstance().addFlag(ReviewTimeDataFlag.REVIEW_TIME_DATA_FLAG_NONE);

        try {
            ScoreboardLibraryImplementation.init();
        } catch (ScoreboardLibraryLoadException e) {
            // Couldn't load the library.
            // Probably because the servers version is unsupported.
            e.printStackTrace();
            return;
        }

        scoreboardManager = ScoreboardManager.scoreboardManager(this);

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
        //initialize commands
        CommandHandler.registerCommands("com.mcmiddleearth.plotsquared.command", this);

    }
    @Override
    public void onDisable() {
        getLogger().info("onDisable is called!");
//        PlotSquared.get().getEventDispatcher().unregisterListener(new P2CommandListener());
        for(ReviewParty i : ReviewAPI.getReviewParties().values()){
            i.stopReviewParty();
        }

        scoreboardManager.close();
        ScoreboardLibraryImplementation.close();
    }

    /**
     * Boolean for if a player has permission to build in a given location in a PlotSquared world.
     * @param player player trying to build
     * @param location location where player tries to build
     * @return Whether the player has permission to build there
     */
    public static boolean hasBuildPermission(Player player, org.bukkit.Location location) {
        com.plotsquared.core.location.Location plotSquaredLocation = BukkitUtil.adapt(location);
        PlotArea area = plotSquaredLocation.getPlotArea();
        if (area == null) {
            return false;
        }
        Plot plot = area.getPlot(plotSquaredLocation);
        PlotPlayer pp = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (plot != null){
            if (area.notifyIfOutsideBuildArea(pp, ((int) location.getY()))) {
                return false;
            }
            if (!plot.hasOwner()) {
                if (!Permissions.hasPermission(pp, Permission.PERMISSION_ADMIN_BUILD_UNOWNED)) {
                    reviewPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            templateOf("node", String.valueOf(Permission.PERMISSION_ADMIN_BUILD_UNOWNED))
                    );
                    return false;
                }
            } else if (!plot.isAdded(pp.getUUID())) {
                if (!Permissions.hasPermission(pp, Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    reviewPlayer.sendMessage(
                            TranslatableCaption.of("permission.no_permission_event"),
                            templateOf("node", String.valueOf(Permission.PERMISSION_ADMIN_BUILD_OTHER))
                    );
                    return false;
                }
            } else if (Settings.Done.RESTRICT_BUILDING && DoneFlag.isDone(plot)) {
                if (!Permissions.hasPermission(pp, Permission.PERMISSION_ADMIN_BUILD_OTHER)) {
                    reviewPlayer.sendMessage(
                            TranslatableCaption.of("done.building_restricted")
                    );
                    return false;
                }
            }
        } else if (!Permissions.hasPermission(pp, Permission.PERMISSION_ADMIN_BUILD_ROAD)) {
            reviewPlayer.sendMessage(
                    TranslatableCaption.of("permission.no_permission_event"),
                    templateOf("node", String.valueOf(Permission.PERMISSION_ADMIN_BUILD_ROAD))
            );
            return false;
        }
        return true;
    }

    public static MCMEP2 getInstance() {
        return instance;
    }
    public static PlotAPI getPlotAPI(){
        return plotAPI;
    }
    public static ScoreboardManager getScoreboardManager() {return scoreboardManager;}
    public static String getPlotWorld() { return plotWorld; }
    public static File getReviewPlotDirectory(){ return reviewPlotDirectory; }
}

