package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.rbac.AuthorizationManager
import com.example.core.rbac.Permission
import com.example.R
import com.example.data.model.User
import com.example.data.model.UserType
import com.example.ui.theme.AmanDarkSlate
import com.example.ui.theme.AmanDrawerActivePill
import com.example.ui.theme.AmanDrawerDeep
import com.example.ui.theme.AmanDrawerMid
import com.example.ui.theme.AmanTealLight
import com.example.ui.theme.AmanTealDark
import com.example.ui.theme.AmanTealUltraLight
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextSecondary

data class DrawerMenuItem(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val isDividerBefore: Boolean = false,
    val permission: Permission? = null
)

// ========================================================
// AMAN Navigation Drawer - Single App with Customer & Admin
// ========================================================
@Composable
fun AmanDrawerContent(
    currentUser: User,
    currentRoute: String,
    onNavigate: (String) -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isCustomer = currentUser.userType == UserType.CUSTOMER

    val customerItems = listOf(
        DrawerMenuItem("home", "الرئيسية", Icons.Default.Home),
        DrawerMenuItem("phones", "الأرقام", Icons.Default.Phone),
        DrawerMenuItem("requests", "الطلبات", Icons.Default.AssignmentTurnedIn),
        DrawerMenuItem("protections", "الحمايات", Icons.Default.Security),
        DrawerMenuItem("notifications", "الإشعارات", Icons.Default.Notifications),
        DrawerMenuItem("account", "الحساب", Icons.Default.Person, isDividerBefore = true),
        DrawerMenuItem("more", "المزيد", Icons.Default.MoreVert, isDividerBefore = true)
    )

    val allAdminItems = listOf(
        DrawerMenuItem("overview", "الرئيسية", Icons.Default.Home),
        DrawerMenuItem("customers", "العملاء", Icons.Default.People, permission = Permission.CUSTOMERS_READ),
        DrawerMenuItem("phones", "الأرقام", Icons.Default.Phone, permission = Permission.NUMBERS_READ),
        DrawerMenuItem("requests", "الطلبات", Icons.Default.AssignmentTurnedIn, permission = Permission.REQUESTS_READ),
        DrawerMenuItem("protections", "الحمايات", Icons.Default.Security, permission = Permission.PROTECTIONS_READ),
        DrawerMenuItem("tasks", "مهام السداد", Icons.Default.DateRange, permission = Permission.TASKS_READ),
        DrawerMenuItem("notifications", "الإشعارات", Icons.Default.Notifications, permission = Permission.NOTIFICATIONS_READ),
        DrawerMenuItem("task_settings", "إعدادات المهام", Icons.Default.DateRange, permission = Permission.TASK_SETTINGS_MANAGE),
        DrawerMenuItem("employees", "الموظفون والأدوار", Icons.Default.People, permission = Permission.EMPLOYEES_MANAGE),
        DrawerMenuItem("roles", "الأدوار والصلاحيات", Icons.Default.Security, permission = Permission.ROLES_MANAGE),
        DrawerMenuItem("telecom", "شركات الاتصالات", Icons.Default.Phone, isDividerBefore = true, permission = Permission.PROVIDERS_MANAGE),
        DrawerMenuItem("wallets", "وسائل الدفع", Icons.Default.AccountBalance, permission = Permission.PAYMENT_METHODS_MANAGE),
        DrawerMenuItem("settings", "الإعدادات", Icons.Default.Settings, permission = Permission.SETTINGS_MANAGE),
        DrawerMenuItem("audit", "سجل العمليات", Icons.Default.History, permission = Permission.AUDIT_READ),
        DrawerMenuItem("account", "الحساب", Icons.Default.Person, isDividerBefore = true)
    )

    // Dynamic RBAC Filter: Only show items the user has permission to view
    val adminItems = allAdminItems.filter { item ->
        item.permission == null || AuthorizationManager.hasPermission(currentUser.userType, currentUser.role, item.permission)
    }

    val menuItems = if (isCustomer) customerItems else adminItems

    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    text = "تسجيل الخروج",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmanDarkSlate
                )
            },
            text = {
                Text(
                    text = "هل تريد بالتأكيد تسجيل الخروج من حسابك؟",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("تسجيل الخروج", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("إلغاء", color = TextSecondary, fontSize = 12.sp)
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = SurfaceWhite
        )
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(SurfaceWhite)
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // 1. Top Logo: Shield + "أمان AMAN"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp)
            ) {
                    Image(painter = painterResource(R.drawable.aman_logo), contentDescription = "أمان", contentScale = ContentScale.Crop, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "أمان | AMAN",
                        color = AmanDarkSlate,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isCustomer) "منطقة العميل" else (currentUser.role?.titleAr ?: "لوحة الإدارة"),
                        color = AmanTealDark,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. User Info Card in Drawer Header
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.10f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onNavigate("account") }
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentUser.fullName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = currentUser.email,
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            // 3. Dynamic Menu Navigation Items
            menuItems.forEach { item ->
                if (item.isDividerBefore) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(6.dp))
                }

                val isSelected = currentRoute == item.route

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isSelected) AmanTealUltraLight else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onNavigate(item.route) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            tint = if (isSelected) AmanTealDark else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = item.title,
                            color = if (isSelected) AmanTealDark else TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
            Spacer(modifier = Modifier.height(8.dp))

            // 4. Logout Button at bottom
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showLogoutDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "تسجيل الخروج",
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "تسجيل الخروج",
                        color = Color(0xFFFF8A80),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
