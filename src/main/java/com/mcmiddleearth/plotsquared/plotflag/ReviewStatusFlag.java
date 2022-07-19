package com.mcmiddleearth.plotsquared.plotflag;

import com.mcmiddleearth.plotsquared.review.ReviewPlot;
import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.plot.Plot;
import com.plotsquared.core.plot.flag.PlotFlag;
import org.checkerframework.checker.nullness.qual.NonNull;


public class ReviewStatusFlag extends PlotFlag<ReviewPlot.ReviewStatus, ReviewStatusFlag> {

    public static final ReviewStatusFlag BEING_REVIEWED_FLAG = new ReviewStatusFlag(ReviewPlot.ReviewStatus.BEING_REVIEWED);
    public static final ReviewStatusFlag NOT_BEING_REVIEWED_FLAG = new ReviewStatusFlag(ReviewPlot.ReviewStatus.NOT_BEING_REVIEWED);
    public static final ReviewStatusFlag ACCEPTED_FLAG = new ReviewStatusFlag(ReviewPlot.ReviewStatus.ACCEPTED);
    public static final ReviewStatusFlag REJECTED_FLAG = new ReviewStatusFlag(ReviewPlot.ReviewStatus.REJECTED);
    public static final ReviewStatusFlag LOCKED_FLAG = new ReviewStatusFlag(ReviewPlot.ReviewStatus.LOCKED);
//    static {
//        ReviewPlot.ReviewStatus.register(DEFAULT);
//    }

    @Override
    protected ReviewStatusFlag flagOf(ReviewPlot.@NonNull ReviewStatus value) {
        return switch (value) {
            case BEING_REVIEWED -> BEING_REVIEWED_FLAG;
            case NOT_BEING_REVIEWED -> NOT_BEING_REVIEWED_FLAG;
            case ACCEPTED -> ACCEPTED_FLAG;
            case REJECTED -> REJECTED_FLAG;
            case LOCKED -> LOCKED_FLAG;
            case TOO_EARLY -> null;
        };
    }

    public static boolean isBeingReviewed(Plot plot){
        return plot.getFlag(ReviewStatusFlag.class) == ReviewPlot.ReviewStatus.BEING_REVIEWED ||
                plot.getFlag(ReviewStatusFlag.class) == ReviewPlot.ReviewStatus.NOT_BEING_REVIEWED;
    }

    public static boolean isAccepted(Plot plot){
        return plot.getFlag(ReviewStatusFlag.class) == ReviewPlot.ReviewStatus.ACCEPTED ||
                plot.getFlag(ReviewStatusFlag.class) == ReviewPlot.ReviewStatus.LOCKED;
    }

    public static boolean isLocked(Plot plot){
        return plot.getFlag(ReviewStatusFlag.class) == ReviewPlot.ReviewStatus.LOCKED;
    }
    @Override
    public String getExample() {
        return "being_reviewed";
    }

    @Override
    public String toString() {
        return getValue().toString();
    }

    @Override
    public ReviewStatusFlag parse(@NonNull String input) {
        return switch (input.toLowerCase()) {
            case "being_reviewed" -> flagOf(ReviewPlot.ReviewStatus.BEING_REVIEWED);
            case "not_being_reviewed" -> flagOf(ReviewPlot.ReviewStatus.NOT_BEING_REVIEWED);
            case "accepted" -> flagOf(ReviewPlot.ReviewStatus.ACCEPTED);
            case "rejected" -> flagOf(ReviewPlot.ReviewStatus.REJECTED);
            case "too_early" -> null;
            case "locked" -> null;
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public ReviewStatusFlag merge(ReviewPlot.@NonNull ReviewStatus newValue) {
        return flagOf(newValue);
    }
    protected ReviewStatusFlag(ReviewPlot.@NonNull ReviewStatus value) {
        super(
                value,
                TranslatableCaption.of("flags.flag_category_weather"), TranslatableCaption.of("flags.flag_category_weather")
        );
    }
    
}