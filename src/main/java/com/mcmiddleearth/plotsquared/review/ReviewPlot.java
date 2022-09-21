package com.mcmiddleearth.plotsquared.review;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.command.ReviewCommands;
import com.mcmiddleearth.plotsquared.plotflag.ReviewRatingDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewTimeDataFlag;
import com.mcmiddleearth.plotsquared.util.FileManagement;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.PlotId;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.Serializable;
import java.util.*;

import static com.mcmiddleearth.plotsquared.review.ReviewPlayer.templateOf;
import static org.bukkit.Bukkit.getLogger;

public class ReviewPlot implements Serializable {
    private final String stringPlotId;
    private HashMap<java.util.UUID, Integer> playerReviewIteration;
    private HashSet<Integer> plotTempRatings;
    private ArrayList<String> plotFinalFeedback;
    private ArrayList<Long> plotFinalRatings;
    private ArrayList<Long> plotFinalReviewTimeStamps;

    public enum ReviewStatus{
        NOT_BEING_REVIEWED,
        BEING_REVIEWED,
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
            this.plotFinalFeedback = new ArrayList<>();
            this.plotFinalRatings = new ArrayList<>();
            this.plotFinalReviewTimeStamps = new ArrayList<>();
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
                if(Bukkit.getPlayer(this.getPlot().getOwner()).isOnline()) {
                    PlotPlayer plotPlayer = BukkitUtil.adapt(Bukkit.getPlayer(this.getPlot().getOwner()));
                    this.getPlot().setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
                    String score = String.valueOf(ReviewAPI.getReviewPlot(this.getPlot()).getFinalRatings().get(ReviewAPI.getReviewPlot(this.getPlot()).getFinalRatings().size() - 1));
                    ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(this.getPlot().getOwner()));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rejected"), templateOf("rating", score));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rejected_2"));
                    int allowedPlots = plotPlayer.getAllowedPlots();
                    if (allowedPlots == 0)
                        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.no_plot"));
                    if (allowedPlots == 1)
                        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plot"), templateOf("amount", String.valueOf(allowedPlots)));
                    else
                        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plots"), templateOf("amount", String.valueOf(allowedPlots)));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
                }
                else this.getPlot().setFlag(ReviewStatusFlag.REJECTED_FLAG);
                plot.removeFlag(DoneFlag.class);
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
                //save data to flag and delete data from disk
                plot.setFlag(new ReviewRatingDataFlag(plotFinalRatings));
                plot.setFlag(new ReviewTimeDataFlag(plotFinalReviewTimeStamps));
                this.deleteReviewPlotData();
                //set plot to done
                long flagValue = System.currentTimeMillis() / 1000;
                PlotFlag<?, ?> doneFlag = plot.getFlagContainer().getFlag(DoneFlag.class).createFlagInstance(Long.toString(flagValue));
                plot.setFlag(doneFlag);
                //set plot to ACCEPTED
                if(Bukkit.getPlayer(this.getPlot().getOwner()).isOnline()){
                    PlotPlayer plotPlayer = BukkitUtil.adapt(Bukkit.getPlayer(this.getPlot().getOwner()));
                    this.getPlot().setFlag(ReviewStatusFlag.LOCKED_FLAG);
                    String score = String.valueOf(ReviewAPI.getReviewPlot(this.getPlot()).getFinalRatings().get(ReviewAPI.getReviewPlot(this.getPlot()).getFinalRatings().size() - 1));
                    ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(this.getPlot().getOwner()));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_accepted"), templateOf("rating", score));
                    int allowedPlots = plotPlayer.getAllowedPlots();
                    if (allowedPlots == 1)
                        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plot"), templateOf("amount", String.valueOf(allowedPlots)));
                    else
                        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plots"), templateOf("amount", String.valueOf(allowedPlots)));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
                }
                else plot.setFlag(ReviewStatusFlag.ACCEPTED_FLAG);
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
        File file = new File(MCMEP2.getReviewPlotDirectory() , plot.getId().toString() + ".yml");
        FileManagement.writeObjectToFile(this, file);
        ReviewAPI.addReviewPlot(this.getPlotId(), this);
        long flagValue = System.currentTimeMillis() / 1000;
        PlotFlag<?, ?> doneFlag = plot.getFlagContainer().getFlag(DoneFlag.class).createFlagInstance(Long.toString(flagValue));
        plot.setFlag(doneFlag);
        plot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
    }

    /**
     * Checks if plot passed minimal time threshold.
     * @return true if passed
     */
    private boolean passedTimeThreshold() {
        if(plotTempRatings.size()<1) return false; // if less than 5 people reviewed the plot //REDUCED TO 3 for debug reasons
        final int DAYINGMILISEC = 86400000;//made one minute for debug reasons 86400000
        if (plotFinalReviewTimeStamps.size() == 0){
            return false;
        }
        else;
            return plotFinalReviewTimeStamps.get(plotFinalReviewTimeStamps.size() - 1) <= ((System.currentTimeMillis() ) - DAYINGMILISEC);
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
        return rating >= 50;
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
        File file = new File(MCMEP2.getReviewPlotDirectory(), File.separator + stringPlotId + ".yml");
        FileManagement.writeObjectToFile(this, file);
    }

    public ReviewPlot loadReviewPlotData() {
        File file = new File(MCMEP2.getReviewPlotDirectory(), File.separator + stringPlotId + ".yml");
        if (!file.exists()) return null;
        else return FileManagement.readObjectFromFile(file);
    }

    public static ReviewPlot loadReviewPlotData(Plot plot){
        String plotId = plot.getId().toString();
        File file = new File(MCMEP2.getReviewPlotDirectory(), File.separator + plotId + ".yml");
        if (!file.exists()) return null;
        else return FileManagement.readObjectFromFile(file);
    }

    public void deleteReviewPlotData() {
        File file = new File(MCMEP2.getReviewPlotDirectory(), stringPlotId + ".yml");
        if(!file.exists()){
            return;
        }
        if (file.delete()) {
            getLogger().info("Deleted the file: " + file.getName());
        } else {
            getLogger().info("Failed to delete: " + file.getName());
        }
    }

    public Plot getPlot() {
        return MCMEP2.getPlotAPI().getPlotSquared().getPlotAreaManager().getPlotArea(MCMEP2.getPlotWorld(), stringPlotId).getPlot(getPlotId());
    }

    public long getTimeSinceLastReview(){
        if (this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.ACCEPTED || this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.LOCKED){
            return this.getPlot().getFlag(ReviewTimeDataFlag.class).get(this.getPlot().getFlag(ReviewTimeDataFlag.class).size()-1);
        }
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

    public void deleteReview(){
        ReviewAPI.removeReviewPlot(this);
        plotFinalFeedback.clear();
        plotFinalRatings.clear();
        plotFinalReviewTimeStamps.clear();
        plotTempRatings.clear();
        this.deleteReviewPlotData();
        this.getPlot().removeFlag(ReviewRatingDataFlag.class);
        this.getPlot().setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
    }

    public void enoughComplexity(Player player, Plot plot) {
        ReviewCommands.submitForRatingComplexity(player, plot, true);
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

    public List<Long> getFinalRatings() {
        if (this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.ACCEPTED || this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.LOCKED){
            return this.getPlot().getFlag(ReviewRatingDataFlag.class);
        }
        return plotFinalRatings;
    }

    public List<Long> getFinalReviewTimeStamps() {
        if (this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.ACCEPTED || this.getPlot().getFlag(ReviewStatusFlag.class) == ReviewStatus.LOCKED){
            return this.getPlot().getFlag(ReviewTimeDataFlag.class);
        }
        return plotFinalReviewTimeStamps;
    }

    public List<String> getPlotFinalFeedback() {
        return plotFinalFeedback;
    }

}
