package com.example.core.rbac

import com.example.data.model.AdminRole
import com.example.data.model.UserType

enum class Permission {
    DASHBOARD_READ, CUSTOMERS_READ, CUSTOMERS_MANAGE, NUMBERS_READ, NUMBERS_MANAGE,
    REQUESTS_READ, REQUESTS_VERIFY_PAYMENT, REQUESTS_APPROVE, REQUESTS_REJECT,
    PROTECTIONS_READ, TASKS_READ, TASKS_COMPLETE, TASKS_RESCHEDULE,
    PROVIDERS_MANAGE, PACKAGES_MANAGE, PAYMENT_METHODS_MANAGE, TASK_SETTINGS_MANAGE,
    NOTIFICATIONS_READ, NOTIFICATIONS_MANAGE, FINANCIAL_READ,
    EMPLOYEES_MANAGE, ROLES_MANAGE, SETTINGS_MANAGE, AUDIT_READ
}

object AuthorizationManager {
    fun hasPermission(userType: UserType, role: AdminRole?, permission: Permission): Boolean {
        if (userType != UserType.ADMIN || role == null) return false
        return when (role) {
            AdminRole.SUPER_ADMIN -> true
            AdminRole.FINANCE_OFFICER -> permission in setOf(
                Permission.DASHBOARD_READ, Permission.REQUESTS_READ, Permission.REQUESTS_VERIFY_PAYMENT,
                Permission.FINANCIAL_READ, Permission.NOTIFICATIONS_READ, Permission.AUDIT_READ
            )
            AdminRole.OPERATIONS_OFFICER -> permission in setOf(
                Permission.DASHBOARD_READ, Permission.CUSTOMERS_READ, Permission.NUMBERS_READ,
                Permission.REQUESTS_READ, Permission.REQUESTS_APPROVE, Permission.REQUESTS_REJECT,
                Permission.PROTECTIONS_READ, Permission.TASKS_READ, Permission.TASKS_COMPLETE,
                Permission.TASKS_RESCHEDULE, Permission.PROVIDERS_MANAGE, Permission.NOTIFICATIONS_READ
            )
            AdminRole.SUPPORT -> permission in setOf(
                Permission.DASHBOARD_READ, Permission.CUSTOMERS_READ, Permission.NUMBERS_READ,
                Permission.REQUESTS_READ, Permission.PROTECTIONS_READ, Permission.NOTIFICATIONS_READ
            )
        }
    }
    fun canApproveRequests(userType: UserType, role: AdminRole?) = hasPermission(userType, role, Permission.REQUESTS_APPROVE)
    fun canExecuteTasks(userType: UserType, role: AdminRole?) = hasPermission(userType, role, Permission.TASKS_COMPLETE)
    fun canViewFinances(userType: UserType, role: AdminRole?) = hasPermission(userType, role, Permission.FINANCIAL_READ)
    fun canManageSettings(userType: UserType, role: AdminRole?) = hasPermission(userType, role, Permission.SETTINGS_MANAGE)
}
