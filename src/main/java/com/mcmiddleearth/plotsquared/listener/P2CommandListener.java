package com.mcmiddleearth.plotsquared.listener;

import com.google.common.eventbus.Subscribe;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.events.*;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import static org.bukkit.Bukkit.getLogger;


public class P2CommandListener implements Listener {

    public P2CommandListener() {
        PlotAPI api = new PlotAPI();
        api.registerListener(this);
    }

    @Subscribe
    public void onPlayerClaimPlot(PlayerClaimPlotEvent playerClaimPlotEvent) {
        PlotPlayer<?> plotPlayer = playerClaimPlotEvent.getPlotPlayer();
        Player player = Bukkit.getPlayer(plotPlayer.getUUID());
        for(Plot plot : plotPlayer.getPlots()){
            if(ReviewStatusFlag.isAccepted(plot)){
                plot.setFlag(ReviewStatusFlag.LOCKED_FLAG);
                return;
            }
        }
    }

    @Subscribe
    public void onPlotDone(PlotDoneEvent plotDoneEvent) {
        getLogger().info("Someone did /plot done");
        plotDoneEvent.setEventResult(Result.DENY); //remove maybe later
//        Player player = Bukkit.getPlayer(plotDoneEvent.getPlot().getOwner());
//        Plot plot = plotDoneEvent.getPlot();
//        plotDoneEvent.setEventResult(Result.DENY);
//        ReviewStatusFlag reviewStatus = ReviewStatusFlag.BEING_REVIEWED_FLAG;
//        switch(plot.getFlag(ReviewStatusFlag.class)) {
//            case BEING_REVIEWED:
//                player.sendMessage("Plot already submitted to be reviewed.");
//                break;
//            case NOT_BEING_REVIEWED:
//                ReviewPlot reviewPlot = new ReviewPlot(plot);
//                File file = new File(MCMEP2.getReviewPlotDirectory().toString() + plot.getId().toString() + ".yml");
//                FlatFile.writeObjectToFile(reviewPlot, file);
//                reviewPlot.getTimeSinceLastReview();
//                final long THREEDAYSINSECONDS = 86400*3;
//                if((System.currentTimeMillis() / 1000) - reviewPlot.getTimeSinceLastReview() >= THREEDAYSINSECONDS){
//                    plot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
//                    ReviewAPI.addReviewPlot(reviewPlot.getId(), reviewPlot);
//                    player.sendMessage("Plot successfully submitted to be reviewed.");
//                }
//                else player.sendMessage("Not enough time has passed since last review.");
//                break;
//            case ACCEPTED:
//                player.sendMessage("Plot successfully permanently locked.");
//                plot.setFlag(ReviewStatusFlag.LOCKED_FLAG);
//                break;
//            case REJECTED:
//                //checkiflongenoughago
//                plot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
//                player.sendMessage("Plot successfully submitted to be reviewed again.");
//                break;
//            case LOCKED:
//                player.sendMessage("Plot is permanently locked.");
//                break;
//            default:
//                getLogger().info("ERROR REACHED DEFAULT BRANCH IN PLOTDONE EVEN LISTENER");
//        }
    }
    @Subscribe
    public void OnPlotClear(PlotClearEvent plotClearEvent){
        Plot plot = plotClearEvent.getPlot();
        plotClearEvent.setEventResult(Result.DENY);
        if (DoneFlag.isDone(plot)){
            getLogger().info("Plot has been locked can't clear");
            return;
        }
        if(ReviewStatusFlag.isBeingReviewed(plot)){
            getLogger().info("Being reviewed can't clear");
            return;
        }
        else{
            plotClearEvent.setEventResult(Result.ACCEPT);
        }
    }

    @Subscribe
    public void onPlotDelete(PlotDeleteEvent plotDeleteEvent){
        Plot plot = plotDeleteEvent.getPlot();
        plotDeleteEvent.setEventResult(Result.DENY);
        if (DoneFlag.isDone(plot)){
            getLogger().info("Plot has been locked can't delete");
            return;
        }
        if(ReviewStatusFlag.isBeingReviewed(plot)){
            getLogger().info("Being reviewed can't delete");
            return;
        }
        else{
            ReviewPlot.loadReviewPlotData(plot).deleteReviewPlotData();
            plotDeleteEvent.setEventResult(Result.ACCEPT);
        }
    }
}
