package com.mcmiddleearth.plotsquared.plotflag;

import org.checkerframework.checker.nullness.qual.NonNull;
import com.plotsquared.core.plot.flag.types.BooleanFlag;
import com.plotsquared.core.configuration.caption.TranslatableCaption;


public class LockedFlag extends BooleanFlag<LockedFlag> {

    public static final LockedFlag LOCKED_TRUE = new LockedFlag(true);
    public static final LockedFlag LOCKED_FALSE = new LockedFlag(false);

    @Override
    protected LockedFlag flagOf(@NonNull Boolean value) {
        return value ? LOCKED_TRUE : LOCKED_FALSE;
    }
    public LockedFlag(final boolean value) {
        super(value, TranslatableCaption.of("flags.Locked"));
    }
}
