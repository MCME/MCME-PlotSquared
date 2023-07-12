package main.java.com.mcmiddleearth.plotsquared.plotflag;

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.plot.flag.FlagParseException;
import com.plotsquared.core.plot.flag.types.ListFlag;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReviewTimeDataFlag extends ListFlag<Long, ReviewTimeDataFlag> {

    public static final ReviewTimeDataFlag REVIEW_TIME_DATA_FLAG_NONE =
            new ReviewTimeDataFlag(Collections.emptyList());

    public ReviewTimeDataFlag(List<Long> valueList) {
        super(valueList, TranslatableCaption.of("flags.flag_category_string_list"),
                TranslatableCaption.of("flags.flag_description_blocked_cmds")
        ); //eeeeuh implement perhaps or ignore cause this don't matter
    }

    @Override
    public ReviewTimeDataFlag parse(@NonNull String input) throws FlagParseException {
        final String[] split = input.split(",");
        final List<Long> numbers = new ArrayList<>();
        for (final String element : split) {
            numbers.add(Long.parseLong(element));
        }
        return flagOf(numbers);
    }

    @Override
    public String getExample() {
        return "0";
    }

    @Override
    protected ReviewTimeDataFlag flagOf(@NonNull List<Long> value) {
        return new ReviewTimeDataFlag(value);

    }
}
