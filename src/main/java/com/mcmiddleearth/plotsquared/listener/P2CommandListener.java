package main.java.com.mcmiddleearth.plotsquared.listener;

import com.google.common.eventbus.Subscribe;
import com.plotsquared.core.PlotAPI;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.events.*;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import main.java.com.mcmiddleearth.plotsquared.review.ReviewAPI;
import main.java.com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static main.java.com.mcmiddleearth.plotsquared.review.ReviewPlayer.Template.templateOf;


public class P2CommandListener {

    public P2CommandListener() {
        PlotAPI api = new PlotAPI();
        api.registerListener(this);
    }

    @Subscribe
    public void onPlotDone(PlotDoneEvent plotDoneEvent) {
        plotDoneEvent.setEventResult(Result.DENY);
    }

    @Subscribe
    public void onPlayerPlotClaim(PlayerClaimPlotEvent playerClaimPlotEvent) {
        PlotPlayer<?> plotPlayer = playerClaimPlotEvent.getPlotPlayer();
        Player player = Bukkit.getPlayer(plotPlayer.getUUID());
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        int amountBeingReviewed = 0;
        for (Plot plot : plotPlayer.getPlots()) {
            if (ReviewAPI.getReviewPlot(plot).isBeingReviewed()) {
                amountBeingReviewed += 1;
            }
        }
        // allowed plots > total plots - accepted - being reviewed + being reviewed
        if (plotPlayer.getAllowedPlots() > plotPlayer.getPlotCount() + amountBeingReviewed) {
            for (Plot plot : playerClaimPlotEvent.getPlotPlayer().getPlots()) {
                if (ReviewAPI.getReviewPlot(plot).isBeingReviewed()) {
                    reviewPlayer.sendMessage(TranslatableCaption.of("permission.cant_claim_more_plots"),
                            templateOf("amount", String.valueOf(plotPlayer.getAllowedPlots())));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.info.submit_to_get_more"));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.info.submit_to_get_more_2"));
                    playerClaimPlotEvent.setEventResult(Result.DENY);
                    return;
                }
            }
        }
    }

    @Subscribe
    public void OnPlotClear(PlotClearEvent plotClearEvent) {
        Plot plot = plotClearEvent.getPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(plotClearEvent.getPlot().getOwner()));
        if (ReviewAPI.getReviewPlot(plot).isAccepted()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.plot_accepted_edit"));
            plotClearEvent.setEventResult(Result.DENY);
            return;
        }
        if (ReviewAPI.getReviewPlot(plot).isBeingReviewed()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.plot_being_reviewed_edit"));
            plotClearEvent.setEventResult(Result.DENY);
        } else {
            ReviewAPI.getReviewPlot(plot).reset();
            plotClearEvent.setEventResult(Result.ACCEPT);
        }
    }

    @Subscribe
    public void onPlotDelete(PlotDeleteEvent plotDeleteEvent) {
        Plot plot = plotDeleteEvent.getPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(plotDeleteEvent.getPlot().getOwner()));
        if (ReviewAPI.getReviewPlot(plot).isAccepted()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.plot_accepted_edit"));
            plotDeleteEvent.setEventResult(Result.DENY);
            return;
        }
        if (ReviewAPI.getReviewPlot(plot).isBeingReviewed()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.plot_being_reviewed_edit"));
            plotDeleteEvent.setEventResult(Result.DENY);
        } else {
            ReviewAPI.getReviewPlot(plot).reset();
            plotDeleteEvent.setEventResult(Result.ACCEPT);
        }
    }
}
