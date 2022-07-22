package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.player.PlotPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * A reviewPlayer is a container for reviewer data related to a single player.
 */
public class ReviewPlayer {
    private final UUID PLAYERUUID;
    private ReviewParty reviewParty;
    private String plotFeedback;
    private Integer plotRating;

    public ReviewPlayer(Player player){
        this.PLAYERUUID = player.getUniqueId();
    }

    public boolean isReviewing(){
        return reviewParty != null;
    }

    public boolean isReviewPartyLeader(){ return reviewParty.getReviewerLeader().getUniqueId() == PLAYERUUID; }

    public boolean hasAlreadyRated(ReviewPlot reviewPlot){
        int playerReviewIteration = reviewPlot.getPlayerReviewIteration(this);
        int reviewIteration = reviewPlot.getReviewIteration();
        return playerReviewIteration >= reviewIteration || playerReviewIteration == 0;
    }

    public void setPlotRating(Integer plotRating){
        this.plotRating = plotRating;
    }

    public void setPlotFeedback(String plotFeedback){
        this.plotFeedback = plotFeedback;
    }

    public void clearFeedback(){
        this.plotFeedback = null;
    }

    public void clearRating(){
        this.plotRating = null;
    }

    public void setReviewParty(ReviewParty reviewParty){
        this.reviewParty = reviewParty;
    }

    public void leaveReviewParty() {
        reviewParty.removeReviewPlayer(this);
    }

    public ReviewParty getReviewParty() {return reviewParty;};

    public Player getBukkitPlayer(){
        return Bukkit.getPlayer(this.getUniqueId());
    }

    public PlotPlayer<?> getPlotPlayer(){
        return BukkitUtil.adapt(getBukkitPlayer());
    }

    public boolean hasGivenFeedback(){
        if (plotFeedback != null)
            return true;
        if(hasAlreadyRated(this.getReviewParty().getCurrentReviewPlot()))
            return true;
        else return false;
    }

    public boolean hasGivenRating(){
        if (plotRating != null)
            return true;
        if(hasAlreadyRated(this.getReviewParty().getCurrentReviewPlot()))
            return true;
        else return false;
    }

    public UUID getUniqueId() {
        return PLAYERUUID;
    }

    public void clearReviewParty() {
        this.reviewParty = null;
    }

    public String getPlotFeedback() {
        return plotFeedback;
    }

    public Integer getPlotRating() {
        return plotRating;
    }
}
