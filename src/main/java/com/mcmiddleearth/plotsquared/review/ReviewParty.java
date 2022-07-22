package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.core.events.TeleportCause;
import com.plotsquared.core.plot.Plot;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;

/**
 * A reviewParty consists of reviewPlayers.
 */
public class ReviewParty {
    private final UUID ID;
    private final ReviewPlayer LEADER;
    private HashSet<ReviewPlayer> partyReviewPlayers = new HashSet<>();
    private LinkedList<ReviewPlot> reviewPlotLinkedList = new LinkedList<>();    //linked list of latest plots to be reviewed
    private HashSet<String> plotFeedbacks = new HashSet<>();
    private HashSet<Integer> plotRatings = new HashSet<>();

    public ReviewParty(UUID id, ReviewPlayer leader) {
        this.ID = id;
        this.LEADER = leader;
        //add all reviewplots which the leader hasn't reviewed
        for (ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsCollection()){
            //getLogger().info(String.valueOf(reviewPlot.getPlayerReviewIteration(leader)) + String.valueOf(reviewPlot.getReviewIteration()));
            if(!leader.hasAlreadyRated(reviewPlot)) {
                //getLogger().info(String.valueOf(reviewPlot.getPlayerReviewIteration(leader)) + String.valueOf(reviewPlot.getReviewIteration()));
                reviewPlotLinkedList.add(reviewPlot);
            }
            else return;
        }
        if(!reviewPlotLinkedList.isEmpty()){
            addReviewPlayer(leader);
        }
    }

    public static ReviewParty startReviewParty(Player player){
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewParty reviewParty = new ReviewParty(reviewPlayer.getUniqueId(), reviewPlayer);
        ReviewAPI.addReviewParty(reviewParty);
        ReviewAPI.addReviewPlayer(reviewPlayer);
        reviewPlayer.setReviewParty(reviewParty);
        return reviewParty;
    }

    public void stopParty(){
        for (ReviewPlayer i : this.getAllReviewers()){
            ReviewAPI.removeReviewPlayer(i);
        }

        ReviewAPI.removeReviewParty(this);

        for (ReviewPlot i : reviewPlotLinkedList) {
            i.preemptPlotReview(this);
        }
    }

    public void goNextPlot(){
        ReviewPlot currentReviewPlot = this.reviewPlotLinkedList.pop();
        for (ReviewPlayer i : this.getAllReviewers()){
            this.plotFeedbacks.add(i.getPlotFeedback());
            this.plotRatings.add(i.getPlotRating());
            i.clearRating();
            i.clearFeedback();
        }

        currentReviewPlot.endPlotReview(this); // IMPORTANT METHOD

        //check for any remaining plots
        if(this.reviewPlotLinkedList.isEmpty()){
            stopParty();
            return;
        }

        ReviewPlot nextPlot = this.reviewPlotLinkedList.getFirst();
        for (ReviewPlayer i : this.getAllReviewers()){
            nextPlot.getPlot().teleportPlayer(i.getPlotPlayer(), TeleportCause.PLUGIN, result -> {
            });
        }
    }

    public void addReviewPlayer(ReviewPlayer reviewPlayer){
        ReviewAPI.addReviewPlayer(reviewPlayer);
        this.partyReviewPlayers.add(reviewPlayer);
        reviewPlayer.setReviewParty(this);

        getCurrentPlot().teleportPlayer(reviewPlayer.getPlotPlayer(), TeleportCause.PLUGIN, result -> {
        });
    }

    public void removeReviewPlayer(ReviewPlayer reviewPlayer){
        ReviewAPI.removeReviewPlayer(reviewPlayer);
        this.partyReviewPlayers.remove(reviewPlayer);
        reviewPlayer.clearRating();
        reviewPlayer.clearFeedback();
        reviewPlayer.clearReviewParty();

        if(reviewPlayer.isReviewPartyLeader()){
            this.stopParty();
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
        if(reviewPlotLinkedList.size() == 1) return null;
        return this.reviewPlotLinkedList.get(1);
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

    public boolean containsReviewer(ReviewPlayer reviewPlayer) {
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
}
