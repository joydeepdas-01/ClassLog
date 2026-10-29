package com.classlog.app.domain

import com.classlog.app.domain.model.Issue
import com.classlog.app.domain.validation.SessionValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionValidatorTest {
    @Test
    fun validSession_noIssues() =
        assertTrue(SessionValidator.validate(600, 660, true, 1L).isEmpty())

    @Test
    fun endEqualsStart_isError() =
        assertTrue(Issue.END_NOT_AFTER_START in SessionValidator.validate(600, 600, false, null))

    @Test
    fun endBeforeStart_isError() =
        assertTrue(Issue.END_NOT_AFTER_START in SessionValidator.validate(660, 600, false, null))

    @Test
    fun pastMidnight_isError() =
        assertTrue(Issue.END_AFTER_MIDNIGHT in SessionValidator.validate(1380, 1500, false, null))

    @Test
    fun teachingWithoutBatch_isError() =
        assertTrue(Issue.BATCH_REQUIRED in SessionValidator.validate(600, 660, true, null))

    @Test
    fun nonTeachingWithoutBatch_isFine() =
        assertTrue(SessionValidator.validate(600, 660, false, null).isEmpty())
}
