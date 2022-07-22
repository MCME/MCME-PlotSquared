package com.mcmiddleearth.plotsquared.command;

import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.player.PlotPlayer;
import me.gleeming.command.Command;
import me.gleeming.command.paramter.Param;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.UUID;

public class ReviewCommands {

    @Command(names = {"review start"}, playerOnly = true)
    public void reviewStart(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if(reviewPlayer.isReviewing()) {
            player.sendMessage("You're already reviewing!");
            return;
        }
        if(ReviewAPI.getReviewPlotsCollection().isEmpty()) {
            player.sendMessage("No plots to review");
            return;
        }
        for(ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsCollection()) {
            if(reviewPlayer.hasAlreadyRated(reviewPlot)) {
                player.sendMessage("You've already reviewed all plots to review");
                return;
            }
        }
        ReviewParty.startReviewParty(player);
        player.sendMessage("You're now reviewing!");
    }

    @Command(names = {"review stop"}, playerOnly = true)
    public void reviewStop(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if(!reviewPlayer.isReviewing()){
            player.sendMessage("You're not reviewing!");
            return;
        }
        if(!reviewPlayer.isReviewPartyLeader()){
            player.sendMessage("You're not the reviewparty leader");
            return;
        }
        reviewPlayer.getReviewParty().stopParty();
        player.sendMessage("Reviewparty ended");
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
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
        if(reviewPlayer.isReviewing()) {
            player.sendMessage("You're already reviewing!");
            return;
        }
        if(reviewTarget.isReviewing()) {
            reviewTarget.getReviewParty().addReviewPlayer(reviewPlayer);
            player.sendMessage("joined reviewparty");
        }
    }

    @Command(names = {"review leave"}, playerOnly = true)
    public void reviewLeave(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if(!reviewPlayer.isReviewing()){
            player.sendMessage("You're not reviewing!");
            return;
        }
        if(reviewPlayer.isReviewPartyLeader()){
            reviewPlayer.getReviewParty().stopParty();
            player.sendMessage("Stopped reviewparty");
            return;
        }
        reviewPlayer.leaveReviewParty();
        player.sendMessage("Left reviewparty");
    }

    @Command(names = {"review kick"}, playerOnly = true)
    public void reviewKick(Player player, @Param(name = "player") Player target) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        ReviewPlayer reviewTarget = ReviewAPI.getReviewPlayer(target);
        if(!reviewPlayer.isReviewing()){
            player.sendMessage("You're not reviewing!");
            return;
        }
        if(!reviewPlayer.isReviewPartyLeader()){
            player.sendMessage("You're not the reviewparty leader");
            return;
        }
        if(!reviewTarget.isReviewing()){
            player.sendMessage("Target is not reviewing");
            return;
        }
        if(reviewTarget.getReviewParty() != reviewPlayer.getReviewParty()){
            player.sendMessage("Target is not in your party!");
            return;
        }
        if(reviewPlayer == reviewTarget){
            player.sendMessage("Can't kick yourself");
            return;
        }
        reviewTarget.leaveReviewParty();
        player.sendMessage("Kicked target");
    }

    @Command(names = {"review next"}, playerOnly = true)
    public void reviewNext(Player player) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if(!reviewPlayer.isReviewing()){
            player.sendMessage("You're not reviewing!");
            return;
        }
        if(!reviewPlayer.isReviewPartyLeader()){
            player.sendMessage("You're not the reviewparty leader");
            return;
        }
        if(!(reviewPlayer.getReviewParty().hasGivenFeedback() && reviewPlayer.getReviewParty().hasGivenRating())){
            player.sendMessage("Not everyone is finished reviewing");
            return;
        }
        if(reviewPlayer.getReviewParty().getNextReviewPlot() == null){
            player.sendMessage("No plots left to review. Ending review party");
            reviewPlayer.getReviewParty().stopParty();
            return;
        }
        reviewPlayer.getReviewParty().goNextPlot();
        player.sendMessage("Going to next plot");
    }

    @Command(names = {"review rate"}, playerOnly = true)
    public void reviewRate(Player player, @Param(name = "number") int rating) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if(!reviewPlayer.isReviewing()){
            player.sendMessage("You're not reviewing!");
            return;
        }
        if(reviewPlayer.getReviewParty().getCurrentPlot().isOwner(player.getUniqueId())){
            player.sendMessage("Can't rate your own plot!");
            return;
        }
        if(reviewPlayer.hasAlreadyRated(reviewPlayer.getReviewParty().getCurrentReviewPlot())){
            player.sendMessage("You've already submitted a rating for this plot");
            return;
        }
        if(!reviewPlayer.hasGivenFeedback()){
            player.sendMessage("First give feedback you lazy duck!");
            return;
        }
        if(0 >= rating || rating >= 100){
            player.sendMessage("give rating between 0 and 100");
            return;
        }

        if(reviewPlayer.hasGivenRating()){
            reviewPlayer.setPlotRating(rating);
            player.sendMessage("Updated rating");
            return;
        }
        reviewPlayer.setPlotRating(rating);
        player.sendMessage("Given rating");
        if(reviewPlayer.getReviewParty().hasGivenRating() && reviewPlayer.getReviewParty().hasGivenFeedback()){
            reviewPlayer.getReviewParty().goNextPlot();
            for(ReviewPlayer i : reviewPlayer.getReviewParty().getAllReviewers())
                Bukkit.getPlayer(i.getUniqueId()).sendMessage("Everyone is finished, going to next plot");
            return;
        }
        player.sendMessage("Waiting for others to finish their review");
    }

    @Command(names = {"review feedback"}, playerOnly = true)
    public void reviewFeedback(Player player, @Param(name = "message") String feedback) {
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        if(!reviewPlayer.isReviewing()){
            player.sendMessage("You're not reviewing!");
            return;
        }
        if(reviewPlayer.getReviewParty().getCurrentPlot().isOwner(player.getUniqueId())){
            player.sendMessage("Can't give feedback to your own plot!");
            return;
        }
        if(reviewPlayer.hasAlreadyRated(reviewPlayer.getReviewParty().getCurrentReviewPlot())){
            player.sendMessage("You've already submitted a rating for this plot");
            return;
        }
        if(reviewPlayer.hasGivenFeedback()){
            reviewPlayer.setPlotFeedback(feedback);
            player.sendMessage("Updated feedback");
            return;
        }
        reviewPlayer.setPlotFeedback(feedback);
        player.sendMessage("Given feedback, now give rating");
    }

    @Command(names = {"review submit"}, playerOnly = true)
    public void submitForRating(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        if (plotPlayer.getCurrentPlot() == null){
            player.sendMessage("You're not in a plot!");
            return;
        }
        player.performCommand("plot done");
    }

    @Command(names = {"review check"}, playerOnly = true)
    public void checkRating(Player player) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        if (plotPlayer.getCurrentPlot() == null){
            player.sendMessage("You're not in a plot!");
            return;
        }
        ReviewPlot reviewPlot = ReviewAPI.getReviewPlot(plotPlayer.getCurrentPlot());

        player.sendMessage("Review Status");
        player.sendMessage(reviewPlot.getPlot().getFlag(ReviewStatusFlag.class).toString());
        player.sendMessage("Player Review Amount");
        for (UUID name: reviewPlot.getPlayerReviewIterationMap().keySet()) {
            String key = Bukkit.getOfflinePlayer(name).getName();
            String value = reviewPlot.getPlayerReviewIterationMap().get(name).toString();
            player.sendMessage(key + " " + value);
        }
        player.sendMessage("Plot Temp ratings");
        for(Integer rating : reviewPlot.getPlotTempRatings()){
            player.sendMessage(rating.toString());
        }
        player.sendMessage("Plot final feedback");
        for(String feedback : reviewPlot.getPlotFinalFeedback()){
            player.sendMessage(feedback);
        }
        player.sendMessage("Plot final ratings");
        for(Long rating : reviewPlot.getPlotFinalRatings()){
            player.sendMessage(rating.toString());
        }
        player.sendMessage("Plot final timestamps");
        for(Long timeStamp : reviewPlot.getPlotFinalReviewTimeStamps()){
            player.sendMessage(timeStamp.toString());
        }


    }
}
