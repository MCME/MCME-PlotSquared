package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

public class ReviewAPI {
    private static HashMap<UUID, ReviewPlayer> reviewerPlayers = new HashMap<>();
    private static HashMap<UUID, ReviewParty> reviewParties = new HashMap<>();
    private static HashMap<PlotId, ReviewPlot> reviewPlots = new HashMap<>();

    public static HashMap<UUID, ReviewPlayer> getReviewerPlayers() {
        return reviewerPlayers;
    }

    public static HashMap<UUID, ReviewParty> getReviewParties() {
        return reviewParties;
    }

    public static void addReviewPlayer(ReviewPlayer reviewPlayer) {
        reviewerPlayers.put(reviewPlayer.getUniqueId(), reviewPlayer);
    }

    public static void addReviewParty(ReviewParty reviewParty) {
        reviewParties.put(reviewParty.getReviewerLeader().getUniqueId(), reviewParty);
    }

    public static void removeReviewPlayer(ReviewPlayer reviewPlayer) {
        reviewerPlayers.remove(reviewPlayer.getUniqueId(), reviewPlayer);
    }

    public static void removeReviewParty(ReviewParty reviewParty) {
        reviewParties.remove(reviewParty.getReviewerLeader().getUniqueId(), reviewParty);
    }

    public static ReviewPlayer getReviewPlayer(Player player){
        if (reviewerPlayers.containsKey(player.getUniqueId())){
                return reviewerPlayers.get(player.getUniqueId());
        }
        else return new ReviewPlayer(player);
    }

    public static boolean isReviewPlayer(Player player){
        return reviewerPlayers.containsKey(player.getUniqueId());
    }

    public static ReviewPlot getReviewPlot(Plot plot){
        if (reviewPlots.containsKey(plot.getId())){
            return reviewPlots.get(plot.getId());
        }
        else return new ReviewPlot(plot);
    }

    public static Collection<ReviewPlot> getReviewPlotsCollection() {
        return reviewPlots.values();
    }

    public static void addReviewPlot(PlotId plotId, ReviewPlot reviewPlot) {
        reviewPlots.put(plotId, reviewPlot);
    }

    public static void removeReviewPlot(ReviewPlot reviewPlot) {
        reviewPlots.remove(reviewPlot.getId());
    }
}
