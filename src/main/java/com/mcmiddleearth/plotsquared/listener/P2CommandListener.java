package com.mcmiddleearth.plotsquared.listener;

import com.google.common.eventbus.Subscribe;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.events.PlotClearEvent;
import com.plotsquared.core.events.PlotDoneEvent;
import com.plotsquared.core.events.Result;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import static com.plotsquared.core.plot.flag.implementations.DoneFlag.isDone;
import static org.bukkit.Bukkit.getLogger;


public class P2CommandListener implements Listener {

    public P2CommandListener() {
        PlotAPI api = new PlotAPI();
        api.registerListener(this);
    }

    @Subscribe
    public void onPlotDone(PlotDoneEvent plotDoneEvent) {
        Player player = Bukkit.getPlayer(plotDoneEvent.getPlot().getOwner());
        Plot plot = plotDoneEvent.getPlot();
        getLogger().info("Done");
        plotDoneEvent.setEventResult(Result.DENY);
        ReviewStatusFlag reviewStatus = ReviewStatusFlag.BEING_REVIEWED_FLAG;
        switch(plot.getFlag(reviewStatus)) {
            case BEING_REVIEWED:
                player.sendMessage("Plot already submitted to be reviewed.");

            case NOT_BEING_REVIEWED:
                //checkiflongenoughago
                ReviewPlot reviewPlot = new ReviewPlot(plot);
                reviewPlot.getTimeSinceLastReview();
                final long THREEDAYSINSECONDS = 86400*3;
                if((System.currentTimeMillis() / 1000) - reviewPlot.getTimeSinceLastReview() >= THREEDAYSINSECONDS){
                    plot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
                    player.sendMessage("Plot successfully submitted to be reviewed.");
                }
                player.sendMessage("Not enough time has passed since last review.");

            case ACCEPTED:
                player.sendMessage("Plot successfully permanently locked.");
                plot.setFlag(ReviewStatusFlag.LOCKED_FLAG);

            case REJECTED:
                //checkiflongenoughago
                plot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
                player.sendMessage("Plot successfully submitted to be reviewed again.");

            case LOCKED:
                player.sendMessage("Plot is permanently locked.");
        }
    }
    @Subscribe
    public void OnPlotClear(PlotClearEvent plotClearEvent){
        Player player = Bukkit.getPlayer(plotClearEvent.getPlot().getOwner());
        Plot plot = plotClearEvent.getPlot();
        if (DoneFlag.isDone(plot)){
            plotClearEvent.setEventResult(Result.DENY);
            getLogger().info("Permanently locked can't clear");
            return;
        }
        if(!isDone(plotClearEvent.getPlot())){
            if(ReviewStatusFlag.isBeingReviewed(plotClearEvent.getPlot())){
                //check if currently in review list, if so cancel
                if(ReviewAPI.isReviewPlot(plotClearEvent.getPlot())){
                    plotClearEvent.setEventResult(Result.DENY);
                    getLogger().info("Being reviewed can't clear");
                }
                //if not currently in review list clear and set reviewFlag to false
                else plotClearEvent.getPlot().setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
            }
        }
    }
}
