package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.events.TeleportCause;
import com.plotsquared.core.plot.Plot;
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
    }

    public static ReviewParty startReviewParty(Player player){
        ReviewPlayer partyLeader = new ReviewPlayer(player);
        ReviewParty reviewParty = new ReviewParty(partyLeader.getUniqueId(), partyLeader);
        ReviewAPI.addReviewParty(reviewParty);
        partyLeader.setParty(reviewParty);
        return reviewParty;
    }

    public int addPlayerToParty(Player player){
        //Can't add to party because he's already reviewing
        if(ReviewAPI.getReviewers().containsKey(player.getUniqueId())){
            return 1;
        }
        ReviewPlayer reviewPlayer = new ReviewPlayer(player);
        ReviewAPI.addReviewPlayer(reviewPlayer);
        this.addToParty(reviewPlayer);
        return 0;
    }

    public int removePlayerFromParty(Player player){
        //can't remove yourself
        if(this.getReviewerLeader().getUniqueId() == player.getUniqueId()){
            return 1;
        }
        //Can't remove from party because he's not reviewing
        if(!ReviewAPI.isReviewPlayer(player)){
            return 2;
        }
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        //isn't in your review party
        if(!this.containsReviewer(reviewPlayer)) {
            return 3;
        }
        this.removeFromParty(reviewPlayer);
        ReviewAPI.removeReviewPlayer(reviewPlayer);
        return 0;
    }

    public int stopParty(){
        for (ReviewPlayer i : this.getAllReviewers()){
            ReviewAPI.removeReviewPlayer(i);
        }
        ReviewAPI.removeReviewParty(this);
        //somehow handle all plots that were waiting for this party instance to be reviewed, but never got reviewed, to be properly reviewed (and not forgotten)
        return 0;
    }

    public void goNextPlot(){
        if(!this.hasGivenFeedback()) return;
        if(!this.hasGivenRating()) return;
        Plot currentPlot = this.plotLinkedList.pop();
        ReviewPlot currentReviewPlot = new ReviewPlot(currentPlot);
        currentReviewPlot.endPlotReview(this);
        //
        //!!!!implement adding rating and feedback to plot!!!!
        //

        Plot nextPlot = this.plotLinkedList.getFirst();
        if (nextPlot == null) return; //no new plot right now, ask if done reviewing?

        for (ReviewPlayer i : this.getAllReviewers()){
            nextPlot.teleportPlayer(i.getPLOTPLAYER(), TeleportCause.PLUGIN, result -> {
            });
        }
    }

    public boolean hasGivenFeedback() {
        boolean result = true;
        for(ReviewPlayer i : partyReviewPlayers){
            if(!i.hasGivenFeedback()){
                result = false;
                i.getPLOTPLAYER().sendMessage(TranslatableCaption.of("give_feedback")); //implement message
            }
        }
        return result;
    }

    public boolean hasGivenRating() {
        boolean result = true;
        for(ReviewPlayer i : partyReviewPlayers){
            if(!i.hasGivenRating()){
                result = false;
                i.getPLOTPLAYER().sendMessage(TranslatableCaption.of("give_rating")); //implement message
            }
        }
        return result;
    }

    public Plot getCurrentPlot(){ return plotLinkedList.getFirst(); }

    public ReviewPlayer getReviewerLeader(){
        return LEADER;
    }

    public void addToParty(ReviewPlayer player){
        partyReviewPlayers.add(player);
    }

    public void removeFromParty(ReviewPlayer player){
        partyReviewPlayers.remove(player);
    }

    public boolean containsReviewer(ReviewPlayer reviewPlayer) {
        return partyReviewPlayers.contains(reviewPlayer);
    }

    public HashSet<ReviewPlayer> getAllReviewers() {
        return partyReviewPlayers;
    }

    public ArrayList<Integer> getPlotRatings(){ return plotRatings; }

    public LinkedList<Plot> getPlotLinkedList(){
        return plotLinkedList;
    }

}
