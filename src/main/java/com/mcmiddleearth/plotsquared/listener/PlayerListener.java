package com.mcmiddleearth.plotsquared.listener;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.plot.Plot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent playerJoinEvent){
        PlotPlayer<?> plotPlayer = BukkitUtil.adapt(playerJoinEvent.getPlayer());
        ReviewStatusFlag reviewStatus = ReviewStatusFlag.BEING_REVIEWED_FLAG;
        for (Plot i : plotPlayer.getPlots()){
            switch (i.getFlag(reviewStatus)) {
                case ACCEPTED -> {
                    i.setFlag(ReviewStatusFlag.LOCKED_FLAG);
                    playerJoinEvent.getPlayer().sendMessage("Congratulations your plot has been accepted!");
                    playerJoinEvent.getPlayer().sendMessage("You can now claim a new plot");
                }
                case REJECTED -> {
                    i.setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG);
                    playerJoinEvent.getPlayer().sendMessage("Unfortunately your plot did not get accepted");
                    playerJoinEvent.getPlayer().sendMessage("Feel free to try again.");
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
                                Bukkit.getPlayer(i.getUniqueId()).sendMessage(player.getName().toString() + "has left the party");
                            }
                            reviewParty.removeReviewPlayer(reviewPlayer);
                            return;
                        }
                        if (reviewPlayer.isReviewPartyLeader()) {
                            for (ReviewPlayer i : reviewParty.getAllReviewers()) {
                                Bukkit.getPlayer(i.getUniqueId()).sendMessage("The reviewparty leader has been gone for too long, ending review.");
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
