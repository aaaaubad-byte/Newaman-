package com.example.ui.screens.admin

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PaymentTask
import com.example.data.model.PaymentWallet
import com.example.data.model.AdminRole
import com.example.data.model.ProtectionPackage
import com.example.data.model.ProtectionRequest
import com.example.data.model.Protection
import com.example.data.model.RequestStatus
import com.example.data.model.SystemSettings
import com.example.data.model.TaskStatus
import com.example.data.model.TaskClassification
import com.example.data.model.TaskTimeClassification
import com.example.data.model.TelecomProvider
import com.example.data.model.User
import com.example.data.model.UserType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.example.ui.components.ActionRequiredCard
import com.example.core.rbac.AuthorizationManager
import com.example.core.rbac.Permission
import com.example.ui.components.ActiveStatusBadge
import com.example.ui.components.EmptyStateView
import com.example.ui.components.ProviderBadge
import com.example.ui.components.RequestStatusPill
import com.example.ui.components.SearchAndDropdownFilterBar
import com.example.ui.components.SearchAndTwoFilterBar
import com.example.ui.components.SearchAndFilterBar
import com.example.ui.components.StatCard

import com.example.ui.components.StoppedStatusBadge
import com.example.ui.components.TrustBadgesFooter
import com.example.ui.theme.AmanBgLight
import com.example.ui.theme.AmanDarkSlate
import com.example.ui.theme.AmanTealDark
import com.example.ui.theme.AmanTealLight
import com.example.ui.theme.AmanTealPrimary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveDot
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingText
import com.example.ui.theme.StatusRejectedBg
import com.example.ui.theme.StatusRejectedText
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AmanViewModel

@Composable
private fun SmallSummaryCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AmanTealDark)
            Text(label, fontSize = 10.sp, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

// ========================================================
// 02. Admin Overview Screen (الرئيسية)

// ========================================================
@Composable
fun AdminOverviewScreen(
    viewModel: AmanViewModel,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val customers by viewModel.customers.collectAsState()
    val numbers by viewModel.customerNumbers.collectAsState()
    val protections by viewModel.protections.collectAsState()
    val requests by viewModel.protectionRequests.collectAsState()
    val notificationSettings by viewModel.notificationSettings.collectAsState()
    val paymentTasks by viewModel.paymentTasks.collectAsState()

    val pendingRequests = requests.count { it.status == RequestStatus.PENDING }
    val renewalWindow = notificationSettings.firstOrNull { it.type == "renewal_needed" }?.daysBefore
    val renewalNeeded = if (renewalWindow != null) protections.count { it.status == "active" && it.daysRemaining <= renewalWindow } else 0
    val unreadNotifications = viewModel.notifications.collectAsState().value.count { !it.isRead }
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    val visibleOpenTasks = paymentTasks.filter { it.status != TaskStatus.COMPLETED && it.status != TaskStatus.CANCELLED }
    val todayTasks = visibleOpenTasks.count { it.dueDate.take(10) == today }
    val overdueTasks = visibleOpenTasks.count { it.dueDate.take(10) < today }
    val upcomingTasks = visibleOpenTasks.count { it.dueDate.take(10) > today }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Owner greeting
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AmanTealLight)
                        .border(1.5.dp, AmanTealPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AmanTealDark,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "مرحباً، ${currentUser?.fullName ?: "مسؤول النظام"}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmanDarkSlate
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${currentUser?.role?.titleAr ?: "مسؤول أمان"} — لوحة التحكم التشغيلية لمنظومة أمان",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Main overview cards — task management remains in the dedicated Tasks tab.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "العملاء",
                value = "${customers.size}",
                icon = Icons.Default.Person,
                iconBgColor = Color(0xFFE0F2FE),
                iconTint = Color(0xFF0284C7),
                onClick = { onNavigate("customers") },
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "الأرقام",
                value = "${numbers.size}",
                icon = Icons.Default.Phone,
                iconBgColor = Color(0xFFEFF6FF),
                iconTint = Color(0xFF3B82F6),
                onClick = { onNavigate("phones") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "الحمايات",
                value = "${protections.size}",
                icon = Icons.Default.Security,
                iconBgColor = Color(0xFFECFDF5),
                iconTint = Color(0xFF059669),
                onClick = { onNavigate("protections") },
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        TaskSummaryCard(
            total = paymentTasks.size,
            today = todayTasks,
            overdue = overdueTasks,
            upcoming = upcomingTasks,
            onClick = { onNavigate("tasks") }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action Required Card ("يحتاج إجراء")
        ActionRequiredCard(pendingCount = pendingRequests)
        Spacer(modifier = Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(8.dp)){
            SmallSummaryCard("طلبات معلقة",pendingRequests.toString(),Modifier.weight(1f))
            SmallSummaryCard("تحتاج تجديد",renewalNeeded.toString(),Modifier.weight(1f))
            SmallSummaryCard("إشعارات غير مقروءة",unreadNotifications.toString(),Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trust Badges Footer
        TrustBadgesFooter()

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TaskSummaryCard(
    total: Int,
    today: Int,
    overdue: Int,
    upcoming: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("ملخص المهام", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
                }
                Text(total.toString(), fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = AmanTealDark)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallSummaryCard("اليوم", today.toString(), Modifier.weight(1f))
                SmallSummaryCard("بعد الموعد", overdue.toString(), Modifier.weight(1f))
                SmallSummaryCard("ضمن نافذة الظهور", upcoming.toString(), Modifier.weight(1f))
            }
            Text("فتح قائمة المهام ←", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmanTealDark)
        }
    }
}

// ========================================================
// 03. Admin Protection Requests Screen (طلبات الحماية)
// ========================================================
@Composable
fun AdminRequestsScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val requests by viewModel.protectionRequests.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val systemSettings by viewModel.systemSettings.collectAsState()
    val canApproveRequests = AuthorizationManager.hasPermission(UserType.ADMIN, currentUser?.role, Permission.REQUESTS_APPROVE)
    val canRejectRequests = AuthorizationManager.hasPermission(UserType.ADMIN, currentUser?.role, Permission.REQUESTS_REJECT)

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }

    var selectedForApproval by remember { mutableStateOf<ProtectionRequest?>(null) }
    var selectedForRejection by remember { mutableStateOf<ProtectionRequest?>(null) }
    var selectedForDetails by remember { mutableStateOf<ProtectionRequest?>(null) }

    val filterOptions = listOf("الكل", "قيد المراجعة", "مقبول", "مرفوض")

    val filtered = requests.filter { r ->
        val matchesQuery = r.phoneNumber.contains(searchQuery) ||
                r.customerName.contains(searchQuery) ||
                r.paymentReference.contains(searchQuery)
        val matchesFilter = when (selectedFilter) {
            "قيد المراجعة" -> r.status == RequestStatus.PENDING
            "مقبول" -> r.status == RequestStatus.APPROVED
            "مرفوض" -> r.status == RequestStatus.REJECTED
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("طلبات الحماية الواردة (${requests.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        SearchAndDropdownFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedFilter = selectedFilter,
            filterOptions = filterOptions,
            onFilterSelected = { selectedFilter = it },
            placeholderText = "بحث بالرقم، العميل، أو المرجع..."
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.AssignmentTurnedIn,
                title = "لا توجد طلبات تطابق البحث"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Surface(color = AmanTealLight.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("الرقم", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
                            Text("العميل", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("الحالة", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            Text("تفاصيل", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.75f))
                        }
                    }
                }
                items(filtered, key = { it.id }) { req ->
                    Surface(color = SurfaceWhite, modifier = Modifier.fillMaxWidth().clickable { selectedForDetails = req }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(req.phoneNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.1f))
                            Text(req.customerName.ifBlank { "غير محدد" }, fontSize = 11.sp, color = TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
                            Text(req.status.titleAr, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            TextButton(onClick = { selectedForDetails = req }, modifier = Modifier.weight(0.75f)) { Text("تفاصيل", fontSize = 10.sp, color = AmanTealDark) }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    selectedForDetails?.let { req ->
        AlertDialog(onDismissRequest = { selectedForDetails = null }, title = { Text("تفاصيل الطلب") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("الرقم: ${req.phoneNumber}", fontWeight = FontWeight.Bold)
                Text("العميل: ${req.customerName}")
                Text("الشركة: ${req.providerNameAr}")
                Text("الباقة: ${req.durationDaysSnapshot} يوم — ${req.priceSnapshot} ${req.currencySnapshot}")
                Text("وسيلة الدفع: ${req.walletNameAr}")
                Text("المرجع: ${req.paymentReference}")
                Text("الحالة: ${req.status.titleAr}", fontWeight = FontWeight.Bold)
            }
        }, confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (req.status == RequestStatus.PENDING) {
                    if (canApproveRequests) {
                        TextButton(onClick = { selectedForDetails = null; selectedForApproval = req }) { Text("قبول") }
                    }
                    if (canRejectRequests) {
                        TextButton(onClick = { selectedForDetails = null; selectedForRejection = req }) { Text("رفض", color = Color(0xFFDC2626)) }
                    }
                }
                TextButton(onClick = { selectedForDetails = null }) { Text("إغلاق") }
            }
        })
    }

    // Approval Dialog
    selectedForApproval?.let { req ->
        AlertDialog(
            onDismissRequest = { selectedForApproval = null },
            title = { Text(if (req.requestType == "renewal") "تأكيد قبول طلب التجديد" else "تأكيد قبول طلب الحماية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("هل تم التأكد من وصول مبلغ ${req.priceSnapshot} ${req.providerNameAr.let { systemSettings.currency }} عبر محفظة ${req.walletNameAr}؟")
                    Text("الرقم: ${req.phoneNumber} (${req.providerNameAr})", fontWeight = FontWeight.Bold)
                    Text("مرجع التحويل: ${req.paymentReference}")
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "إجراء القبول سينفذ كوحدة واحدة داخل قاعدة البيانات:\n• بدء الحماية لمدة ${req.durationDaysSnapshot} يوماً.\n• إنشاء الاشتراك والمهمة التشغيلية الأولى وفق إعدادات النظام.\n• تسجيل الإيراد والإجراءات في السجلات.\n• إشعار العميل.",
                        fontSize = 11.sp,
                        color = AmanTealDark
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (req.requestType == "renewal") viewModel.approveRenewalRequest(req.id) else viewModel.approveRequest(req.id)
                        selectedForApproval = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark)
                ) {
                    Text("تأكيد القبول وبدء الحماية", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedForApproval = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }

    // Rejection Dialog with Mandatory Reason
    selectedForRejection?.let { req ->
        var rejectionReason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedForRejection = null },
            title = { Text("رفض طلب الحماية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرقم: ${req.phoneNumber} - المرجع: ${req.paymentReference}")
                    Text("اكتب سبب الرفض (إلزامي ليصل للعميل):", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = rejectionReason,
                        onValueChange = { rejectionReason = it },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.rejectRequest(req.id, rejectionReason)
                        selectedForRejection = null
                    },
                    enabled = rejectionReason.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("تأكيد الرفض", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedForRejection = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun AdminRequestCard(
    request: ProtectionRequest,
    userRole: AdminRole?,
    onApproveClick: () -> Unit,
    onRejectClick: () -> Unit,
    onVerifyClick: () -> Unit
) {
    val canDecide = AuthorizationManager.hasPermission(UserType.ADMIN, userRole, Permission.REQUESTS_APPROVE)
    
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(request.phoneNumber, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("العميل: ${request.customerName}", fontSize = 11.sp, color = TextSecondary)
                }
                RequestStatusPill(status = request.status)
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("المحفظة: ${request.walletNameAr}", fontSize = 11.sp, color = TextSecondary)
                Text("المرجع: ${request.paymentReference}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الرسوم: ${request.priceSnapshot} ${request.currencySnapshot}", fontSize = 11.sp, color = AmanTealDark, fontWeight = FontWeight.Bold)
                Text("الشركة: ${request.providerNameAr}", fontSize = 11.sp, color = TextSecondary)
            }

            if (request.status == RequestStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canDecide) {
                        Button(
                            onClick = onApproveClick,
                            colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("قبول وتفعيل", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onRejectClick,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("رفض", color = Color(0xFFDC2626), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AmanBgLight,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "الصلاحية",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                modifier = Modifier.padding(6.dp)
                            )
                        }
                    }
                }
            } else if (request.status == RequestStatus.REJECTED && !request.rejectionReason.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("سبب الرفض: ${request.rejectionReason}", fontSize = 11.sp, color = Color(0xFFDC2626))
            }
        }
    }
}

// ========================================================
// مهام السداد
// ========================================================
@Composable
fun AdminPaymentTasksScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.paymentTasks.collectAsState()
    val telecomProviders by viewModel.telecomProviders.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val canCompleteTasks = AuthorizationManager.hasPermission(UserType.ADMIN, currentUser?.role, Permission.TASKS_COMPLETE)
    val canRescheduleTasks = AuthorizationManager.hasPermission(UserType.ADMIN, currentUser?.role, Permission.TASKS_RESCHEDULE)
    val clipboardManager = LocalClipboardManager.current

    var searchQuery by remember { mutableStateOf("") }
    var providerFilter by remember { mutableStateOf("كل الشركات") }
    var taskTab by remember { mutableStateOf("غير مكتملة") }
    var dateFrom by remember { mutableStateOf("") }
    var dateTo by remember { mutableStateOf("") }
    var expandedTaskId by remember { mutableStateOf<String?>(null) }
    var selectedTaskToComplete by remember { mutableStateOf<PaymentTask?>(null) }
    var selectedTaskToReschedule by remember { mutableStateOf<PaymentTask?>(null) }
    var selectedTaskToCancel by remember { mutableStateOf<PaymentTask?>(null) }

    val taskSettings by viewModel.taskSettings.collectAsState()
    val providerOptions = listOf("كل الشركات") + tasks.map { it.providerNameAr }.distinct().sorted()
    val today = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }
    fun beforeDate(date: String, days: Int): String = try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date) ?: return date
        Calendar.getInstance().apply { time = parsed; add(Calendar.DAY_OF_YEAR, -days) }.let { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(it.time) }
    } catch (_: Exception) { date }

    val filtered = tasks.filter { t ->
        val matchesQuery = t.phoneNumber.contains(searchQuery) || t.providerNameAr.contains(searchQuery)
        val matchesProvider = providerFilter == "كل الشركات" || t.providerNameAr == providerFilter
        val dueDay = t.dueDate.take(10)
        val settings = taskSettings.firstOrNull { it.providerId == t.providerId }
        val visibleFrom = beforeDate(dueDay, taskSettings.firstOrNull { it.providerId == t.providerId }?.visibilityDaysBefore ?: 30)
        val isCompleted = t.status == TaskStatus.COMPLETED
        val isCancelled = t.status == TaskStatus.CANCELLED
        val isOpen = t.isOpen
        val isVisible = isCompleted || isCancelled || t.taskType == "initial_activation" || today >= visibleFrom
        val dateMatch = (dateFrom.isBlank() || dueDay >= dateFrom) && (dateTo.isBlank() || dueDay <= dateTo)
        matchesQuery && matchesProvider && dateMatch && isVisible && when (taskTab) {
            "متأخرة" -> isOpen && t.timeClassification == TaskTimeClassification.OVERDUE
            "اليوم" -> isOpen && t.timeClassification == TaskTimeClassification.DUE
            "مكتملة" -> isCompleted
            "ملغاة" -> isCancelled
            "الكل" -> true
            else -> isOpen
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("المهام الدورية (${filtered.size})", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        SearchAndDropdownFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedFilter = providerFilter,
            filterOptions = providerOptions,
            onFilterSelected = { providerFilter = it },
            placeholderText = "بحث برقم الهاتف أو الشركة"
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
        ) {
            listOf("غير مكتملة", "متأخرة", "اليوم", "مكتملة", "ملغاة", "الكل").forEach { tab ->
                OutlinedButton(
                    onClick = { taskTab = tab },
                    colors = if (taskTab == tab) ButtonDefaults.outlinedButtonColors(containerColor = AmanTealLight.copy(alpha = 0.5f)) else ButtonDefaults.outlinedButtonColors()
                ) {
                    Text(if (taskTab == tab) "✓ $tab" else tab, fontSize = 11.sp, fontWeight = if (taskTab == tab) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(value = dateFrom, onValueChange = { dateFrom = it }, label = { Text("من: YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
            OutlinedTextField(value = dateTo, onValueChange = { dateTo = it }, label = { Text("إلى: YYYY-MM-DD") }, singleLine = true, modifier = Modifier.weight(1f))
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.DateRange,
                title = "لا توجد مهام سداد حالياً"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(Modifier.fillMaxWidth().background(AmanTealLight.copy(alpha = 0.35f)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الرقم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("الشركة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("الدورة والحالة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("إجراء", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                items(filtered, key = { it.id }) { task ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(task.phoneNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                                Text(telecomProviders.firstOrNull { it.id == task.providerId }?.code ?: task.providerNameAr.take(3).uppercase(), fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                                val statusColor = when {
                                    task.status == TaskStatus.COMPLETED -> StatusActiveText
                                    task.status == TaskStatus.CANCELLED -> Color(0xFFDC2626)
                                    task.timeClassification == TaskTimeClassification.OVERDUE -> Color(0xFFDC2626)
                                    task.timeClassification == TaskTimeClassification.DUE -> Color(0xFFEA580C)
                                    else -> AmanTealDark
                                }
                                val statusLabel = when {
                                    task.status == TaskStatus.COMPLETED -> "مكتملة"
                                    task.status == TaskStatus.CANCELLED -> "ملغاة"
                                    else -> task.timeClassification.titleAr
                                }
                                Text(statusLabel, fontSize = 10.sp, color = statusColor, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                IconButton(onClick = { expandedTaskId = if (expandedTaskId == task.id) null else task.id }, modifier = Modifier.weight(0.8f)) {
                                    Icon(Icons.Default.Info, contentDescription = if (expandedTaskId == task.id) "إخفاء التفاصيل" else "عرض التفاصيل", tint = AmanTealDark, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (task.isOpen) {
                                Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    if (canCompleteTasks) {
                                        IconButton(onClick = { selectedTaskToComplete = task }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Check, contentDescription = "إكمال المهمة", tint = AmanTealDark) }
                                    }
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(task.phoneNumber)) }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.ContentCopy, contentDescription = "نسخ الرقم", tint = AmanTealDark) }
                                    if (canRescheduleTasks) {
                                        IconButton(onClick = { selectedTaskToReschedule = task }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.DateRange, contentDescription = "جدولة المهمة", tint = AmanTealDark) }
                                    }
                                    IconButton(onClick = { selectedTaskToCancel = task }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Close, contentDescription = "إلغاء المهمة", tint = Color(0xFFDC2626)) }
                                }
                            }
                            if (expandedTaskId == task.id) {
                                HorizontalDivider(color = BorderSubtle)
                                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("الاستحقاق: ${task.dueDate}", fontSize = 11.sp)
                                    Text("نوع المهمة: ${if (task.taskType == "initial_activation") "تفعيل أولي" else "دورية"}", fontSize = 11.sp)
                                    task.daysRemaining?.let { Text("المتبقي قبل الاستحقاق الخارجي: $it يومًا", fontSize = 11.sp, color = AmanTealDark) }
                                    Text("المبلغ: ${task.amountSnapshot} ريال", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    if (task.status != TaskStatus.COMPLETED && task.status != TaskStatus.CANCELLED) {
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            if (canCompleteTasks) {
                                                IconButton(onClick = { selectedTaskToComplete = task }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.Check, contentDescription = "إكمال المهمة", tint = AmanTealDark) }
                                            }
                                            if (canRescheduleTasks) {
                                                IconButton(onClick = { selectedTaskToReschedule = task }, modifier = Modifier.weight(1f)) { Icon(Icons.Default.DateRange, contentDescription = "جدولة المهمة", tint = AmanTealDark) }
                                            }
                                        }
                                    } else if (task.status == TaskStatus.CANCELLED) {
                                        Text("سبب الإلغاء: ${task.cancellationReason ?: task.notes ?: "تم إلغاء المهمة"}", fontSize = 11.sp, color = Color(0xFFDC2626))
                                    } else {
                                        Text("تم السداد: ${task.completedAt} — ${task.paymentReference ?: "بدون مرجع"}", fontSize = 11.sp, color = StatusActiveText)
                                    }
                                }
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    // Reschedule Task Dialog — changes this task only and is enforced by RPC.
    selectedTaskToReschedule?.let { task ->
        var newDate by remember(task.id) { mutableStateOf(task.dueDate) }
        var reason by remember(task.id) { mutableStateOf("") }
        val context = LocalContext.current
        val calendar = remember(task.id) { Calendar.getInstance() }
        AlertDialog(
            onDismissRequest = { selectedTaskToReschedule = null },
            title = { Text("إعادة جدولة المهمة", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرقم: ${task.phoneNumber}")
                    Text("التاريخ الحالي: ${task.dueDate}", fontSize = 12.sp, color = TextSecondary)
                    OutlinedButton(
                        onClick = {
                            DatePickerDialog(context, { _, year, month, day ->
                                newDate = String.format(Locale.ENGLISH, "%04d-%02d-%02d", year, month + 1, day)
                            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("اختيار التاريخ: $newDate") }
                    OutlinedTextField(value=reason,onValueChange={reason=it},label={Text("سبب إعادة الجدولة (اختياري)")},modifier=Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick={ viewModel.reschedulePaymentTask(task.id, "${newDate.trim()}T12:00:00Z", reason); selectedTaskToReschedule=null }) { Text("تأكيد") }
            },
            dismissButton = { TextButton(onClick={selectedTaskToReschedule=null}) { Text("إلغاء") } }
        )
    }

    // Complete Task Dialog
    selectedTaskToComplete?.let { task ->
        var taskNotes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { selectedTaskToComplete = null },
            title = { Text("تسجيل سداد دورة تشغيلية للرقم", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرقم: ${task.phoneNumber} (${task.providerNameAr})")
                    Text("المبلغ المطلوب للشبكة: ${task.amountSnapshot} ريال يمني", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("سيتم تسجيل مبلغ المهمة المعروف بالنظام تلقائياً.", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = taskNotes,
                        onValueChange = { taskNotes = it },
                        label = { Text("مرجع السداد لدى شركة الاتصالات (إلزامي)") },
                        placeholder = { Text("أدخل رقم العملية أو إشعار التحويل", color = TextSecondary) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.completePaymentTask(task.id, taskNotes.trim())
                        selectedTaskToComplete = null
                    },
                    enabled = taskNotes.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark)
                ) {
                    Text("تأكيد السداد وقيد المصروف", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTaskToComplete = null }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }

    // Cancel Task Dialog
    selectedTaskToCancel?.let { task ->
        var cancelReason by remember(task.id) { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { selectedTaskToCancel = null },
            title = { Text("إلغاء مهمة السداد", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرقم: ${task.phoneNumber} (${task.providerNameAr})")
                    Text("المبلغ: ${task.amountSnapshot} ريال", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("سبب الإلغاء (إلزامي)") },
                        placeholder = { Text("اكتب سبب إلغاء المهمة...") },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelPaymentTask(task.id, cancelReason.trim())
                        selectedTaskToCancel = null
                    },
                    enabled = cancelReason.trim().isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("تأكيد الإلغاء", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedTaskToCancel = null }) {
                    Text("تراجع")
                }
            }
        )
    }
}

// ========================================================
// 08. Admin Audit Log Screen
// ========================================================
@Composable
fun AdminAuditLogScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val logs by viewModel.auditLogs.collectAsState()
    var query by remember { mutableStateOf("") }
    var selectedLog by remember { mutableStateOf<com.example.data.model.AuditLog?>(null) }
    val visibleLogs = logs.filter { log ->
        val q = query.trim().lowercase()
        q.isBlank() || log.action.lowercase().contains(q) || log.entityType.lowercase().contains(q) || log.details.lowercase().contains(q)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("سجل العمليات والتدقيق غير القابل للتعديل (${visibleLogs.size})", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("بحث في نوع العملية أو المنفذ أو العنصر") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(visibleLogs, key = { it.id }) { log ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                        Text(log.action, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmanTealDark)
                            Text("الدور: ${log.performedByRole}", fontSize = 10.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.details, fontSize = 11.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(log.createdAt, fontSize = 9.sp, color = TextSecondary)
                        TextButton(onClick = { selectedLog = log }) { Text("عرض التفاصيل قبل/بعد", fontSize = 10.sp, color = AmanTealDark) }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(20.dp)) }
        }
    }
    selectedLog?.let { log ->
        AlertDialog(onDismissRequest = { selectedLog = null }, title = { Text("تفاصيل العملية") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("النوع: ${log.action}", fontWeight = FontWeight.Bold)
                Text("الكيان: ${log.entityType} — ${log.entityId ?: "غير مرتبط"}", fontSize = 11.sp)
                Text("المنفذ: ${log.performedByName.ifBlank { log.performedByRole }}", fontSize = 11.sp)
                Text("الوقت: ${log.createdAt}", fontSize = 11.sp, color = TextSecondary)
                Text("قبل العملية", fontWeight = FontWeight.Bold)
                Text(log.beforeData ?: "لا تتوفر بيانات قبل العملية", fontSize = 10.sp, color = TextSecondary)
                Text("بعد العملية", fontWeight = FontWeight.Bold)
                Text(log.afterData ?: log.details.ifBlank { "لا تتوفر بيانات بعد العملية" }, fontSize = 10.sp, color = TextSecondary)
            }
        }, confirmButton = { TextButton(onClick = { selectedLog = null }) { Text("إغلاق") } })
    }
}

// ========================================================
// Admin settings, telecom and wallet screens
// ========================================================
@Composable
fun AdminPackagesScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier,
    packagesOnly: Boolean = false
) {
    val settings by viewModel.systemSettings.collectAsState()
    val packages by viewModel.packages.collectAsState()
    val providers by viewModel.telecomProviders.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    var editingPackage by remember { mutableStateOf<ProtectionPackage?>(null) }
    var showPackage by remember { mutableStateOf(false) }
    var packageSearch by remember { mutableStateOf("") }
    var packageProviderFilter by remember { mutableStateOf("كل الشركات") }
    var showInactivePackages by remember { mutableStateOf(true) }
    val filteredPackages = packages.filter { pkg ->
        val providerName = providers.firstOrNull { it.id == pkg.providerId }?.nameAr.orEmpty()
        val queryMatch = packageSearch.isBlank() || pkg.name.contains(packageSearch, true) || providerName.contains(packageSearch, true)
        val providerMatch = packageProviderFilter == "كل الشركات" || providerName == packageProviderFilter
        queryMatch && providerMatch && (showInactivePackages || pkg.isActive)
    }

    Column(modifier.fillMaxSize().background(AmanBgLight).padding(16.dp).verticalScroll(rememberScrollState())) {
        if (!packagesOnly) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                Text("الإعدادات العامة",fontSize=18.sp,fontWeight=FontWeight.Bold)
                Button(onClick={showEdit=true},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("تعديل",color=Color.White)}
            }
            Spacer(Modifier.height(12.dp))
            Card(colors=CardDefaults.cardColors(containerColor=SurfaceWhite),modifier=Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    Text("العملة: ${settings.currency}")
                    Text("المنطقة الزمنية: ${settings.timezone}")
                    Text("تنسيق التاريخ: ${settings.dateFormat}")
                    Text("تنسيق الوقت: ${settings.timeFormat}")
                    Text("وضع الصيانة: ${if(settings.maintenanceMode) "مفعل" else "معطل"}")
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        if (packagesOnly) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
            Text(if (packagesOnly) "إدارة الباقات" else "الباقات",fontSize=16.sp,fontWeight=FontWeight.Bold)
            OutlinedButton(onClick={editingPackage=null;showPackage=true}){Text("إضافة باقة")}
        }
        Spacer(Modifier.height(8.dp))
        SearchAndDropdownFilterBar(
            query = packageSearch,
            onQueryChange = { packageSearch = it },
            selectedFilter = packageProviderFilter,
            filterOptions = listOf("كل الشركات") + providers.map { it.nameAr }.distinct(),
            onFilterSelected = { packageProviderFilter = it },
            placeholderText = "بحث في اسم الباقة أو الشركة"
        )
        Row(verticalAlignment=Alignment.CenterVertically){
            Switch(checked=showInactivePackages,onCheckedChange={showInactivePackages=it})
            Text("عرض الباقات المعطلة",fontSize=12.sp)
        }
        filteredPackages.forEach { pkg ->
            Card(colors=CardDefaults.cardColors(containerColor=SurfaceWhite),modifier=Modifier.fillMaxWidth().padding(vertical=4.dp)){
                Row(Modifier.fillMaxWidth().padding(12.dp),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){
                    Column(Modifier.weight(1f)){Text(pkg.name,fontWeight=FontWeight.Bold);Text("${pkg.durationDays} يوم — ${pkg.price} ${pkg.currency}",fontSize=11.sp,color=TextSecondary)}
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(if(pkg.isActive) "نشطة" else "معطلة",fontSize=11.sp)
                        Spacer(Modifier.width(6.dp)); OutlinedButton(onClick={viewModel.upsertPackage(pkg.id,pkg.providerId,pkg.name,pkg.durationDays,pkg.price,pkg.currency,!pkg.isActive,pkg.sortOrder)}){Text(if(pkg.isActive) "إيقاف" else "تفعيل")}
                        Spacer(Modifier.width(4.dp)); OutlinedButton(onClick={editingPackage=pkg}){Text("تعديل")}
                    }
                }
            }
        }
        }
    }

    if(showEdit){
        var currency by remember(settings.currency){mutableStateOf(settings.currency)}
        var timezone by remember(settings.timezone){mutableStateOf(settings.timezone)}
        var dateFormat by remember(settings.dateFormat){mutableStateOf(settings.dateFormat)}
        var timeFormat by remember(settings.timeFormat){mutableStateOf(settings.timeFormat)}
        var maintenance by remember(settings.maintenanceMode){mutableStateOf(settings.maintenanceMode)}
        AlertDialog(onDismissRequest={showEdit=false},title={Text("الإعدادات العامة")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(currency,{currency=it},label={Text("العملة")},singleLine=true)
            OutlinedTextField(timezone,{timezone=it},label={Text("المنطقة الزمنية")},singleLine=true)
            OutlinedTextField(dateFormat,{dateFormat=it},label={Text("تنسيق التاريخ")},singleLine=true)
            OutlinedTextField(timeFormat,{timeFormat=it},label={Text("تنسيق الوقت")},singleLine=true)
            Row(verticalAlignment=Alignment.CenterVertically){Switch(maintenance,{maintenance=it});Text("وضع الصيانة")}
        }},confirmButton={Button(onClick={viewModel.updateSettings(SystemSettings(currency.trim(),timezone.trim(),dateFormat.trim(),timeFormat.trim(),maintenance));showEdit=false},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("حفظ",color=Color.White)}},dismissButton={TextButton(onClick={showEdit=false}){Text("إلغاء")}})
    }

    if(packagesOnly && (showPackage || editingPackage!=null)){
        val ep=editingPackage
        var name by remember(ep?.id){mutableStateOf(ep?.name.orEmpty())}
        var duration by remember(ep?.id){mutableStateOf(ep?.durationDays?.toString().orEmpty())}
        var price by remember(ep?.id){mutableStateOf(ep?.price?.toString().orEmpty())}
        var providerId by remember(ep?.id){mutableStateOf(ep?.providerId ?: providers.firstOrNull()?.id.orEmpty())}
        var providerMenuExpanded by remember(ep?.id){mutableStateOf(false)}
        AlertDialog(onDismissRequest={showPackage=false;editingPackage=null},title={Text(if(ep==null) "إضافة باقة" else "تعديل الباقة")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(name,{name=it},label={Text("اسم الباقة")},singleLine=true)
            OutlinedTextField(duration,{duration=it.filter(Char::isDigit)},label={Text("المدة بالأيام")},singleLine=true)
            OutlinedTextField(price,{price=it},label={Text("السعر")},singleLine=true)
            Box {
                OutlinedButton(onClick={providerMenuExpanded=true},modifier=Modifier.fillMaxWidth()) {
                    Text("الشركة: ${providers.firstOrNull{it.id==providerId}?.nameAr ?: "اختر الشركة"}")
                }
                androidx.compose.material3.DropdownMenu(expanded=providerMenuExpanded,onDismissRequest={providerMenuExpanded=false}) {
                    providers.filter { it.isActive }.forEach { provider ->
                        androidx.compose.material3.DropdownMenuItem(text={Text(provider.nameAr)},onClick={providerId=provider.id;providerMenuExpanded=false})
                    }
                }
            }
        }},confirmButton={Button(onClick={
            val parsedDuration = duration.toIntOrNull()
            val parsedPrice = price.toDoubleOrNull()
            if (name.isBlank() || providerId.isBlank()) viewModel.showMessage("اسم الباقة والشركة مطلوبان", true)
            else if (parsedDuration == null || parsedDuration <= 0) viewModel.showMessage("مدة الحماية يجب أن تكون رقمًا موجبًا", true)
            else if (parsedPrice == null || parsedPrice < 0) viewModel.showMessage("السعر يجب أن يكون رقمًا صالحًا", true)
            else { viewModel.upsertPackage(ep?.id,providerId,name,parsedDuration,parsedPrice,settings.currency,ep?.isActive ?: true,ep?.sortOrder?:packages.size);showPackage=false;editingPackage=null }
        },colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("حفظ",color=Color.White)}},dismissButton={TextButton(onClick={showPackage=false;editingPackage=null}){Text("إلغاء")}})
    }
}

@Composable
fun AdminSettingsScreen(viewModel: AmanViewModel, modifier: Modifier = Modifier) {
    val settings by viewModel.systemSettings.collectAsState()
    var showEdit by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().background(AmanBgLight).padding(16.dp).verticalScroll(rememberScrollState())) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
            Text("الإعدادات العامة",fontSize=18.sp,fontWeight=FontWeight.Bold)
            Button(onClick={showEdit=true},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("تعديل",color=Color.White)}
        }
        Spacer(Modifier.height(12.dp))
        Card(colors=CardDefaults.cardColors(containerColor=SurfaceWhite),modifier=Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("العملة: ${settings.currency}")
                Text("المنطقة الزمنية: ${settings.timezone}")
                Text("تنسيق التاريخ: ${settings.dateFormat}")
                Text("تنسيق الوقت: ${settings.timeFormat}")
                Text("وضع الصيانة: ${if(settings.maintenanceMode) "مفعل" else "معطل"}")
            }
        }
    }
    if(showEdit){
        var currency by remember(settings.currency){mutableStateOf(settings.currency)}
        var timezone by remember(settings.timezone){mutableStateOf(settings.timezone)}
        var dateFormat by remember(settings.dateFormat){mutableStateOf(settings.dateFormat)}
        var timeFormat by remember(settings.timeFormat){mutableStateOf(settings.timeFormat)}
        var maintenance by remember(settings.maintenanceMode){mutableStateOf(settings.maintenanceMode)}
        AlertDialog(onDismissRequest={showEdit=false},title={Text("الإعدادات العامة")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(currency,{currency=it},label={Text("العملة")},singleLine=true)
            OutlinedTextField(timezone,{timezone=it},label={Text("المنطقة الزمنية")},singleLine=true)
            OutlinedTextField(dateFormat,{dateFormat=it},label={Text("تنسيق التاريخ")},singleLine=true)
            OutlinedTextField(timeFormat,{timeFormat=it},label={Text("تنسيق الوقت")},singleLine=true)
            Row(verticalAlignment=Alignment.CenterVertically){Switch(maintenance,{maintenance=it});Text("وضع الصيانة")}
        }},confirmButton={Button(onClick={viewModel.updateSettings(SystemSettings(currency.trim(),timezone.trim(),dateFormat.trim(),timeFormat.trim(),maintenance));showEdit=false},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("حفظ",color=Color.White)}},dismissButton={TextButton(onClick={showEdit=false}){Text("إلغاء")}})
    }
}

// ========================================================
// Admin Telecom Providers Screen
// ========================================================
@Composable
fun AdminTelecomScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val providers by viewModel.telecomProviders.collectAsState()
    var editingProvider by remember { mutableStateOf<TelecomProvider?>(null) }
    var addingProvider by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("شركات الاتصالات المعتمدة (${providers.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold);Button(onClick={addingProvider=true},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("إضافة شركة",color=Color.White)}}
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(providers, key = { it.id }) { prov ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(prov.nameAr, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            ProviderBadge(providerName = prov.nameAr)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("البادئات المعتمدة: ${prov.prefixes.joinToString(", ")}", fontSize = 12.sp, color = TextSecondary)
                        Text("طول الرقم: ${prov.numberLength} أرقام", fontSize = 11.sp, color = TextSecondary)
                        Text("${if (prov.isActive) "مفعلة" else "موقوفة"}", fontSize = 11.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick = { editingProvider = prov }, modifier = Modifier.weight(1f)) { Text("تعديل") }
                            OutlinedButton(onClick = { viewModel.setTelecomProviderActive(prov.id, !prov.isActive) }, modifier = Modifier.weight(1f)) { Text(if (prov.isActive) "إيقاف" else "تفعيل") }
                        }
                    }
                }
            }
        }
    }
    if(addingProvider){
        var nameAr by remember{mutableStateOf("")}; var nameEn by remember{mutableStateOf("")}; var prefixes by remember{mutableStateOf("")}; var numberLength by remember{mutableStateOf("9")}; var order by remember{mutableStateOf(providers.size.toString())}
        AlertDialog(onDismissRequest={addingProvider=false},title={Text("إضافة شركة اتصالات")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){OutlinedTextField(nameAr,{nameAr=it},label={Text("اسم الشركة")},singleLine=true);OutlinedTextField(nameEn,{nameEn=it},label={Text("الاسم الإنجليزي")},singleLine=true);OutlinedTextField(prefixes,{prefixes=it},label={Text("البادئات مفصولة بفاصلة")},singleLine=true);OutlinedTextField(numberLength,{numberLength=it.filter(Char::isDigit)},label={Text("طول الرقم")},singleLine=true);OutlinedTextField(order,{order=it.filter(Char::isDigit)},label={Text("الترتيب")},singleLine=true)}},confirmButton={Button(onClick={if(nameAr.isNotBlank()&&nameEn.isNotBlank()&&prefixes.isNotBlank()){viewModel.addTelecomProvider(nameAr,nameEn,prefixes.split(',').map{it.trim()}.filter{it.isNotBlank()},order.toIntOrNull()?:providers.size,numberLength.toIntOrNull()?:9);addingProvider=false}}){Text("حفظ")}},dismissButton={TextButton(onClick={addingProvider=false}){Text("إلغاء")}})
    }
    editingProvider?.let { provider ->
        TelecomProviderEditDialog(provider,viewModel){editingProvider=null}
    }

}

@Composable
private fun TelecomProviderEditDialog(provider:TelecomProvider,viewModel:AmanViewModel,onClose:()->Unit){
    var nameAr by remember(provider) { mutableStateOf(provider.nameAr) }
    var nameEn by remember(provider) { mutableStateOf(provider.nameEn) }
    var prefixes by remember(provider) { mutableStateOf(provider.prefixes.joinToString(",")) }
    var numberLength by remember(provider) { mutableStateOf(provider.numberLength.toString()) }
    var order by remember(provider) { mutableStateOf(provider.sortOrder.toString()) }
    AlertDialog(onDismissRequest=onClose,title={Text("تعديل شركة الاتصالات")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
        OutlinedTextField(nameAr,{nameAr=it},label={Text("اسم الشركة")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(nameEn,{nameEn=it},label={Text("الاسم الإنجليزي")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(prefixes,{prefixes=it},label={Text("البادئات مفصولة بفاصلة")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(numberLength,{numberLength=it.filter(Char::isDigit)},label={Text("طول الرقم")},singleLine=true,modifier=Modifier.fillMaxWidth())
        OutlinedTextField(order,{order=it.filter(Char::isDigit)},label={Text("الترتيب")},singleLine=true,modifier=Modifier.fillMaxWidth())
    }},confirmButton={Button(onClick={viewModel.updateTelecomProvider(provider.id,nameAr,nameEn,prefixes.split(',').map{it.trim()}.filter{it.isNotBlank()},order.toIntOrNull()?:provider.sortOrder,numberLength.toIntOrNull()?:provider.numberLength);onClose()},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("حفظ",color=Color.White)}},dismissButton={TextButton(onClick=onClose){Text("إلغاء")}})
}

// ========================================================
// Admin Payment Wallets Screen
// ========================================================
@Composable
fun AdminWalletsScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val wallets by viewModel.paymentWallets.collectAsState()
    var editingWallet by remember { mutableStateOf<PaymentWallet?>(null) }
    var addingWallet by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){Text("وسائل الدفع المعتمدة (${wallets.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold);Button(onClick={addingWallet=true},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("إضافة وسيلة",color=Color.White)}}
        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(wallets, key = { it.id }) { wal ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(wal.nameAr, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = AmanDarkSlate)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("رقم الحساب / المستفيد: ${wal.beneficiaryAccount} (${wal.beneficiaryName})", fontSize = 12.sp, color = AmanTealDark, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(wal.instructionsAr, fontSize = 11.sp, color = TextSecondary)
                        Text("${if (wal.isActive) "مفعلة" else "موقوفة"}", fontSize = 11.sp, color = TextSecondary)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick={editingWallet=wal}, modifier=Modifier.weight(1f)){Text("تعديل")}
                            OutlinedButton(onClick = { viewModel.setPaymentWalletActive(wal.id, !wal.isActive) }, modifier = Modifier.weight(1f)) { Text(if (wal.isActive) "إيقاف" else "تفعيل") }
                        }
                    }
                }
            }
        }
    }
    if(addingWallet){
        var type by remember{mutableStateOf("wallet")}; var name by remember{mutableStateOf("")}; var recipient by remember{mutableStateOf("")}; var account by remember{mutableStateOf("")}; var instructions by remember{mutableStateOf("")}
        AlertDialog(onDismissRequest={addingWallet=false},title={Text("إضافة وسيلة دفع")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("النوع: ${if(type=="bank")"بنك" else if(type=="transfer")"تحويل مالي" else "محفظة"}",fontSize=12.sp)
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){OutlinedButton(onClick={type="bank"}){Text("بنك")};OutlinedButton(onClick={type="wallet"}){Text("محفظة")};OutlinedButton(onClick={type="transfer"}){Text("تحويل مالي")}}
            OutlinedTextField(name,{name=it},label={Text("اسم وسيلة الدفع")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(recipient,{recipient=it},label={Text("اسم الجهة المستلمة")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(account,{account=it},label={Text("رقم الاستلام / الحساب")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(instructions,{instructions=it},label={Text("التعليمات")},modifier=Modifier.fillMaxWidth())
        }},confirmButton={Button(enabled=name.isNotBlank()&&recipient.isNotBlank()&&account.isNotBlank(),onClick={viewModel.upsertPaymentMethod(null,type,name,recipient,account,instructions,true,wallets.size);addingWallet=false},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("حفظ",color=Color.White)}},dismissButton={TextButton(onClick={addingWallet=false}){Text("إلغاء")}})
    }
    editingWallet?.let { wallet ->
        var nameAr by remember(wallet) { mutableStateOf(wallet.nameAr) }
        var nameEn by remember(wallet) { mutableStateOf(wallet.nameEn) }
        var account by remember(wallet) { mutableStateOf(wallet.beneficiaryAccount) }
        var beneficiary by remember(wallet) { mutableStateOf(wallet.beneficiaryName) }
        var instructions by remember(wallet) { mutableStateOf(wallet.instructionsAr) }
        AlertDialog(onDismissRequest={editingWallet=null}, title={Text("تعديل وسيلة الدفع")}, text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(nameAr,{nameAr=it},label={Text("الاسم العربي")},singleLine=true)
                OutlinedTextField(nameEn,{nameEn=it},label={Text("الاسم الإنجليزي")},singleLine=true)
                OutlinedTextField(account,{account=it},label={Text("حساب المستفيد")},singleLine=true)
                OutlinedTextField(beneficiary,{beneficiary=it},label={Text("اسم المستفيد")},singleLine=true)
                OutlinedTextField(instructions,{instructions=it},label={Text("تعليمات التحويل")})
            }
        }, confirmButton={Button(onClick={viewModel.updatePaymentWallet(wallet.id,nameAr,nameEn,account,beneficiary,instructions,0);editingWallet=null}){Text("حفظ")}}, dismissButton={TextButton(onClick={editingWallet=null}){Text("إلغاء")}})
    }
}

// ========================================================
// Admin Protections Screen
// ========================================================
@Composable
fun AdminProtectionsScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val protections by viewModel.protections.collectAsState()
    val customers by viewModel.customers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    val filterOptions = listOf("الكل", "نشط", "منتهي")

    val customerNameMap = remember(customers) { customers.associate { it.id to it.name } }
    var selectedProtection by remember { mutableStateOf<Protection?>(null) }

    val filtered = protections.filter { p ->
        val cName = customerNameMap[p.customerId] ?: ""
        val matchesQuery = p.phoneNumber.contains(searchQuery.trim()) ||
                cName.contains(searchQuery.trim()) ||
                p.providerNameAr.contains(searchQuery.trim())
        val matchesFilter = when (selectedFilter) {
            "نشط" -> p.status == "active"
            "منتهي" -> p.status != "active"
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("سجل الحمايات الشامل (${filtered.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        SearchAndDropdownFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedFilter = selectedFilter,
            filterOptions = filterOptions,
            onFilterSelected = { selectedFilter = it },
            placeholderText = "بحث برقم الهاتف أو اسم العميل..."
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyStateView(
                title = "لا توجد حمايات مطابقة",
                subtitle = "قم بتعديل خيارات البحث أو التصفية"
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { prot ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(prot.phoneNumber, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                if (prot.status == "active") ActiveStatusBadge() else StoppedStatusBadge(label = "منتهية")
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("العميل: ${customerNameMap[prot.customerId] ?: "عميل أمان"}", fontSize = 12.sp, color = TextSecondary)
                                ProviderBadge(providerName = prot.providerNameAr)
                            }
                            HorizontalDivider(color = BorderSubtle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("تاريخ البدء: ${prot.startDate}", fontSize = 11.sp, color = TextSecondary)
                                Text("تاريخ الانتهاء: ${prot.endDate}", fontSize = 11.sp, color = TextSecondary)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الأيام المتبقية للحماية:", fontSize = 11.sp, color = TextSecondary)
                                Text("${prot.daysRemaining} يوم", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmanTealDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// Admin Customers Screen
// ========================================================
@Composable
fun AdminCustomersScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val customers by viewModel.customers.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    val filtered = customers.filter { c ->
        c.name.contains(searchQuery.trim()) ||
                c.phone.contains(searchQuery.trim()) ||
                c.email.contains(searchQuery.trim())
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("إدارة العملاء المشتركين (${filtered.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("بحث باسم العميل أو رقمه أو بريده...", color = TextSecondary) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyStateView(
                title = "لا يوجد عملاء مطابقون",
                subtitle = "لم يتم العثور على نتائج للبحث الحالي"
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.id }) { cust ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(cust.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (cust.isSuspended) {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEE2E2)) {
                                        Text("معلق", color = Color(0xFFDC2626), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                } else {
                                    Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFDCFCE7)) {
                                        Text("نشط", color = Color(0xFF16A34A), fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Text("${cust.phone} • ${cust.email}", fontSize = 12.sp, color = TextSecondary)
                            HorizontalDivider(color = BorderSubtle)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الأرقام المسجلة: ${cust.registeredNumbersCount}", fontSize = 11.sp, color = TextPrimary)
                                Text("الحمايات النشطة: ${cust.activeProtectionsCount}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AmanTealDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// Admin Phones Screen
// ========================================================
@Composable
fun AdminPhonesScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val phones by viewModel.customerNumbers.collectAsState()
    val telecomProviders by viewModel.telecomProviders.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    var expandedPhoneId by remember { mutableStateOf<String?>(null) }
    val filterOptions = listOf("الكل", "مفعل", "غير مفعل")

    val filtered = phones.filter { p ->
        val matchesQuery = p.phoneNumber.contains(searchQuery.trim()) || p.providerNameAr.contains(searchQuery.trim())
        val matchesFilter = when (selectedFilter) {
            "مفعل" -> p.isProtected
            "غير مفعل" -> !p.isProtected
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("أرقام الهواتف المسجلة في المنظومة (${filtered.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(10.dp))

        SearchAndDropdownFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedFilter = selectedFilter,
            filterOptions = filterOptions,
            onFilterSelected = { selectedFilter = it },
            placeholderText = "بحث برقم الهاتف أو الشبكة..."
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyStateView(
                title = "لا توجد أرقام مسجلة",
                subtitle = "لم يتم العثور على أرقام تطابق شروط البحث"
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Row(Modifier.fillMaxWidth().background(AmanTealLight.copy(alpha = 0.35f)).padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("الرقم", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("الشركة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("الحالة", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("إجراء", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                // Use a position-qualified key as a defensive measure: old
                // database rows may have a missing/duplicated phone id, and
                // duplicate LazyColumn keys crash the whole admin screen.
                itemsIndexed(filtered, key = { index, phone -> "${phone.id}_${phone.phoneNumber}_$index" }) { _, phone ->
                    val providerName = telecomProviders.firstOrNull { it.id == phone.providerId }?.nameAr ?: phone.providerNameAr
                    Surface(color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.fillMaxWidth()) {
                            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(phone.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.2f))
                                Text(providerName, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), maxLines = 1)
                                Text(if (phone.isProtected) "مفعل" else "غير مفعل", fontSize = 11.sp, color = if (phone.isProtected) StatusActiveText else TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                                TextButton(onClick = { expandedPhoneId = if (expandedPhoneId == phone.id) null else phone.id }, modifier = Modifier.weight(0.8f)) { Text("تفاصيل", fontSize = 10.sp, color = AmanTealDark) }
                            }
                            if (expandedPhoneId == phone.id) {
                                HorizontalDivider(color = BorderSubtle)
                                Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("الرقم: ${phone.phoneNumber}", fontSize = 11.sp)
                                    Text("الشركة: $providerName", fontSize = 11.sp)
                                    Text(if (phone.isProtected) "الحماية: مفعلة" else "الحماية: غير مفعلة", fontSize = 11.sp, color = if (phone.isProtected) StatusActiveText else TextSecondary, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ========================================================
// Admin Notifications Screen
// ========================================================
@Composable
fun AdminNotificationsScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    var unreadOnly by remember { mutableStateOf(false) }
    var typeFilter by remember { mutableStateOf<String?>(null) }
    val types = notifications.map { it.type }.filter { it.isNotBlank() }.distinct()
    val visibleNotifications = notifications.filter { (!unreadOnly || !it.isRead) && (typeFilter == null || it.type == typeFilter) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
        Text("الإشعارات (${visibleNotifications.size})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            if (notifications.any { !it.isRead }) {
                TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                    Text("تحديد الكل كمقروء", color = AmanTealDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = { unreadOnly = false }) { Text(if (!unreadOnly) "الكل ✓" else "الكل") }
            OutlinedButton(onClick = { unreadOnly = true }) { Text(if (unreadOnly) "غير المقروءة ✓" else "غير المقروءة") }
            types.take(2).forEach { type -> OutlinedButton(onClick = { typeFilter = if (typeFilter == type) null else type }) { Text(if (typeFilter == type) "$type ✓" else type) } }
        }
        Spacer(modifier = Modifier.height(8.dp))

        if (visibleNotifications.isEmpty()) {
            EmptyStateView(
                title = if (notifications.isEmpty()) "لا توجد إشعارات" else "لا توجد نتائج مطابقة",
                subtitle = "ستظهر هنا إشعارات النظام الموجهة للإدارة"
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    Surface(color = AmanTealLight.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("الإشعار", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.25f))
                            Text("الحالة", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            Text("التاريخ", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        }
                    }
                }
                items(visibleNotifications, key = { it.id }) { notif ->
                    Surface(color = if (notif.isRead) SurfaceWhite else Color(0xFFF0FDF4), modifier = Modifier.fillMaxWidth().clickable { viewModel.markNotificationRead(notif.id); notif.actionRoute?.let { viewModel.navigateTo(it) } }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(notif.titleAr, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1, modifier = Modifier.weight(1.25f))
                            Text(if (notif.isRead) "مقروء" else "جديد", fontSize = 11.sp, color = if (notif.isRead) TextSecondary else AmanTealDark, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            Text(notif.createdAt, fontSize = 10.sp, color = TextSecondary, maxLines = 1, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}
