package com.mcmiddleearth.plotsquared.review;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.PlotSquared;
import com.plotsquared.core.configuration.Settings;
import com.plotsquared.core.configuration.caption.LocaleHolder;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.database.DBFunc;
import com.plotsquared.core.events.TeleportCause;
import com.plotsquared.core.player.ConsolePlayer;
import com.plotsquared.core.player.PlotPlayer;
import com.plotsquared.core.uuid.UUIDMapping;
import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.minimessage.*;
import net.kyori.adventure.text.serializer.ComponentSerializer;
import net.kyori.adventure.util.Buildable;
import net.kyori.examination.Examinable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.mcmiddleearth.plotsquared.review.ReviewPlayer.Template.templateOf;

/**
 * A reviewPlayer is a container for reviewer data related to a single player.
 */
public class ReviewPlayer {
    private static final String NON_EXISTENT_CAPTION = "<red>PlotSquared does not recognize the caption: ";

    private final UUID PLAYERUUID;
    private ReviewParty reviewParty;
    private String plotFeedback;
    private Integer plotRating;
    private String lastMessage;

    public ReviewPlayer(Player player){
        this.PLAYERUUID = player.getUniqueId();
    }

    public boolean hasAlreadyReviewed(ReviewPlot reviewPlot){
        if(reviewPlot.getPlot().getOwner() == PLAYERUUID) return true;
        int playerReviewIteration = reviewPlot.getPlayerReviewIteration(this);
        int reviewIteration = reviewPlot.getReviewIteration();
        return playerReviewIteration > reviewIteration;
    }

    public void teleportToReviewPlot(ReviewPlot reviewPlot){
        reviewPlot.getPlot().teleportPlayer(this.getPlotPlayer(), TeleportCause.PLUGIN, result -> {
        });
    }

    public boolean hasGivenFeedback(){
        if (plotFeedback != null)
            return true;
        if(hasAlreadyReviewed(this.getReviewParty().getCurrentReviewPlot()))
            return true;
        else return false;
    }

    public boolean hasGivenRating(){
        if (plotRating != null)
            return true;
        if(hasAlreadyReviewed(this.getReviewParty().getCurrentReviewPlot()))
            return true;
        else return false;
    }

    @SuppressWarnings({"unchecked","rawtypes"})
    public <T> void sendMessage(TranslatableCaption caption, T... templates) {
        PlotPlayer plotPlayer = this.getPlotPlayer();
        Class[] param;
        try {
            Class templateClass = Class.forName("com.plotsquared.core.configuration.adventure.text.minimessage.Template");
            Object templateArray = Array.newInstance(templateClass, templates.length);
            for (int i = 0; i < templates.length; i++) {
                if(templates[i].getClass().equals(Template.ComponentTemplate.class)) {
                    Method templateCreator = templateClass.getMethod("of", String.class, Component.class);
                    Template.ComponentTemplate template = (Template.ComponentTemplate) templates[i];
                    Array.set(templateArray,i,templateCreator.invoke(null,template.key, template.value));
                }
                if(templates[i].getClass().equals(Template.StringTemplate.class)) {
                    Method templateCreator = templateClass.getMethod("of", String.class, String.class);
                    Template.StringTemplate template = (Template.StringTemplate) templates[i];
                    Array.set(templateArray,i,templateCreator.invoke(null,template.key, template.value));
                }
            }
            param = new Class[]{Class.forName("com.plotsquared.core.configuration.caption.Caption"),
                    templateArray.getClass()};
            Method method = plotPlayer.getClass().getMethod("sendMessage", param);
            method.invoke(plotPlayer,caption,templateArray);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public interface FakeMiniMessage extends ComponentSerializer<Component, Component, String>, Buildable<MiniMessage, MiniMessage.Builder> {
        @SuppressWarnings({"unchecked", "rawtypes"})
         static <T> @NonNull Component parse(String input, T... templates) {
            Class[] param;
            //                Class templateClass = Class.forName("com.plotsquared.core.configuration.adventure.text.minimessage.Template");
            Object templateArray = Array.newInstance(Template.class, templates.length);
            for (int i = 0; i < templates.length; i++) {
                if(templates[i].getClass().equals(Template.ComponentTemplate.class)) {
//                        Method templateCreator = templateClass.getMethod("of", String.class, Component.class);
                    Template.ComponentTemplate template = (Template.ComponentTemplate) templates[i];
                    Array.set(templateArray,i,new Template.ComponentTemplate(template.getKey(), template.getValue()));
                }
                if(templates[i].getClass().equals(Template.StringTemplate.class)) {
//                        Method templateCreator = templateClass.getMethod("of", String.class, String.class);
                    Template.StringTemplate template = (Template.StringTemplate) templates[i];
//                        Array.set(templateArray,i,templateCreator.invoke(null,template.key, template.value));
                }
            }
            param = new Class[]{String.class,
                    templateArray.getClass()};
//                Method method = Class.forName("com.plotsquared.core.configuration.adventure.text.minimessage.Minimessage").getMethod("parse", param);
            return MiniMessage.builder().build().parse(input, templates);
//                return (Component) method.invoke(miniMessage, input, templateArray);
//            return Component.text('2');
        }
    }

    public interface Template extends Examinable {

        static StringTemplate templateOf(@NonNull String search, @NonNull String replacement) {
            return new StringTemplate(search, replacement);
        }

        static ComponentTemplate templateOf(@NonNull String search, @NonNull Component replacement) {
            return new ComponentTemplate(search, replacement);
        }

        class StringTemplate implements Template {
            private final String key;
            private final String value;

            public StringTemplate(String key, String replacement) {
                this.key = key;
                this.value = replacement;
            }

            public String getSearch() {
                return key;
            }

            public String getValue() {
                return value;
            }
        }

        class ComponentTemplate implements Template {
            private final String key;
            private final Component value;

            public ComponentTemplate(final String key, final Component replacement) {
                this.key = key;
                this.value = replacement;
            }

            public String getKey() {
                return key;
            }

            public Component getValue() {
                return value;
            }
        }
    }

    public static Component getPlayerList(final Collection<UUID> uuids, LocaleHolder localeHolder) {
        MiniMessage MINI_MESSAGE = MiniMessage.builder().build();
        if (uuids.isEmpty()) {
            return MINI_MESSAGE.deserialize(TranslatableCaption.of("info.none").getComponent(localeHolder));
        }

        final List<UUID> players = new LinkedList<>();
        final List<String> users = new LinkedList<>();
        for (final UUID uuid : uuids) {
            if (uuid == null) {
                users.add(MINI_MESSAGE.stripTokens(TranslatableCaption.of("info.none").getComponent(localeHolder)));
            } else if (DBFunc.EVERYONE.equals(uuid)) {
                users.add(MINI_MESSAGE.stripTokens(TranslatableCaption.of("info.everyone").getComponent(localeHolder)));
            } else if (DBFunc.SERVER.equals(uuid)) {
                users.add(MINI_MESSAGE.stripTokens(TranslatableCaption.of("info.console").getComponent(localeHolder)));
            } else {
                players.add(uuid);
            }
        }

        try {
            for (final UUIDMapping mapping : PlotSquared.get().getImpromptuUUIDPipeline()
                    .getNames(players).get(Settings.UUID.BLOCKING_TIMEOUT, TimeUnit.MILLISECONDS)) {
                users.add(mapping.getUsername());
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }

        String c = TranslatableCaption.of("info.plot_user_list").getComponent(ConsolePlayer.getConsole());
        TextComponent.Builder list = Component.text();
        for (int x = 0; x < users.size(); x++) {
            if (x + 1 == uuids.size()) {
                list.append(MINI_MESSAGE.parse(c, templateOf("user", users.get(x))));
            } else {
                list.append(MINI_MESSAGE.parse(c + ", ", templateOf("user", users.get(x))));
            }
        }
        return list.asComponent();
    }




    public boolean isReviewing(){
        return reviewParty != null;
    }

    public boolean isReviewPartyLeader(){ return reviewParty.getReviewerLeader().getUniqueId() == PLAYERUUID; }

    public void setPlotRating(Integer plotRating){
        this.plotRating = plotRating;
    }

    public void setPlotFeedback(String plotFeedback){
        this.plotFeedback = plotFeedback;
    }

    public void clearFeedback(){
        this.plotFeedback = null;
    }

    public void clearRating(){
        this.plotRating = null;
    }

    public void setReviewParty(ReviewParty reviewParty){
        this.reviewParty = reviewParty;
    }

    public void leaveReviewParty() {
        reviewParty.removeReviewPlayer(this);
    }

    public ReviewParty getReviewParty() {
        return reviewParty;
    }

    public Player getBukkitPlayer(){
        return Bukkit.getPlayer(this.getUniqueId());
    }

    public PlotPlayer<?> getPlotPlayer(){
        return BukkitUtil.adapt(getBukkitPlayer());
    }

    public UUID getUniqueId() {
        return PLAYERUUID;
    }

    public void clearReviewParty() {
        this.reviewParty = null;
    }

    public String getPlotFeedback() {
        return plotFeedback;
    }

    public Integer getPlotRating() {
        return plotRating;
    }
}
