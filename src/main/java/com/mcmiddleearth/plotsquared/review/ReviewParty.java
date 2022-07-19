package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.core.plot.Plot;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
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
    private LinkedList<Plot> plotLinkedList;    //linked list of latest plots to be reviewed
    private ArrayList<String> plotFeedbacks;
    private ArrayList<Integer> plotRatings;

    public ReviewParty(UUID id, ReviewPlayer leader){
        this.ID = id;
        this.LEADER = leader;
        this.partyReviewPlayers.add(leader);
    }

    public static ReviewParty startReviewParty(Player player){
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewParty reviewParty = new ReviewParty(reviewPlayer.getUniqueId(), reviewPlayer);
        ReviewAPI.addReviewParty(reviewParty);
        ReviewAPI.addReviewPlayer(reviewPlayer);
        reviewPlayer.setReviewParty(reviewParty);
        return reviewParty;
    }

    public void addReviewPlayerToParty(ReviewPlayer reviewPlayer){
        ReviewAPI.addReviewPlayer(reviewPlayer);
        this.addToParty(reviewPlayer);
    }

    public void removeReviewPlayer(ReviewPlayer reviewPlayer){
        if(reviewPlayer.isReviewPartyLeader()){
            this.stopParty();
            return;
        }
        reviewPlayer.clearRating();
        reviewPlayer.clearFeedback();
        partyReviewPlayers.remove(reviewPlayer);
        ReviewAPI.removeReviewPlayer(reviewPlayer);
        reviewPlayer.setReviewParty(null);
    }

    public void stopParty(){
        for (ReviewPlayer i : this.getAllReviewers()){
            ReviewAPI.removeReviewPlayer(i);
        }
        ReviewAPI.removeReviewParty(this);

        //go over all remaining plots in the linked list
//        for (Plot i : plotLinkedList){
//            for (ReviewParty j : ReviewAPI.getReviewParties().values()){
//                if(!j.getPlotLinkedList().contains(i)){
//                    ReviewPlot reviewPlot = new ReviewPlot(i);
//                    reviewPlot.preemptPlotReview(this);
//                }
//            }
//        }
        for (Plot i : plotLinkedList) {
            ReviewPlot reviewPlot = new ReviewPlot(i);
            reviewPlot.preemptPlotReview(this);
        }
    }

    public void goNextPlot(){
        Plot currentPlot = this.plotLinkedList.pop();
        ReviewPlot currentReviewPlot = new ReviewPlot(currentPlot);

        currentReviewPlot.endPlotReview(this); // IMPORTANT METHOD

        Plot nextPlot = this.plotLinkedList.getFirst();
        for (ReviewPlayer i : this.getAllReviewers()){
            World world = Bukkit.getWorlds().get(1);
            Bukkit.getPlayer(i.getUniqueId()).teleport(new Location(Bukkit.getWorld(nextPlot.getWorldName()), nextPlot.getPosition().getX(), nextPlot.getPosition().getY(), nextPlot.getPosition().getZ(), nextPlot.getPosition().getYaw(),nextPlot.getPosition().getPitch()));
//            nextPlot.teleportPlayer(i.getPLOTPLAYER(), TeleportCause.PLUGIN, result -> {
//            });
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

    public Plot getNextPlot(){
        return this.plotLinkedList.get(1);
    }

    public Plot getCurrentPlot(){
        return plotLinkedList.getFirst();
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

    public ArrayList<String> getFeedbacks() {
        return plotFeedbacks;
    }

    public ArrayList<Integer> getPlotRatings(){
        return plotRatings;
    }

    public LinkedList<Plot> getPlotLinkedList(){
        return plotLinkedList;
    }

    private void addToParty(ReviewPlayer player){
        partyReviewPlayers.add(player);
    }

    public UUID getId() {
        return getReviewerLeader().getUniqueId();
    }
}
