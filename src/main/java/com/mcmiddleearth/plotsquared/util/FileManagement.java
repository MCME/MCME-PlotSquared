package com.mcmiddleearth.plotsquared.util;

import com.mcmiddleearth.plotsquared.command.ReviewCommands;
import com.mcmiddleearth.plotsquared.plotflag.ReviewStatusFlag;
import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.plotsquared.core.configuration.caption.Templates;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.permissions.Permission;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.implementations.DoneFlag;
import com.plotsquared.core.util.query.PlotQuery;
import com.plotsquared.core.util.query.SortingStrategy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Consumer;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

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
    public static ReviewPlot readObjectFromFile(File file){
        ReviewPlot result = null;
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            result = (ReviewPlot) ois.readObject();
            ois.close();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return result;
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
                try {
                    ReviewCommands.sendLeaderboardMessage(player, list, option.toString(), page);
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
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
                        .thatPasses(ReviewStatusFlag::isAccepted)
                        .thatPasses(plot -> (Long.parseLong(plot.getFlag(DoneFlag.class)) < currentTimeInSeconds + DayInSeconds))//check if it's been accepted for less than 30 days
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }
            case oneWeek -> {
                plotConsumer.accept(PlotQuery
                        .newQuery()
                        .whereBasePlot()
                        .thatPasses(ReviewStatusFlag::isAccepted)
                        .thatPasses(plot -> (Long.parseLong(plot.getFlag(DoneFlag.class)) < currentTimeInSeconds + DayInSeconds*7))//check if it's been accepted for less than 30 days
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }
            case oneMonth -> {
                plotConsumer.accept(PlotQuery
                        .newQuery()
                        .whereBasePlot()
                        .thatPasses(ReviewStatusFlag::isAccepted)
                        .thatPasses(plot -> (Long.parseLong(plot.getFlag(DoneFlag.class)) < currentTimeInSeconds + DayInSeconds*30))//check if it's been accepted for less than 30 days
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }
            case oneYear -> {
                plotConsumer.accept(PlotQuery
                        .newQuery()
                        .whereBasePlot()
                        .thatPasses(ReviewStatusFlag::isAccepted)
                        .thatPasses(plot -> (Long.parseLong(plot.getFlag(DoneFlag.class)) < currentTimeInSeconds + DayInSeconds*356))//check if it's been accepted for less than 30 days
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }
            case allTime -> {
                plotConsumer.accept(PlotQuery
                        .newQuery()
                        .whereBasePlot()
                        .thatPasses(ReviewStatusFlag::isAccepted)
                        .withSortingStrategy(SortingStrategy.SORT_BY_RATING));
            }

        }
    }


}