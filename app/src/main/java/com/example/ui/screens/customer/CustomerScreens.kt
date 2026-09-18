package com.example.ui.screens.customer

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerNumber
import com.example.data.model.PaymentWallet
import com.example.data.model.ProtectionPackage
import com.example.data.model.Protection
import com.example.data.model.ProtectionRequest
import com.example.data.model.RequestStatus
import com.example.data.model.TelecomProvider
import com.example.data.repository.AmanRepository
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
import com.example.ui.theme.BorderField
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveDot
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AmanViewModel


@Composable
fun CustomerHomeScreen(
    viewModel: AmanViewModel,
    onNavigate: (String) -> Unit,
    onOpenAddPhone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentUser by viewModel.currentUser.collectAsState()
    val numbers by viewModel.customerNumbers.collectAsState()
    val requests by viewModel.protectionRequests.collectAsState()
    val protections by viewModel.protections.collectAsState()
    val activeProtections = protections.count { it.status == "active" && it.daysRemaining > 0 }
    val notifications by viewModel.notifications.collectAsState()

    val unreadNotifs = notifications.count { !it.isRead }
    val pendingRequests = requests.count { it.status == RequestStatus.PENDING }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        // Welcome greeting banner with customer's real name
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
                        text = "أهلًا بك في أمان، ${currentUser?.fullName ?: "عميلنا"}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmanDarkSlate
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "خدمة تساعدك على الحفاظ على أرقامك",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Spacer(modifier = Modifier.height(14.dp))

        // 1. Exactly Four Stat Cards (2x2 Grid)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "الأرقام",
                value = "${numbers.size}",
                icon = Icons.Default.Phone,
                iconBgColor = Color(0xFFE0F2FE),
                iconTint = Color(0xFF0284C7),
                onClick = { onNavigate("phones") },
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "الطلبات",
                value = "${requests.size}",
                icon = Icons.Default.AssignmentTurnedIn,
                iconBgColor = Color(0xFFFFFBEB),
                iconTint = Color(0xFFD97706),
                onClick = { onNavigate("requests") },
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
                value = "${activeProtections}",
                icon = Icons.Default.Security,
                iconBgColor = Color(0xFFECFDF5),
                iconTint = Color(0xFF059669),
                onClick = { onNavigate("protections") },
                modifier = Modifier.weight(1f)
            )
            StatCard(
                title = "الإشعارات",
                value = "$unreadNotifs",
                icon = Icons.Default.Notifications,
                iconBgColor = Color(0xFFFEF2F2),
                iconTint = Color(0xFFDC2626),
                onClick = { onNavigate("notifications") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (pendingRequests > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFFFFBEB),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "لديك ${pendingRequests} طلب${if (pendingRequests == 1) "" else "ات"} قيد المراجعة.",
                    fontSize = 12.sp,
                    color = Color(0xFF92400E),
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(18.dp))
    }
}

// ========================================================
// 03. Customer Phones Screen (الأرقام)
// ========================================================
@Composable
fun CustomerPhonesScreen(
    viewModel: AmanViewModel,
    onProtectNumber: (CustomerNumber) -> Unit,
    onOpenAddPhone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val numbers by viewModel.customerNumbers.collectAsState()
    val protections by viewModel.protections.collectAsState()
    val providers by viewModel.telecomProviders.collectAsState()
    val requests by viewModel.protectionRequests.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var providerFilter by remember { mutableStateOf("كل الشركات") }
    var protectionFilter by remember { mutableStateOf("كل الحالات") }
    var selectedNumberForDetails by remember { mutableStateOf<CustomerNumber?>(null) }

    val providerOptions = listOf("كل الشركات") + providers.map { it.nameAr }
    val protectionOptions = listOf("كل الحالات", "مفعل", "غير مفعل")

    val filtered = numbers.filter { item ->
        val matchesQuery = item.phoneNumber.contains(searchQuery) || item.providerNameAr.contains(searchQuery)
        val matchesProvider = providerFilter == "كل الشركات" || item.providerNameAr == providerFilter
        val matchesProtection = when (protectionFilter) {
            "مفعل" -> item.isProtected
            "غير مفعل" -> !item.isProtected
            else -> true
        }
        matchesQuery && matchesProvider && matchesProtection
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "الأرقام (${numbers.size})",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        SearchAndTwoFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            firstFilter = providerFilter,
            firstOptions = providerOptions,
            onFirstFilterChange = { providerFilter = it },
            secondFilter = protectionFilter,
            secondOptions = protectionOptions,
            onSecondFilterChange = { protectionFilter = it },
            placeholderText = "بحث برقم الهاتف أو الشركة..."
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Phone,
                title = "لا توجد أرقام مسجلة"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Surface(color = AmanTealLight.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("الرقم", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.25f))
                            Text("الشركة", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("الحالة", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            Text("تفاصيل", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.75f))
                        }
                    }
                }
                items(filtered, key = { it.id }) { number ->
                    val pending = requests.firstOrNull { it.numberId == number.id && it.status == RequestStatus.PENDING }
                    Surface(color = SurfaceWhite, modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(number.phoneNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.25f))
                            Text(number.providerNameAr, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(if (number.isProtected) "مفعل" else "غير مفعل", fontSize = 11.sp, color = if (number.isProtected) StatusActiveText else TextSecondary, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            Row(modifier = Modifier.weight(0.95f), horizontalArrangement = Arrangement.End) {
                                if (!number.isProtected && pending == null) {
                                    TextButton(onClick = { onProtectNumber(number) }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp)) {
                                        Text("تفعيل", fontSize = 10.sp, color = AmanTealDark, fontWeight = FontWeight.Bold)
                                    }
                                }
                                TextButton(onClick = { selectedNumberForDetails = number }, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 3.dp)) {
                                    Text("تفاصيل", fontSize = 10.sp, color = AmanTealDark)
                                }
                            }
                        }
                        if (pending != null && !number.isProtected) {
                            Text("طلب حماية قيد المراجعة", fontSize = 10.sp, color = Color(0xFF92400E), modifier = Modifier.padding(start = 10.dp, bottom = 5.dp))
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    selectedNumberForDetails?.let { num ->
        val activeProt = protections.firstOrNull { it.numberId == num.id }
        val pendingReq = requests.firstOrNull { it.phoneNumber == num.phoneNumber && it.status == RequestStatus.PENDING }
        NumberDetailsDialog(
            number = num,
            protection = activeProt,
            pendingRequest = pendingReq,
            onDismiss = { selectedNumberForDetails = null },
            onActivateProtection = {
                selectedNumberForDetails = null
                onProtectNumber(num)
            }
        )
    }
}

@Composable
fun CustomerNumberCard(
    number: CustomerNumber,
    pendingRequest: ProtectionRequest? = null,
    onClick: () -> Unit,
    onProtectClick: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(AmanTealLight.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = AmanTealDark, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = number.phoneNumber,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        ProviderBadge(nameAr = number.providerNameAr)
                    }
                }

                if (number.isProtected) {
                    ActiveStatusBadge()
                } else {
                    StoppedStatusBadge("غير محمي")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderSubtle)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "أضيف في ${number.createdAt}",
                    fontSize = 11.sp,
                    color = TextSecondary
                )

                if (!number.isProtected && pendingRequest == null) {
                    Button(
                        onClick = onProtectClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تفعيل الحماية", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else if (pendingRequest != null && !number.isProtected) {
                    Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFFBEB)) {
                        Text("طلب قيد المراجعة", color = Color(0xFF92400E), fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusActiveBg
                    ) {
                        Text(
                            text = "✓ محمي",
                            color = StatusActiveText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

// ========================================================
// Number Details Dialog (تفاصيل الرقم)
// ========================================================
@Composable
fun NumberDetailsDialog(
    number: CustomerNumber,
    protection: com.example.data.model.Protection?,
    pendingRequest: com.example.data.model.ProtectionRequest?,
    onDismiss: () -> Unit,
    onActivateProtection: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("تفاصيل الرقم", fontWeight = FontWeight.Bold, fontSize = 17.sp, color = AmanDarkSlate)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Phone & Provider Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmanBgLight,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(number.phoneNumber, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(3.dp))
                            ProviderBadge(nameAr = number.providerNameAr)
                        }
                        if (number.isProtected) {
                            ActiveStatusBadge()
                        } else {
                            StoppedStatusBadge("غير محمي")
                        }
                    }
                }

                // Protection Status Breakdown
                if (number.isProtected && protection != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusActiveBg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmanTealLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("حالة الحماية:", fontSize = 12.sp, color = StatusActiveText)
                                Text("محمي ✓", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusActiveText)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("الأيام المتبقية:", fontSize = 12.sp, color = StatusActiveText)
                                Text("${protection.daysRemaining} يوماً من ${protection.durationDaysSnapshot}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = StatusActiveText)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("تاريخ البدء:", fontSize = 11.sp, color = TextSecondary)
                                Text(protection.startDate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("تاريخ الانتهاء:", fontSize = 11.sp, color = TextSecondary)
                                Text(protection.endDate, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                "حمايتك فعالة وفق المدة المسجلة في طلب التفعيل.",
                                fontSize = 10.sp,
                                color = AmanTealDark
                            )
                        }
                    }
                } else if (pendingRequest != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFFFBEB),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("طلب الحماية القائم:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                RequestStatusPill(status = pendingRequest.status)
                            }
                            Text("رقم المرجع: ${pendingRequest.paymentReference}", fontSize = 11.sp, color = Color(0xFF78350F))
                            Text("تاريخ الطلب: ${pendingRequest.createdAt}", fontSize = 11.sp, color = Color(0xFF78350F))
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFFEF2F2),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("الرقم غير محمي", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB91C1C))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!number.isProtected && pendingRequest == null) {
                Button(
                    onClick = onActivateProtection,
                    colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("تفعيل الحماية", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق", color = TextSecondary)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceWhite
    )
}

// ========================================================
// Add Customer Phone Dialog
// ========================================================
@Composable
fun AddPhoneDialog(
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
    providers: List<TelecomProvider> = emptyList(),
    isLoading: Boolean = false
) {
    var phoneNumber by remember { mutableStateOf("") }
    val detectedProvider: TelecomProvider? = remember(phoneNumber) {
        AmanRepository.detectProvider(phoneNumber)
    }
    val maximumLength = providers.maxOfOrNull { it.numberLength } ?: 20

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceWhite,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("إضافة رقم", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = TextPrimary)
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary) }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = {
                        if (it.length <= maximumLength && it.all { char -> char.isDigit() }) {
                            phoneNumber = it
                        }
                    },
                    label = { Text("رقم الهاتف") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AmanTealPrimary,
                        unfocusedBorderColor = BorderField
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (detectedProvider != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(StatusActiveBg, RoundedCornerShape(12.dp))
                            .padding(8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = StatusActiveDot, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = detectedProvider.nameAr,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusActiveText
                        )
                    }
                } else if (phoneNumber.length >= 2) {
                    Text("صيغة الرقم غير مدعومة", fontSize = 11.sp, color = Color(0xFFEF4444))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(phoneNumber) },
                enabled = !isLoading && phoneNumber.length == (detectedProvider?.numberLength ?: 9),
                colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark)
            ) {
                Text(if (isLoading) "جارٍ الإضافة..." else "إضافة الرقم", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AddedNumberSuccessDialog(
    number: CustomerNumber,
    onDismiss: () -> Unit,
    onAddAnother: () -> Unit,
    onActivateProtection: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceWhite,
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(12.dp), color = StatusActiveBg) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = StatusActiveText, modifier = Modifier.padding(8.dp).size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("تمت إضافة الرقم", fontWeight = FontWeight.Bold, color = AmanDarkSlate, fontSize = 18.sp)
                    Text("الرقم غير مفعل للحماية حاليًا", fontSize = 11.sp, color = TextSecondary)
                }
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "إغلاق", tint = TextSecondary) }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(shape = RoundedCornerShape(12.dp), color = AmanBgLight, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text("بيانات الرقم", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("الرقم", color = TextSecondary, fontSize = 12.sp); Text(number.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("الشركة", color = TextSecondary, fontSize = 12.sp); Text(number.providerNameAr, fontWeight = FontWeight.Medium, fontSize = 12.sp) }
                    }
                }
                Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFFF7ED), modifier = Modifier.fillMaxWidth()) {
                }
            }
        },
        confirmButton = { Button(onClick = onActivateProtection, shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark)) { Text("تفعيل الحماية", color = Color.White) } },
        dismissButton = { OutlinedButton(onClick = onAddAnother, shape = RoundedCornerShape(12.dp)) { Text("إضافة رقم جديد", color = AmanTealDark) } }
    )
}

// ========================================================
// 04. Submit Protection Request Dialog (تفعيل الحماية)
// ========================================================
@Composable
fun ProtectionRequestDialog(
    number: CustomerNumber,
    wallets: List<com.example.data.model.PaymentWallet>,
    packages: List<com.example.data.model.ProtectionPackage>,
    settings: com.example.data.model.SystemSettings,
    isLoading: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (packageId: String, walletId: String, reference: String) -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var selectedWallet by remember { mutableStateOf(wallets.firstOrNull()) }
    val providerPackages = remember(number, packages) { packages.filter { it.providerId == number.providerId && it.isActive }.sortedBy { it.sortOrder } }
    var selectedPackage by remember(providerPackages) { mutableStateOf(providerPackages.firstOrNull()) }
    var referenceNumber by remember { mutableStateOf("") }
    var packageMenuExpanded by remember { mutableStateOf(false) }
    var walletMenuExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (step == 1) "تفعيل الحماية" else "مراجعة الطلب",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = AmanDarkSlate
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (step == 1) {
                    // Number and price summary
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmanBgLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("الرقم المراد حمايته:", fontSize = 12.sp, color = TextSecondary)
                                Text(number.phoneNumber, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("شركة الاتصالات:", fontSize = 12.sp, color = TextSecondary)
                                Text(number.providerNameAr, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AmanTealDark)
                            }
                            HorizontalDivider(color = BorderSubtle, modifier = Modifier.padding(vertical = 4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("قيمة الحماية:", fontSize = 12.sp, color = TextSecondary)
                                Text(
                                    "${selectedPackage?.price ?: 0.0} ${selectedPackage?.currency ?: settings.currency}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AmanTealDark
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("مدة الحماية:", fontSize = 12.sp, color = TextSecondary)
                                Text("${selectedPackage?.durationDays ?: 0} يوماً", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }

                    Text("اختر باقة الحماية:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    if (providerPackages.isEmpty()) {
                        Text("لا توجد باقات حماية نشطة لهذه الشركة.", fontSize = 12.sp, color = Color(0xFFB91C1C))
                    } else {
                        Box(modifier = Modifier.fillMaxWidth().clickable { packageMenuExpanded = true }) {
                            OutlinedTextField(
                                value = selectedPackage?.let { "${it.name} — ${it.durationDays} يوم" } ?: "اختر الباقة",
                                onValueChange = {},
                                readOnly = true,
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                trailingIcon = { Text("⌄", fontSize = 18.sp, color = TextSecondary) }
                            )
                            DropdownMenu(expanded = packageMenuExpanded, onDismissRequest = { packageMenuExpanded = false }) {
                                providerPackages.forEach { pkg ->
                                    DropdownMenuItem(
                                        text = { Text("${pkg.name} — ${pkg.price} ${pkg.currency} / ${pkg.durationDays} يوم") },
                                        onClick = { selectedPackage = pkg; packageMenuExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    // Wallets choices
                    Box(modifier = Modifier.fillMaxWidth().clickable { walletMenuExpanded = true }) {
                        OutlinedTextField(
                            value = selectedWallet?.nameAr ?: "اختر وسيلة الدفع",
                            onValueChange = {},
                            readOnly = true,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { Text("⌄", fontSize = 18.sp, color = TextSecondary) }
                        )
                        DropdownMenu(expanded = walletMenuExpanded, onDismissRequest = { walletMenuExpanded = false }) {
                            wallets.forEach { wallet ->
                                DropdownMenuItem(
                                    text = { Text(wallet.nameAr) },
                                    onClick = { selectedWallet = wallet; walletMenuExpanded = false }
                                )
                            }
                        }
                    }

                    // Selected wallet instructions
                    selectedWallet?.let { wallet ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFFFBEB),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFDE68A)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text("بيانات التحويل:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                Text("رقم الحساب: ${wallet.beneficiaryAccount} (${wallet.beneficiaryName})", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(wallet.instructionsAr, fontSize = 11.sp, color = Color(0xFF78350F))
                            }
                        }
                    }

                    Text("رقم العملية / المرجع:", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        placeholder = { Text("أدخل رقم السند أو المرجع المالي...", color = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Step 2: Review Screen
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AmanBgLight,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("ملخص طلب الحماية", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
                            HorizontalDivider(color = BorderSubtle)

                            ReviewItemRow("الرقم المراد حمايته", number.phoneNumber)
                            ReviewItemRow("شركة الاتصالات", number.providerNameAr)
                            ReviewItemRow("قيمة الحماية", "${selectedPackage?.price ?: 0.0} ${selectedPackage?.currency ?: settings.currency}")
                            ReviewItemRow("الباقة", selectedPackage?.name ?: "")
                            ReviewItemRow("مدة الحماية", "${selectedPackage?.durationDays ?: 0} يوماً")
                            ReviewItemRow("وسيلة الدفع", selectedWallet?.nameAr ?: "")
                            ReviewItemRow("الحساب المحول إليه", selectedWallet?.beneficiaryAccount ?: "")
                            ReviewItemRow("رقم العملية / المرجع", referenceNumber)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = StatusActiveBg,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = StatusActiveText, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (step == 1) {
                Button(
                    onClick = { step = 2 },
                    enabled = referenceNumber.trim().length >= 4 && selectedWallet != null && selectedPackage != null,
                    colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("متابعة للمراجعة", color = Color.White, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        selectedWallet?.let { wallet -> selectedPackage?.let { pkg -> onSubmit(pkg.id, wallet.id, referenceNumber) } }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (isLoading) "جارٍ الإرسال..." else "إرسال طلب الحماية", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            if (step == 2) {
                TextButton(onClick = { step = 1 }) {
                    Text("رجوع للتعديل", color = TextSecondary)
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = SurfaceWhite
    )
}

@Composable
private fun ReviewItemRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 11.sp, color = TextSecondary)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}

// ========================================================
// 05. Customer Requests Screen (الطلبات)
// ========================================================
@Composable
fun CustomerRequestsScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val requests by viewModel.protectionRequests.collectAsState()
    var selectedFilter by remember { mutableStateOf("الكل") }
    var searchQuery by remember { mutableStateOf("") }
    var resubmitRequest by remember { mutableStateOf<ProtectionRequest?>(null) }
    var selectedRequest by remember { mutableStateOf<ProtectionRequest?>(null) }

    val filterOptions = listOf("الكل", "قيد المراجعة", "مقبول", "مرفوض")

    val filtered = requests.filter { r ->
        val matchesQuery = r.phoneNumber.contains(searchQuery) || r.paymentReference.contains(searchQuery)
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
        Text(
            text = "طلبات الحماية (${requests.size})",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        SearchAndDropdownFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedFilter = selectedFilter,
            filterOptions = filterOptions,
            onFilterSelected = { selectedFilter = it },
            placeholderText = "بحث برقم الهاتف أو المرجع"
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filtered.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.AssignmentTurnedIn,
                title = "لا توجد طلبات حالياً"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Surface(color = AmanTealLight.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("الرقم", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            Text("الباقة", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("الحالة", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            Text("تفاصيل", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.75f))
                        }
                    }
                }
                items(filtered, key = { it.id }) { req ->
                    Surface(color = SurfaceWhite, modifier = Modifier.fillMaxWidth().clickable { selectedRequest = req }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(req.phoneNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            Text("${req.durationDaysSnapshot} يوم", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f))
                            Text(req.status.titleAr, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            TextButton(onClick = { selectedRequest = req }, modifier = Modifier.weight(0.75f)) { Text("تفاصيل", fontSize = 10.sp, color = AmanTealDark) }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
    selectedRequest?.let { req ->
        AlertDialog(onDismissRequest = { selectedRequest = null }, title = { Text("تفاصيل طلب الحماية") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("الرقم: ${req.phoneNumber}", fontWeight = FontWeight.Bold)
                Text("الشركة: ${req.providerNameAr}")
                Text("الباقة: ${req.priceSnapshot} ${req.currencySnapshot} — ${req.durationDaysSnapshot} يوم")
                Text("طريقة الدفع: ${req.walletNameAr}")
                Text("مرجع الدفع: ${req.paymentReference}")
                Text("تاريخ الإرسال: ${req.createdAt}")
                Text("الحالة: ${req.status.titleAr}", fontWeight = FontWeight.Bold)
                if (req.status == RequestStatus.REJECTED) Text("سبب الرفض: ${req.rejectionReason ?: "غير متوفر"}", color = Color(0xFFB91C1C))
            }
        }, confirmButton = { TextButton(onClick = { selectedRequest = null }) { Text("إغلاق") } })
    }
    resubmitRequest?.let { req ->
        var reference by remember(req.id){mutableStateOf(req.paymentReference)}
        val pkg= viewModel.packages.collectAsState().value.firstOrNull { it.providerId==req.providerId && it.isActive }
        val method= viewModel.paymentWallets.collectAsState().value.firstOrNull { it.isActive }
        AlertDialog(onDismissRequest={resubmitRequest=null},title={Text("إعادة تقديم الطلب")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text("الباقة: ${pkg?.name ?: "لا توجد باقة نشطة"}",fontSize=12.sp)
            Text("وسيلة الدفع: ${method?.nameAr ?: "لا توجد وسيلة دفع نشطة"}",fontSize=12.sp)
            OutlinedTextField(reference,{reference=it},label={Text("رقم المرجع")},singleLine=true,modifier=Modifier.fillMaxWidth())
        }},confirmButton={Button(enabled=pkg!=null&&method!=null&&reference.trim().length>=4,onClick={ val selectedPackage=pkg; val selectedMethod=method; if (selectedPackage != null && selectedMethod != null) viewModel.resubmitRejectedRequest(req.id,selectedPackage.id,selectedMethod.id,reference){resubmitRequest=null} },colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("إعادة الإرسال",color=Color.White)}},dismissButton={TextButton(onClick={resubmitRequest=null}){Text("إلغاء")}})
    }

}

// ========================================================
// 06. Customer Protections Screen (الحمايات)
// ========================================================
@Composable
fun CustomerProtectionsScreen(
    viewModel: AmanViewModel,
    paymentMethods: List<PaymentWallet> = emptyList(),
    packages: List<ProtectionPackage> = emptyList(),
    modifier: Modifier = Modifier
) {
    var renewalProtection by remember { mutableStateOf<Protection?>(null) }
    var selectedProtection by remember { mutableStateOf<Protection?>(null) }
    val protections by viewModel.protections.collectAsState()
    val providers by viewModel.telecomProviders.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("الكل") }
    val filterOptions = listOf("الكل") + providers.map { it.nameAr }

    val filteredProtections = protections.filter { prot ->
        val matchesQuery = prot.phoneNumber.contains(searchQuery.trim()) ||
                prot.providerNameAr.contains(searchQuery.trim())
        val matchesFilter = when (selectedFilter) {
            "الكل" -> true
            else -> prot.providerNameAr == selectedFilter
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
        Text(
            text = "الحمايات (${protections.size})",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(12.dp))

        SearchAndDropdownFilterBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            selectedFilter = selectedFilter,
            filterOptions = filterOptions,
            onFilterSelected = { selectedFilter = it },
            placeholderText = "بحث برقم الهاتف أو المزود..."
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredProtections.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Shield,
                title = "لا توجد حمايات نشطة"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Surface(color = AmanTealLight.copy(alpha = 0.35f), modifier = Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("الرقم", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            Text("الشركة", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                            Text("الحالة", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            Text("تفاصيل", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.75f))
                        }
                    }
                }
                items(filteredProtections, key = { it.id }) { prot ->
                    Surface(color = SurfaceWhite, modifier = Modifier.fillMaxWidth().clickable { selectedProtection = prot }) {
                        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(prot.phoneNumber, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.2f))
                            Text(prot.providerNameAr, fontSize = 11.sp, color = TextSecondary, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(if (prot.status == "active" && prot.daysRemaining > 0) "مفعلة" else "منتهية", fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.9f))
                            TextButton(onClick = { selectedProtection = prot }, modifier = Modifier.weight(0.75f)) { Text("تفاصيل", fontSize = 10.sp, color = AmanTealDark) }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }

    selectedProtection?.let { prot ->
        AlertDialog(onDismissRequest = { selectedProtection = null }, title = { Text("تفاصيل الحماية") }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text("الرقم: ${prot.phoneNumber}", fontWeight = FontWeight.Bold)
                Text("الشركة: ${prot.providerNameAr}")
                Text("الباقة: ${prot.durationDaysSnapshot} يوم — ${prot.priceSnapshot}")
                Text("البداية: ${prot.startDate}")
                Text("النهاية: ${prot.endDate}")
                Text(if (prot.status == "active" && prot.daysRemaining > 0) "الحالة: سارية — متبقي ${prot.daysRemaining} يوم" else "الحالة: منتهية", fontWeight = FontWeight.Bold)
            }
        }, confirmButton = { TextButton(onClick = { selectedProtection = null }) { Text("إغلاق") } })
    }
    renewalProtection?.let { prot ->
        var selectedPackage by remember(prot.id) { mutableStateOf(packages.firstOrNull { it.providerId == providers.firstOrNull { p -> p.nameAr == prot.providerNameAr }?.id }) }
        var selectedMethod by remember(prot.id) { mutableStateOf(paymentMethods.firstOrNull()) }
        var reference by remember(prot.id) { mutableStateOf("") }
        var packageMenuExpanded by remember(prot.id) { mutableStateOf(false) }
        var methodMenuExpanded by remember(prot.id) { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { renewalProtection = null },
            title = { Text("تجديد الحماية", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("الرقم: ${prot.phoneNumber}")
                    Box {
                        OutlinedButton(onClick = { packageMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedPackage?.let { "${it.name} — ${it.price} ${it.currency} / ${it.durationDays} يوم" } ?: "اختر الباقة") }
                        DropdownMenu(expanded = packageMenuExpanded, onDismissRequest = { packageMenuExpanded = false }) {
                            packages.filter { it.providerId == providers.firstOrNull { p -> p.nameAr == prot.providerNameAr }?.id && it.isActive }.forEach { pkg ->
                                DropdownMenuItem(text = { Text("${pkg.name} — ${pkg.price} ${pkg.currency} / ${pkg.durationDays} يوم") }, onClick = { selectedPackage = pkg; packageMenuExpanded = false })
                            }
                        }
                    }
                    Text("وسيلة الدفع:", fontSize = 12.sp, color = TextSecondary)
                    Box {
                        OutlinedButton(onClick = { methodMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) { Text(selectedMethod?.nameAr ?: "اختر وسيلة الدفع") }
                        DropdownMenu(expanded = methodMenuExpanded, onDismissRequest = { methodMenuExpanded = false }) {
                            paymentMethods.filter { it.isActive }.forEach { method ->
                                DropdownMenuItem(text = { Text(method.nameAr) }, onClick = { selectedMethod = method; methodMenuExpanded = false })
                            }
                        }
                    }
                    OutlinedTextField(value = reference, onValueChange = { reference = it }, label = { Text("رقم المرجع") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pkg = selectedPackage; val method = selectedMethod
                        if (pkg != null && method != null && reference.trim().isNotBlank()) {
                            viewModel.submitRenewalRequest(prot.id, pkg.id, method.id, reference) { renewalProtection = null }
                        }
                    },
                    enabled = selectedPackage != null && selectedMethod != null && reference.trim().isNotBlank()
                ) { Text("إرسال طلب التجديد", color = Color.White) }
            },
            dismissButton = { TextButton(onClick = { renewalProtection = null }) { Text("إلغاء") } }
        )
    }
}

// ========================================================
// 07. Customer Notifications Screen (الإشعارات)
// ========================================================
@Composable
fun CustomerNotificationsScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val notifications by viewModel.notifications.collectAsState()
    var unreadOnly by remember { mutableStateOf(false) }
    val visibleNotifications = notifications.filter { !unreadOnly || !it.isRead }

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
            Text("الإشعارات", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            if (notifications.any { !it.isRead }) {
                TextButton(onClick = { viewModel.markAllNotificationsRead() }) {
                    Text("تحديد الكل كمقروء", color = AmanTealDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(onClick = { unreadOnly = !unreadOnly }, modifier = Modifier.fillMaxWidth()) { Text(if (unreadOnly) "عرض كل الإشعارات" else "عرض غير المقروءة فقط") }
        Spacer(modifier = Modifier.height(8.dp))

        if (visibleNotifications.isEmpty()) {
            EmptyStateView(
                icon = Icons.Default.Notifications,
                title = "لا توجد إشعارات"
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
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
                item { Spacer(modifier = Modifier.height(20.dp)) }
            }
        }
    }
}

// ========================================================
// 08 & 09. Customer Account & Security Screens
// ========================================================
@Composable
fun CustomerAccountScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    val user by viewModel.currentUser.collectAsState()
    var editing by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("الحساب", fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))

        // Profile Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(AmanTealLight.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Person, contentDescription = null, tint = AmanTealDark, modifier = Modifier.size(30.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(user?.fullName ?: "عميل أمان", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text(user?.email ?: "", fontSize = 12.sp, color = TextSecondary)
                        Text(user?.phone ?: "غير محدد", fontSize = 12.sp, color = AmanTealDark, fontWeight = FontWeight.Medium)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick={editing=true},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark),modifier=Modifier.fillMaxWidth()){Text("تعديل الحساب",color=Color.White)}
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
    if(editing){
        var name by remember(user?.fullName){mutableStateOf(user?.fullName.orEmpty())}
        var phone by remember(user?.phone){mutableStateOf(user?.phone.orEmpty())}
        AlertDialog(onDismissRequest={editing=false},title={Text("تعديل الحساب")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(name,{name=it},label={Text("الاسم الكامل")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(phone,{phone=it.filter(Char::isDigit)},label={Text("رقم الهاتف (اختياري)")},singleLine=true,keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Phone),modifier=Modifier.fillMaxWidth())
        }},confirmButton={Button(onClick={viewModel.updateMyProfile(name,phone){editing=false}},colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark)){Text("حفظ",color=Color.White)}},dismissButton={TextButton(onClick={editing=false}){Text("إلغاء")}})
    }
}
