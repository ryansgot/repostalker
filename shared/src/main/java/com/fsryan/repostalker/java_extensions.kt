package com.fsryan.repostalker

import java.util.Date
import java.util.TimeZone

/**
 * forsuredb stores created and modified time in UTC only. This allows for
 * consistency across all timezones such that the same conversion code will
 * work appropriately for all timezones.
 *
 * Unfortunately, this means that the following tricky timezone conversion
 * logic is required if you want to create a date that makes sense with
 * respect to either the created or modified fields.
 * @return a new [Date] object which is this date in UTC
 */
fun Date.toUTC(): Date {
    val offset = TimeZone.getDefault().getOffset(time)
    // the offset time is the offset assuming the input is UTC. Since the
    // input is actually the local time, the offset must be subtracted
    // rather than added. I know this is tricky.
    return Date(time - offset)
}

fun String.swapOnCondition(alt: String, matches: (String) -> Boolean) = if (matches(this)) alt else this