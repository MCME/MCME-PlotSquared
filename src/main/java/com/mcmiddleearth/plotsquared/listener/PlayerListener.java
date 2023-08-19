package com.mcmiddleearth.plotsquared.listener;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.mcmiddleearth.plotsquared.review.plot.ReviewPlot;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static com.mcmiddleearth.plotsquared.review.ReviewPlayer.Template.templateOf;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent playerJoinEvent) {
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(playerJoinEvent.getPlayer());
        for (Plot plot : plotPlayer.getPlots()) {
            switch (plot.getFlag(ReviewStatusFlag.class)) {
                case ACCEPTED_NOT_SEEN -> Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), () -> {
                    if (!playerJoinEvent.getPlayer().isOnline()) return;
                    plot.setFlag(ReviewStatusFlag.LOCKED_FLAG);
                    sendAcceptedMessage(playerJoinEvent.getPlayer(), plot);
                }, 20 * 3);
                case REJECTED_NOT_SEEN -> Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), () -> {
                    if (!playerJoinEvent.getPlayer().isOnline()) return;
                    plot.setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
                    sendRejectedMessage(playerJoinEvent.getPlayer(), plot);
                }, 20 * 3);
                case FEEDBACK_NOT_SEEN -> Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), () -> {
                    if (!playerJoinEvent.getPlayer().isOnline()) return;
                    plot.setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
                    sendFeedbackMessage(playerJoinEvent.getPlayer());
                }, 20 * 3);
            }
        }
        if (playerJoinEvent.getPlayer().hasPermission("mcmep2.review")) {
            Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), () -> {
                if (!playerJoinEvent.getPlayer().isOnline()) return;
                ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(playerJoinEvent.getPlayer());
                int reviewablePlotsCount = 0;
                for (ReviewPlot reviewPlot : ReviewAPI.getReviewPlotsToBeReviewed()) {
                    if (!reviewPlot.wasReviewedBy(reviewPlayer)) {
                        reviewablePlotsCount++;
                    }
                }
                if (reviewablePlotsCount != 0) {
                    playNotificationSound(playerJoinEvent.getPlayer());
                    reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.join_review_available"), templateOf("amount", String.valueOf(reviewablePlotsCount)));
                }
            }, 20 * 5);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent) {
        Player player = playerQuitEvent.getPlayer();
        if (!ReviewAPI.isReviewPlayer(player)) {
            return;
        }
        Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), () -> {
            if (player.isOnline()) return;
            if (!ReviewAPI.isReviewPlayer(player)) {
                return;
            }
            ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
            ReviewParty reviewParty = reviewPlayer.getReviewParty();
            if (reviewPlayer.isReviewPartyLeader()) {
                for (ReviewPlayer i : reviewParty.getReviewPlayers()) {
                    i.sendMessage(TranslatableCaption.of("mcme.review.leader_left"));
                }
                reviewParty.stopReviewParty();
                return;
            }
            if (ReviewAPI.getReviewParties().containsKey(reviewParty.getId())) {
                for (ReviewPlayer i : reviewParty.getReviewPlayers()) {
                    i.sendMessage(TranslatableCaption.of("mcme.review.left_notif"), templateOf("player", player.getName()));
                }
                reviewParty.removeReviewPlayer(reviewPlayer);
            }
        }, 20 * 60 * 5);
    }

    private void sendAcceptedMessage(Player player, Plot plot){
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        playNotificationSound(player);
        String rating = String.valueOf(ReviewAPI.getReviewPlot(plot).getFinalRatings().get(ReviewAPI.getReviewPlot(plot).getFinalRatings().size() - 1));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_accepted"), templateOf("rating", rating));
        int allowedPlots = plotPlayer.getAllowedPlots();
        if (allowedPlots == 1)
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plot"), templateOf("amount", String.valueOf(allowedPlots)));
        else
            reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plots"), templateOf("amount", String.valueOf(allowedPlots)));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
    }

    private void sendRejectedMessage(Player player, Plot plot){
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(player);
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        playNotificationSound(player);
        String rating = String.valueOf(ReviewAPI.getReviewPlot(plot).getFinalRatings().get(ReviewAPI.getReviewPlot(plot).getFinalRatings().size() - 1));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rejected"), templateOf("rating", rating));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rejected_2"));
        int allowedPlots = plotPlayer.getAllowedPlots();
        switch (allowedPlots) {
            case 0 -> reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.no_plot"));
            case 1 -> reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plot"), templateOf("amount", String.valueOf(allowedPlots)));
            default -> reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.new_plots"), templateOf("amount", String.valueOf(allowedPlots)));
        }
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
    }

    private void sendFeedbackMessage(Player player){
        ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
        playNotificationSound(player);
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.header"));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.plot_rating_received"));
        reviewPlayer.sendMessage(TranslatableCaption.of("mcme.review.status.footer"));
    }

    private void playNotificationSound(Player player){
        Sound myCustomSound = Sound.sound(Key.key("note.pling"), Sound.Source.AMBIENT, 5F, 0.3F);
        player.playSound(myCustomSound, Sound.Emitter.self());
    }

}
