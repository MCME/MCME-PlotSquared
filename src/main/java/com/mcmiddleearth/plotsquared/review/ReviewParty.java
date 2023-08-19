package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.core.plot.Plot;
import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.review.plot.ReviewPlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.UUID;

/**
 * A reviewParty consists of reviewPlayers.
 */
public class ReviewParty {
    private final ReviewPlayer LEADER;
    private Sidebar sidebar;
    private final ArrayList<ReviewPlayer> partyReviewPlayers = new ArrayList<>();
    private final LinkedList<ReviewPlot> reviewPlots = new LinkedList<>();

    Comparator<ReviewPlayer> compareReviewPlayerName = Comparator.comparing(reviewPlayer -> Bukkit.getPlayer(reviewPlayer.getUniqueId()).getName());

    private ReviewParty(ReviewPlayer leader) {
        this.LEADER = leader;
        for (ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsToBeReviewed()) {
            if (!reviewPlot.wasReviewedBy(leader)) reviewPlots.add(reviewPlot);
        }
        if (!reviewPlots.isEmpty()) addReviewPlayer(leader);
    }

    public static void startReviewParty(ReviewPlayer reviewPlayer) {
        ReviewParty reviewParty = new ReviewParty(reviewPlayer);
        if (!reviewParty.reviewPlots.isEmpty()) {
            ReviewAPI.addReviewParty(reviewParty);
        }
    }

    public void stopReviewParty() {
        while (!reviewPlots.isEmpty()) {
            reviewPlots.pop().endPlotReview();
        }
        for (ReviewPlayer reviewPlayer : this.getReviewPlayers()) {
            reviewPlayer.leaveReviewParty();
        }
        this.sidebar.close();
        ReviewAPI.removeReviewParty(this);
    }

    public void goNextPlot() {
        this.getReviewPlayers().forEach((ReviewPlayer::submitReview));
        reviewPlots.pop().endPlotReview();
        if (reviewPlots.isEmpty()) {
            stopReviewParty();
        } else {
            updateScoreboard();
            this.getReviewPlayers().forEach(ReviewPlayer::teleportToCurrentReviewPlot);
        }
    }

    public void addReviewPlayer(ReviewPlayer reviewPlayer) {
        if (this.getReviewPlayers().size() > 14) return; // max reviewparty size is 14

        reviewPlayer.joinReviewParty(this);
        this.partyReviewPlayers.add(reviewPlayer);
        partyReviewPlayers.sort(compareReviewPlayerName);
        updateScoreboard();
    }

    public void removeReviewPlayer(ReviewPlayer reviewPlayer) {
        reviewPlayer.leaveReviewParty();
        this.partyReviewPlayers.remove(reviewPlayer);
        Player player = Bukkit.getPlayer(reviewPlayer.getUniqueId());
        if (player != null) this.sidebar.removePlayer(player);
        if (reviewPlayer.isReviewPartyLeader()) {
            this.stopReviewParty();
        }
        else updateScoreboard();
    }

    /**
     * Checks if everyone in the reviewparty has given feedback this plots review iteration now or previously
     * @return true if everyone has given feedback this plot review iteration now or previously, false otherwise
     */
    public boolean hasReviewed() {
        boolean result = true;
        for (ReviewPlayer i : partyReviewPlayers) {
            if (!i.hasReviewed()) {
                result = false;
            }
        }
        return result;
    }

    public void updateScoreboard(){
        ScoreboardLibrary scoreboardLibrary = MCMEP2.getScoreboardLibrary();
        Sidebar sidebar = scoreboardLibrary.createSidebar(this.getReviewPlayers().size());
        sidebar.title(Component.text("Review Progress").color(NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true));
        int counter = 0;
        for (ReviewPlayer reviewPlayers: this.getReviewPlayers()) {
            Player player = Bukkit.getPlayer(reviewPlayers.getUniqueId());
            String reviewStatusSymbol = "❌";
            NamedTextColor reviewSatusColor = NamedTextColor.RED;
            if(getCurrentReviewPlot().wasReviewedBy(reviewPlayers)){
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
        this.sidebar = sidebar;
    }

    public ReviewPlot getCurrentReviewPlot() {
        return reviewPlots.peek();
    }

    public ReviewPlot getNextReviewPlot() {
        if(reviewPlots.size() < 2) return null;
        return  reviewPlots.get(1);
    }

    public Plot getCurrentPlot() {
        return reviewPlots.peek().getPlot();
    }

    public boolean hasReviewPlotsLeft() {
        return reviewPlots.size() != 0;
    }

    public ReviewPlayer getReviewerLeader() {
        return LEADER;
    }

    public boolean containsReviewPlayer(ReviewPlayer reviewPlayer) {
        return partyReviewPlayers.contains(reviewPlayer);
    }

    public ArrayList<ReviewPlayer> getReviewPlayers() {
        return partyReviewPlayers;
    }

    public ArrayList<ReviewPlot> getReviewPlotLinkedList() {
        return new ArrayList<>(reviewPlots);
    }

    public UUID getId() {
        return getReviewerLeader().getUniqueId();
    }

    //debug party needed for being able to /review force end
    public ReviewParty() {
        this.LEADER = null;
    }
}
