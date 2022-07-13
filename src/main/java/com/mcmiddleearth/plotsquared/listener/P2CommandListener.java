package com.mcmiddleearth.plotsquared.listener;

import com.google.common.eventbus.Subscribe;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.events.*;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import org.bukkit.event.Listener;
import com.mcmiddleearth.plotsquared.plotflag.ReviewFlag;

import static com.plotsquared.core.plot.flag.implementations.DoneFlag.isDone;
import static org.bukkit.Bukkit.getLogger;


public class P2CommandListener implements Listener {

    public P2CommandListener() {
        PlotAPI api = new PlotAPI();
        api.registerListener(this);
    }

    @Subscribe
    public void onPlayerClaim(PlayerClaimPlotEvent playerClaimPlot){
        playerClaimPlot.setEventResult(Result.ACCEPT);
        //playerClaimPlot.getPlot().setFlag(ReviewFlag.REVIEW_FALSE); IS THIS NEEDED??
    }

    @Subscribe
    public void onPlotDone(PlotDoneEvent plotDone) {
        Plot plot = plotDone.getPlot();
        getLogger().info("Done");
        plotDone.setEventResult(Result.DENY);

        if(!DoneFlag.isDone(plot) && !plot.getFlag(ReviewFlag.class)){
            plot.setFlag(ReviewFlag.REVIEW_TRUE);
            getLogger().info("True");
            return;
        }

        else if (DoneFlag.isDone(plot)) {
            getLogger().info("Permanently locked can't put to done");
            return;
        }
    }
    @Subscribe
    public void OnPlotClear(PlotClearEvent plotClear){
        Plot plot = plotClear.getPlot();
        if (DoneFlag.isDone(plot)){
            plotClear.setEventResult(Result.DENY);
            getLogger().info("Permanently locked can't clear");
            return;
        }
        if(!isDone(plotClear.getPlot())){
            if(plotClear.getPlot().getFlag(ReviewFlag.REVIEW_TRUE)){
                //check if currently in review list, if so cancel
                if(ReviewAPI.getCurrentPlots().containsKey(plotClear.getPlot().getId())){
                    plotClear.setEventResult(Result.DENY);
                    getLogger().info("Being reviewed can't clear");
                }
                //if not currently in review list clear and set reviewFlag to false
                else plotClear.getPlot().setFlag(ReviewFlag.REVIEW_FALSE);
            }
        }
    }
}
