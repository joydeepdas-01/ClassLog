package com.classlog.app.domain.model

/**
 * Validation issues that prevent saving a work session.
 * These represent hard errors.
 */
enum class Issue {
    /** The end time is less than or equal to the start time */
    END_NOT_AFTER_START,

    /** The end time exceeds midnight (1440 minutes) or start time is negative */
    END_AFTER_MIDNIGHT,

    /** The work category is classified as teaching, but no batch has been selected */
    BATCH_REQUIRED
}
