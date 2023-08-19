package com.mcmiddleearth.plotsquared.review.plot;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewRatingDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewTimeDataFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.mcmiddleearth.plotsquared.util.FileManagement;
import org.bukkit.Bukkit;

import static com.mcmiddleearth.plotsquared.review.ReviewPlayer.Template.templateOf;

public class RatingState extends ReviewState {

    public RatingState(ReviewPlot reviewPlot) {
        super(reviewPlot);
    }

    private void endReview() {
        Plot plot = reviewPlot.getPlot();
        boolean ownerIsOnline = Bukkit.getOfflinePlayer(reviewPlot.getPlot().getOwner()).isOnline();
        ReviewAPI.removeReviewPlot(reviewPlot);
        if (calculateRating() < 50) {
            reviewPlot.allowBuilding();
            reviewPlot.plotFinalRatings.add(calculateRating());
            reviewPlot.plotFinalReviewTimeStamps.add(System.currentTimeMillis());
            reviewPlot.plotTempRatings.clear();
            reviewPlot.saveToDisk();
            if (ownerIsOnline) {
                reviewPlot.getPlot().setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
                sendRejectedMessage();
            } else reviewPlot.getPlot().setFlag(ReviewStatusFlag.REJECTED_NOT_SEEN_FLAG);
        }
        else {
            reviewPlot.blockBuilding();
            reviewPlot.plotFinalRatings.add(calculateRating());
            reviewPlot.plotFinalReviewTimeStamps.add(System.currentTimeMillis());
            //save data to flags in PlotSquared database and delete own data from disk
            plot.setFlag(new ReviewRatingDataFlag(reviewPlot.plotFinalRatings));
            plot.setFlag(new ReviewTimeDataFlag(reviewPlot.plotFinalReviewTimeStamps));
            FileManagement.deleteReviewPlotFromDisk(reviewPlot);
            if (ownerIsOnline) {
                reviewPlot.getPlot().setFlag(ReviewStatusFlag.LOCKED_FLAG);
                sendAcceptedMessage();
            } else plot.setFlag(ReviewStatusFlag.ACCEPTED_NOT_SEEN_FLAG);
        }
    }

    @Override
    public void normalEndReview() {
        if(isQueuedForReview() || !passedRequirements()) reviewPlot.saveToDisk();
        endReview();
    }

    @Override
    public void forceEndReview() {
        endReview();
    }

    private long calculateRating() {
        int ratingSum = 0;
        for (int i : reviewPlot.getPlotTempRatings()) {
            ratingSum += i;
        }
        return Math.floorDiv(ratingSum, reviewPlot.getPlotTempRatings().size());
    }

    private void sendAcceptedMessage() {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(reviewPlot.getPlot().getOwner()));
        String score = String.valueOf(reviewPlot.getFinalRatings().get(reviewPlot.getFinalRatings().size() - 1));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_accepted"), templateOf("rating", score));
        int allowedPlots = BukkitUtil.adapt(Bukkit.getPlayer(reviewPlot.getPlot().getOwner())).getAllowedPlots();
        if (allowedPlots == 1)
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plot"), templateOf("amount", String.valueOf(allowedPlots)));
        else
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plots"), templateOf("amount", String.valueOf(allowedPlots)));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
    }

    private void sendRejectedMessage() {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(reviewPlot.getPlot().getOwner()));
        String score = String.valueOf(reviewPlot.getFinalRatings().get(reviewPlot.getFinalRatings().size() - 1));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rejected"), templateOf("rating", score));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rejected_2"));
        int allowedPlots = BukkitUtil.adapt(Bukkit.getPlayer(reviewPlot.getPlot().getOwner())).getAllowedPlots();
        switch (allowedPlots) {
            case 0 -> reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.no_plot"));
            case 1 ->
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plot"), templateOf("amount", String.valueOf(allowedPlots)));
            default ->
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plots"), templateOf("amount", String.valueOf(allowedPlots)));
        }
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
    }

    /**
     * Checks if plot passed minimal time threshold and received the minimal amount of reviews.
     * @return true if passed
     */
    public boolean passedRequirements() {
        final int DAYINGMILISEC = 86400000;
        long timeSinceSubmitting = Long.parseLong(reviewPlot.getPlot().getFlag(DoneFlag.class)) * 1000;
        if (timeSinceSubmitting - System.currentTimeMillis() + DAYINGMILISEC <= 0) return reviewPlot.getPlotTempRatings().size() >= 5;
        return false;
    }
}
