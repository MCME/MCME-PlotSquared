package com.mcmiddleearth.plotsquared.review;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.plotflag.ReviewDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class ReviewPlot {
    private PlotId plotId;
    private HashMap<java.util.UUID, Integer> playerReviewAmount;
    private ArrayList<Integer> plotTempRatings;
    private ArrayList<String> plotFinalFeedback;
    private ArrayList<Long> plotFinalRatings;
    private ArrayList<Long> plotFinalReviewTimeStamps;


    public ReviewPlot(Plot plot){
        //if the reviewPlot is not yet saved to disk
        if (loadReviewPlotData(plot) == null){
            plotId = plot.getId();
        }
        else
            plotId = loadReviewPlotData(plot).plotId;
            playerReviewAmount = loadReviewPlotData(plot).playerReviewAmount;
            plotTempRatings = loadReviewPlotData(plot).plotTempRatings;
            plotFinalRatings = loadReviewPlotData(plot).plotFinalRatings;
            plotFinalReviewTimeStamps = loadReviewPlotData(plot).plotFinalReviewTimeStamps;
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
                //someone is still reviewing this plot lets handle it after everyone is done.
            case TOO_EARLY:
                //save file with no conclusion, review process continues.
                this.saveReviewPlotData();
            case REJECTED:
                //save file with fail
                this.saveReviewPlotData();
                //set reviewFlag to false (end review process)
                this.getPlot().setFlag(ReviewStatusFlag.REJECTED_FLAG);
                this.plotTempRatings.clear();
            case ACCEPTED:
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
        }
    }

    public void preemptPlotReview(ReviewParty reviewParty) {
        ReviewStatus reviewStatus = getReviewStatus();
        Plot plot = getPlot();
        switch(reviewStatus){
            case BEING_REVIEWED:
                //someone is still reviewing this plot lets handle it after everyone is done.
            case TOO_EARLY:
                //save file with no conclusion, review process continues.
                this.saveReviewPlotData();
            case REJECTED:
                //save file with fail
                this.saveReviewPlotData();
                //set reviewFlag to false (end review process)
                this.getPlot().setFlag(ReviewStatusFlag.REJECTED_FLAG);
                this.plotTempRatings.clear();
            case ACCEPTED:
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
            if(i.getPlotLinkedList().contains(this.getPlot())) return ReviewStatus.BEING_REVIEWED;
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
    public void addTempRatings(ArrayList<Integer> ratingList){
        plotTempRatings.addAll(ratingList);
    }

    /**
     * Adds feedbacks of reviewPlayers in reviewParty to array;
     * @param feedbackList list of feedbacks in reviewParty;
     */
    public void addFeedback(ArrayList<String> feedbackList) {
        plotFinalFeedback.addAll(feedbackList);
    }


    /**
     * Sets the amount of times a player has reviewed this plot
     * @param reviewPlayers list of reviewPlayers in reviewParty
     */
    public void setPlayerReviewAmounts(HashSet<ReviewPlayer> reviewPlayers){
        for(ReviewPlayer i: reviewPlayers){
            playerReviewAmount.put(i.getUniqueId(), plotFinalRatings.size() + 1);
        }
    }

    public void saveReviewPlotData() {
        String plotId = this.getId().toString();
        //need to create proper yaml file;
        File reviewPlotYamlFile = new File(MCMEP2.getReviewPlotDirectory() + File.separator + plotId + ".yaml");
        try {
            MCMEP2.getObjectMapper().writeValue(reviewPlotYamlFile, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ReviewPlot loadReviewPlotData(){
        String plotId = this.getId().toString();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File reviewPlotYamlFile = new File(classLoader.getResource(plotId + ".yaml").getFile());
        try {
            return MCMEP2.getObjectMapper().readValue(reviewPlotYamlFile, ReviewPlot.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static ReviewPlot loadReviewPlotData(Plot plot){
        String plotId = plot.getId().toString();
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File reviewPlotYamlFile = new File(classLoader.getResource(plotId + ".yaml").getFile());
        try {
            return MCMEP2.getObjectMapper().readValue(reviewPlotYamlFile, ReviewPlot.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void deleteReviewPlotData() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        File reviewPlotYamlFile = new File(classLoader.getResource(plotId + ".yaml").getFile());
        reviewPlotYamlFile.delete();
    }

    public Plot getPlot() {
        return MCMEP2.getPlotArea().getPlot(plotId);
    }

    public long getTimeSinceLastReview(){
        if(this.plotFinalReviewTimeStamps.isEmpty()) return System.currentTimeMillis() / 1000;
        else return plotFinalReviewTimeStamps.get(plotFinalReviewTimeStamps.size() - 1);
    }

    public PlotId getId() { return plotId; }

}
