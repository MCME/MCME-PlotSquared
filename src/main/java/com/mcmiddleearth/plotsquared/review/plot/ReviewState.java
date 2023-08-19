package com.mcmiddleearth.plotsquared.review.plot;

import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;

public abstract class ReviewState {
    protected ReviewPlot reviewPlot;

    public ReviewState(ReviewPlot reviewPlot){
        this.reviewPlot = reviewPlot;
    }

    public abstract void normalEndReview();

    public abstract void forceEndReview();

    public abstract boolean passedRequirements();

    /**
     * Checks whether this ReviewPlot is in queue to be reviewed of a current ReviewParty.
     * @return true if the ReviewPlot is queued by a current ReviewParty.
     */
    public boolean isQueuedForReview() {
        for (ReviewParty i : ReviewAPI.getReviewParties().values()) {
            if (i.getReviewPlotLinkedList().contains(this.reviewPlot)) return true;
        }
        return false;
    }
}
