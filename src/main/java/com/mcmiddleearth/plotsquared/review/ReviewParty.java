package com.mcmiddleearth.plotsquared.review;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.plotsquared.core.plot.Plot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.ScoreboardManager;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

/**
 * A reviewParty consists of reviewPlayers.
 */
public class ReviewParty {
    private final ReviewPlayer LEADER;
    private Sidebar sidebar;
    private ArrayList<ReviewPlayer> partyReviewPlayers = new ArrayList<>();
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
        for (ReviewPlayer i : this.getReviewPlayers()){
            ReviewAPI.removeReviewPlayer(i);
        }
        this.sidebar.close();
        ReviewAPI.removeReviewParty(this);
    }

    public void goCurrentPlot(){
        ReviewPlot currentReviewPlot = this.reviewPlotLinkedList.getFirst();
        for (ReviewPlayer i : this.getReviewPlayers()) {
            i.teleportToReviewPlot(currentReviewPlot);
        }
    }

    public void goNextPlot(){
        ReviewPlot currentReviewPlot = this.reviewPlotLinkedList.pop();
        for (ReviewPlayer i : this.getReviewPlayers()){
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
            updateScoreboard();
            for (ReviewPlayer i : this.getReviewPlayers()) {
                i.teleportToReviewPlot(nextPlot);
            }
        }
    }

    Comparator<ReviewPlayer> compareReviewPlayerName = new Comparator<ReviewPlayer>() {
        @Override
        public int compare(ReviewPlayer reviewPlayer1, ReviewPlayer reviewPlayer2) {
            return Bukkit.getPlayer(reviewPlayer1.getUniqueId()).getName().compareTo(Bukkit.getPlayer(reviewPlayer2.getUniqueId()).getName());
        }
    };

    public void addReviewPlayer(ReviewPlayer reviewPlayer){
        if(this.getReviewPlayers().size() > 14) return; // max reviewparty size is 14
        ReviewAPI.addReviewPlayer(reviewPlayer);
        this.partyReviewPlayers.add(reviewPlayer);
        partyReviewPlayers.sort(compareReviewPlayerName);
        reviewPlayer.setReviewParty(this);
        updateScoreboard();
        reviewPlayer.teleportToReviewPlot(getCurrentReviewPlot());
    }

    public void removeReviewPlayer(ReviewPlayer reviewPlayer){
        ReviewAPI.removeReviewPlayer(reviewPlayer);
        this.partyReviewPlayers.remove(reviewPlayer);
        reviewPlayer.clearRating();
        reviewPlayer.clearFeedback();
        reviewPlayer.clearReviewParty();
        Player player = Bukkit.getPlayer(reviewPlayer.getUniqueId());
        if(player != null) this.sidebar.removePlayer(player);
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

    public void updateScoreboard(){
        ScoreboardManager scoreboardManager = MCMEP2.getScoreboardManager();
        Sidebar sidebar = scoreboardManager.sidebar(this.getReviewPlayers().size());
        sidebar.title(Component.text("Review Progress").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        int counter = 0;
        for (ReviewPlayer reviewPlayers: this.getReviewPlayers()) {
            Player player = Bukkit.getPlayer(reviewPlayers.getUniqueId());
            String reviewStatusSymbol = "❌";
            NamedTextColor reviewSatusColor = NamedTextColor.RED;
            if(reviewPlayers.hasAlreadyReviewed(getCurrentReviewPlot())){
                reviewStatusSymbol = "✔";
                reviewSatusColor = NamedTextColor.GREEN;
            }
            sidebar.line(counter,Component.text(reviewStatusSymbol + " ").color(reviewSatusColor).decoration(TextDecoration.BOLD, true)
                    .append(Component.text(player.getName())).color(reviewSatusColor));
            sidebar.addPlayer(player);
            counter += 1;
        }
        if(this.getNextReviewPlot() == null) sidebar.line(counter+1, Component.text("No more plots left.").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        else if(this.getReviewPlotLinkedList().size() == 2) sidebar.line(counter+1, Component.text(this.getReviewPlotLinkedList().size() -1 + " plot left.").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        else sidebar.line(counter+1, Component.text(this.getReviewPlotLinkedList().size() -1 + " plots left.").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        sidebar.visible(true);
        this.sidebar = sidebar;
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
        if(reviewPlotLinkedList.size() <= 1) return null;
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

    public ArrayList<ReviewPlayer> getReviewPlayers() {
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
