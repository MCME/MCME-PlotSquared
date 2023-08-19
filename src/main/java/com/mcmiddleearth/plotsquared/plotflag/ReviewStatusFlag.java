package com.mcmiddleearth.plotsquared.plotflag;

import com.plotsquared.core.configuration.caption.TranslatableCaption;
import com.plotsquared.core.plot.flag.PlotFlag;
import org.checkerframework.checker.nullness.qual.NonNull;


public class ReviewStatusFlag extends PlotFlag<ReviewStatus, ReviewStatusFlag> {

    public static final ReviewStatusFlag NOT_BEING_REVIEWED_FLAG = new ReviewStatusFlag(ReviewStatus.NOT_BEING_REVIEWED);
    public static final ReviewStatusFlag BEING_GIVEN_RATING_FLAG = new ReviewStatusFlag(ReviewStatus.BEING_GIVEN_RATING);
    public static final ReviewStatusFlag BEING_GIVEN_FEEDBACK_FLAG = new ReviewStatusFlag(ReviewStatus.BEING_GIVEN_FEEDBACK);
    public static final ReviewStatusFlag FEEDBACK_DONE_FLAG = new ReviewStatusFlag(ReviewStatus.FEEDBACK_DONE);
    public static final ReviewStatusFlag ACCEPTED_FLAG = new ReviewStatusFlag(ReviewStatus.ACCEPTED);
    public static final ReviewStatusFlag ACCEPTED_NOT_SEEN_FLAG = new ReviewStatusFlag(ReviewStatus.ACCEPTED_NOT_SEEN);
    public static final ReviewStatusFlag REJECTED_NOT_SEEN_FLAG = new ReviewStatusFlag(ReviewStatus.REJECTED_NOT_SEEN);
    public static final ReviewStatusFlag FEEDBACK_NOT_SEEN_FLAG = new ReviewStatusFlag(ReviewStatus.FEEDBACK_NOT_SEEN);
    public static final ReviewStatusFlag LOCKED_FLAG = new ReviewStatusFlag(ReviewStatus.LOCKED);
    public static final ReviewStatusFlag REJECTED_FLAG = new ReviewStatusFlag(ReviewStatus.REJECTED);


    @Override
    protected ReviewStatusFlag flagOf(ReviewStatus value) {
        return switch (value) {
            case NOT_BEING_REVIEWED -> NOT_BEING_REVIEWED_FLAG;
            case BEING_GIVEN_FEEDBACK -> BEING_GIVEN_FEEDBACK_FLAG;
            case FEEDBACK_DONE -> FEEDBACK_DONE_FLAG;
            case FEEDBACK_NOT_SEEN -> FEEDBACK_NOT_SEEN_FLAG;
            case BEING_GIVEN_RATING -> BEING_GIVEN_RATING_FLAG;
            case ACCEPTED_NOT_SEEN -> ACCEPTED_NOT_SEEN_FLAG;
            case REJECTED_NOT_SEEN -> REJECTED_NOT_SEEN_FLAG;
            case REJECTED -> REJECTED_FLAG;
            case ACCEPTED -> ACCEPTED_FLAG;
            case LOCKED -> LOCKED_FLAG;
            case TOO_EARLY -> null;
        };
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
            case "never_reviewed" -> flagOf(ReviewStatus.NOT_BEING_REVIEWED);
            case "being_given_rating" -> flagOf(ReviewStatus.BEING_GIVEN_RATING);
            case "being_given_feedback" -> flagOf(ReviewStatus.BEING_GIVEN_FEEDBACK);
            case "accepted" -> flagOf(ReviewStatus.ACCEPTED);
            case "rejected" -> flagOf(ReviewStatus.REJECTED);
            case "accepted_not_seen" -> flagOf(ReviewStatus.ACCEPTED_NOT_SEEN);
            case "rejected_not_seen" -> flagOf(ReviewStatus.REJECTED_NOT_SEEN);
            case "feedback_not_seen" -> flagOf(ReviewStatus.FEEDBACK_NOT_SEEN);
            case "feedback_done" -> flagOf(ReviewStatus.FEEDBACK_DONE);
            case "too_early" -> flagOf(ReviewStatus.TOO_EARLY);
            case "locked" -> flagOf(ReviewStatus.LOCKED);
            default -> throw new IllegalArgumentException();
        };
    }

    @Override
    public ReviewStatusFlag merge(ReviewStatus newValue) {
        return flagOf(newValue);
    }

    protected ReviewStatusFlag(ReviewStatus value) {
        super(
                value,
                TranslatableCaption.of("flags.flag_category_weather"),
                TranslatableCaption.of("flags.flag_category_weather")
        );
    }

}