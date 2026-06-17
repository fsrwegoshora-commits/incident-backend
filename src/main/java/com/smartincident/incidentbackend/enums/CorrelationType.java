package com.smartincident.incidentbackend.enums;

public enum CorrelationType {

    /** Reporter and location/time/category all strongly match — treated as the same real-world event. */
    DUPLICATE,

    /** Location/time are close but category or type differs — likely a related but distinct event. */
    RELATED
}
