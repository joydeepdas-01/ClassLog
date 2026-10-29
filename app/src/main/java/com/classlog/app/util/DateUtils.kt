package com.classlog.app.util

import java.time.YearMonth

/**
 * Returns a pair of epoch days (from start of month to end of month) for the given [YearMonth].
 */
fun monthRange(ym: YearMonth): Pair<Long, Long> =
    ym.atDay(1).toEpochDay() to ym.atEndOfMonth().toEpochDay()
