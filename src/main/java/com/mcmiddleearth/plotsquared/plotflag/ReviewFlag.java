package com.mcmiddleearth.plotsquared.plotflag;

import org.checkerframework.checker.nullness.qual.NonNull;
import com.plotsquared.core.plot.flag.types.BooleanFlag;
import com.plotsquared.core.configuration.caption.TranslatableCaption;


public class ReviewFlag extends BooleanFlag<ReviewFlag> {

    public static final ReviewFlag REVIEW_TRUE = new ReviewFlag(true);
    public static final ReviewFlag REVIEW_FALSE = new ReviewFlag(false);

    @Override
    protected ReviewFlag flagOf(@NonNull Boolean value) {
        return value ? REVIEW_TRUE : REVIEW_FALSE;
    }
    public ReviewFlag(final boolean value) {
        super(value, TranslatableCaption.of("flags.flag_description_review"));
    }
}
