package com.mcmiddleearth.plotsquared.review;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.plotflag.ReviewDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.util.FileManagement;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static org.bukkit.Bukkit.getLogger;

public class ReviewPlot implements Serializable {
    private final String stringPlotId;
    private HashMap<java.util.UUID, Integer> playerReviewIteration;
    private HashSet<Integer> plotTempRatings;
    private LinkedList<String> plotFinalFeedback;
    private LinkedList<Long> plotFinalRatings;
    private LinkedList<Long> plotFinalReviewTimeStamps;

    public enum ReviewStatus{
        BEING_REVIEWED,
        NOT_BEING_REVIEWED,
        ACCEPTED,
        REJECTED,
        LOCKED,
        TOO_EARLY
    }

    public ReviewPlot(Plot plot){
        //if the reviewPlot is not yet saved to disk
        ReviewPlot reviewPlot = loadReviewPlotData(plot);
        if (reviewPlot == null){
            this.stringPlotId = plot.getId().toString();
            this.playerReviewIteration = new HashMap<>();
            this.plotTempRatings = new HashSet<>();
            this.plotFinalFeedback = new LinkedList<>();
            this.plotFinalRatings = new LinkedList<>();
            this.plotFinalReviewTimeStamps = new LinkedList<>();
        }
        else{
            this.stringPlotId = reviewPlot.stringPlotId;
            this.playerReviewIteration = reviewPlot.playerReviewIteration;
            this.plotTempRatings = reviewPlot.plotTempRatings;
            this.plotFinalFeedback = reviewPlot.plotFinalFeedback;
            this.plotFinalRatings = reviewPlot.plotFinalRatings;
            this.plotFinalReviewTimeStamps = reviewPlot.plotFinalReviewTimeStamps;
        }
    }

    public ReviewPlot(ReviewPlot reviewPlot){
        //if the reviewPlot is not yet saved to disk
            this.stringPlotId = reviewPlot.stringPlotId;
            this.playerReviewIteration = reviewPlot.playerReviewIteration;
            this.plotTempRatings = reviewPlot.plotTempRatings;
            this.plotFinalFeedback = reviewPlot.plotFinalFeedback;
            this.plotFinalRatings = reviewPlot.plotFinalRatings;
            this.plotFinalReviewTimeStamps = reviewPlot.plotFinalReviewTimeStamps;
    }

    public void endPlotReview(ReviewParty reviewParty) {
        if(!reviewParty.getFeedbacks().isEmpty()) {
            addFeedback(reviewParty.getFeedbacks());
            addTempRatings(reviewParty.getPlotRatings());
            setPlayerReviewAmounts(reviewParty.getAllReviewers());
        }
        ReviewStatus reviewStatus = this.getReviewStatus();
        Plot plot = this.getPlot();
        switch (reviewStatus) {
            case BEING_REVIEWED -> getLogger().info("Being reviewed");
            case TOO_EARLY -> {
                //save file with no conclusion, review process continues.
                getLogger().info("Too early");
                this.saveReviewPlotData();
            }
            case REJECTED -> {
                getLogger().info("Rejected");
                ReviewAPI.removeReviewPlot(this);
                int ratingSum = 0;
                int count = 0;
                for(int i : plotTempRatings){
                    ratingSum += i;
                    count += 1;
                }
                long rating = Math.floorDiv(ratingSum, count);
                plotFinalRatings.add(rating);
                plotFinalReviewTimeStamps.add(System.currentTimeMillis());
                //save file with
                this.plotTempRatings.clear();
                this.saveReviewPlotData();
                //set reviewFlag to false (end review process)
                this.getPlot().setFlag(ReviewStatusFlag.REJECTED_FLAG);
            }
            case ACCEPTED -> {
                getLogger().info("Accepted");
                ReviewAPI.removeReviewPlot(this);
                int ratingSum = 0;
                int count = 0;
                for(int i : plotTempRatings){
                    ratingSum += i;
                    count += 1;
                }
                long rating = Math.floorDiv(ratingSum, count);
                plotFinalRatings.add(rating);
                plotFinalReviewTimeStamps.add(System.currentTimeMillis());
                List<Long> reviewDataList = new ArrayList<>();
                reviewDataList.addAll(plotFinalRatings);
                reviewDataList.addAll(plotFinalReviewTimeStamps);
                //save data to flag and delete data from disk
                PlotFlag<?, ?> reviewDataFlag = plot.getFlagContainer().getFlag(ReviewDataFlag.class).createFlagInstance(reviewDataList);
                plot.setFlag(reviewDataFlag);
                this.deleteReviewPlotData();
                //set plot to done
                long flagValue = System.currentTimeMillis() / 1000;
                PlotFlag<?, ?> doneFlag = plot.getFlagContainer().getFlag(DoneFlag.class).createFlagInstance(Long.toString(flagValue));
                plot.setFlag(doneFlag);
                //set plot to ACCEPTED
                plot.setFlag(ReviewStatusFlag.ACCEPTED_FLAG);
                this.plotTempRatings.clear();
            }
        }
    }

    public ReviewStatus getReviewStatus() {
        if(isBeingReviewed()) return ReviewStatus.BEING_REVIEWED;
        if(!passedTimeThreshold()) return ReviewStatus.TOO_EARLY;
        if(passedTimeThreshold() && passedRatingThreshold()) return ReviewStatus.ACCEPTED;
        return ReviewStatus.REJECTED;
    }

    public void submitReviewPlot(Plot plot) {
        File file = new File(MCMEP2.getReviewPlotDirectory().toString() + plot.getId().toString() + ".yml");
        FileManagement.writeObjectToFile(this, file);
        ReviewAPI.addReviewPlot(this.getPlotId(), this);
    }

    /**
     * Checks if plot passed minimal time threshold.
     * @return true if passed
     */
    private boolean passedTimeThreshold() {
        if(plotTempRatings.size()<1) return false; // if less than 5 people reviewed the plot //REDUCED TO 3 for debug reasons
        final int DAYINGMILISEC = 60000;//made one minute for debug reasons 86400000
//        if (plotFinalReviewTimeStamps.size() == 0){
//            return false;
//        }
//        else;
//            return plotFinalReviewTimeStamps.get(plotFinalReviewTimeStamps.size() - 1) <= ((System.currentTimeMillis() ) - DAYINSECONDS);
        return true;
    }

    /**
     * Checks if plot passed minimal rating threshold.
     * @return true if passed
     */
    public boolean passedRatingThreshold(){
        if(plotTempRatings.size()<1) return false; // if less than 5 people reviewed the plot //REDUCED TO 3 for debug reasons
        int ratingSum = 0;
        int count = 0;
        for(int i : plotTempRatings){
            ratingSum += i;
            count += 1;
        }
        int rating = Math.floorDiv(ratingSum, count);
        if (plotFinalRatings.size() == 0){
            return rating >= 75;
        }
        else {
            int plotFinalReviewTimes = plotFinalRatings.size() - 1;
            int leniencyFactor = (plotFinalReviewTimes) * 5;
            if (plotFinalReviewTimes > 4) leniencyFactor = 15;
            return rating >= 75 - (leniencyFactor);
        }
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
        File file = new File(MCMEP2.getReviewPlotDirectory().toString() + File.separator + stringPlotId + ".yml");
        FileManagement.writeObjectToFile(this, file);
    }

    public ReviewPlot loadReviewPlotData() {
        File file = new File(MCMEP2.getReviewPlotDirectory().toString() + File.separator + stringPlotId + ".yml");
        if (!file.exists()) return null;
        else return FileManagement.readObjectFromFile(file);
    }

    public static ReviewPlot loadReviewPlotData(Plot plot){
        String plotId = plot.getId().toString();
        File file = new File(MCMEP2.getReviewPlotDirectory().toString() + File.separator + plotId + ".yml");
        if (!file.exists()) return null;
        else return FileManagement.readObjectFromFile(file);
    }

    public void deleteReviewPlotData() {
        File reviewPlotYamlFile = new File(MCMEP2.getReviewPlotDirectory().toString() + File.separator + stringPlotId + ".yml");
        if (reviewPlotYamlFile.delete()) {
            getLogger().info("Deleted the file: " + reviewPlotYamlFile.getName());
        } else {
            getLogger().info("Failed to delete the file.");
        }
    }

    public Plot getPlot() {
        return MCMEP2.getPlotAPI().getPlotSquared().getPlotAreaManager().getPlotArea(MCMEP2.getPlotWorld(), stringPlotId).getPlot(getPlotId());
    }

    public long getTimeSinceLastReview(){
        if(this.plotFinalReviewTimeStamps.size() == 0) return 0;
        else return plotFinalReviewTimeStamps.get(plotFinalReviewTimeStamps.size() - 1);
    }

    public PlotId getPlotId() { return PlotId.fromString(stringPlotId); }

    public int getPlayerReviewIteration(ReviewPlayer reviewPlayer){
        Integer iteration = playerReviewIteration.get(reviewPlayer.getUniqueId());
        if (iteration == null) return  0;
        else return iteration;
    }

    public boolean isBeingReviewed(){
        for (ReviewParty i : ReviewAPI.getReviewParties().values()){
            if(i.getReviewPlotLinkedList().contains(this)) return true;
        }
        return false;
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
