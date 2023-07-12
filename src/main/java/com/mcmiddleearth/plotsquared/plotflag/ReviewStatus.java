package main.java.com.mcmiddleearth.plotsquared.plotflag;

public enum ReviewStatus {
    NOT_BEING_REVIEWED,  //PERMANENT FLAG
    BEING_GIVEN_FEEDBACK,//TEMP FLAG
    FEEDBACK_DONE,       //PERMANENT FLAG
    FEEDBACK_NOT_SEEN,   //TEMP FLAG
    BEING_GIVEN_RATING,  //TEMP FLAG
    ACCEPTED_NOT_SEEN,   //TEMP FLAG
    REJECTED_NOT_SEEN,   //TEMP FLAG
    TOO_EARLY,           //SYSTEM LOGIC FLAG
    REJECTED,            //SYSTEM LOGIC FLAG
    ACCEPTED,            //SYSTEM LOGIC FLAG
    LOCKED,              //PERMANENT FLAG
}

