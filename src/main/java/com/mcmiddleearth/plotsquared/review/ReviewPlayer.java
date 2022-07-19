package com.mcmiddleearth.plotsquared.review;

import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * A reviewPlayer is a container for reviewer data related to a single player.
 */
public class ReviewPlayer {
//    private final PlotPlayer<?> PLOTPLAYER;
    private final UUID PLAYERUUID;
    private ReviewParty reviewParty;
    private String feedback;
    private Integer rating;

    public ReviewPlayer(Player player){
//        this.PLOTPLAYER = MCMEP2.getPlotAPI().wrapPlayer(player.getUniqueId());
        this.PLAYERUUID = player.getUniqueId();
    }

    public boolean isReviewing(){
        return reviewParty != null;
    }

    public boolean isReviewPartyLeader(){ return reviewParty.getReviewerLeader().getUniqueId() == PLAYERUUID; }

    public void setRating(int rating){
        this.rating = rating;
    }

    public void setFeedback(String feedback){
        this.feedback = feedback;
    }

    public void clearFeedback(){
        this.feedback = null;
    }

    public void clearRating(){
        this.rating = null;
    }

    public void setReviewParty(ReviewParty reviewParty){
        this.reviewParty = reviewParty;
    }

    public void leaveReviewParty() {
        reviewParty.removeReviewPlayer(this);
    }

    public ReviewParty getReviewParty() {return reviewParty;};

//    public PlotPlayer<?> getPLOTPLAYER(){
//        return PLOTPLAYER;
//    }

    public boolean hasGivenFeedback(){
        return feedback == null;
    }

    public boolean hasGivenRating(){
        return rating == null;
    }

    public UUID getUniqueId() {
        return PLAYERUUID;
    }
}
