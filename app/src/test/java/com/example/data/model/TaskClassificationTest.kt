package com.example.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskClassificationTest {
    private fun task(due: String, status: TaskStatus = TaskStatus.DUE) = PaymentTask(
        id = "t", protectionId = "p", numberId = "n", phoneNumber = "000000000", providerId = "provider",
        providerNameAr = "", dueDate = due, amountSnapshot = 100.0, status = status
    )

    @Test fun overdueIsCalculatedFromDate() = assertEquals(TaskTemporalClass.OVERDUE, classifyTaskStatus(task("2026-09-10"), 7, "2026-09-11"))
    @Test fun dueIsToday() = assertEquals(TaskTemporalClass.TODAY, classifyTaskStatus(task("2026-09-11"), 7, "2026-09-11"))
    @Test fun dueSoonIsWithinSevenDays() = assertEquals(TaskTemporalClass.DUE_SOON, classifyTaskStatus(task("2026-09-18"), 7, "2026-09-11"))
    @Test fun upcomingIsAfterSevenDays() = assertEquals(TaskTemporalClass.UPCOMING, classifyTaskStatus(task("2026-09-19"), 7, "2026-09-11"))
    @Test fun completedRemainsFinal() = assertEquals(TaskTemporalClass.COMPLETED, classifyTaskStatus(task("2020-01-01", TaskStatus.COMPLETED), 7, "2026-09-11"))
}
