package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.events.TeleportCause;
import com.plotsquared.core.player.PlotPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * A reviewPlayer is a container for reviewer data related to a single player.
 */
public class ReviewPlayer {
    private static final String NON_EXISTENT_CAPTION = "<red>PlotSquared does not recognize the caption: ";

    private final UUID PLAYERUUID;
    private ReviewParty reviewParty;
    private String plotFeedback;
    private Integer plotRating;
    private String lastMessage;

    public ReviewPlayer(Player player){
        this.PLAYERUUID = player.getUniqueId();
    }

    public boolean hasAlreadyReviewed(ReviewPlot reviewPlot){
        if(reviewPlot.getPlot().getOwner() == PLAYERUUID) return true;
        int playerReviewIteration = reviewPlot.getPlayerReviewIteration(this);
        int reviewIteration = reviewPlot.getReviewIteration();
        return playerReviewIteration > reviewIteration;
    }

    public void teleportToReviewPlot(ReviewPlot reviewPlot){
        reviewPlot.getPlot().teleportPlayer(this.getPlotPlayer(), TeleportCause.PLUGIN, result -> {
        });
    }

    public boolean hasGivenFeedback(){
        if (plotFeedback != null)
            return true;
        if(hasAlreadyReviewed(this.getReviewParty().getCurrentReviewPlot()))
            return true;
        else return false;
    }

    public boolean hasGivenRating(){
        if (plotRating != null)
            return true;
        if(hasAlreadyReviewed(this.getReviewParty().getCurrentReviewPlot()))
            return true;
        else return false;
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public void sendMessage(TranslatableCaption caption, MiniMessageTemplate... templates) {
        PlotPlayer plotPlayer = this.getPlotPlayer();
        Class[] param = new Class[0];
        try {
            Class templateClass = Class.forName("com.plotsquared.core.configuration.adventure.text.minimessage.Template");
            Method templateCreator = templateClass.getMethod("of", String.class, String.class);
            Object templateArray = Array.newInstance(templateClass,templates.length);
            for(int i = 0; i < templates.length; i++) {
                MiniMessageTemplate template = templates[i];
                Array.set(templateArray,i,templateCreator.invoke(null,template.search,template.replacement));
            }
            param = new Class[]{Class.forName("com.plotsquared.core.configuration.caption.Caption"),
                    templateArray.getClass()};
            Method method = plotPlayer.getClass().getMethod("sendMessage", param);
            method.invoke(plotPlayer,caption,templateArray);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static MiniMessageTemplate templateOf(String search, String replacement) {
        return new MiniMessageTemplate(search,replacement);
    }

    public static class MiniMessageTemplate {
        private final String search;
        private final String replacement;
        public MiniMessageTemplate(String search, String replacement) {
            this.search = search;
            this.replacement = replacement;
        }
    }

    public boolean isReviewing(){
        return reviewParty != null;
    }

    public boolean isReviewPartyLeader(){ return reviewParty.getReviewerLeader().getUniqueId() == PLAYERUUID; }

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

    public ReviewParty getReviewParty() {
        return reviewParty;
    }

    public Player getBukkitPlayer(){
        return Bukkit.getPlayer(this.getUniqueId());
    }

    public PlotPlayer<?> getPlotPlayer(){
        return BukkitUtil.adapt(getBukkitPlayer());
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
