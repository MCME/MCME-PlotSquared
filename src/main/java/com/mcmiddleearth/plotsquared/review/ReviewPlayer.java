package com.mcmiddleearth.plotsquared.review;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.plotsquared.core.player.PlotPlayer;
import org.bukkit.entity.Player;
import java.util.UUID;

/**
 * A reviewPlayer is a container for reviewer data related to a single player.
 */
public class ReviewPlayer {
    private final PlotPlayer<?> PLOTPLAYER;
    private final UUID PLAYERUUID;
    private ReviewParty reviewParty;
    private String feedback;
    private int rating;
    private boolean givenFeedBack;
    private boolean givenRating;

    public ReviewPlayer(Player player){
        this.PLOTPLAYER = MCMEP2.getPlotAPI().wrapPlayer(player.getUniqueId());
        this.PLAYERUUID = player.getUniqueId();
        this.givenFeedBack = false;
        this.givenRating = false;
    }

    public void setRating(int rating){
        this.rating = rating;
    }

    public void setFeedback(String feedback){
        this.feedback = feedback;
    }

    public void setParty(ReviewParty reviewParty){
        this.reviewParty = reviewParty;
    }

    public PlotPlayer<?> getPLOTPLAYER(){
        return PLOTPLAYER;
    }

    public boolean hasGivenFeedback(){
        return givenFeedBack;
    }

    public boolean hasGivenRating(){
        return givenRating;
    }

    public UUID getUniqueId() {
        return PLAYERUUID;
    }
}
