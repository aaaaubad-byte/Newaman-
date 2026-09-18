package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RequestStatus
import com.example.data.model.User
import com.example.data.model.UserType
import com.example.ui.theme.AmanDarkSlate
import com.example.ui.theme.AmanTealDark
import com.example.ui.theme.AmanTealLight
import com.example.ui.theme.AmanTealPrimary
import com.example.ui.theme.AmanTextSmall
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.InputBg
import com.example.ui.theme.ProviderSabaFon
import com.example.ui.theme.ProviderSabaFonBg
import com.example.ui.theme.ProviderY
import com.example.ui.theme.ProviderYBg
import com.example.ui.theme.ProviderYOU
import com.example.ui.theme.ProviderYOUBg
import com.example.ui.theme.ProviderYemenMobile
import com.example.ui.theme.ProviderYemenMobileBg
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveBorder
import com.example.ui.theme.StatusActiveDot
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.StatusPendingBg
import com.example.ui.theme.StatusPendingBorder
import com.example.ui.theme.StatusPendingDot
import com.example.ui.theme.StatusPendingText
import com.example.ui.theme.StatusRejectedBg
import com.example.ui.theme.StatusRejectedBorder
import com.example.ui.theme.StatusRejectedDot
import com.example.ui.theme.StatusRejectedText
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

// ========================================================
// "الفلترة يجب ان تكون قوائم منسدلة بجوار خانة البحث"
// ========================================================
@Composable
fun SearchAndDropdownFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    filterOptions: List<String>,
    onFilterSelected: (String) -> Unit,
    placeholderText: String = "بحث عن رقم، عميل...",
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Search TextField (Flexible width)
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = {
                Text(
                    text = placeholderText,
                    fontSize = 13.sp,
                    color = TextMuted
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = AmanTealDark,
                    modifier = Modifier.size(20.dp)
                )
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { onQueryChange("") },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "مسح البحث",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = SurfaceWhite,
                unfocusedContainerColor = InputBg,
                focusedBorderColor = AmanTealDark,
                unfocusedBorderColor = BorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
        )

        // Dropdown Menu Filter Button (Directly adjacent to search field)
        Box {
            Surface(
                onClick = { dropdownExpanded = true },
                shape = RoundedCornerShape(12.dp),
                color = if (selectedFilter != "الكل") AmanTealLight else SurfaceWhite,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (selectedFilter != "الكل") AmanTealPrimary else BorderSubtle
                ),
                shadowElevation = 0.dp,
                modifier = Modifier.height(46.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "تصفية",
                        tint = if (selectedFilter != "الكل") AmanTealDark else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = selectedFilter,
                        fontSize = AmanTextSmall,
                        fontWeight = if (selectedFilter != "الكل") FontWeight.Bold else FontWeight.Medium,
                        color = if (selectedFilter != "الكل") AmanTealDark else TextPrimary
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = if (selectedFilter != "الكل") AmanTealDark else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // The Dropdown Menu
            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
                shape = RoundedCornerShape(12.dp),
                containerColor = SurfaceWhite,
                tonalElevation = 6.dp,
                modifier = Modifier
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .padding(vertical = 4.dp)
            ) {
                filterOptions.forEach { option ->
                    val isSelected = option == selectedFilter
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = option,
                                    fontSize = AmanTextSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) AmanTealDark else TextPrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = AmanTealDark,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        onClick = {
                            onFilterSelected(option)
                            dropdownExpanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SearchAndTwoFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    firstFilter: String,
    firstOptions: List<String>,
    onFirstFilterChange: (String) -> Unit,
    secondFilter: String,
    secondOptions: List<String>,
    onSecondFilterChange: (String) -> Unit,
    placeholderText: String = "بحث...",
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            placeholder = { Text(placeholderText, fontSize = AmanTextSmall, color = TextMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "بحث", tint = AmanTealDark) },
            trailingIcon = { if (query.isNotEmpty()) IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Default.Clear, contentDescription = "مسح البحث", tint = TextMuted) } },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = SurfaceWhite, unfocusedContainerColor = InputBg, focusedBorderColor = AmanTealDark, unfocusedBorderColor = BorderSubtle),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CompactDropdownFilter(firstFilter, firstOptions, onFirstFilterChange, Modifier.weight(1f))
            CompactDropdownFilter(secondFilter, secondOptions, onSecondFilterChange, Modifier.weight(1f))
        }
    }
}

@Composable
private fun CompactDropdownFilter(selected: String, options: List<String>, onSelected: (String) -> Unit, modifier: Modifier = Modifier) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(onClick = { expanded = true }, shape = RoundedCornerShape(12.dp), color = if (selected != options.firstOrNull()) AmanTealLight else SurfaceWhite, border = androidx.compose.foundation.BorderStroke(1.dp, if (selected != options.firstOrNull()) AmanTealPrimary else BorderSubtle), modifier = Modifier.fillMaxWidth().height(44.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(selected, fontSize = AmanTextSmall, color = if (selected != options.firstOrNull()) AmanTealDark else TextPrimary, maxLines = 1)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(option, fontSize = AmanTextSmall) }, onClick = { onSelected(option); expanded = false }) }
        }
    }
}

// Backward compatibility alias for SearchAndFilterBar
@Composable
fun SearchAndFilterBar(
    query: String,
    onQueryChange: (String) -> Unit,
    selectedFilter: String,
    filterOptions: List<String>,
    onFilterSelected: (String) -> Unit,
    placeholderText: String = "بحث...",
    modifier: Modifier = Modifier
) {
    SearchAndDropdownFilterBar(
        query = query,
        onQueryChange = onQueryChange,
        selectedFilter = selectedFilter,
        filterOptions = filterOptions,
        onFilterSelected = onFilterSelected,
        placeholderText = placeholderText,
        modifier = modifier
    )
}

// ========================================================
// 2. High-Fidelity Modern StatCard (Ref: 20260907_085730.jpg)
// ========================================================
@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Icon container with rounded square pastel tint
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            // Value and Title
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AmanDarkSlate
                )
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1
                )
            }
        }
    }
}

// ========================================================
// 3. Action Required Card ("يحتاج إجراء")
// Ref: 20260907_085730.jpg
// ========================================================
@Composable
fun ActionRequiredCard(
    pendingCount: Int,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AmanTealLight),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = AmanTealDark,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "يحتاج إجراء",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmanDarkSlate
                    )
                }

                // Count Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (pendingCount > 0) StatusPendingBg else StatusActiveBg,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (pendingCount > 0) StatusPendingBorder else StatusActiveBorder
                    )
                ) {
                    Text(
                        text = if (pendingCount > 0) "$pendingCount معلق" else "لا توجد مهام معلقة",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (pendingCount > 0) StatusPendingText else StatusActiveText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider(color = BorderSubtle, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (pendingCount > 0)
                        "يوجد عمليات وبلاغات تتطلب اتخاذ قرار سريع."
                    else
                        "كل الأرقام والاشتراكات في حالة آمنة ومستقرة.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ========================================================
// 4. Trust Badges Footer (Ref: 20260907_085730.jpg)
// ========================================================
@Composable
fun TrustBadgesFooter(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // High Security Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(AmanTealLight),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AmanTealDark,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "حماية وخصوصية",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmanDarkSlate
                    )
                    Text(
                        text = "نعتني ببياناتك وخصوصية حسابك",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Annual Protection Cycle Badge
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceWhite,
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Repeat,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = "حماية واضحة",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmanDarkSlate
                    )
                    Text(
                        text = "تابع حالة رقمك واشتراكك بسهولة",
                        fontSize = 10.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

// ========================================================
// 5. Status Badges & Pills
// ========================================================
@Composable
fun ActiveStatusBadge(
    modifier: Modifier = Modifier,
    label: String = "محمي ونشط"
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = StatusActiveBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusActiveBorder),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(StatusActiveDot)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StatusActiveText
            )
        }
    }
}

@Composable
fun StoppedStatusBadge(
    label: String = "غير محمي",
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = StatusRejectedBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusRejectedBorder),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(StatusRejectedDot)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StatusRejectedText
            )
        }
    }
}

@Composable
fun PendingStatusBadge(
    label: String = "قيد التفعيل",
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = StatusPendingBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, StatusPendingBorder),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(StatusPendingDot)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StatusPendingText
            )
        }
    }
}

@Composable
fun RequestStatusPill(
    status: RequestStatus,
    modifier: Modifier = Modifier
) {
    val (dotColor, bgColor, textColor, borderColor, label) = when (status) {
        RequestStatus.PENDING -> Quintuple(
            StatusPendingDot, StatusPendingBg, StatusPendingText, StatusPendingBorder, "قيد المراجعة"
        )
        RequestStatus.APPROVED -> Quintuple(
            StatusActiveDot, StatusActiveBg, StatusActiveText, StatusActiveBorder, "مقبول ومفعل"
        )
        RequestStatus.REJECTED -> Quintuple(
            StatusRejectedDot, StatusRejectedBg, StatusRejectedText, StatusRejectedBorder, "مرفوض"
        )
    }

    Surface(
        shape = RoundedCornerShape(50),
        color = bgColor,
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = textColor
            )
        }
    }
}

// ========================================================
// 6. Telecom Provider Badge (Brand Colors)
// Supports both provider code ("YM", "YOU", "SB", "Y")
// and Arabic name ("يمن موبايل", "يو", "سبأفون", "واي")
// ========================================================
@Composable
fun ProviderBadge(
    providerCodeOrName: String = "",
    nameAr: String = providerCodeOrName,
    providerName: String = if (nameAr.isNotEmpty()) nameAr else providerCodeOrName,
    modifier: Modifier = Modifier
) {
    val targetText = if (providerName.isNotEmpty()) providerName else if (nameAr.isNotEmpty()) nameAr else providerCodeOrName
    val (name, color, bg) = when {
        targetText == "YM" || targetText.contains("يمن موبايل") ->
            Triple("يمن موبايل", ProviderYemenMobile, ProviderYemenMobileBg)
        targetText == "YOU" || targetText.contains("يو") || targetText.contains("YOU") ->
            Triple("يو (YOU)", ProviderYOU, ProviderYOUBg)
        targetText == "SB" || targetText.contains("سبأفون") ->
            Triple("سبأفون", ProviderSabaFon, ProviderSabaFonBg)
        targetText == "Y" || targetText.contains("واي") ->
            Triple("واي (Y)", ProviderY, ProviderYBg)
        else ->
            Triple(targetText, AmanTealDark, AmanTealLight)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f)),
        modifier = modifier
    ) {
        Text(
            text = name,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

// ========================================================
// 7. Modern Aman Top App Bar
// ========================================================
@Composable
fun AmanTopAppBar(
    unreadNotificationsCount: Int,
    onNotificationsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = SurfaceWhite,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "أمان",
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = AmanTealDark
            )
            IconButton(
                onClick = onNotificationsClick,
                modifier = Modifier.align(Alignment.CenterStart).size(42.dp)
            ) {
                BadgedBox(
                    badge = {
                        if (unreadNotificationsCount > 0) {
                            Badge(containerColor = Color(0xFFEF4444), contentColor = Color.White) {
                                Text("$unreadNotificationsCount", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                ) {
                    Icon(Icons.Default.Notifications, contentDescription = "الإشعارات", tint = AmanDarkSlate, modifier = Modifier.size(22.dp))
                }
            }
        }
    }
}

// ========================================================
// 8. Empty State View (Friendly & Visual)
// ========================================================
@Composable
fun EmptyStateView(
    title: String,
    subtitle: String = "",
    icon: ImageVector = Icons.Default.Shield,
    description: String = subtitle,
    actionButtonText: String? = null,
    onActionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val displayDesc = if (description.isNotEmpty()) description else subtitle
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(68.dp)
                .clip(CircleShape)
                .background(AmanTealLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AmanTealDark,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = AmanDarkSlate
        )
        if (displayDesc.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = displayDesc,
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
        if (actionButtonText != null && onActionClick != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onActionClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark)
            ) {
                Text(actionButtonText, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

// Helper tuple for 5 elements
data class Quintuple<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
