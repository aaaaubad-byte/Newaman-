package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TaskContractTest {

    @Test
    fun taskStatusesMatchTheCurrentDatabaseContract() {
        assertEquals("open", TaskStatus.OPEN.value)
        assertEquals("due", TaskStatus.DUE.value)
        assertEquals("completed", TaskStatus.COMPLETED.value)
        assertEquals("cancelled", TaskStatus.CANCELLED.value)
    }

    @Test
    fun taskStatusParserPreservesDatabaseStates() {
        assertEquals(TaskStatus.OPEN, TaskStatus.fromValue("open"))
        assertEquals(TaskStatus.DUE, TaskStatus.fromValue("due"))
        assertEquals(TaskStatus.COMPLETED, TaskStatus.fromValue("completed"))
        assertEquals(TaskStatus.CANCELLED, TaskStatus.fromValue("cancelled"))
        assertEquals(TaskStatus.OPEN, TaskStatus.fromValue("unknown"))
    }

    @Test
    fun taskStateFlagsReflectLifecycleStatus() {
        val openTask = PaymentTask(
            protectionId = "p1",
            numberId = "n1",
            phoneNumber = "770000000",
            providerId = "prov1",
            providerNameAr = "يمن موبايل",
            dueDate = "2026-10-01",
            amountSnapshot = 1000.0,
            status = TaskStatus.OPEN
        )
        assertTrue(openTask.isOpen)
        assertFalse(openTask.isCompleted)
        assertFalse(openTask.isCancelled)

        val dueTask = openTask.copy(status = TaskStatus.DUE)
        assertTrue(dueTask.isOpen)
        assertFalse(dueTask.isCompleted)
        assertFalse(dueTask.isCancelled)

        val completedTask = openTask.copy(status = TaskStatus.COMPLETED, completedAt = "2026-10-01", paymentReference = "REF123")
        assertFalse(completedTask.isOpen)
        assertTrue(completedTask.isCompleted)
        assertFalse(completedTask.isCancelled)

        val cancelledTask = openTask.copy(status = TaskStatus.CANCELLED, cancellationReason = "طلب العميل")
        assertFalse(cancelledTask.isOpen)
        assertFalse(cancelledTask.isCompleted)
        assertTrue(cancelledTask.isCancelled)
        assertEquals("طلب العميل", cancelledTask.cancellationReason)
    }

    @Test
    fun taskTimeClassificationCorrectlyEvaluatesDates() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
        val today = Calendar.getInstance()

        val pastCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -2) }
        val pastStr = sdf.format(pastCal.time)
        assertEquals(TaskTimeClassification.OVERDUE, TaskTimeClassification.calculate(pastStr))

        val todayStr = sdf.format(today.time)
        assertEquals(TaskTimeClassification.DUE, TaskTimeClassification.calculate(todayStr))

        val soonCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 3) }
        val soonStr = sdf.format(soonCal.time)
        assertEquals(TaskTimeClassification.DUE_SOON, TaskTimeClassification.calculate(soonStr))

        val distantCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 20) }
        val distantStr = sdf.format(distantCal.time)
        assertEquals(TaskTimeClassification.UPCOMING, TaskTimeClassification.calculate(distantStr))
    }

    @Test
    fun providerSettingsCarryTheVisibilityWindow() {
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
