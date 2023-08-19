package com.mcmiddleearth.plotsquared.command;

import dev.jorel.commandapi.annotations.Command;
import dev.jorel.commandapi.annotations.Default;
import dev.jorel.commandapi.annotations.Permission;
import dev.jorel.commandapi.annotations.Subcommand;
import com.mcmiddleearth.plotsquared.review.ReviewAPI;
import org.bukkit.entity.Player;

@Command("theme")
public class ThemeCommands {

    @Default
    @Permission("mcmep2.review")
    public static void themeSuggest(Player player) {
        String answer = generateThemeSuggestion();
    }

    private static String generateThemeSuggestion() {
        System.currentTimeMillis();
        return "";
    }
}
