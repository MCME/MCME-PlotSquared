package com.mcmiddleearth.plotsquared.review;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.plotflag.ReviewDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.util.FlatFile;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class ReviewPlot implements Serializable {
    private final String plotId;
    private HashMap<java.util.UUID, Integer> playerReviewIteration;
    private HashSet<Integer> plotTempRatings;
    private LinkedList<String> plotFinalFeedback;
    private LinkedList<Long> plotFinalRatings;
    private LinkedList<Long> plotFinalReviewTimeStamps;


    public ReviewPlot(Plot plot){
        //if the reviewPlot is not yet saved to disk
        ReviewPlot reviewPlot = loadReviewPlotData(plot);
        if (reviewPlot == null){
            this.plotId = plot.getId().toString();
            this.playerReviewIteration = new HashMap<>();
            this.plotTempRatings = new HashSet<>();
            this.plotFinalFeedback = new LinkedList<>();
            this.plotFinalRatings = new LinkedList<>();
            this.plotFinalReviewTimeStamps = new LinkedList<>();
        }
        else{
            this.plotId = reviewPlot.plotId;
            this.playerReviewIteration = reviewPlot.playerReviewIteration;
            this.plotTempRatings = reviewPlot.plotTempRatings;
            this.plotFinalFeedback = reviewPlot.plotFinalFeedback;
            this.plotFinalRatings = reviewPlot.plotFinalRatings;
            this.plotFinalReviewTimeStamps = reviewPlot.plotFinalReviewTimeStamps;
        }
    }

    public ReviewPlot(ReviewPlot reviewPlot){
        //if the reviewPlot is not yet saved to disk
            this.plotId = reviewPlot.plotId;
            this.playerReviewIteration = reviewPlot.playerReviewIteration;
            this.plotTempRatings = reviewPlot.plotTempRatings;
            this.plotFinalFeedback = reviewPlot.plotFinalFeedback;
            this.plotFinalRatings = reviewPlot.plotFinalRatings;
            this.plotFinalReviewTimeStamps = reviewPlot.plotFinalReviewTimeStamps;
    }

    public enum ReviewStatus{
        BEING_REVIEWED,
        NOT_BEING_REVIEWED,
        ACCEPTED,
        REJECTED,
        LOCKED,
        TOO_EARLY
    }

    public void endPlotReview(ReviewParty reviewParty) {
        addFeedback(reviewParty.getFeedbacks());
        addTempRatings(reviewParty.getPlotRatings());
        setPlayerReviewAmounts(reviewParty.getAllReviewers());

        ReviewStatus reviewStatus = getReviewStatus();
        Plot plot = getPlot();
        switch(reviewStatus){
            case BEING_REVIEWED:
                getLogger().info("Being reviewed");
                //someone is still reviewing this plot lets handle it after everyone is done.
                break;
            case TOO_EARLY:
                //save file with no conclusion, review process continues.
                getLogger().info("Too early");
                this.saveReviewPlotData();
                break;
            case REJECTED:
                getLogger().info("Rejected");
                ReviewAPI.removeReviewPlot(this);
                //save file with fail
                this.saveReviewPlotData();
                //set reviewFlag to false (end review process)
                this.getPlot().setFlag(ReviewStatusFlag.REJECTED_FLAG);
                this.plotTempRatings.clear();
                break;
            case ACCEPTED:
                getLogger().info("Accepted");
                ReviewAPI.removeReviewPlot(this);
                //save data to flag and delete misc data from disk
                plot.getFlag(ReviewDataFlag.class).addAll(preparedReviewData());
                deleteReviewPlotData();
                //set plot to done
                long flagValue = System.currentTimeMillis() / 1000;
                PlotFlag<?, ?> plotFlag = plot.getFlagContainer().getFlag(DoneFlag.class)
                        .createFlagInstance(Long.toString(flagValue));
                plot.setFlag(plotFlag);
                //set plot to ACCEPTED
                plot.setFlag(ReviewStatusFlag.ACCEPTED_FLAG);
                this.plotTempRatings.clear();
                break;
        }
    }

    public void preemptPlotReview(ReviewParty reviewParty) {
        ReviewStatus reviewStatus = getReviewStatus();
        Plot plot = getPlot();
        switch(reviewStatus){
            case BEING_REVIEWED:
                getLogger().info("Being reviewed");
                //someone is still reviewing this plot lets handle it after everyone is done.
                break;
            case TOO_EARLY:
                //save file with no conclusion, review process continues.
                getLogger().info("Too early");
                this.saveReviewPlotData();
                break;
            case REJECTED:
                getLogger().info("Rejected");
                ReviewAPI.removeReviewPlot(this);
                //save file with fail
                this.saveReviewPlotData();
                //set reviewFlag to false (end review process)
                this.getPlot().setFlag(ReviewStatusFlag.REJECTED_FLAG);
                this.plotTempRatings.clear();
                break;
            case ACCEPTED:
                getLogger().info("Accepted");
                ReviewAPI.removeReviewPlot(this);
                //save data to flag and delete misc data from disk
                plot.getFlag(ReviewDataFlag.class).addAll(preparedReviewData());
                deleteReviewPlotData();
                //set plot to done
                long flagValue = System.currentTimeMillis() / 1000;
                PlotFlag<?, ?> plotFlag = plot.getFlagContainer().getFlag(DoneFlag.class)
                        .createFlagInstance(Long.toString(flagValue));
                plot.setFlag(plotFlag);
                //set plot to ACCEPTED
                plot.setFlag(ReviewStatusFlag.ACCEPTED_FLAG);
                this.plotTempRatings.clear();
                break;
        }
    }

    private List<Long> preparedReviewData() {
        List<Long> reviewDataList = new ArrayList<>();
        reviewDataList.addAll(plotFinalRatings);
        reviewDataList.addAll(plotFinalReviewTimeStamps);
        return reviewDataList;
    }

    public ReviewStatus getReviewStatus() {
        for (ReviewParty i : ReviewAPI.getReviewParties().values()){
            if(i.getReviewPlotLinkedList().contains(this)) return ReviewStatus.BEING_REVIEWED;
        }
        if(passedTimeThreshold() && passedRatingThreshold()) return ReviewStatus.ACCEPTED;
        if(!passedTimeThreshold()) return ReviewStatus.TOO_EARLY;
        return ReviewStatus.REJECTED;
    }

    /**
     * Checks if plot passed minimal time threshold.
     * @return true if passed
     */
    private boolean passedTimeThreshold() {
        if(plotTempRatings.size()<5) return false; // if less than 5 people reviewed the plot
        final int DAYINSECONDS = 86400;
        return plotFinalReviewTimeStamps.get(plotFinalReviewTimeStamps.size() - 1) <= ((System.currentTimeMillis() / 1000) - DAYINSECONDS);
    }

    /**
     * Checks if plot passed minimal rating threshold.
     * @return true if passed
     */
    public boolean passedRatingThreshold(){
        if(plotTempRatings.size()<5) return false; // if less than 5 people reviewed the plot
        int ratingSum = 0;
        int count = 0;
        for(int i : plotTempRatings){
            ratingSum += i;
            count += 1;
        }
        int rating = Math.floorDiv(ratingSum, count);
        int plotFinalReviewTimes = plotFinalRatings.size() - 1;
        int leniencyFactor = (plotFinalReviewTimes) * 5;
        if(plotFinalReviewTimes > 4) leniencyFactor = 15;
        return rating >= 75 - (leniencyFactor);
    }

    /**
     * Adds ratings of reviewPlayers in reviewParty to array;
     * @param ratingList list of ratings in reviewParty;
     */
    public void addTempRatings(HashSet<Integer> ratingList){
        plotTempRatings.addAll(ratingList);
    }

    /**
     * Adds feedbacks of reviewPlayers in reviewParty to array;
     * @param feedbackList list of feedbacks in reviewParty;
     */
    public void addFeedback(HashSet<String> feedbackList) {
        plotFinalFeedback.addAll(feedbackList);
    }


    /**
     * Sets the amount of times a player has reviewed this plot
     * @param reviewPlayers list of reviewPlayers in reviewParty
     */
    public void setPlayerReviewAmounts(HashSet<ReviewPlayer> reviewPlayers){
        for(ReviewPlayer i: reviewPlayers){
            playerReviewIteration.put(i.getUniqueId(), plotFinalRatings.size() + 1);
        }
    }

    public void saveReviewPlotData() {
        String plotId = this.getId().toString();
        File file = new File(MCMEP2.getReviewPlotDirectory().toString() + File.separator + plotId + ".yml");
        FlatFile.writeObjectToFile(this, file);
    }

    public ReviewPlot loadReviewPlotData() {
        String plotId = this.getId().toString();
        File file = new File(MCMEP2.getReviewPlotDirectory().toString() + File.separator + plotId + ".yml");
        if (!file.exists()) return null;
        else return FlatFile.readObjectFromFile(file);
    }

    public static ReviewPlot loadReviewPlotData(Plot plot){
        String plotId = plot.getId().toString();
        File file = new File(MCMEP2.getReviewPlotDirectory().toString() + File.separator + plotId + ".yml");
        if (!file.exists()) return null;
        else return FlatFile.readObjectFromFile(file);
    }

    private void deleteReviewPlotData() {
        File reviewPlotYamlFile = new File(MCMEP2.getReviewPlotDirectory().toString() + File.separator + plotId + ".yml");
        reviewPlotYamlFile.delete();
    }

    public Plot getPlot() {
        String plotIdString = plotId.toString();
        return MCMEP2.getPlotAPI().getPlotSquared().getPlotAreaManager().getPlotArea(MCMEP2.getPlotWorld(), plotIdString).getPlot(getId());
    }

    public long getTimeSinceLastReview(){
        if(this.plotFinalReviewTimeStamps.size() == 0) return 0;
        else return plotFinalReviewTimeStamps.get(plotFinalReviewTimeStamps.size() - 1);
    }

    public PlotId getId() { return PlotId.fromString(plotId); }

    public int getPlayerReviewIteration(ReviewPlayer reviewPlayer){
        Integer iteration = playerReviewIteration.get(reviewPlayer.getUniqueId());
        if (iteration == null) return  0;
        else return iteration;
    }

    public int getReviewIteration(){
        if (plotFinalRatings.isEmpty()) return 0;
        else return plotFinalRatings.size();
    }

    public HashMap<UUID, Integer> getPlayerReviewIterationMap(){
        return playerReviewIteration;
    }

    public HashSet<Integer> getPlotTempRatings() {
        return plotTempRatings;
    }

    public LinkedList<Long> getPlotFinalRatings() {
        return plotFinalRatings;
    }

    public LinkedList<Long> getPlotFinalReviewTimeStamps() {
        return plotFinalReviewTimeStamps;
    }

    public LinkedList<String> getPlotFinalFeedback() {
        return plotFinalFeedback;
    }

}
