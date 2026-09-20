package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaskContractTest {
    @Test fun taskStatusesMatchTheCurrentDatabaseContract() {
        assertEquals("open", TaskStatus.OPEN.value)
        assertEquals("due", TaskStatus.DUE.value)
        assertEquals("completed", TaskStatus.COMPLETED.value)
        assertEquals("cancelled", TaskStatus.CANCELLED.value)
    }

    @Test fun taskStatusParserPreservesDatabaseStates() {
        assertEquals(TaskStatus.OPEN, TaskStatus.fromValue("open"))
        assertEquals(TaskStatus.DUE, TaskStatus.fromValue("due"))
        assertEquals(TaskStatus.COMPLETED, TaskStatus.fromValue("completed"))
        assertEquals(TaskStatus.CANCELLED, TaskStatus.fromValue("cancelled"))
        assertEquals(TaskStatus.OPEN, TaskStatus.fromValue("unknown"))
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
