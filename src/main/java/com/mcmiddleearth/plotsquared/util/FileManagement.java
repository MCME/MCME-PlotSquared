package main.java.com.mcmiddleearth.plotsquared.util;

import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.util.query.PlotQuery;
import com.plotsquared.core.util.query.SortingStrategy;
import main.java.com.mcmiddleearth.plotsquared.MCMEP2;
import main.java.com.mcmiddleearth.plotsquared.command.ReviewCommands;
import main.java.com.mcmiddleearth.plotsquared.review.ReviewAPI;
import main.java.com.mcmiddleearth.plotsquared.review.plot.ReviewPlot;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Consumer;

import java.io.*;
import java.util.List;

import static org.bukkit.Bukkit.getLogger;

public class FileManagement {

    public static void writeObjectToFile(ReviewPlot obj, File file){
        try (FileOutputStream fos = new FileOutputStream(file);
             ObjectOutputStream oos = new ObjectOutputStream(fos)) {
            oos.writeObject(obj);
            oos.flush();
            oos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Deserialization
    // Get object from a file.
    public static Object readObjectFromFile(File file){
        Object result = null;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            result = ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ReviewPlot loadReviewPlot(Plot plot){
        String plotId = plot.getId().toString();
        File file = new File(MCMEP2.getReviewPlotDirectory(), File.separator + plotId + ".yml");
        if (!file.exists()) return null;
        else return (ReviewPlot) FileManagement.readObjectFromFile(file);
    }

    public static void deleteReviewPlotFromDisk(ReviewPlot reviewPlot) {
        File file = new File(MCMEP2.getReviewPlotDirectory(), reviewPlot.getPlotId().toString() + ".yml");
        if (!file.exists()) {
            return;
        }
        if (file.delete()) {
            getLogger().info("Deleted the file: " + file.getName());
        } else {
            getLogger().info("Failed to delete: " + file.getName());
        }
    }


    public enum queryOption{
        oneDay,
        oneWeek,
        oneMonth,
        oneYear,
        allTime
    }

    public static void topPlots(String consumer, queryOption option, int page) {
        final Consumer<PlotQuery> plotConsumer = query -> {
            final List<Plot> list = query.asList();
            if(!consumer.equals("showCasePlots")){
                Player player = Bukkit.getPlayer(consumer);
                    ReviewCommands.sendLeaderboardMessage(player, list, page);
            }
            else{
            }
        };
        long currentTimeInSeconds = System.currentTimeMillis()/1000;
        long DayInSeconds = 86400;
        switch (option) {
            case oneDay -> {
                plotConsumer.accept(PlotQuery
                        .newQuery()
                        .whereBasePlot()
                        .thatPasses(plot -> ReviewAPI.getReviewPlot(plot).isAccepted())
                        .thatPasses(plot -> (Long.parseLong(plot.getFlag(DoneFlag.class)) < currentTimeInSeconds + DayInSeconds))//check if it's been accepted for less than 30 days
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }
            case oneWeek -> {
                plotConsumer.accept(PlotQuery
                        .newQuery()
                        .whereBasePlot()
                        .thatPasses(plot -> ReviewAPI.getReviewPlot(plot).isAccepted())
                        .thatPasses(plot -> (Long.parseLong(plot.getFlag(DoneFlag.class)) < currentTimeInSeconds + DayInSeconds*7))//check if it's been accepted for less than 30 days
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }
            case oneMonth -> {
                plotConsumer.accept(PlotQuery
                        .newQuery()
                        .whereBasePlot()
                        .thatPasses(plot -> ReviewAPI.getReviewPlot(plot).isAccepted())
                        .thatPasses(plot -> (Long.parseLong(plot.getFlag(DoneFlag.class)) < currentTimeInSeconds + DayInSeconds*30))//check if it's been accepted for less than 30 days
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }
            case oneYear -> {
                plotConsumer.accept(PlotQuery
                        .newQuery()
                        .whereBasePlot()
                        .thatPasses(plot -> ReviewAPI.getReviewPlot(plot).isAccepted())
                        .thatPasses(plot -> (Long.parseLong(plot.getFlag(DoneFlag.class)) < currentTimeInSeconds + DayInSeconds*356))//check if it's been accepted for less than 30 days
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }
            case allTime -> {
                plotConsumer.accept(PlotQuery
                        .newQuery()
                        .whereBasePlot()
                        .thatPasses(plot -> ReviewAPI.getReviewPlot(plot).isAccepted())
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }

        }
    }


}