package com.mcmiddleearth.plotsquared.plotflag;

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.plot.flag.FlagParseException;
import com.plotsquared.core.plot.flag.types.ListFlag;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Collections;
import java.util.List;


public class ReviewDataFlag extends ListFlag<Long, ReviewDataFlag> {

    public static final ReviewDataFlag REVIEW_DATA_FLAG_NONE =
            new ReviewDataFlag(Collections.emptyList());

    protected ReviewDataFlag(List<Long> valueList) {
        super(valueList, TranslatableCaption.of("flags.flag_category_string_list"),
                TranslatableCaption.of("flags.flag_description_blocked_cmds")
        ); //eeeeuh implement perhaps or ignore cause this don't matter
    }

    @Override
    public ReviewDataFlag parse(@NonNull String input) throws FlagParseException {
        return null;
    }

    @Override
    public String getExample() {
        return null;
    }

    @Override
    protected ReviewDataFlag flagOf(@NonNull List<Long> value) {
        return new ReviewDataFlag(value);

    }
}
