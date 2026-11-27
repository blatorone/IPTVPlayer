package com.iptvplayer.app

import androidx.core.content.edit

import android.content.SharedPreferences
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * How long before a portal's end date the notice comes: 1–14 days, 1–3 weeks or one month.
 * Weeks and the month are calendar based (java.time), so a month before 31 March is 28 or 29
 * February depending on the year, and a month before 30 April is 30 March.
 */
internal data class ExpiryLead(val unit: Char, val amount: Int) {
    val key: String get() = "$unit$amount"

    /** The first day on which the notice is due. */
    fun noticeFrom(expiry: LocalDate): LocalDate = when (unit) {
        'w' -> expiry.minusWeeks(amount.toLong())
        'm' -> expiry.minusMonths(amount.toLong())
        else -> expiry.minusDays(amount.toLong())
    }

    companion object {
        val ALL: List<ExpiryLead> =
            (1..14).map { ExpiryLead('d', it) } + (1..3).map { ExpiryLead('w', it) } + ExpiryLead('m', 1)
        val DEFAULT = ExpiryLead('w', 1)

        fun parse(key: String?): ExpiryLead = ALL.firstOrNull { it.key == key } ?: DEFAULT
    }
}

internal enum class ExpiryState { SOON, EXPIRED }

/** Null: no end date known, or it is further away than the chosen lead time. The end date itself still counts as valid. */
internal fun expiryState(expiry: LocalDate?, today: LocalDate, lead: ExpiryLead): ExpiryState? = when {
    expiry == null -> null
    today.isAfter(expiry) -> ExpiryState.EXPIRED
    !today.isBefore(lead.noticeFrom(expiry)) -> ExpiryState.SOON
    else -> null
}

internal data class ExpiryNotice(
    val portalId: String, val name: String, val expiry: LocalDate, val daysLeft: Long, val state: ExpiryState
)

/**
 * Portals whose notice was not confirmed yet: "läuft bald ab" once, and "abgelaufen" once more.
 * [confirmed] gives what was already confirmed for that portal and end date; a renewed
 * subscription has a new date and so starts over.
 */
internal fun pendingExpiryNotices(
    portals: List<SavedPortal>, today: LocalDate, lead: ExpiryLead, name: (SavedPortal) -> String,
    confirmed: (portalId: String, expiry: LocalDate) -> ExpiryState?
): List<ExpiryNotice> = portals.mapNotNull { portal ->
    val expiry = portal.subscriptionExpiry ?: return@mapNotNull null
    val state = expiryState(expiry, today, lead) ?: return@mapNotNull null
    val done = confirmed(portal.identity, expiry)
    if (done != null && done.ordinal >= state.ordinal) return@mapNotNull null
    ExpiryNotice(portal.identity, name(portal), expiry, ChronoUnit.DAYS.between(today, expiry), state)
}.sortedBy { it.expiry }

/** Confirmed notices, kept on this device only. */
internal class ExpiryConfirmations(private val preferences: SharedPreferences?) {
    fun state(portalId: String, expiry: LocalDate): ExpiryState? =
        preferences?.getString("expiry_ack:$portalId", null)
            ?.split('|')?.takeIf { it.size == 2 && it[0] == expiry.toString() }
            ?.let { parts -> ExpiryState.entries.firstOrNull { it.name == parts[1] } }

    fun confirm(notices: List<ExpiryNotice>) {
        preferences?.edit { notices.forEach { putString("expiry_ack:${it.portalId}", "${it.expiry}|${it.state.name}") } }
    }
}
