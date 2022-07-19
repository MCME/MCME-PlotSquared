package com.mcmiddleearth.plotsquared.listener;

import com.mcmiddleearth.plotsquared.MCMEP2;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import com.mcmiddleearth.plotsquared.review.ReviewPlayer;
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
        PlotPlayer<?> plotPlayer = MCMEP2.getPlotAPI().wrapPlayer(playerJoinEvent.getPlayer().getUniqueId());
        ReviewStatusFlag reviewStatus = ReviewStatusFlag.BEING_REVIEWED_FLAG;
        for (Plot i : plotPlayer.getPlots()){
            switch (i.getFlag(reviewStatus)){
                case BEING_REVIEWED:
                    playerJoinEvent.getPlayer().sendMessage("Your plot is being reviewed");
                case NOT_BEING_REVIEWED:
                    // do nothing
                case ACCEPTED:
                    playerJoinEvent.getPlayer().sendMessage("Congratulations your plot has been accepted!");
                    playerJoinEvent.getPlayer().sendMessage("Go to it now to lock it and obtain a new plot");
                case REJECTED:
                    playerJoinEvent.getPlayer().sendMessage("Unfortunately your plot did not get accepted");
                    i.setFlag(ReviewStatusFlag.NOT_BEING_REVIEWED_FLAG); // do not bother them with it the next join :P
                case LOCKED:
                    // do nothing
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent playerQuitEvent){
        Player player = playerQuitEvent.getPlayer();
        Bukkit.getScheduler().scheduleSyncDelayedTask(MCMEP2.getInstance(), new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) return;
                if(ReviewAPI.isReviewPlayer(player)){
                    ReviewPlayer reviewPlayer = ReviewAPI.getReviewPlayer(player);
                    ReviewParty reviewParty = reviewPlayer.getReviewParty();
                    if(reviewPlayer.isReviewPartyLeader()) {
                        for(ReviewPlayer i : reviewParty.getAllReviewers()){
                            Bukkit.getPlayer(i.getUniqueId()).sendMessage("The reviewparty leader has been gone for too long, ending review.");
                        }
                        reviewParty.stopParty();
                        return;
                    }
                    if(ReviewAPI.getReviewParties().containsKey(reviewParty.getId())) {
                        for(ReviewPlayer i : reviewParty.getAllReviewers()){
                            Bukkit.getPlayer(i.getUniqueId()).sendMessage(player.getName().toString() + "has left the party");
                        }
                        reviewParty.removeReviewPlayer(reviewPlayer);
                        return;
                    }
                }
            }
        }, 20*60*5);
    }
}
