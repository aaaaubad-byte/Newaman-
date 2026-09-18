package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskContractTest {
    @Test fun taskStatusesMatchTheCurrentDatabaseContract() {
        assertEquals("due", TaskStatus.DUE.value)
        assertEquals("completed", TaskStatus.COMPLETED.value)
    }

    @Test fun providerSettingsCarryTheVisibilityWindow() {
        val settings = TaskSettings(
            id = "settings",
            providerId = "provider-a",
            firstTaskEnabled = true,
            manualRescheduleEnabled = true,
            intervalDays = 30,
            isActive = true,
            visibilityDaysBefore = 5
        )
        assertTrue(settings.firstTaskEnabled)
        assertEquals(30, settings.intervalDays)
        assertEquals(5, settings.visibilityDaysBefore)
    }
}
