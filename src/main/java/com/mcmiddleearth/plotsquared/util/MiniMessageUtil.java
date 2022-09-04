package com.mcmiddleearth.plotsquared.util;

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.player.PlotPlayer;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

public class MiniMessageUtil {

    @SuppressWarnings({"unchecked","rawtypes"})
    public static void sendMessage(PlotPlayer<?> player, TranslatableCaption caption, MiniMessageTemplate... templates) {
        Class[] param = new Class[0];
        try {
            Class templateClass = Class.forName("com.plotsquared.core.configuration.adventure.text.minimessage.Template");
            Method templateCreator = templateClass.getMethod("of", String.class, String.class);
            Object templateArray = Array.newInstance(templateClass,templates.length);
            for(int i = 0; i < templates.length; i++) {
                MiniMessageTemplate template = templates[i];
                Array.set(templateArray,i,templateCreator.invoke(null,template.search,template.replacement));
            }
            param = new Class[]{Class.forName("com.plotsquared.core.configuration.caption.Caption"),
                                templateArray.getClass()};
            Method method = player.getClass().getMethod("sendMessage", param);
            method.invoke(player,caption,templateArray);
        } catch (ClassNotFoundException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public static MiniMessageTemplate templateOf(String search, String replacement) {
        return new MiniMessageTemplate(search,replacement);
    }

    public static class MiniMessageTemplate {

        private final String search;
        private final String replacement;

        public MiniMessageTemplate(String search, String replacement) {
            this.search = search;
            this.replacement = replacement;
        }
    }
}
