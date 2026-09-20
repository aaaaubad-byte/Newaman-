package com.example.data.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class UserType {
    CUSTOMER,
    ADMIN
}

enum class AdminRole(val code: String, val titleAr: String) {
    SUPER_ADMIN("super_admin", "Super Admin"),
    FINANCE_OFFICER("finance_officer", "Finance Officer"),
    OPERATIONS_OFFICER("operations_officer", "Operations Officer"),
    SUPPORT("support", "Support");
    companion object {
        fun fromCode(code: String?): AdminRole? {
            val normalized = code?.trim()?.lowercase()?.replace(" ", "_")?.replace("-", "_")
            return entries.firstOrNull { it.code == normalized }
        }
    }
}


data class User(
    val id: String = UUID.randomUUID().toString(),
    val email: String = "",
    val fullName: String,
    val phone: String? = null,
    val userType: UserType = UserType.CUSTOMER,
    val role: AdminRole? = null,
    val status: String = "active"
)

data class TelecomProvider(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val code: String,
    val prefixes: List<String>,
    val numberLength: Int = 9,
    val primaryColorHex: Long = 0xFF00695C,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val isVisibleToCustomer: Boolean = true,
    val logoUrl: String? = null
)

data class ProtectionPackage(
    val id: String,
    val providerId: String,
    val name: String,
    val durationDays: Int,
    val price: Double,
    val currency: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0,
    val description: String? = null,
    val isVisibleToCustomer: Boolean = true
)

data class PaymentWallet(
    val id: String,
    val nameAr: String,
    val nameEn: String,
    val code: String,
    val beneficiaryAccount: String,
    val beneficiaryName: String,
    val instructionsAr: String,
    val isActive: Boolean = true,
    val sortOrder: Int = 0
)

data class CustomerNumber(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val phoneNumber: String,
    val providerId: String,
    val providerNameAr: String,
    val status: String = "active",
    val isProtected: Boolean = false,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
)

enum class RequestStatus(val value: String, val titleAr: String) {
    PENDING("pending", "قيد المراجعة"),
    APPROVED("approved", "مقبول"),
    REJECTED("rejected", "مرفوض")
}

data class ProtectionRequest(
    val id: String = UUID.randomUUID().toString(),
    val customerId: String,
    val customerName: String,
    val numberId: String,
    val phoneNumber: String,
    val providerId: String,
    val providerNameAr: String,
    val walletId: String,
    val walletNameAr: String,
    val paymentReference: String,
    val priceSnapshot: Double,
    val durationDaysSnapshot: Int,
    val currencySnapshot: String = "YER",
    val paymentStatus: String = "pending",
    val requestType: String = "new",
    val previousProtectionId: String? = null,
    val status: RequestStatus = RequestStatus.PENDING,
    val rejectionReason: String? = null,
    val verifiedAt: String? = null,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
)

data class Protection(
    val id: String = UUID.randomUUID().toString(),
    val requestId: String,
    val customerId: String,
    val numberId: String,
    val phoneNumber: String,
    val providerNameAr: String,
    val status: String = "active",
    val startDate: String,
    val endDate: String,
    val durationDaysSnapshot: Int,
    val priceSnapshot: Double
) {
    // Dynamic remaining days calculation as specified in the business rules (never fixed)
    val daysRemaining: Int
        get() = calculateDaysRemaining(endDate)

    companion object {
        fun calculateDaysRemaining(endDateStr: String): Int {
            return try {
                val format = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH)
                val endDate = format.parse(endDateStr) ?: return 0
                val now = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time
                val diffMs = endDate.time - now.time
                val days = (diffMs / (1000 * 60 * 60 * 24)).toInt()
                if (days < 0) 0 else days
            } catch (e: Exception) {
                0
            }
        }
    }
}

data class Subscription(
    val id: String,
    val customerId: String,
    val protectedPhoneId: String,
    val startDate: String,
    val endDate: String,
    val durationDaysSnapshot: Int,
    val priceSnapshot: Double,
    val currencySnapshot: String,
    val status: String = "active",
    val createdAt: String = ""
)

enum class TaskStatus(val value: String, val titleAr: String) {
    OPEN("open", "مفتوحة"),
    DUE("due", "مستحقة"),
    COMPLETED("completed", "مكتملة"),
    CANCELLED("cancelled", "ملغاة");

    companion object {
        fun fromValue(value: String?): TaskStatus =
            entries.firstOrNull { it.value == value?.trim()?.lowercase() } ?: OPEN
    }
}

enum class TaskTimeClassification(val value: String, val titleAr: String) {
    OVERDUE("overdue", "متأخرة"),
    DUE("due", "مستحقة اليوم"),
    DUE_SOON("due_soon", "قريبة الاستحقاق"),
    UPCOMING("upcoming", "قادمة");

    companion object {
        fun calculate(dueDateStr: String?, dueSoonDays: Int = 7): TaskTimeClassification {
            if (dueDateStr.isNullOrBlank()) return UPCOMING
            return try {
                val clean = dueDateStr.take(10)
                val parts = clean.split("-")
                if (parts.size != 3) return UPCOMING
                val cal = java.util.Calendar.getInstance().apply {
                    set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt(), 0, 0, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val today = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, 0)
                    set(java.util.Calendar.MINUTE, 0)
                    set(java.util.Calendar.SECOND, 0)
                    set(java.util.Calendar.MILLISECOND, 0)
                }
                val diffDays = ((cal.timeInMillis - today.timeInMillis) / (1000L * 60 * 60 * 24)).toInt()
                when {
                    diffDays < 0 -> OVERDUE
                    diffDays == 0 -> DUE
                    diffDays <= dueSoonDays -> DUE_SOON
                    else -> UPCOMING
                }
            } catch (_: Exception) {
                UPCOMING
            }
        }
    }
}

data class TaskSettings(
    val id: String,
    val providerId: String,
    val firstTaskEnabled: Boolean,
    val manualRescheduleEnabled: Boolean,
    val intervalDays: Int?,
    val isActive: Boolean,
    val visibilityDaysBefore: Int = 30
)

data class TaskAmountSetting(
    val id: String?,
    val providerId: String,
    val taskType: String,
    val amount: Double,
    val currency: String = "YER",
    val isActive: Boolean = true
)

data class TaskClassification(
    val id:String, val taskSettingsId:String, val name:String, val minDaysRemaining:Int?, val maxDaysRemaining:Int?, val sortOrder:Int, val isActive:Boolean
)

data class NotificationSetting(
    val id: String,
    val type: String,
    val recipient: String,
    val enabled: Boolean,
    val daysBefore: Int?
)

data class EmployeeAccount(
    val id: String,
    val fullName: String,
    val email: String,
    val status: String,
    val roleId: String?,
    val roleName: String?
)

data class PaymentTask(
    val id: String = UUID.randomUUID().toString(),
    val protectionId: String,
    val numberId: String,
    val phoneNumber: String,
    val providerId: String,
    val providerNameAr: String,
    val cycleNumber: Int = 1,
    val dueDate: String,
    val amountSnapshot: Double,
    val status: TaskStatus = TaskStatus.DUE,
    val completedAt: String? = null,
    val paymentReference: String? = null,
    val notes: String? = null,
    val taskType: String = "recurring",
    val telecomDueAt: String? = null,
    val daysRemaining: Int? = null,
    val classificationName: String? = null,
    val cancellationReason: String? = null
) {
    val timeClassification: TaskTimeClassification
        get() = TaskTimeClassification.calculate(telecomDueAt ?: dueDate)

    val isOpen: Boolean
        get() = status == TaskStatus.OPEN || status == TaskStatus.DUE

    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED

    val isCancelled: Boolean
        get() = status == TaskStatus.CANCELLED
}

data class FinancialTransaction(
    val id: String = UUID.randomUUID().toString(),
    val type: String, // "income" or "expense"
    val amount: Double,
    val currency: String = "YER",
    val descriptionAr: String,
    val reference: String? = null,
    val relatedEntityType: String? = null,
    val relatedEntityId: String? = null,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
)

data class FinancialSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netBalance: Double = 0.0,
    val currency: String = "YER"
)

data class AuditLog(
    val id: String = UUID.randomUUID().toString(),
    val action: String,
    val entityType: String,
    val entityId: String? = null,
    val performedByRole: String,
    val performedByName: String = "",
    val details: String,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date()),
    val beforeData: String? = null,
    val afterData: String? = null,
    val metadata: String? = null
)

data class StateTransitionLog(
    val id: String = UUID.randomUUID().toString(),
    val entityType: String,
    val entityId: String,
    val fromState: String,
    val toState: String,
    val actorId: String,
    val actorRole: String,
    val reason: String? = null,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.ENGLISH).format(Date())
)

data class NotificationItem(
    val id: String = UUID.randomUUID().toString(),
    val recipientId: String? = null,
    val titleAr: String,
    val bodyAr: String,
    val isRead: Boolean = false,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date()),
    val actionRoute: String? = null,
    val type: String = "",
    val relatedType: String? = null,
    val relatedId: String? = null
)

data class Role(
    val id: String,
    val name: String,
    val description: String? = null,
    val isActive: Boolean = true,
    val permissionCodes: List<String> = emptyList(),
    val assignedEmployees: Int = 0
)

data class PermissionItem(
    val id: String,
    val code: String,
    val name: String,
    val description: String? = null
)

data class CustomerAccount(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val phone: String,
    val isSuspended: Boolean = false,
    val registeredNumbersCount: Int = 0,
    val activeProtectionsCount: Int = 0,
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(Date())
)

data class SystemSettings(
    val currency: String = "YER",
    val timezone: String = "Asia/Aden",
    val dateFormat: String = "yyyy-MM-dd",
    val timeFormat: String = "24h",
    val maintenanceMode: Boolean = false
)
