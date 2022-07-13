package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import org.bukkit.entity.Player;
import java.util.HashMap;
import java.util.UUID;

public class ReviewAPI {
    private static HashMap<UUID, ReviewPlayer> reviewers = new HashMap<>();
    private static HashMap<UUID, ReviewParty> reviewParties = new HashMap<>();
    private static HashMap<PlotId, Plot> currentPlots = new HashMap<>();

    public static HashMap<UUID, ReviewPlayer> getReviewers() {
        return reviewers;
    }

    public static HashMap<UUID, ReviewParty> getReviewParties() {
        return reviewParties;
    }

    public static void addReviewPlayer(ReviewPlayer reviewPlayer) {
        reviewers.put(reviewPlayer.getUniqueId(), reviewPlayer);
    }

    public static void addReviewParty(ReviewParty reviewParty) {
        reviewParties.put(reviewParty.getReviewerLeader().getUniqueId(), reviewParty);
    }

    public static void removeReviewPlayer(ReviewPlayer reviewPlayer) {
        reviewers.remove(reviewPlayer.getUniqueId(), reviewPlayer);
    }

    public static void removeReviewParty(ReviewParty reviewParty) {
        reviewParties.remove(reviewParty.getReviewerLeader().getUniqueId(), reviewParty);
    }

    public static boolean isReviewing(Player player) { return reviewers.containsKey(player.getUniqueId()); }

    public static ReviewPlayer getReviewPlayer(Player player){
        return reviewers.get(player.getUniqueId());
    }

    public static boolean isReviewPlayer(Player player){
        return reviewers.containsKey(player.getUniqueId());
    }

    public static HashMap<PlotId, Plot> getCurrentPlots() {
        return currentPlots;
    }
}
