package com.iptvplayer.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateChannelTest {
    @Test
    fun releasesAreAlwaysOffered() {
        assertTrue(offersRelease("v1.0.91", draft = false, prerelease = false, allowBeta = false))
        assertTrue(offersRelease("v1.0.91", draft = false, prerelease = false, allowBeta = true))
        assertFalse(offersRelease("v1.0.91", draft = true, prerelease = false, allowBeta = true))
    }

    @Test
    fun betasOnlyWithTheSwitch() {
        assertFalse(offersRelease("v1.0.91-beta.1", draft = false, prerelease = true, allowBeta = false))
        assertTrue(offersRelease("v1.0.91-beta.1", draft = false, prerelease = true, allowBeta = true))
    }

    @Test
    fun mislabelledReleasesAreNeverOffered() {
        // A beta tag published as a normal release must not reach everyone.
        assertFalse(offersRelease("v1.0.91-beta.1", draft = false, prerelease = false, allowBeta = false))
        assertFalse(offersRelease("v1.0.91-beta.1", draft = false, prerelease = false, allowBeta = true))
        assertFalse(offersRelease("v1.0.91", draft = false, prerelease = true, allowBeta = true))
        assertFalse(offersRelease("v1.0.91-rc1", draft = false, prerelease = true, allowBeta = true))
        assertFalse(offersRelease("1.0.91", draft = false, prerelease = false, allowBeta = false))
    }
}
