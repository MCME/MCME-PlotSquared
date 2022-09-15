package com.mcmiddleearth.plotsquared.listener;

import com.google.common.eventbus.Subscribe;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.events.*;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;


public class P2CommandListener {

    public P2CommandListener() {
        PlotAPI api = new PlotAPI();
        api.registerListener(this);
    }

    @Subscribe
    public void onPlotDone(PlotDoneEvent plotDoneEvent) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(plotDoneEvent.getPlot().getOwner()));
        plotDoneEvent.setEventResult(Result.DENY);
    }

    @Subscribe
    public void onPlayerPlotClaim(PlayerClaimPlotEvent playerClaimPlotEvent) {
        PlotPlayer plotPlayer = playerClaimPlotEvent.getPlotPlayer();
        Player player = Bukkit.getPlayer(plotPlayer.getUUID());
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if(plotPlayer.getAllowedPlots() > plotPlayer.getPlotCount()) {
            for (Plot plot : playerClaimPlotEvent.getPlotPlayer().getPlots()) {
                if (ReviewStatusFlag.isBeingReviewed(plot)) {
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.plot_being_reviewed_edit"));
                    playerClaimPlotEvent.setEventResult(Result.DENY);
                    return;
                }
            }
        }
    }

    @Subscribe
    public void OnPlotClear(PlotClearEvent plotClearEvent){
        Plot plot = plotClearEvent.getPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(plotClearEvent.getPlot().getOwner()));
        plotClearEvent.setEventResult(Result.DENY);
        if (ReviewStatusFlag.isAccepted(plot)){
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.plot_accepted_edit"));
            return;
        }
        if(ReviewStatusFlag.isBeingReviewed(plot)){
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.plot_being_reviewed_edit"));
            return;
        }
        else{
            plotClearEvent.setEventResult(Result.ACCEPT);
        }
    }

    @Subscribe
    public void onPlotDelete(PlotDeleteEvent plotDeleteEvent){
        Plot plot = plotDeleteEvent.getPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(plotDeleteEvent.getPlot().getOwner()));
        plotDeleteEvent.setEventResult(Result.DENY);
        if (ReviewStatusFlag.isAccepted(plot)){
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.plot_accepted_edit"));
            return;
        }
        if(ReviewStatusFlag.isBeingReviewed(plot)){
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.plot_being_reviewed_edit"));
            return;
        }
        else{
            ReviewAPI.getReviewPlot(plot).deleteReviewPlotData();
            plotDeleteEvent.setEventResult(Result.ACCEPT);
        }
    }
}
