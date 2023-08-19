package com.mcmiddleearth.plotsquared.review.plot;

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import org.bukkit.Bukkit;

public class FeedBackState extends ReviewState {

    public FeedBackState(ReviewPlot reviewPlot) {
        super(reviewPlot);
    }
    private void endReview(){
        boolean ownerIsOnline = Bukkit.getOfflinePlayer(reviewPlot.getPlot().getOwner()).isOnline();
        ReviewAPI.removeReviewPlot(reviewPlot);
        reviewPlot.allowBuilding();
        reviewPlot.saveToDisk();
        reviewPlot.plotFinalReviewTimeStamps.add(System.currentTimeMillis());
        if (ownerIsOnline) {
            reviewPlot.getPlot().setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
            sendFeedbackDoneMessage();
        } else {
            reviewPlot.getPlot().setFlag(ReviewStatusFlag.FEEDBACK_NOT_SEEN_FLAG);
        }
    }

    @Override
    public void normalEndReview() {
        if(isQueuedForReview() || !passedRequirements()) {
            reviewPlot.saveToDisk();
        } else {
            endReview();
        }
    }

    @Override
    public void forceEndReview() {
        endReview();
    }

    /**
     * Checks if plot passed minimal time threshold and received the minimal amount of reviews.
     * @return true if passed
     */
    public boolean passedRequirements() {
        final int DAYINGMILISEC = 86400000;
        long timeSinceSubmitting = Long.parseLong(reviewPlot.getPlot().getFlag(DoneFlag.class)) * 1000;
        if (timeSinceSubmitting - System.currentTimeMillis() + DAYINGMILISEC <= 0) return reviewPlot.getPlotFinalFeedback().size() >= 5;
        return false;
    }

    private void sendFeedbackDoneMessage() {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(reviewPlot.getPlot().getOwner()));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rating_received")); //TODO DOESNT MAKE SENSE TO GIVE RATING, SHOULD GIVE FEEDBACK INSTEAD!!!
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
    }
}
