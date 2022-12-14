package com.mcmiddleearth.plotsquared.command;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.plotflag.ReviewRatingDataFlag;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.mcmiddleearth.plotsquared.util.FileManagement;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.backup.BackupManager;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.Caption;
import com.plotsquared.core.configuration.caption.CaptionHolder;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.database.DBFunc;
import com.plotsquared.core.events.TeleportCause;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.PlotFlag;
import com.plotsquared.core.plot.flag.implementations.AnalysisFlag;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.plot.flag.implementations.ServerPlotFlag;
import com.plotsquared.core.util.PlayerManager;
import com.plotsquared.core.util.task.RunnableVal3;
import com.plotsquared.core.util.task.TaskManager;
import com.plotsquared.core.uuid.UUIDMapping;
import me.gleeming.command.Command;
import me.gleeming.command.paramter.Param;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.mcmiddleearth.plotsquared.review.ReviewAPI.ReviewCommands.*;
import static com.mcmiddleearth.plotsquared.review.ReviewAPI.commandConfirm;
import static com.mcmiddleearth.plotsquared.review.ReviewPlayer.FakeMiniMessage.parse;
import static com.mcmiddleearth.plotsquared.review.ReviewPlayer.Template.templateOf;
import static org.bukkit.Bukkit.getLogger;

public class ReviewCommands {
    /**
     * Start a ReviewParty.
     *
     * @param player
     */
    @Command(names = {"review start"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewStart(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewing"));
            return;
        }
        if (ReviewAPI.getReviewPlotsCollection().isEmpty()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.no_plots"));
            return;
        }
        for (ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsCollection()) {
            if (!reviewPlayer.hasAlreadyReviewed(reviewPlot)) {
                ReviewParty.startReviewParty(player);
                reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.start"));
                return;
            }
        }
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.reviewed_all_plots"));
    }

    /**
     * End a ReviewParty.
     *
     * @param player
     */
    @Command(names = {"review end"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewStop(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        reviewPlayer.getReviewParty().stopReviewParty();
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.end"));
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
     *
     * @param player
     * @param target
     */
    @Command(names = {"review join"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewJoin(Player player, @Param(name = "player") Player target) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
        if (reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewing"));
            return;
        }
        if (!reviewTarget.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewing_other"));
            return;
        }
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.join"));
        for (ReviewPlayer i : reviewPlayer.getReviewParty().getReviewPlayers()) {
            i.sendMessage(TranslatableCaption.of("mcme.review.joined_notif"), templateOf("player", player.getName()));
        }
        reviewTarget.getReviewParty().addReviewPlayer(reviewPlayer);
    }

    /**
     * Leave a ReviewParty.
     *
     * @param player
     */
    @Command(names = {"review leave"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewLeave(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (reviewPlayer.isReviewPartyLeader()) {
            for (ReviewPlayer i : reviewPlayer.getReviewParty().getReviewPlayers()) {
                i.sendMessage(TranslatableCaption.of("mcme.review.leader_left"));
            }
            reviewPlayer.getReviewParty().stopReviewParty();
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.end"));
            return;
        }
        reviewPlayer.leaveReviewParty();
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.left"));
        for (ReviewPlayer i : reviewPlayer.getReviewParty().getReviewPlayers()) {
            i.sendMessage(TranslatableCaption.of("mcme.review.left_notif"), templateOf("player", player.getName()));
        }
    }

    /**
     * Kick a target Player from your ReviewParty.
     *
     * @param player
     * @param target
     */
    @Command(names = {"review kick"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewKick(Player player, @Param(name = "player") Player target) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
        if (!reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        if (!reviewTarget.isReviewing() || reviewTarget.getReviewParty() != reviewPlayer.getReviewParty()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_party"));
            return;
        }
        if (reviewPlayer == reviewTarget) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.kick_self"));
            return;
        }
        reviewTarget.sendMessage(TranslatableCaption.of("mcme.review.kicked"));
        reviewTarget.leaveReviewParty();
        for (ReviewPlayer i : reviewPlayer.getReviewParty().getReviewPlayers()) {
            i.sendMessage(TranslatableCaption.of("mcme.review.kicked_notif"), templateOf("player", reviewTarget.getPlotPlayer().getName()));
        }
    }

    /**
     * Teleport to the current Plot.
     *
     * @param player
     */
    @Command(names = {"review tp"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewToPlot(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        reviewPlayer.teleportToReviewPlot(reviewPlayer.getReviewParty().getCurrentReviewPlot());
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.tp"));
    }

    /**
     * Continue on to next Plot.
     *
     * @param player
     */
    @Command(names = {"review next"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewNext(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (!reviewPlayer.isReviewPartyLeader()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_leader"));
            return;
        }
        if (!(reviewPlayer.getReviewParty().hasGivenRating())) { // && reviewPlayer.getReviewParty().hasGivenFeedback()
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_everyone_finished"));
            return;
        }
        if (reviewPlayer.getReviewParty().getNextReviewPlot() == null) {
            for (ReviewPlayer i : reviewPlayer.getReviewParty().getReviewPlayers()) {
                i.sendMessage(TranslatableCaption.of("mcme.review.finished"));
                i.sendMessage(TranslatableCaption.of("mcme.review.end"));
            }
            reviewPlayer.getReviewParty().stopReviewParty();
            return;
        }
        reviewPlayer.getReviewParty().goNextPlot();
        player.sendMessage("Teleported to next plot");
    }

    /**
     * Rate a Plot between 0 and 100.
     *
     * @param player
     * @param rating
     */
    @Command(names = {"review rate"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewRate(Player player, @Param(name = "number") int rating) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (reviewPlayer.getReviewParty().getCurrentPlot().isOwner(player.getUniqueId())) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.is_plot_owner"));
            return;
        }
        if (reviewPlayer.hasAlreadyReviewed(reviewPlayer.getReviewParty().getCurrentReviewPlot())) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewed"));
            return;
        }
        if (0 > rating || rating > 100) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.rating_range"));
            return;
        }
        if (reviewPlayer.hasGivenRating()) {
            reviewPlayer.setPlotRating(rating);
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.rating_update"));
            return;
        }
        reviewPlayer.setPlotRating(rating);
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.rating"));
        if (reviewPlayer.getReviewParty().hasGivenRating() || reviewPlayer.hasAlreadyReviewed(reviewPlayer.getReviewParty().getCurrentReviewPlot())) {
            //if not last plot
            if (reviewPlayer.getReviewParty().getNextReviewPlot() != null) {
                for (ReviewPlayer i : reviewPlayer.getReviewParty().getReviewPlayers()) {
                    i.sendMessage(TranslatableCaption.of("mcme.review.next"));
                }
                reviewPlayer.getReviewParty().goNextPlot();
                reviewPlayer.getReviewParty().updateScoreboard();
                return;
            }
            //if last plot
            else {
                for (ReviewPlayer i : reviewPlayer.getReviewParty().getReviewPlayers()) {
                    i.sendMessage(TranslatableCaption.of("mcme.review.finished"));
                }
                reviewPlayer.getReviewParty().goNextPlot();
                reviewPlayer.getReviewParty().updateScoreboard();
                return;
            }
        }
        reviewPlayer.getReviewParty().updateScoreboard();
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.waiting"));
    }

    /**
     * Give feedback to a Plot.
     *
     * @param player
     * @param feedback
     */
    @Command(names = {"review feedback"}, permission = "mcmep2.review", playerOnly = true)
    public void reviewFeedback(Player player, @Param(name = "message", concated = true) String feedback) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!reviewPlayer.isReviewing()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_reviewing"));
            return;
        }
        if (reviewPlayer.getReviewParty().getCurrentPlot().isOwner(player.getUniqueId())) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.is_plot_owner"));
            return;
        }
        if (reviewPlayer.hasAlreadyReviewed(reviewPlayer.getReviewParty().getCurrentReviewPlot())) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.already_reviewed"));
            return;
        }
        if (reviewPlayer.hasGivenFeedback()) {
            reviewPlayer.setPlotFeedback(feedback);
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.feedback_updated"));
            return;
        }
        reviewPlayer.setPlotFeedback(feedback);
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.feedback"));
    }

    /**
     * Submit a Plot for review.
     *
     * @param player
     */
    @Command(names = {"review submit"}, permission = "mcmep2.submit", playerOnly = true)
    public void submitForRating(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot currentPlot = plotPlayer.getCurrentPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (plotPlayer.getCurrentPlot() == null) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        if (!currentPlot.isOwner(player.getUniqueId())) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.is_not_plot_owner"));
            return;
        }
        if (ReviewStatusFlag.isBeingReviewed(currentPlot)) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.submit_being_reviewed_this"));
            return;
        }
        if (ReviewStatusFlag.isAccepted(currentPlot)) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.submit_accepted"));
            return;
        }
        ReviewAPI.getReviewPlot(currentPlot).enoughComplexity(player, currentPlot);
    }

    public static void submitForRatingComplexity(Player player, Plot currentPlot, Boolean passed) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (!passed) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.submit_not_enough_complexity"));
            return;
        }
        final long THREEDAYSINMILISEC = 86400000 * 3;//DAYINMILISEC * 3
        ReviewPlot currentReviewPlot = ReviewAPI.getReviewPlot(currentPlot);
        long timestamp = currentPlot.getTimestamp();
        if (currentReviewPlot.getTimeOfLastReview() > timestamp) timestamp = currentReviewPlot.getTimeOfLastReview();
        for (Plot plot : plotPlayer.getPlots()) {
            if (plot == currentPlot) continue;
            //if ReviewAPI contains the plot it is being reviewed, or has been reviewed but not accepted
            if (ReviewAPI.getReviewPlot(plot) != null) {
                reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.submit_being_reviewed_other"));
                return;
            }
            //if the player has an accepted plot, check if it's been accepted more recently than the current timestamp
            if (ReviewStatusFlag.isAccepted(plot)) {
                ReviewPlot acceptedPlot = ReviewAPI.getReviewPlot(plot);
                if (acceptedPlot.getTimeOfLastReview() < timestamp) timestamp = acceptedPlot.getTimeOfLastReview();
            }
        }
        if (timestamp - System.currentTimeMillis() + THREEDAYSINMILISEC <= 0) {
            currentPlot.setFlag(ReviewStatusFlag.BEING_REVIEWED_FLAG);
            currentReviewPlot.submitReviewPlot(currentPlot);
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.submit"));
            return;
        }
        long durationInMillis = (timestamp - System.currentTimeMillis() + THREEDAYSINMILISEC);
        String minutes = String.valueOf((durationInMillis / (1000 * 60)) % 60) + " minutes, ";
        String hours = String.valueOf((durationInMillis / (1000 * 60 * 60)) % 24) + " hours and ";
        String days = String.valueOf((durationInMillis / (1000 * 60 * 60 * 24))) + " days";
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.submit_too_early"), templateOf("time", minutes + hours + days));
    }

    /**
     * Checks the review status of a Plot.
     *
     * @param player
     */
    @Command(names = {"review status"}, permission = "mcmep2.submit", playerOnly = true)
    public void reviewStatus(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        Plot plot = plotPlayer.getCurrentPlot();
        if (plot == null) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plot);
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
        String lastState = "";
        String lastRating = "";
        ReviewPlot.ReviewStatus flag = plot.getFlag(ReviewStatusFlag.class);
        if (flag == ReviewPlot.ReviewStatus.BEING_REVIEWED) {
            lastState = "<gray>Being Reviewed</gray>";
        } else if (flag == ReviewPlot.ReviewStatus.NOT_BEING_REVIEWED) {
            if (!reviewPlot.getFinalRatings().isEmpty()) {
                lastState = "<red>Rejected</red>";
                lastRating = "<red>" + reviewPlot.getFinalRatings().get(reviewPlot.getFinalRatings().size() - 1).toString() + "</red>";
            } else lastState = "<gray>Never Been Reviewed</gray>";
        } else if (flag == ReviewPlot.ReviewStatus.REJECTED) {
            lastState = "<red>Rejected</red>";
            lastRating = "<red>" + reviewPlot.getFinalRatings().get(reviewPlot.getFinalRatings().size() - 1).toString() + "</red>";
        } else if (flag == ReviewPlot.ReviewStatus.ACCEPTED || flag == ReviewPlot.ReviewStatus.LOCKED) {
            lastState = "<green>Accepted</green>";
            //get last rating
            getLogger().info("accepted " + reviewPlot.getFinalRatings().get(0).toString());
            lastRating = "<green>" + reviewPlot.getFinalRatings().get(reviewPlot.getFinalRatings().size() - 1).toString() + "</green>";
        }
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.review_state"), templateOf("state", lastState), templateOf("rating", lastRating));
        if (!reviewPlot.getFinalRatings().isEmpty()) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.id"), templateOf("id", plot.getId().toString()));
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.owner"), templateOf("owner", Bukkit.getOfflinePlayer(plot.getOwner()).getName()));
            List<Long> finalReviewTimeStamps = new ArrayList<>(reviewPlot.getFinalReviewTimeStamps());
            List<Long> finalReviewRatings = new ArrayList<>(reviewPlot.getFinalRatings());
            String previousRating = "";
            String notSameRating = "";
            for (int i = 0; i < finalReviewTimeStamps.size(); i++) {
                Long timeStamp = finalReviewTimeStamps.get(i);
                String rating = finalReviewRatings.get(i).toString();
                DateFormat simple = new SimpleDateFormat("dd MMM yyyy");
                Date date = new Date(timeStamp);
                if (rating.equals(notSameRating)) {
                    notSameRating = previousRating + " ";
                } else notSameRating = rating;
                previousRating = rating;
                reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.timestamp_and_rating"), templateOf("timestamp", simple.format(date) + " GMT"), templateOf("rating", notSameRating));
            }
        }
        if (!(reviewPlot.getPlotFinalFeedback().isEmpty())) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.feedback_click"), templateOf("command", "/review status feedback 0"));
        }
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
    }

    @Command(names = {"review status feedback"}, permission = "mcmep2.submit", playerOnly = true)
    public void reviewFeedback(Player player, @Param(name = "number") int page) {
        if(page < 0) return; // error page must be larger than 0
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        Plot plot = plotPlayer.getCurrentPlot();
        if (plot == null) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plot);
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.feedback_header"));
        int FEEDBACKPERPAGE = 5;
        // if amount of feedback is 5 then we get 5-1 / 5 rounded down which is 0, for 6 we get 1, amount of feedback can not be 0 according to method call
        int numberOfPages = Math.floorDiv(reviewPlot.getPlotFinalFeedback().size() - 1, FEEDBACKPERPAGE);
        int lastpage = Math.floorMod(reviewPlot.getPlotFinalFeedback().size(), FEEDBACKPERPAGE);
        ArrayList<String> feedback = new ArrayList();
        if (numberOfPages >= page) {
            for (int i = page * FEEDBACKPERPAGE; i < page * FEEDBACKPERPAGE + FEEDBACKPERPAGE && i < numberOfPages * FEEDBACKPERPAGE + lastpage; i++) {
                feedback.add(reviewPlot.getPlotFinalFeedback().get(i));
                getLogger().info(reviewPlot.getPlotFinalFeedback().get(i));
            }
        }
        String lastString = "";
        for (String s : feedback) {
            //check for same message because plotsquared has forced 5sec delay on same messages.
            String differentString;
            if (s.equals(lastString)) differentString = lastString + " ";
            else differentString = s;
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.feedback"), templateOf("string", differentString));
            lastString = s;
        }
        //logic for deciding whether to show back/next arrow
        if (0 < page && page < numberOfPages) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.feedback_page_both"), templateOf("back_command", "/review status feedback " + (page - 1)), templateOf("arrow_back", " ???"), templateOf("next_command", "/review status feedback " + (page + 1)), templateOf("arrow_next", "??? "));
            return;
        }
        if (0 == page && page < numberOfPages) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.feedback_page_next"), templateOf("next_command", "/review status feedback " + (page + 1)), templateOf("arrow_next", "??? "));
            return;
        }
        if (0 < page && page == numberOfPages) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.feedback_page_back"), templateOf("back_command", "/review status feedback " + (page - 1)), templateOf("arrow_back", " ???"));
            return;
        }
        if (0 == page && page == numberOfPages) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.feedback_page_none"));
            return;
        }

    }

    @Command(names = {"review debug"}, permission = "mcmep2.review.admin", playerOnly = true)
    public void debugPlots(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot plot = plotPlayer.getCurrentPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (plot == null) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        if (!(ReviewStatusFlag.isAccepted(plot))) {
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
            for (Long rating : reviewPlot.getFinalRatings()) {
                player.sendMessage(rating.toString());
            }
            player.sendMessage("Plot final timestamps");
            for (Long timeStamp : reviewPlot.getFinalReviewTimeStamps()) {
                DateFormat simple = new SimpleDateFormat("dd MMM yyyy");
                Date date = new Date(timeStamp);
                player.sendMessage(simple.format(date));
            }
        } else {
            player.sendMessage("Review Status");
            player.sendMessage(plot.getFlag(ReviewStatusFlag.class).toString());
            player.sendMessage("Plot final ratings");
            for (Long number : plot.getFlag(ReviewRatingDataFlag.class)) {
                if (number <= 100) {
                    player.sendMessage(number.toString());
                } else {
                    DateFormat simple = new SimpleDateFormat("dd MMM yyyy");
                    Date date = new Date(number);
                    player.sendMessage(simple.format(date));
                }
            }
        }
    }

    @Command(names = {"review delete"}, permission = "mcmep2.review.mod", playerOnly = true)
    public void deleteReview(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot plot = plotPlayer.getCurrentPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (plot == null) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        if (!commandConfirm.containsKey(player)) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.confirm_delete"));
            commandConfirm.put(player, new ReviewAPI.storeData(plot, reviewDelete));
            Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), () -> commandConfirm.remove(player), 20 * 10);
            return;
        }
        if (commandConfirm.get(player).storedCommand == reviewConfirm) {
            plot = commandConfirm.get(player).storedPlot;
            commandConfirm.remove(player);

            ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plot);
            if (reviewPlot.isBeingReviewed()) {
                player.sendMessage("This is currently being reviewed, try again later");
                return;
            }
            reviewPlot.deleteReview();
            Plot finalPlot = plot;
            final long start = System.currentTimeMillis();
            if (Settings.Teleport.ON_DELETE) {
                finalPlot.getPlayersInPlot().forEach(playerInPlot -> finalPlot.teleportPlayer(playerInPlot, TeleportCause.COMMAND_DELETE,
                        result -> {
                        }
                ));
            }
            PlotPlayer<?> plotOwner = BukkitUtil.adapt((Player) Bukkit.getOfflinePlayer(finalPlot.getOwner()));
            finalPlot.getPlotModificationManager().deletePlot(plotOwner, () -> {
                finalPlot.removeRunning();
                reviewPlayer.sendMessage(
                        TranslatableCaption.of("working.deleting_done"),
                        templateOf("amount", String.valueOf(System.currentTimeMillis() - start)),
                        templateOf("plot", finalPlot.getId().toString())
                );
            });
        }
    }

    @Command(names = {"review clear"}, permission = "mcmep2.review.mod", playerOnly = true)
    public void clearReview(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot plot = plotPlayer.getCurrentPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (plot == null) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        if (!commandConfirm.containsKey(player)) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.confirm_clear"));
            commandConfirm.put(player, new ReviewAPI.storeData(plot, reviewClear));
            Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), () -> commandConfirm.remove(player), 20 * 10);
            return;
        }
        if (commandConfirm.get(player).storedCommand == reviewConfirm) {
            plot = commandConfirm.get(player).storedPlot;
            commandConfirm.remove(player);

            ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plot);
            if (reviewPlot.isBeingReviewed()) {
                player.sendMessage("This is currently being reviewed, try again later");
                return;
            }
            reviewPlot.deleteReview();
            Plot finalPlot = plot;
            PlotPlayer<?> plotOwner = BukkitUtil.adapt((Player) Bukkit.getOfflinePlayer(finalPlot.getOwner()));
            BackupManager.backup(plotOwner, plot, () -> {
                final long start = System.currentTimeMillis();
                boolean result = finalPlot.getPlotModificationManager().clear(true, false, plotOwner, () -> {
                    finalPlot.getPlotModificationManager().unlink();
                    TaskManager.runTask(() -> {
                        finalPlot.removeRunning();
                        // If the state changes, then mark it as no longer done
                        if (DoneFlag.isDone(finalPlot)) {
                            PlotFlag<?, ?> plotFlag =
                                    finalPlot.getFlagContainer().getFlag(DoneFlag.class);
                        }
                        if (!finalPlot.getFlag(AnalysisFlag.class).isEmpty()) {
                            PlotFlag<?, ?> plotFlag =
                                    finalPlot.getFlagContainer().getFlag(AnalysisFlag.class);
                        }
                        reviewPlayer.sendMessage(
                                TranslatableCaption.of("working.clearing_done"),
                                templateOf("amount", String.valueOf(System.currentTimeMillis() - start)),
                                templateOf("plot", finalPlot.getId().toString())
                        );
                    });
                });
                if (!result) {
                    reviewPlayer.sendMessage(TranslatableCaption.of("errors.wait_for_timer"));
                } else {
                    finalPlot.addRunning();
                }
            });
        }
    }

    @Command(names = {"review leaderboard"}, playerOnly = true)
    public void reviewLeaderboard(Player player, @Param(name = "e") String queryOption, @Param(name = "ee") int page) {
        page = 0;
        FileManagement.topPlots(player.getName(), FileManagement.queryOption.allTime, page);

//        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.leaderboard_header"));
//        String entry = "";
//        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.leaderboard_entry"), templateOf("entry", entry));
//        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.leaderboard_footer"));
    }

    public static void sendLeaderboardMessage(Player player, List<Plot> list, String queryOption, int page) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, IllegalAccessException {
        if(page < 0) return; // no negative pages
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        if(list.isEmpty()){
            reviewPlayer.sendMessage(TranslatableCaption.of("invalid.found_no_plots"));
            return;
        }

        final int ENTRIESPERPAGE = 20;
        final int numberOfPages = Math.floorDiv(list.size() - 1, ENTRIESPERPAGE);
        final int lastpage = Math.floorMod(list.size(), ENTRIESPERPAGE);
        ArrayList<String> messages = new ArrayList<>();
        if (numberOfPages >= page) {
            for (int i = page * ENTRIESPERPAGE; i < page * ENTRIESPERPAGE + ENTRIESPERPAGE && i < numberOfPages * ENTRIESPERPAGE + lastpage; i++) {
                Plot plot = list.get(i);
                Caption color;
                if (plot.getOwner() == null) {
                    color = TranslatableCaption.of("info.plot_list_no_owner");
                } else if (plot.isOwner(plotPlayer.getUUID()) || plot.getOwner().equals(DBFunc.EVERYONE)) {
                    color = TranslatableCaption.of("info.plot_list_owned_by");
                } else if (plot.isAdded(plotPlayer.getUUID())) {
                    color = TranslatableCaption.of("info.plot_list_added_to");
                } else if (plot.isDenied(plotPlayer.getUUID())) {
                    color = TranslatableCaption.of("info.plot_list_denied_on");
                } else {
                    color = TranslatableCaption.of("info.plot_list_default");
                }
                Component trusted = ReviewPlayer.FakeMiniMessage.parse(
                        TranslatableCaption.of("info.plot_info_trusted").getComponent(plotPlayer),
                        templateOf("trusted", ReviewPlayer.getPlayerList(plot.getTrusted(), plotPlayer))
                );

                Component members = ReviewPlayer.FakeMiniMessage.parse(
                        TranslatableCaption.of("info.plot_info_members").getComponent(plotPlayer),
                        templateOf("members", ReviewPlayer.getPlayerList(plot.getMembers(), plotPlayer))
                );
                ReviewPlayer.Template.StringTemplate command_tp = templateOf("command_tp", "/plot visit " + plot.getArea() + ";" + plot.getId());
                ReviewPlayer.Template.StringTemplate command_info = templateOf("command_info", "/plot info " + plot.getArea() + ";" + plot.getId());
                ReviewPlayer.Template.StringTemplate hover_info =
                        templateOf(
                                "hover_info",
                                MiniMessage.builder().build().serialize(Component
                                        .text()
                                        .append(trusted)
                                        .append(Component.newline())
                                        .append(members)
                                        .asComponent())
                        );
                ReviewPlayer.Template.StringTemplate numberTemplate = templateOf("number", String.valueOf(i+1));
                ReviewPlayer.Template.ComponentTemplate plotTemplate = templateOf(
                        "plot",
                        parse(color.getComponent(plotPlayer), templateOf("plot", plot.toString()))
                );

                String prefix = "";
                String online = TranslatableCaption.of("info.plot_list_player_online").getComponent(plotPlayer);
                String offline = TranslatableCaption.of("info.plot_list_player_offline").getComponent(plotPlayer);
                String unknown = TranslatableCaption.of("info.plot_list_player_unknown").getComponent(plotPlayer);
                String server = TranslatableCaption.of("info.plot_list_player_server").getComponent(plotPlayer);
                String everyone = TranslatableCaption.of("info.plot_list_player_everyone").getComponent(plotPlayer);
                TextComponent.Builder builder = Component.text();
                if (plot.getFlag(ServerPlotFlag.class)) {
                    ReviewPlayer.Template.StringTemplate serverTemplate = templateOf(
                            "info.server",
                            TranslatableCaption.of("info.server").getComponent(plotPlayer)
                    );
                    builder.append(parse(server, serverTemplate));
                } else {
                    try {
                        final List<UUIDMapping> names = PlotSquared.get().getImpromptuUUIDPipeline().getNames(plot.getOwners())
                                .get(Settings.UUID.BLOCKING_TIMEOUT, TimeUnit.MILLISECONDS);
                        for (final UUIDMapping uuidMapping : names) {
                            PlotPlayer<?> pp = PlotSquared.platform().playerManager().getPlayerIfExists(uuidMapping.getUuid());
                            ReviewPlayer.Template.StringTemplate prefixTemplate = templateOf("prefix", prefix);
                            ReviewPlayer.Template.StringTemplate playerTemplate = templateOf("player", uuidMapping.getUsername());
                            if (pp != null) {
                                builder.append(parse(online, prefixTemplate, playerTemplate));
                            } else if (uuidMapping.getUsername().equalsIgnoreCase("unknown")) {
                                ReviewPlayer.Template.StringTemplate unknownTemplate = templateOf(
                                        "info.unknown",
                                        TranslatableCaption.of("info.unknown").getComponent(plotPlayer)
                                );
                                builder.append(MiniMessage.builder().build().parse(unknown, unknownTemplate));
                            } else if (uuidMapping.getUuid().equals(DBFunc.EVERYONE)) {
                                ReviewPlayer.Template.StringTemplate everyoneTemplate = templateOf(
                                        "info.everyone",
                                        TranslatableCaption.of("info.everyone").getComponent(plotPlayer)
                                );
                                builder.append(parse(everyone, everyoneTemplate));
                            } else {
                                builder.append(parse(offline, prefixTemplate, playerTemplate));
                            }
                            prefix = ", ";
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        final StringBuilder playerBuilder = new StringBuilder();
                        final Iterator<UUID> uuidIterator = plot.getOwners().iterator();
                        while (uuidIterator.hasNext()) {
                            final UUID uuid = uuidIterator.next();
                            playerBuilder.append(uuid);
                            if (uuidIterator.hasNext()) {
                                playerBuilder.append(", ");
                            }
                        }
                        reviewPlayer.sendMessage(
                                TranslatableCaption.of("errors.invalid_player"),
                                templateOf("value", playerBuilder.toString())
                        );
                    } catch (TimeoutException e) {
                        reviewPlayer.sendMessage(TranslatableCaption.of("players.fetching_players_timeout"));
                    }
                }
                ReviewPlayer.Template.ComponentTemplate players = templateOf("players", builder.asComponent());

//                caption.set(TranslatableCaption.of("info.plot_list_item"));
//                caption.setTemplates(command_tp, command_info, hover_info, numberTemplate, plotTemplate, players);

//                if(!plot.getAlias().isEmpty()) messages.add(plot.getAverageRating() + " - " + plot.getAlias() + " by " + Bukkit.getPlayer(plot.getOwner()).getName()); //if plot does not have a name
//                else messages.add(plot.getAverageRating() + " - " + plot.getId() + " by " + Bukkit.getPlayer(plot.getOwner()).getName()); //if plot has a bane
            }
        }
//        for(String i : messages){
//            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.leaderboard.entry"), templateOf("string", i));
//        }
        //logic for deciding whether to show back/next arrow
//        if (0 < page && page < numberOfPages) {
//            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.leaderboard.footer_page_both"), templateOf("back_command", "/review leaderboard " + queryOption + (page - 1)), templateOf("arrow_back", " ???"), templateOf("next_command", "/review leaderboard " + queryOption + (page + 1)), templateOf("arrow_next", "??? "));
//            return;
//        }
//        if (0 == page && page < numberOfPages) {
//            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.leaderboard.footer_page_next"), templateOf("next_command", "/review leaderboard " + queryOption + (page + 1)), templateOf("arrow_next", "??? "));
//            return;
//        }
//        if (0 < page && page == numberOfPages) {
//            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.leaderboard.footer_page_back"), templateOf("back_command", "/review leaderboard " + queryOption + (page - 1)), templateOf("arrow_back", " ???"));
//            return;
//        }
//        if (0 == page && page == numberOfPages) {
//            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.leaderboard.footer_page_none"));
//            return;
//        }
    }
        public <T> void paginate(
                PlotPlayer<?> player, List<T> c, int size, int page,
                RunnableVal3<Integer, T, CaptionHolder> add, String baseCommand, Caption header
    ) {
            // Calculate pages & index
            if (page < 0) {
                page = 0;
            }
            int totalPages = (int) Math.floor((double) c.size() / size);
            if (page > totalPages) {
                page = totalPages;
            }
            int max = page * size + size;
            if (max > c.size()) {
                max = c.size();
            }
            // Send the header
            ReviewPlayer.Template.StringTemplate curTemplate = templateOf("cur", String.valueOf(page + 1));
            ReviewPlayer.Template.StringTemplate maxTemplate = templateOf("max", String.valueOf(totalPages + 1));
            ReviewPlayer.Template.StringTemplate amountTemplate = templateOf("amount", String.valueOf(c.size()));
            ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(Bukkit.getPlayer(player.getUUID()));
            reviewPlayer.sendMessage((TranslatableCaption) header, curTemplate, maxTemplate, amountTemplate);
            // Send the page content
            List<T> subList = c.subList(page * size, max);
            int i = page * size;
            for (T obj : subList) {
                i++;
                final CaptionHolder msg = new CaptionHolder();
                add.run(i, obj, msg);
                player.sendMessage(msg.get(), msg.getTemplates());
            }
            // Send the footer
            ReviewPlayer.Template.StringTemplate command1 = templateOf("command1", baseCommand + " " + page);
            ReviewPlayer.Template.StringTemplate command2 = templateOf("command2", baseCommand + " " + (page + 2));
            ReviewPlayer.Template.StringTemplate clickable = templateOf("clickable", TranslatableCaption.of("list.clickable").getComponent(player));
            reviewPlayer.sendMessage(TranslatableCaption.of("list.page_turn"), command1, command2, clickable);
        }




    @Command(names = {"review restart"}, permission = "mcmep2.review.admin", playerOnly = true)
    public void restartReview(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot plot = plotPlayer.getCurrentPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (plot == null) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        if (!commandConfirm.containsKey(player)) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.confirm_restart"));
            commandConfirm.put(player, new ReviewAPI.storeData(plot, reviewRestart));
            Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), () -> commandConfirm.remove(player), 20 * 10);
            return;
        }
        if (commandConfirm.get(player).storedCommand == reviewConfirm) {
            plot = commandConfirm.get(player).storedPlot;
            commandConfirm.remove(player);

            ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plot);
            if (reviewPlot.isBeingReviewed()) {
                player.sendMessage("This is currently being reviewed, try again later");
                return;
            }
            reviewPlot.deleteReview();
        }
    }

    @Command(names = {"review force submit"}, permission = "mcmep2.review.admin", playerOnly = true)
    public void forceReview(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot plot = plotPlayer.getCurrentPlot();
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if (plot == null) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.error.not_in_plot"));
            return;
        }
        if (!commandConfirm.containsKey(player)) {
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.confirm_force"));
            commandConfirm.put(player, new ReviewAPI.storeData(plot, reviewForce));
            Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), () -> commandConfirm.remove(player), 20 * 10);
            return;
        }
        if (commandConfirm.get(player).storedCommand == reviewForce) {
            plot = commandConfirm.get(player).storedPlot;
            commandConfirm.remove(player);

            ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plot);
            if (reviewPlot.isBeingReviewed()) {
                player.sendMessage("This is currently being reviewed, try again later");
                return;
            }
            reviewPlot.submitReviewPlot(plot);
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.submit"));
        }
    }

    @Command(names = {"review force rate"}, permission = "mcmep2.review.admin", playerOnly = true)
    public void forceRating(Player player, @Param(name = "number") int rating) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot plot = plotPlayer.getCurrentPlot();
        ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plot);
        reviewPlot.getPlotTempRatings().add(rating);
    }

    @Command(names = {"review force end"}, permission = "mcmep2.review.admin", playerOnly = true)
    public void forceEnd(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot plot = plotPlayer.getCurrentPlot();
        ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plot);
        ReviewParty reviewParty = new ReviewParty();
        reviewPlot.endPlotReview(reviewParty);
    }

    @Command(names = {"review force done"}, permission = "mcmep2.review.admin", playerOnly = true)
    public void forceDone(Player player, @Param(name = "seconds since epoch") int rating) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        Plot plot = plotPlayer.getCurrentPlot();
        PlotFlag<?, ?> doneFlag = plot.getFlagContainer().getFlag(DoneFlag.class).createFlagInstance(Integer.toString(rating));
        plot.setFlag(doneFlag);
    }

    @Command(names = {"review confirm"}, permission = "mcmep2.review.mod", playerOnly = true)
    public void confirmReview(Player player) {
        if (commandConfirm.containsKey(player)) {
            ReviewAPI.ReviewCommands storedCommand = commandConfirm.get(player).getStoredCommand();
            Plot plot = commandConfirm.get(player).getStoredPlot();
            if (storedCommand == reviewClear) {
                commandConfirm.put(player, new ReviewAPI.storeData(plot, reviewConfirm));
                clearReview(player);
                return;
            } else if (storedCommand == reviewDelete) {
                commandConfirm.put(player, new ReviewAPI.storeData(plot, reviewConfirm));
                deleteReview(player);
                return;
            } else if (storedCommand == reviewRestart) {
                commandConfirm.put(player, new ReviewAPI.storeData(plot, reviewConfirm));
                restartReview(player);
                return;
            } else if (storedCommand == reviewForce) {
                commandConfirm.put(player, new ReviewAPI.storeData(plot, reviewForce));
                forceReview(player);
                return;
            }
        }
        player.sendMessage("nothing to confirm");
    }
}