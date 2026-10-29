package com.classlog.app.domain.validation

import com.classlog.app.domain.model.Issue

/**
 * Validates session parameters against core domain rules.
 * Pure Kotlin logic with zero Android dependencies.
 */
object SessionValidator {
    /**
     * Checks for hard errors.
     * Note: Session overlaps are checked separately via the DAO and presented as warnings rather than blocks.
     *
     * @param start Start time in minutes since midnight (0 to 1440)
     * @param end End time in minutes since midnight (0 to 1440)
     * @param isTeaching Whether the category represents teaching work
     * @param batchId Selected batch ID, if any
     * @return List of [Issue] validation errors (empty if completely valid)
     */
    fun validate(start: Int, end: Int, isTeaching: Boolean, batchId: Long?): List<Issue> = buildList {
        if (end <= start) add(Issue.END_NOT_AFTER_START)
        if (end > 1440 || start < 0) add(Issue.END_AFTER_MIDNIGHT)
        if (isTeaching && batchId == null) add(Issue.BATCH_REQUIRED)
    }
}
