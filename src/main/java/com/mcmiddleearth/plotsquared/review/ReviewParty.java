package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.core.plot.Plot;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;

/**
 * A reviewParty consists of reviewPlayers.
 */
public class ReviewParty {
    private final ReviewPlayer LEADER;
    private HashSet<ReviewPlayer> partyReviewPlayers = new HashSet<>();
    private LinkedList<ReviewPlot> reviewPlotLinkedList = new LinkedList<>();    //linked list of latest plots to be reviewed
    private HashSet<String> plotFeedbacks = new HashSet<>();
    private HashSet<Integer> plotRatings = new HashSet<>();

    public ReviewParty(ReviewPlayer leader) {
        this.LEADER = leader;
        //add all reviewplots which the leader hasn't reviewed
        for (ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsCollection()){
            if(!leader.hasAlreadyReviewed(reviewPlot)) reviewPlotLinkedList.add(reviewPlot);
        }
        //if any such plots are found add the leader and by that start the party
        if(!reviewPlotLinkedList.isEmpty()) addReviewPlayer(leader);
    }

    public static void startReviewParty(Player player){
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewParty reviewParty = new ReviewParty(reviewPlayer);
        if(!reviewParty.reviewPlotLinkedList.isEmpty()) {
            ReviewAPI.addReviewParty(reviewParty);
            ReviewAPI.addReviewPlayer(reviewPlayer);
        }
    }

    public void stopReviewParty(){
        for (ReviewPlot i : reviewPlotLinkedList) {
            i.endPlotReview(this);
        }
        for (ReviewPlayer i : this.getAllReviewers()){
            ReviewAPI.removeReviewPlayer(i);
        }
        ReviewAPI.removeReviewParty(this);
    }

    public void goCurrentPlot(){
        ReviewPlot currentReviewPlot = this.reviewPlotLinkedList.getFirst();
        for (ReviewPlayer i : this.getAllReviewers()) {
            i.teleportToReviewPlot(currentReviewPlot);
        }
    }

    public void goNextPlot(){
        ReviewPlot currentReviewPlot = this.reviewPlotLinkedList.pop();
        for (ReviewPlayer i : this.getAllReviewers()){
            if(i.getPlotRating() != null) {
                this.plotFeedbacks.add(i.getPlotFeedback());
                this.plotRatings.add(i.getPlotRating());
            }
            i.clearRating();
            i.clearFeedback();
        }
        currentReviewPlot.endPlotReview(this);
        this.clearFeedbacks();
        this.clearRatings();

        //check for any remaining plots
        if(this.reviewPlotLinkedList.isEmpty()){
            stopReviewParty();
        }
        else {
            ReviewPlot nextPlot = this.reviewPlotLinkedList.getFirst();
            for (ReviewPlayer i : this.getAllReviewers()) {
                i.teleportToReviewPlot(nextPlot);
            }
        }
    }

    public void addReviewPlayer(ReviewPlayer reviewPlayer){
        ReviewAPI.addReviewPlayer(reviewPlayer);
        this.partyReviewPlayers.add(reviewPlayer);
        reviewPlayer.setReviewParty(this);

        reviewPlayer.teleportToReviewPlot(getCurrentReviewPlot());
    }

    public void removeReviewPlayer(ReviewPlayer reviewPlayer){
        ReviewAPI.removeReviewPlayer(reviewPlayer);
        this.partyReviewPlayers.remove(reviewPlayer);
        reviewPlayer.clearRating();
        reviewPlayer.clearFeedback();
        reviewPlayer.clearReviewParty();
        if(reviewPlayer.isReviewPartyLeader()){
            this.stopReviewParty();
        }
    }

    public boolean hasGivenFeedback() {
        boolean result = true;
        for(ReviewPlayer i : partyReviewPlayers){
            if(!i.hasGivenFeedback()){
                result = false;
            }
        }
        return result;
    }

    public boolean hasGivenRating() {
        boolean result = true;
        for(ReviewPlayer i : partyReviewPlayers){
            if(!i.hasGivenRating()){
                result = false;
            }
        }
        return result;
    }

    public ReviewPlot getNextReviewPlot(){
        if(reviewPlotLinkedList.size() == 0) return null;
        return this.reviewPlotLinkedList.get(1);
    }

    private void clearRatings() {
        this.plotRatings.clear();
    }

    private void clearFeedbacks() {
        this.plotFeedbacks.clear();
    }

    public ReviewPlot getCurrentReviewPlot(){
        return reviewPlotLinkedList.getFirst();
    }

    public Plot getCurrentPlot(){
        return reviewPlotLinkedList.getFirst().getPlot();
    }

    public ReviewPlayer getReviewerLeader(){
        return LEADER;
    }

    public boolean containsReviewPlayer(ReviewPlayer reviewPlayer) {
        return partyReviewPlayers.contains(reviewPlayer);
    }

    public HashSet<ReviewPlayer> getAllReviewers() {
        return partyReviewPlayers;
    }

    public HashSet<String> getFeedbacks() {
        return plotFeedbacks;
    }

    public HashSet<Integer> getPlotRatings(){
        return plotRatings;
    }

    public LinkedList<ReviewPlot> getReviewPlotLinkedList(){
        return reviewPlotLinkedList;
    }

    public UUID getId() {
        return getReviewerLeader().getUniqueId();
    }

    //debug party needed for being able to /review force end
    public ReviewParty() {
        this.LEADER = null;
    }
}
