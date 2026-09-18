package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalConfiguration
import com.example.core.rbac.AuthorizationManager
import com.example.core.rbac.Permission
import com.example.data.model.User
import com.example.data.model.UserType
import com.example.ui.theme.AmanTealDark
import com.example.ui.theme.AmanTealLight
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

private data class BottomTab(val route: String, val title: String, val icon: ImageVector)

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AmanBottomNavigation(
    currentUser: User,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMore by remember { mutableStateOf(false) }
    val isCustomer = currentUser.userType == UserType.CUSTOMER
    val tabs = if (isCustomer) {
        listOf(
            BottomTab("home", "الرئيسية", Icons.Default.Home),
            BottomTab("phones", "الأرقام", Icons.Default.Phone),
            BottomTab("requests", "الطلبات", Icons.Default.AssignmentTurnedIn),
            BottomTab("protections", "الحمايات", Icons.Default.Security)
        )
    } else {
        listOf(
            BottomTab("overview", "الرئيسية", Icons.Default.Home),
            BottomTab("requests", "الطلبات", Icons.Default.AssignmentTurnedIn),
            BottomTab("tasks", "المهام", Icons.Default.DateRange),
            BottomTab("notifications", "الإشعارات", Icons.Default.Notifications)
        )
    }

    NavigationBar(
        modifier = modifier.navigationBarsPadding(),
        containerColor = SurfaceWhite,
        tonalElevation = 6.dp
    ) {
        // In RTL the first item is rendered on the right; keep More there explicitly.
        NavigationBarItem(
            selected = showMore || currentRoute in moreRoutes(isCustomer),
            onClick = { showMore = true },
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "المزيد") },
            label = { Text("المزيد", fontSize = 10.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = AmanTealDark,
                selectedTextColor = AmanTealDark,
                indicatorColor = AmanTealLight.copy(alpha = 0.72f),
                unselectedIconColor = TextSecondary,
                unselectedTextColor = TextSecondary
            )
        )
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = currentRoute == tab.route,
                onClick = { onNavigate(tab.route) },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AmanTealDark,
                    selectedTextColor = AmanTealDark,
                    indicatorColor = AmanTealLight.copy(alpha = 0.72f),
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }

    if (showMore) {
        Dialog(onDismissRequest = { showMore = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.20f)), contentAlignment = Alignment.CenterEnd) {
            Column(modifier = Modifier.fillMaxHeight().width(maxOf(280.dp, LocalConfiguration.current.screenWidthDp.dp * 0.333f)).background(SurfaceWhite).verticalScroll(rememberScrollState()).padding(bottom = 8.dp)) {
                Text("AMAN | أمان", style = MaterialTheme.typography.titleMedium, color = AmanTealDark, modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp))
                Text(
                    text = "كل الأقسام",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
                HorizontalDivider(color = BorderSubtle)
                moreItems(currentUser).forEach { item ->
                    ListItem(
                        headlineContent = { Text(item.title, color = TextPrimary) },
                        leadingContent = { Icon(item.icon, contentDescription = null, tint = AmanTealDark) },
                        modifier = Modifier.fillMaxWidth().clickable {
                            showMore = false
                            onNavigate(item.route)
                        }
                    )
                }
                ListItem(
                    headlineContent = { Text("تسجيل الخروج", color = TextSecondary) },
                    leadingContent = { Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().clickable {
                        showMore = false
                        onLogout()
                    }
                )
            }
            }
        }
    }
}

private fun moreRoutes(isCustomer: Boolean) = if (isCustomer) {
    setOf("notifications", "account", "settings", "security", "help", "terms", "about")
} else {
    setOf("customers", "phones", "protections", "notifications", "task_settings", "employees", "roles", "telecom", "packages", "wallets", "settings", "audit", "account", "security", "help", "terms", "about")
}

private fun moreItems(user: User): List<DrawerMenuItem> {
    val customer = listOf(
        DrawerMenuItem("home", "الرئيسية", Icons.Default.Home),
        DrawerMenuItem("phones", "الأرقام", Icons.Default.Phone),
        DrawerMenuItem("requests", "الطلبات", Icons.Default.AssignmentTurnedIn),
        DrawerMenuItem("protections", "الحمايات", Icons.Default.Security),
        DrawerMenuItem("notifications", "الإشعارات", Icons.Default.Notifications),
        DrawerMenuItem("account", "الحساب", Icons.Default.Person),
        DrawerMenuItem("settings", "الإعدادات", Icons.Default.Settings),
        DrawerMenuItem("security", "الأمان", Icons.Default.Security),
        DrawerMenuItem("help", "المساعدة", Icons.Default.Person),
        DrawerMenuItem("terms", "الشروط والأحكام", Icons.Default.Person),
        DrawerMenuItem("about", "حول أمان", Icons.Default.Person)
    )
    val admin = listOf(
        DrawerMenuItem("overview", "الرئيسية", Icons.Default.Home),
        DrawerMenuItem("customers", "العملاء", Icons.Default.People, permission = Permission.CUSTOMERS_READ),
        DrawerMenuItem("phones", "الأرقام", Icons.Default.Phone, permission = Permission.NUMBERS_READ),
        DrawerMenuItem("requests", "الطلبات", Icons.Default.AssignmentTurnedIn, permission = Permission.REQUESTS_READ),
        DrawerMenuItem("protections", "الحمايات", Icons.Default.Security, permission = Permission.PROTECTIONS_READ),
        DrawerMenuItem("tasks", "المهام", Icons.Default.DateRange, permission = Permission.TASKS_READ),
        DrawerMenuItem("notifications", "الإشعارات", Icons.Default.Notifications, permission = Permission.NOTIFICATIONS_READ),
        DrawerMenuItem("task_settings", "إعدادات المهام", Icons.Default.DateRange, permission = Permission.TASK_SETTINGS_MANAGE),
        DrawerMenuItem("employees", "الموظفون", Icons.Default.People, permission = Permission.EMPLOYEES_MANAGE),
        DrawerMenuItem("roles", "الأدوار والصلاحيات", Icons.Default.Security, permission = Permission.ROLES_MANAGE),
        DrawerMenuItem("telecom", "شركات الاتصالات", Icons.Default.Phone, permission = Permission.PROVIDERS_MANAGE),
        DrawerMenuItem("packages", "الباقات", Icons.Default.Payment, permission = Permission.PACKAGES_MANAGE),
        DrawerMenuItem("wallets", "وسائل الدفع", Icons.Default.AccountBalance, permission = Permission.PAYMENT_METHODS_MANAGE),
        DrawerMenuItem("settings", "الإعدادات", Icons.Default.Settings, permission = Permission.SETTINGS_MANAGE),
        DrawerMenuItem("audit", "سجل العمليات", Icons.Default.History, permission = Permission.AUDIT_READ),
        DrawerMenuItem("account", "الحساب", Icons.Default.Person)
    ).filter { it.permission == null || AuthorizationManager.hasPermission(user.userType, user.role, it.permission) }
    return if (user.userType == UserType.CUSTOMER) customer else admin
}
