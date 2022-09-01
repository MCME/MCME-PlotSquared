package com.mcmiddleearth.plotsquared.command;

import com.mcmiddleearth.plotsquared.plotflag.ReviewDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import me.gleeming.command.Command;
import me.gleeming.command.paramter.Param;
import net.kyori.adventure.text.minimessage.Template;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class ReviewCommands {

    @Command(names = {"review start"}, playerOnly = true)
    public void reviewStart(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (reviewPlayer.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewing"));
            return;
        }
        if (ReviewAPI.getReviewPlotsCollection().isEmpty()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.no_plots"));
            return;
        }
        for (ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsCollection()) {
            if (reviewPlayer.hasAlreadyReviewed(reviewPlot)) {
                plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.reviewed_all_plots"));
                return;
            }
        }
        ReviewParty.startReviewParty(player);
        plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.start"));
    }

    @Command(names = {"review end"}, playerOnly = true)
    public void reviewStop(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        reviewPlayer.getReviewParty().stopReviewParty();
        plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.end"));
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

    @Command(names = {"review join"}, playerOnly = true)
    public void reviewJoin(Player player, @Param(name = "player") Player target) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
        if (reviewPlayer.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewing"));
            return;
        }
        if (!reviewTarget.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewing_other"));
            return;
        }
        plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.join"));
        reviewTarget.getReviewParty().addReviewPlayer(reviewPlayer);
        for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
            i.getPlotPlayer().sendMessage(TranslatableCaption.of("mcme.review.joined_notif"),  Template.of("player", player.getName()));
        }
    }

    @Command(names = {"review leave"}, playerOnly = true)
    public void reviewLeave(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (reviewPlayer.isReviewPartyLeader()) {
            for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
                i.getPlotPlayer().sendMessage(TranslatableCaption.of("mcme.review.leader_left"));
            }
            reviewPlayer.getReviewParty().stopReviewParty();
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.end"));
            return;
        }
        reviewPlayer.leaveReviewParty();
        plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.left"));
        for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
            i.getPlotPlayer().sendMessage(TranslatableCaption.of("mcme.review.left_notif"),  Template.of("player", player.getName()));
        }
    }

    @Command(names = {"review kick"}, playerOnly = true)
    public void reviewKick(Player player, @Param(name = "player") Player target) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
        if (!reviewPlayer.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        if (!reviewTarget.isReviewing() || reviewTarget.getReviewParty() != reviewPlayer.getReviewParty()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_party"));
            return;
        }
        if (reviewPlayer == reviewTarget) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.kick_self"));
            return;
        }
        reviewTarget.getPlotPlayer().sendMessage(TranslatableCaption.of("mcme.review.kicked"));
        reviewTarget.leaveReviewParty();
        for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
            i.getPlotPlayer().sendMessage(TranslatableCaption.of("mcme.review.kicked_notif"),  Template.of("player", reviewTarget.getPlotPlayer().getName()));
        }
    }

    @Command(names = {"review tp"}, playerOnly = true)
    public void reviewToPlot(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        reviewPlayer.teleportToReviewPlot(reviewPlayer.getReviewParty().getCurrentReviewPlot());
        plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.tp"));
    }

    @Command(names = {"review next"}, playerOnly = true)
    public void reviewNext(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        if (!(reviewPlayer.getReviewParty().hasGivenFeedback() && reviewPlayer.getReviewParty().hasGivenRating())) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_everyone_finished"));
            return;
        }
        if (reviewPlayer.getReviewParty().getNextReviewPlot() == null) {
            for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
                i.getPlotPlayer().sendMessage(TranslatableCaption.of("mcme.review.finished"));
                i.getPlotPlayer().sendMessage(TranslatableCaption.of("mcme.review.end"));
            }
            reviewPlayer.getReviewParty().stopReviewParty();
            return;
        }
        reviewPlayer.getReviewParty().goNextPlot();
        player.sendMessage("Teleported to next plot");
    }

    @Command(names = {"review rate"}, playerOnly = true)
    public void reviewRate(Player player, @Param(name = "number") int rating) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
//        if (reviewPlayer.getReviewParty().getCurrentPlot().isOwner(player.getUniqueId())) {
//            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.is_plot_owner"));
//            return;
//        }
        if (reviewPlayer.hasAlreadyReviewed(reviewPlayer.getReviewParty().getCurrentReviewPlot())) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewed"));
            return;
        }
        if (!reviewPlayer.hasGivenFeedback()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.first_feedback"));
            return;
        }
        if (0 > rating || rating > 100) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.rating_range"));
            return;
        }

        if (reviewPlayer.hasGivenRating()) {
            reviewPlayer.setPlotRating(rating);
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.rating_update"));
            return;
        }
        reviewPlayer.setPlotRating(rating);
        plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.rating"));
        if (reviewPlayer.getReviewParty().hasGivenRating() && reviewPlayer.getReviewParty().hasGivenFeedback()) {
            reviewPlayer.getReviewParty().goNextPlot();
            for (ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers()) {
                i.getPlotPlayer().sendMessage(TranslatableCaption.of("mcme.review.next"));
            }
            return;
        }
        plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.waiting"));
    }

    @Command(names = {"review feedback"}, playerOnly = true)
    public void reviewFeedback(Player player, @Param(name = "message", concated = true) String feedback) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.error.review.not_reviewing"));
            return;
        }
//        if (reviewPlayer.getReviewParty().getCurrentPlot().isOwner(player.getUniqueId())) {
//            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.is_plot_owner"));
//            return;
//        }
        if (reviewPlayer.hasAlreadyReviewed(reviewPlayer.getReviewParty().getCurrentReviewPlot())) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewed"));
            return;
        }
        if (reviewPlayer.hasGivenFeedback()) {
            reviewPlayer.setPlotFeedback(feedback);
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.feedback_updated"));
            return;
        }
        reviewPlayer.setPlotFeedback(feedback);
        plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.feedback"));
    }

    @Command(names = {"review submit"}, playerOnly = true)
    public void submitForRating(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot currentPlot = plotPlayer.getCurrentPlot();
        if (plotPlayer.getCurrentPlot() == null) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        if (!currentPlot.isOwner(player.getUniqueId())) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.is_not_plot_owner"));
            return;
        }
        if (ReviewStatusFlag.isBeingReviewed(currentPlot)){
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.submit_being_reviewed_this"));
            return;
        }
        if( ReviewStatusFlag.isLocked(currentPlot) || ReviewStatusFlag.isAccepted(currentPlot)) {
            plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.submit_accepted"));
        }
        long currentTimeSec = System.currentTimeMillis() / 1000;
        for (Plot plot : plotPlayer.getPlots()) {
            Set plotFlags = plot.getFlags();
            if (plotFlags.contains(ReviewPlot.ReviewStatus.BEING_REVIEWED) || plotFlags.contains(ReviewPlot.ReviewStatus.REJECTED)) {
                plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.submit_being_reviewed_other"));
                return;
            }
            if (plotFlags.contains(ReviewPlot.ReviewStatus.LOCKED)) {
                final long THREEDAYSINMILISEC = 60000;//made one minute for debug reasons 86400000 * 3
                long timeSinceReviewSec = Long.parseLong(plot.getFlag(DoneFlag.class));
                if ((currentTimeSec) - timeSinceReviewSec > THREEDAYSINMILISEC) {
                    currentPlot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
                    ReviewPlot reviewPlot = new ReviewPlot(currentPlot);
                    reviewPlot.submitReviewPlot(currentPlot);
                    plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.submit"));
                    return;
                }
                else plotPlayer.sendMessage(TranslatableCaption.of("mcme.error.submit_too_early"));
            }
            if (!plotFlags.contains(ReviewPlot.ReviewStatus.BEING_REVIEWED)) {
                currentPlot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
                ReviewPlot reviewPlot = new ReviewPlot(currentPlot);
                reviewPlot.submitReviewPlot(currentPlot);
                plotPlayer.sendMessage(TranslatableCaption.of("mcme.review.submit"));
                return;
            }
        }
    }

    @Command(names = {"review check"}, playerOnly = true)
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

    @Command(names = {"review plotdebug"}, playerOnly = true)
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
