package com.mcmiddleearth.plotsquared.listener;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static com.mcmiddleearth.plotsquared.review.ReviewPlayer.templateOf;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent playerJoinEvent){
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(playerJoinEvent.getPlayer());
        for (Plot i : plotPlayer.getPlots()){
            switch (i.getFlag(ReviewStatusFlag.class)) {
                case ACCEPTED -> {
                    i.setFlag(ReviewStatusFlag.LOCKED_FLAG);
                    String rating = String.valueOf(ReviewAPI.getReviewPlot(i).getFinalRatings().get(ReviewAPI.getReviewPlot(i).getFinalRatings().size()-1));
                    ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(playerJoinEvent.getPlayer());
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_accepted"), templateOf("rating", rating));
                    int allowedPlots = plotPlayer.getAllowedPlots();
                    if(allowedPlots == 1) reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plot"), templateOf("amount", String.valueOf(allowedPlots)));
                    else reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plots"), templateOf("amount", String.valueOf(allowedPlots)));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
                }
                case REJECTED -> {
                    i.setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
                    String rating = String.valueOf(ReviewAPI.getReviewPlot(i).getFinalRatings().get(ReviewAPI.getReviewPlot(i).getFinalRatings().size()-1));
                    ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(playerJoinEvent.getPlayer());
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rejected"), templateOf("rating", rating));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rejected_2"));
                    int allowedPlots = plotPlayer.getAllowedPlots();
                    if(allowedPlots == 0) reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.no_plot"));
                    if(allowedPlots == 1) reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plot"), templateOf("amount", String.valueOf(allowedPlots)));
                    else reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plots"), templateOf("amount", String.valueOf(allowedPlots)));
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
                }
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent){
        Player player = playerQuitEvent.getPlayer();
        if (ReviewAPI.isReviewPlayer(player)) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), new Runnable() {
                @Override
                public void run() {
                    if (player.isOnline()) return;
                    if (ReviewAPI.isReviewPlayer(player)) {
                        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
                        ReviewParty reviewParty = reviewPlayer.getReviewParty();
                        if (ReviewAPI.getReviewParties().containsKey(reviewParty.getId())) {
                            for (ReviewPlayer i : reviewParty.getAllReviewers()) {
                                i.sendMessage(TranslatableCaption.of("mcme.review.left_notif"), templateOf("player", player.getName()));
                            }
                            reviewParty.removeReviewPlayer(reviewPlayer);
                            return;
                        }
                        if (reviewPlayer.isReviewPartyLeader()) {
                            for (ReviewPlayer i : reviewParty.getAllReviewers()) {
                                i.sendMessage(TranslatableCaption.of("mcme.review.leader_left"));
                            }
                            reviewParty.stopReviewParty();
                            return;
                        }
                    }
                }
            }, 20 * 60 * 5);
        }
    }
}
