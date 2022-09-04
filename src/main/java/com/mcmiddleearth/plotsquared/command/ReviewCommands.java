package com.mcmiddleearth.plotsquared.command;

import com.mcmiddleearth.plotsquared.plotflag.ReviewDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.mcmiddleearth.plotsquared.util.MiniMessageUtil;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import me.gleeming.command.Command;
import me.gleeming.command.paramter.Param;
//import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class ReviewCommands {
    /**
     * Start a ReviewParty.
     * @param player
     */
    @Command(names = {"review start"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewStart(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (reviewPlayer.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.already_reviewing"));
            return;
        }
        if (ReviewAPI.getReviewPlotsCollection().isEmpty()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.no_plots"));
            return;
        }
        for (ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsCollection()) {
            if (reviewPlayer.hasAlreadyReviewed(reviewPlot)) {
                MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.reviewed_all_plots"));
                return;
            }
        }
        ReviewParty.startReviewParty(player);
        MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.start"));
    }

    /**
     * End a ReviewParty.
     * @param player
     */
    @Command(names = {"review end"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewStop(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        reviewPlayer.getReviewParty().stopReviewParty();
        MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.end"));
    }

//    @Command(names = {"review invite"}, playerOnly = true)
//    public void reviewInvite(Player player, @Param(name = "player") Player target) {
//        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
//        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
//        if(!reviewPlayer.isReviewing()) {
//            player.sendMessage("You're not reviewing!");
//            return;
//        }
//        if(reviewTarget.getReviewParty() == reviewPlayer.getReviewParty()){
//            player.sendMessage("Target is already in your party!");
//            return;
//        }
//        if(reviewTarget.isReviewing()){
//            player.sendMessage("Target is already reviewing!");
//            return;
//        }
//        //implement inviter
//        reviewPlayer.getReviewParty().addReviewPlayerToParty(reviewTarget);
//        player.sendMessage("");
//    }

    /**
     * Join a ReviewParty.
     * @param player
     * @param target
     */
    @Command(names = {"review join"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewJoin(Player player, @Param(name = "player") Player target) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
        if (reviewPlayer.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.already_reviewing"));
            return;
        }
        if (!reviewTarget.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.already_reviewing_other"));
            return;
        }
        MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.join"));
        reviewTarget.getReviewParty().addReviewPlayer(reviewPlayer);
        for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
            MiniMessageUtil.sendMessage(i.getPlotPlayer(),TranslatableCaption.of("mcme.review.joined_notif"), MiniMessageUtil.templateOf("player", player.getName()));
        }
    }

    /**
     * Leave a ReviewParty.
     * @param player
     */
    @Command(names = {"review leave"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewLeave(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (reviewPlayer.isReviewPartyLeader()) {
            for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
                MiniMessageUtil.sendMessage(i.getPlotPlayer(),TranslatableCaption.of("mcme.review.leader_left"));
            }
            reviewPlayer.getReviewParty().stopReviewParty();
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.end"));
            return;
        }
        reviewPlayer.leaveReviewParty();
        MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.left"));
        for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
            MiniMessageUtil.sendMessage(i.getPlotPlayer(),TranslatableCaption.of("mcme.review.left_notif"), MiniMessageUtil.templateOf("player", player.getName()));
        }
    }

    /**
     * Kick a target Player from your ReviewParty.
     * @param player
     * @param target
     */
    @Command(names = {"review kick"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewKick(Player player, @Param(name = "player") Player target) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
        if (!reviewPlayer.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        if (!reviewTarget.isReviewing() || reviewTarget.getReviewParty() != reviewPlayer.getReviewParty()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_in_party"));
            return;
        }
        if (reviewPlayer == reviewTarget) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.kick_self"));
            return;
        }
        MiniMessageUtil.sendMessage(reviewTarget.getPlotPlayer(),TranslatableCaption.of("mcme.review.kicked"));
        reviewTarget.leaveReviewParty();
        for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
            MiniMessageUtil.sendMessage(i.getPlotPlayer(),TranslatableCaption.of("mcme.review.kicked_notif"),  MiniMessageUtil.templateOf("player", reviewTarget.getPlotPlayer().getName()));
        }
    }

    /**
     * Teleport to the current Plot.
     * @param player
     */
    @Command(names = {"review tp"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewToPlot(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        reviewPlayer.teleportToReviewPlot(reviewPlayer.getReviewParty().getCurrentReviewPlot());
        MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.tp"));
    }

    /**
     * Continue on to next Plot.
     * @param player
     */
    @Command(names = {"review next"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewNext(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        if (!(reviewPlayer.getReviewParty().hasGivenFeedback() && reviewPlayer.getReviewParty().hasGivenRating())) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_everyone_finished"));
            return;
        }
        if (reviewPlayer.getReviewParty().getNextReviewPlot() == null) {
            for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
                MiniMessageUtil.sendMessage(i.getPlotPlayer(),TranslatableCaption.of("mcme.review.finished"));
                MiniMessageUtil.sendMessage(i.getPlotPlayer(),TranslatableCaption.of("mcme.review.end"));
            }
            reviewPlayer.getReviewParty().stopReviewParty();
            return;
        }
        reviewPlayer.getReviewParty().goNextPlot();
        player.sendMessage("Teleported to next plot");
    }

    /**
     * Rate a Plot between 0 and 100.
     * @param player
     * @param rating
     */
    @Command(names = {"review rate"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewRate(Player player, @Param(name = "number") int rating) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
//        if (reviewPlayer.getReviewParty().getCurrentPlot().isOwner(player.getUniqueId())) {
//            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.is_plot_owner"));
//            return;
//        }
        if (reviewPlayer.hasAlreadyReviewed(reviewPlayer.getReviewParty().getCurrentReviewPlot())) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.already_reviewed"));
            return;
        }
        if (!reviewPlayer.hasGivenFeedback()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.first_feedback"));
            return;
        }
        if (0 > rating || rating > 100) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.rating_range"));
            return;
        }

        if (reviewPlayer.hasGivenRating()) {
            reviewPlayer.setPlotRating(rating);
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.rating_update"));
            return;
        }
        reviewPlayer.setPlotRating(rating);
        MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.rating"));
        if (reviewPlayer.getReviewParty().hasGivenRating() && reviewPlayer.getReviewParty().hasGivenFeedback()) {
            reviewPlayer.getReviewParty().goNextPlot();
            for (ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
                MiniMessageUtil.sendMessage(i.getPlotPlayer(),TranslatableCaption.of("mcme.review.next"));
            }
            return;
        }
        MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.waiting"));
    }

    /**
     * Give feedback to a Plot.
     * @param player
     * @param feedback
     */
    @Command(names = {"review feedback"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewFeedback(Player player, @Param(name = "message", concated = true) String feedback) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.error.review.not_reviewing"));
            return;
        }
//        if (reviewPlayer.getReviewParty().getCurrentPlot().isOwner(player.getUniqueId())) {
//            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.is_plot_owner"));
//            return;
//        }
        if (reviewPlayer.hasAlreadyReviewed(reviewPlayer.getReviewParty().getCurrentReviewPlot())) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.already_reviewed"));
            return;
        }
        if (reviewPlayer.hasGivenFeedback()) {
            reviewPlayer.setPlotFeedback(feedback);
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.feedback_updated"));
            return;
        }
        reviewPlayer.setPlotFeedback(feedback);
        MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.feedback"));
    }

    /**
     * Submit a Plot for review.
     * @param player
     */
    @Command(names = {"review submit"}, permission = "mcmep2.submit", playerOnly = true)
    public void submitForRating(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot currentPlot = plotPlayer.getCurrentPlot();
        if (plotPlayer.getCurrentPlot() == null) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        if (!currentPlot.isOwner(player.getUniqueId())) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.is_not_plot_owner"));
            return;
        }
        if (ReviewStatusFlag.isBeingReviewed(currentPlot)){
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.submit_being_reviewed_this"));
            return;
        }
        if( ReviewStatusFlag.isLocked(currentPlot) || ReviewStatusFlag.isAccepted(currentPlot)) {
            MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.submit_accepted"));
        }
        long currentTimeSec = System.currentTimeMillis() / 1000;
        for (Plot plot : plotPlayer.getPlots()) {
            Set plotFlags = plot.getFlags();
            if (plotFlags.contains(ReviewPlot.ReviewStatus.BEING_REVIEWED) || plotFlags.contains(ReviewPlot.ReviewStatus.REJECTED)) {
                MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.error.submit_being_reviewed_other"));
                return;
            }
            if (plotFlags.contains(ReviewPlot.ReviewStatus.LOCKED)) {
                final long THREEDAYSINMILISEC = 60000;//made one minute for debug reasons 86400000 * 3
                long timeSinceReviewSec = Long.parseLong(plot.getFlag(DoneFlag.class));
                if ((currentTimeSec) - timeSinceReviewSec > THREEDAYSINMILISEC) {
                    currentPlot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
                    ReviewPlot reviewPlot = new ReviewPlot(currentPlot);
                    reviewPlot.submitReviewPlot(currentPlot);
                    MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.submit"));
                    return;
                }
                else MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.error.submit_too_early"));
            }
            if (!plotFlags.contains(ReviewPlot.ReviewStatus.BEING_REVIEWED)) {
                currentPlot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
                ReviewPlot reviewPlot = new ReviewPlot(currentPlot);
                reviewPlot.submitReviewPlot(currentPlot);
                MiniMessageUtil.sendMessage(plotPlayer,TranslatableCaption.of("mcme.review.submit"));
                return;
            }
        }
    }

    /**
     * Checks the review status of a Plot.
     * @param player
     */
    @Command(names = {"review check"}, permission = "mcmep2.submit", playerOnly = true)
    public void checkRating(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot plot = plotPlayer.getCurrentPlot();
        if (plot == null) {
            player.sendMessage("You're not in a plot!");
            return;
        }
        if(!(ReviewStatusFlag.isLocked(plot) || ReviewStatusFlag.isAccepted(plot))) {
            ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plotPlayer.getCurrentPlot());
            player.sendMessage("Review Status");
            player.sendMessage(reviewPlot.getPlot().getFlag(ReviewStatusFlag.class).toString());
            player.sendMessage("Player Review Amount");
            for (UUID name : reviewPlot.getPlayerReviewIterationMap().keySet()) {
                String key = Bukkit.getOfflinePlayer(name).getName();
                String value = reviewPlot.getPlayerReviewIterationMap().get(name).toString();
                player.sendMessage(key + " " + value);
            }
            player.sendMessage("Plot Temp ratings");
            for (Integer rating : reviewPlot.getPlotTempRatings()) {
                player.sendMessage(rating.toString());
            }
            player.sendMessage("Plot final feedback");
            for (String feedback : reviewPlot.getPlotFinalFeedback()) {
                player.sendMessage(feedback);
            }
            player.sendMessage("Plot final ratings");
            for (Long rating : reviewPlot.getPlotFinalRatings()) {
                player.sendMessage(rating.toString());
            }
            player.sendMessage("Plot final timestamps");
            for (Long timeStamp : reviewPlot.getPlotFinalReviewTimeStamps()) {
                DateFormat simple = new SimpleDateFormat("dd MMM yyyy");
                Date date = new Date(timeStamp);
                player.sendMessage(simple.format(date));
            }
        }
        else{
            player.sendMessage("Review Status");
            player.sendMessage(plot.getFlag(ReviewStatusFlag.class).toString());
            player.sendMessage("Plot final ratings");
            for (Long number : plot.getFlag(ReviewDataFlag.class)) {
                if(number<=100) {
                    player.sendMessage(number.toString());
                }
                else {
                    DateFormat simple = new SimpleDateFormat("dd MMM yyyy");
                    Date date = new Date(number);
                    player.sendMessage(simple.format(date));
                }
            }
        }
    }

    @Command(names = {"review plotdebug"}, permission = "mcmep2.review", playerOnly = true)
    public void debugPlots(Player player) {
        for (ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsCollection()) {
            if (reviewPlot.getReviewStatus() == ReviewPlot.ReviewStatus.ACCEPTED) {
                Plot plot = reviewPlot.getPlot();
                //plot.getFlag(ReviewDataFlag.class).addAll(reviewPlot.preparedReviewData()); FIX LATER
                reviewPlot.deleteReviewPlotData();
                //set plot to done
                long flagValue = System.currentTimeMillis() / 1000;
                PlotFlag<?, ?> plotFlag = plot.getFlagContainer().getFlag(DoneFlag.class)
                        .createFlagInstance(Long.toString(flagValue));
                plot.setFlag(plotFlag);
                //set plot to ACCEPTED
                plot.setFlag(ReviewStatusFlag.LOCKED_FLAG);
                player.sendMessage("a plot was accepted and locked");
            }
        }
        player.sendMessage("no more plots to be accepted");
    }
}
