package com.mcmiddleearth.plotsquared.review.plot;

import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.command.ReviewCommands;
import com.mcmiddleearth.plotsquared.plotflag.ReviewRatingDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatus;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewTimeDataFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.mcmiddleearth.plotsquared.util.FileManagement;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import static com.mcmiddleearth.plotsquared.review.ReviewPlayer.Template.templateOf;

public class ReviewPlot implements Serializable {
    protected final String stringPlotId;
    protected int reviewIteration;
    protected HashMap<java.util.UUID, Integer> playerReviewIteration;
    protected ArrayList<Integer> plotTempRatings;
    protected ArrayList<String> plotFinalFeedback;
    protected ArrayList<Long> plotFinalRatings;
    protected ArrayList<Long> plotFinalReviewTimeStamps;
    private ReviewState reviewState;

    public ReviewPlot(Plot plot) {
        //if the reviewPlot is not yet saved to disk
        ReviewPlot reviewPlot = FileManagement.loadReviewPlot(plot);
        if (reviewPlot == null) {
            this.stringPlotId = plot.getId().toString();
            this.reviewIteration = 0;
            this.playerReviewIteration = new HashMap<>();
            this.plotTempRatings = new ArrayList<>();
            this.plotFinalFeedback = new ArrayList<>();
            this.plotFinalRatings = new ArrayList<>();
            this.plotFinalReviewTimeStamps = new ArrayList<>();
        } else {
            this.stringPlotId = reviewPlot.stringPlotId;
            this.reviewIteration = reviewPlot.reviewIteration;
            this.playerReviewIteration = reviewPlot.playerReviewIteration;
            this.plotTempRatings = reviewPlot.plotTempRatings;
            this.plotFinalFeedback = reviewPlot.plotFinalFeedback;
            this.plotFinalRatings = reviewPlot.plotFinalRatings;
            this.plotFinalReviewTimeStamps = reviewPlot.plotFinalReviewTimeStamps;
            this.reviewState = reviewPlot.reviewState;
        }
    }

    public void submit() {
        this.reviewIteration += 1;
        saveToDisk();
        ReviewAPI.addReviewPlotToBeReviewed(this.getPlotId(), this);
        blockBuilding();
        if (reviewIteration % 2 == 0) {
            this.reviewState = new FeedBackState(this);
            this.getPlot().setFlag(ReviewStatusFlag.BEING_GIVEN_RATING_FLAG);
        } else {
            this.reviewState = new RatingState(this);
            this.getPlot().setFlag(ReviewStatusFlag.BEING_GIVEN_FEEDBACK_FLAG);
        }
    }

    public void endPlotReview() {
        reviewState.normalEndReview();
    }

    protected void blockBuilding() {
        long flagValue = System.currentTimeMillis() / 1000;
        PlotFlag<?, ?> doneFlag = this.getPlot().getFlagContainer().getFlag(DoneFlag.class).createFlagInstance(Long.toString(flagValue));
        this.getPlot().setFlag(doneFlag);
    }

    protected void allowBuilding() {
        this.getPlot().removeFlag(DoneFlag.class);
    }

    public void reset() {
        allowBuilding();
        ReviewAPI.removeReviewPlot(this);
        FileManagement.deleteReviewPlotFromDisk(this);
        this.getPlot().removeFlag(ReviewRatingDataFlag.class);
        this.getPlot().setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
    }

    protected void saveToDisk() {
        File file = new File(MCMEP2.getReviewPlotDirectory(), File.separator + stringPlotId + ".yml");
        FileManagement.writeObjectToFile(this, file);
    }

    /**
     * Adds ratings of reviewPlayers to array;
     * @param reviewPlayer reviewPlayer;
     */
    public void addTempRatings(ReviewPlayer reviewPlayer) {
        if (reviewPlayer.getPlotRating() == null) return;
        plotTempRatings.add(reviewPlayer.getPlotRating());
        this.setPlayerReviewAmounts(reviewPlayer);
    }

    /**
     * Adds feedbacks of reviewPlayers to array;
     * @param reviewPlayer reviewPlayer;
     */
    public void addFeedback(ReviewPlayer reviewPlayer) {
        if (reviewPlayer.getPlotFeedback() == null) return;
        plotFinalFeedback.add(reviewPlayer.getPlotFeedback());
        this.setPlayerReviewAmounts(reviewPlayer);
    }

    /**
     * Sets the amount of times a player has reviewed this plot
     * @param reviewPlayer reviewPlayer
     */
    private void setPlayerReviewAmounts(ReviewPlayer reviewPlayer) {
        playerReviewIteration.put(reviewPlayer.getUniqueId(), reviewIteration);
    }

    public PlotId getPlotId() {
        return PlotId.fromString(stringPlotId);
    }

    /**
     * Gets the amount of times a ReviewPlayer has reviewed this ReviewPlot
     * @param reviewPlayer
     * @return 0 if the ReviewPlayer has not yet reviewed the plot, otherwise the number of times he's reviewed it.
     */
    private int getPlayerReviewIteration(ReviewPlayer reviewPlayer) {
        Integer iteration = playerReviewIteration.get(reviewPlayer.getUniqueId());
        if (iteration == null) return 0;
        else return iteration;
    }

    /**
     * UNFINISHED
     * @param player
     * @param plot
     */
    public void enoughComplexity(Player player, Plot plot) {
        ReviewCommands.submitForReviewComplexity(player, plot, true);
        // yeah idk how to do this shit IMPLEMENT LATER

//        Settings.Auto_Clear settings = new Settings.Auto_Clear();
//        Config.ConfigBlock<Settings.Auto_Clear>  AUTO_CLEAR = new Config.ConfigBlock<>();
//        final Settings.Auto_Clear doneRequirements = AUTO_CLEAR.get("done");
//        if (doneRequirements == null) {
//            ReviewCommands.submitForRatingComplexity(player, true);
//            plot.removeRunning();
//        }
//        else {
//            HybridUtils hybridUtils = PlotSquared.platform().hybridUtils();
//            hybridUtils.analyzePlot(plot, new RunnableVal<>() {
//                @Override
//                public void run(PlotAnalysis value) {
//                    plot.removeRunning();
//                    boolean result =
//                            value.getComplexity(doneRequirements) <= doneRequirements.THRESHOLD;
//                    ReviewCommands.submitForRatingComplexity(player, result);
//                }
//            });
//        }
    }

    public boolean isBeingGivenRating() {
        return getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.BEING_GIVEN_RATING;
    }

    public boolean isBeingGivenFeedback() {
        return getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.BEING_GIVEN_FEEDBACK;
    }

    public boolean isBeingReviewed() {
        return isBeingGivenFeedback() || isBeingGivenRating();
    }

    public boolean isAccepted() {
        if (getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.ACCEPTED) return true;
        if (getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.ACCEPTED_NOT_SEEN) return true;
        return getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.LOCKED;
    }

    public boolean wasReviewedBy(ReviewPlayer reviewPlayer) {
        if (this.getOwner() == reviewPlayer.getUniqueId()) return true;
        return reviewIteration == this.getPlayerReviewIteration(reviewPlayer);
    }

    public List<String> getPlotFinalFeedback() {
        return plotFinalFeedback;
    }

    public UUID getOwner() {
        return this.getPlot().getOwner();
    }

    public int getReviewIteration() {
        return reviewIteration;
    }

    public HashMap<UUID, Integer> getPlayerReviewIterationMap() {
        return playerReviewIteration;
    }

    public ArrayList<Integer> getPlotTempRatings() {
        return plotTempRatings;
    }

    public List<Long> getFinalRatings() {
        if (this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.ACCEPTED || this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.LOCKED) {
            return this.getPlot().getFlag(ReviewRatingDataFlag.class);
        }
        return plotFinalRatings;
    }

    public List<Long> getFinalReviewTimeStamps() {
        if (this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.ACCEPTED || this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.LOCKED) {
            return this.getPlot().getFlag(ReviewTimeDataFlag.class);
        }
        return plotFinalReviewTimeStamps;
    }

    /**
     * Gets the PlotSquaredPlot from a ReviewPlot
     * @return PlotSquared Plot
     */
    public Plot getPlot() {
        return MCMEP2.getPlotAPI().getPlotSquared().getPlotAreaManager().getPlotArea(MCMEP2.getPlotWorld(), stringPlotId).getPlot(getPlotId());
    }

    /**
     * Gives the time since the last review of the plot.
     * @return 0 If the plot hasn't been reviewed. The time since review otherwise.
     */
    public long getTimeOfLastReview() {
        if (isAccepted()) {
            return this.getPlot().getFlag(ReviewTimeDataFlag.class).get(this.getPlot().getFlag(ReviewTimeDataFlag.class).size() - 1);
        }
        if (this.plotFinalReviewTimeStamps.size() == 0) return 0;
        else return plotFinalReviewTimeStamps.get(plotFinalReviewTimeStamps.size() - 1);
    }

    public void forceEndReview() {
        reviewState.normalEndReview();
        if(isBeingReviewed()) {
            if (isBeingGivenRating()) {
                if (getPlotTempRatings().size() == 0) {
                    plotTempRatings.add(50);
                }
            }
            reviewState.forceEndReview();
        }
    }
}
