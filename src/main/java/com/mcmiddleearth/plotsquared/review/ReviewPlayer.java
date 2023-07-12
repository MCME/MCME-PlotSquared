package main.java.com.mcmiddleearth.plotsquared.review;

import com.plotsquared.bukkit.util.BukkitUtil;
import com.plotsquared.core.configuration.caption.Caption;
import com.plotsquared.core.events.TeleportCause;
import com.plotsquared.core.player.PlotPlayer;
import lombok.NonNull;
import main.java.com.mcmiddleearth.plotsquared.review.plot.ReviewPlot;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.examination.Examinable;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

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

    public ReviewPlayer(Player player) {
        this.PLAYERUUID = player.getUniqueId();
    }

    public void teleportToCurrentReviewPlot() {
        this.getReviewParty().getCurrentPlot().teleportPlayer(this.getPlotPlayer(), TeleportCause.PLUGIN, result -> {
        });
    }

    public boolean hasReviewed() {
        if (plotFeedback != null || plotRating != null) return true;
        else return this.getCurrentReviewPlot().wasReviewedBy(this);
    }

    public void submitReview() {
        if (plotRating != null) {
            this.getCurrentReviewPlot().addTempRatings(this);
            this.plotRating = null;
        }
        if (plotFeedback != null) {
            this.getCurrentReviewPlot().addFeedback(this);
            this.plotFeedback = null;
        }
    }

    public boolean isReviewing() {
        return reviewParty != null;
    }

    public boolean isReviewPartyLeader() {
        return reviewParty.getReviewerLeader().getUniqueId() == PLAYERUUID;
    }

    public void setRating(Integer plotRating) {
        this.plotRating = plotRating;
    }

    public void setFeedback(String plotFeedback) {
        this.plotFeedback = plotFeedback;
    }

    public void joinReviewParty(ReviewParty reviewParty) {
        ReviewAPI.addReviewPlayer(this);
        this.reviewParty = reviewParty;
        this.teleportToCurrentReviewPlot();
    }

    public void leaveReviewParty(){
        ReviewAPI.removeReviewPlayer(this);
        this.submitReview();
        this.reviewParty = null;
    }

    public void setReviewParty(ReviewParty reviewParty) {
        this.reviewParty = reviewParty;
    }

    public ReviewParty getReviewParty() {
        return reviewParty;
    }

    public PlotPlayer<?> getPlotPlayer() {
        return BukkitUtil.adapt(Bukkit.getPlayer(PLAYERUUID));
    }

    public UUID getUniqueId() {
        return PLAYERUUID;
    }

    public ReviewPlot getCurrentReviewPlot() {
        return reviewParty.getCurrentReviewPlot();
    }

    public String getPlotFeedback() {
        return plotFeedback;
    }

    public Integer getPlotRating() {
        return plotRating;
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void sendMessage(Caption caption, T... templates) {

        PlotPlayer plotPlayer = this.getPlotPlayer();
        Class[] param;
        try {
            Class templateClass = Class.forName("com.plotsquared.core.configuration.adventure.text.minimessage.Template");
            Object templateArray = Array.newInstance(templateClass, templates.length);
            for (int i = 0; i < templates.length; i++) {
                if (templates[i].getClass().equals(Template.ComponentTemplate.class)) {
                    Method templateCreator = templateClass.getMethod("of", String.class,
                            Class.forName("com.plotsquared.core.configuration.adventure.text.Component"));
                    Template.ComponentTemplate template = (Template.ComponentTemplate) templates[i];
                    String serialized = GsonComponentSerializer.gson().serialize(template.value);

                    Class deserializedclass = Class.forName("com.plotsquared.core.configuration.adventure.text.serializer.gson.GsonComponentSerializer");
                    for (Field field : deserializedclass.getFields()) {
                        field.setAccessible(true);
                    }
                    Method deserializer = Class.forName("com.plotsquared.core.configuration.adventure.text.serializer.gson.GsonComponentSerializer")
                            .getMethod("gson");
                    deserializer.setAccessible(true);
                    Object desiralzed = deserializer.invoke(null);
                    for (Field field : desiralzed.getClass().getFields()) {
                        field.setAccessible(true);
                    }
                    Method component = desiralzed.getClass().getMethod("deserialize", String.class);
                    component.setAccessible(true);
                    Object componnt = component.invoke(desiralzed, serialized);
                    Array.set(templateArray, i, templateCreator.invoke(null, template.getKey(), componnt));
                }
                if (templates[i].getClass().equals(Template.StringTemplate.class)) {
                    Method templateCreator = templateClass.getMethod("of", String.class, String.class);
                    Template.StringTemplate template = (Template.StringTemplate) templates[i];
                    Array.set(templateArray, i, templateCreator.invoke(null, template.key, template.value));
                }
            }
            param = new Class[]{Class.forName("com.plotsquared.core.configuration.caption.Caption"),
                    templateArray.getClass()};
            Method method = plotPlayer.getClass().getMethod("sendMessage", param);
            method.invoke(plotPlayer, caption, templateArray);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException |
                 IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    public interface Template extends Examinable {

        static Template templateOf(@NonNull String search, @NonNull String replacement) {
            return new StringTemplate(search, replacement);
        }

        static Template templateOf(@NonNull String search, @NonNull Component replacement) {
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
}

//    public static Component getPlayerList(final Collection<UUID> uuids, LocaleHolder localeHolder) {
//        MiniMessage MINI_MESSAGE = MiniMessage.builder().build();
//        if (uuids.isEmpty()) {
//            return MINI_MESSAGE.deserialize(TranslatableCaption.of("info.none").getComponent(localeHolder));
//        }
//
//        final List<UUID> players = new LinkedList<>();
//        final List<String> users = new LinkedList<>();
//        for (final UUID uuid : uuids) {
//            if (uuid == null) {
//                users.add(MINI_MESSAGE.stripTokens(TranslatableCaption.of("info.none").getComponent(localeHolder)));
//            } else if (DBFunc.EVERYONE.equals(uuid)) {
//                users.add(MINI_MESSAGE.stripTokens(TranslatableCaption.of("info.everyone").getComponent(localeHolder)));
//            } else if (DBFunc.SERVER.equals(uuid)) {
//                users.add(MINI_MESSAGE.stripTokens(TranslatableCaption.of("info.console").getComponent(localeHolder)));
//            } else {
//                players.add(uuid);
//            }
//        }
//
//        try {
//            for (final UUIDMapping mapping : PlotSquared.get().getImpromptuUUIDPipeline()
//                    .getNames(players).get(Settings.UUID.BLOCKING_TIMEOUT, TimeUnit.MILLISECONDS)) {
//                users.add(mapping.getUsername());
//            }
//        } catch (final Exception e) {
//            e.printStackTrace();
//        }
//
//        String c = TranslatableCaption.of("info.plot_user_list").getComponent(ConsolePlayer.getConsole());
//        TextComponent.Builder list = Component.text();
//        for (int x = 0; x < users.size(); x++) {
//            if (x + 1 == uuids.size()) {
//                list.append(MINI_MESSAGE.parse(c, templateOf("user", users.get(x))));
//            } else {
//                list.append(MINI_MESSAGE.parse(c + ", ", templateOf("user", users.get(x))));
//            }
//        }
//        return list.asComponent();
//    }
//
//    public static class CaptionHolder {
//
//        private Caption caption = StaticCaption.of("");
//        private Template[] templates = new Template[0];
//
//        public void set(Caption caption) {
//            this.caption = caption;
//        }
//
//        public Caption get() {
//            return this.caption;
//        }
//
//        public Template[] getTemplates() {
//            return this.templates;
//        }
//
//        public void setTemplates(Template... templates) {
//            this.templates = templates;
//        }
//
//    }
