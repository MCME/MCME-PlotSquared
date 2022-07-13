package com.mcmiddleearth.plotsquared.command;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import com.mcmiddleearth.plotsquared.review.ReviewParty;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getLogger;

public class ReviewPartyStart implements CommandExecutor {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player)){
            getLogger().info("Command can only be sent by player");
            return false;
        }
        Player player = (Player) sender;

        if(args.length == 0 || args[0].equalsIgnoreCase("help")) { // if no args or help
            //showHelp(p);
        } else if(args[0].equalsIgnoreCase("start")) { // if the first arg is what you want
            //if player is not already reviewing
            if(!ReviewAPI.isReviewing(player)){
                ReviewParty reviewParty = ReviewParty.startReviewParty(player);
                return true;
            }
        }
       else
           player.sendMessage("You're already reviewing!");
           return false;
    }

}
