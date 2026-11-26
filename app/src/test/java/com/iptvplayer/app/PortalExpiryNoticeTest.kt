package com.iptvplayer.app

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PortalExpiryNoticeTest {
    private val today = LocalDate.of(2026, 11, 26)
    private val week = ExpiryLead.DEFAULT

    private fun portal(mac: String, expiry: LocalDate?) =
        SavedPortal("http://portal.test", mac, emptyList(), emptyMap(), emptyMap(), subscriptionExpiry = expiry)

    @Test
    fun soonWithinTheLeadTimeExpiredAfterTheEndDate() {
        assertNull(expiryState(null, today, week))
        assertNull(expiryState(today.plusDays(8), today, week))
        assertEquals(ExpiryState.SOON, expiryState(today.plusDays(7), today, week))
        assertEquals(ExpiryState.SOON, expiryState(today, today, week))
        assertEquals(ExpiryState.EXPIRED, expiryState(today.minusDays(1), today, week))
    }

    @Test
    fun aMonthIsACalendarMonthIncludingFebruaryAndLeapYears() {
        val month = ExpiryLead('m', 1)
        assertEquals(LocalDate.of(2027, 2, 28), month.noticeFrom(LocalDate.of(2027, 3, 31)))
        assertEquals(LocalDate.of(2028, 2, 29), month.noticeFrom(LocalDate.of(2028, 3, 31)))
        assertEquals(LocalDate.of(2026, 3, 30), month.noticeFrom(LocalDate.of(2026, 4, 30)))
        assertEquals(LocalDate.of(2026, 12, 31), month.noticeFrom(LocalDate.of(2027, 1, 31)))
    }

    @Test
    fun choicesAreOneToFourteenDaysThreeWeeksAndAMonth() {
        assertEquals(18, ExpiryLead.ALL.size)
        assertEquals(ExpiryLead('w', 1), ExpiryLead.parse(null))
        assertEquals(ExpiryLead('d', 10), ExpiryLead.parse("d10"))
        assertEquals(LocalDate.of(2026, 11, 5), ExpiryLead('w', 3).noticeFrom(LocalDate.of(2026, 11, 26)))
    }

    @Test
    fun eachNoticeIsConfirmedOnceThenExpiredComesOnceMore() {
        val soon = portal("00:1A:79:00:00:01", today.plusDays(5))
        val shown = pendingExpiryNotices(listOf(soon), today, week, { "A" }) { _, _ -> null }
        assertEquals(ExpiryState.SOON, shown.single().state)
        assertTrue(pendingExpiryNotices(listOf(soon), today.plusDays(3), week, { "A" }) { _, _ -> ExpiryState.SOON }.isEmpty())
        val after = pendingExpiryNotices(listOf(soon), today.plusDays(6), week, { "A" }) { _, _ -> ExpiryState.SOON }
        assertEquals(ExpiryState.EXPIRED, after.single().state)
        assertTrue(pendingExpiryNotices(listOf(soon), today.plusDays(9), week, { "A" }) { _, _ -> ExpiryState.EXPIRED }.isEmpty())
    }

    @Test
    fun portalsWithoutDateOrFarAwayGetNoNotice() {
        val portals = listOf(portal("00:1A:79:00:00:02", null), portal("00:1A:79:00:00:03", today.plusDays(30)))
        assertTrue(pendingExpiryNotices(portals, today, week, { "x" }) { _, _ -> null }.isEmpty())
    }
}
