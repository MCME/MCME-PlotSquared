package main.java.com.mcmiddleearth.plotsquared.review;

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import main.java.com.mcmiddleearth.plotsquared.review.plot.ReviewPlot;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import static main.java.com.mcmiddleearth.plotsquared.review.ReviewPlayer.Template.templateOf;

public class ReviewAPI {
    private static HashMap<UUID, ReviewPlayer> reviewerPlayers = new HashMap<>();
    private static HashMap<UUID, ReviewParty> reviewParties = new HashMap<>();
    private static HashMap<PlotId, ReviewPlot> reviewPlots = new HashMap<>();
    private static HashMap<UUID, UUID> invites = new HashMap<>(); // implement invites

    public static HashMap<Player, ReviewAPI.storeData> commandConfirm = new HashMap<>();

    public enum ReviewCommands {
        reviewClear,
        reviewDelete,
        reviewRestart,
        reviewConfirm,
        reviewForce
    }

    public static class storeData {
        public Plot storedPlot;
        public ReviewCommands storedCommand;

        public storeData(Plot plot, ReviewCommands storedCommand) {
            this.storedPlot = plot;
            this.storedCommand = storedCommand;
        }

        public ReviewCommands getStoredCommand() {
            return storedCommand;
        }

        public Plot getStoredPlot() {
            return storedPlot;
        }
    }

    public static void startReviewParty(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewing"));
            return;
        }
        if (ReviewAPI.getReviewPlotsToBeReviewed().isEmpty()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.no_plots"));
            return;
        }
        for (ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsToBeReviewed()) {
            if (!reviewPlot.wasReviewedBy(reviewPlayer)) {
                ReviewParty.startReviewParty(reviewPlayer);
                reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.start"));
                return;
            }
        }
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.reviewed_all_plots"));
    }

    public static void stopReviewParty(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        reviewPlayer.getReviewParty().stopReviewParty();
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.end"));
    }

    public static void reviewJoin(Player player, Player target) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
        if (reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewing"));
            return;
        }
        if (!reviewTarget.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewing_other"));
            return;
        }
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.join")); //IMPLEMENT// TODO
        for (ReviewPlayer i : reviewPlayer.getReviewParty().getReviewPlayers()) {
            i.sendMessage(TranslatableCaption.of("mcme.review.joined_notif"), templateOf("player", player.getName())); //IMPLEMENT// TODO
        }
        reviewTarget.getReviewParty().addReviewPlayer(reviewPlayer);
    }

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

    public static ReviewPlayer getReviewPlayer(Player player) {
        if (reviewerPlayers.containsKey(player.getUniqueId())) {
            return reviewerPlayers.get(player.getUniqueId());
        } else return new ReviewPlayer(player);
    }

    public static boolean isReviewPlayer(Player player) {
        return reviewerPlayers.containsKey(player.getUniqueId());
    }

    public static ReviewPlot getReviewPlot(Plot plot) {
        if (reviewPlots.containsKey(plot.getId())) {
            return reviewPlots.get(plot.getId());
        } else return new ReviewPlot(plot);
    }

    public static boolean isToBeReviewed(ReviewPlot reviewPlot) {
        return reviewPlots.containsKey(reviewPlot.getPlotId());
    }

    public static Collection<ReviewPlot> getReviewPlotsToBeReviewed() {
        return reviewPlots.values();
    }

    public static void addReviewPlotToBeReviewed(PlotId plotId, ReviewPlot reviewPlot) {
        reviewPlots.put(plotId, reviewPlot);
    }

    public static void removeReviewPlot(ReviewPlot reviewPlot) {
        reviewPlots.remove(reviewPlot.getPlotId());
    }
}
