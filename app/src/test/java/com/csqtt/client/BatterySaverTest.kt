package com.csqtt.client

import org.junit.Assert.*
import org.junit.Test

class BatterySaverTest {
    @Test fun autoFollowsSystemSaverInsteadOfChargeThreshold() {
        assertTrue(BatterySaver.shouldSave("auto", 30, 80, false, true))
        assertFalse(BatterySaver.shouldSave("auto", 30, 5, false, false))
    }

    @Test fun ownLevelRequiresKnownLowChargeAndNoCharger() {
        assertTrue(BatterySaver.shouldSave("level", 30, 30, false, false))
        assertTrue(BatterySaver.shouldSave("level", 30, 0, false, false))
        assertFalse(BatterySaver.shouldSave("level", 30, 31, false, true))
        assertFalse(BatterySaver.shouldSave("level", 30, 10, true, true))
        assertFalse(BatterySaver.shouldSave("level", 30, -1, false, true))
    }

    @Test fun disabledAndUnknownModesDoNotReleaseCpuForSaving() {
        for (mode in listOf("off", "", "unknown")) assertFalse(BatterySaver.shouldSave(mode, 30, 5, false, true))
    }

    @Test fun defaultsAreVisibleChoices() {
        assertEquals("auto", BatterySaver.DEFAULT_MODE)
        assertEquals(BatterySaver.DEFAULT_MODE, BatterySaver.MODES.first().first)
        assertTrue(BatterySaver.DEFAULT_PERCENT in BatterySaver.PERCENT_CHOICES)
    }
}
