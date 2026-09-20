package com.example.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.AmanBgLight
import com.example.ui.theme.AmanTealDark
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AmanViewModel

@Composable
fun AdminTaskSettingsScreen(viewModel: AmanViewModel, modifier: Modifier = Modifier) {
    val providers by viewModel.telecomProviders.collectAsState()
    val settings by viewModel.taskSettings.collectAsState()
    val amountSettings by viewModel.taskAmountSettings.collectAsState()
    val notifications by viewModel.notificationSettings.collectAsState()
    val classifications by viewModel.taskClassifications.collectAsState()
    val byProvider = settings.associateBy { it.providerId }
    var classificationSettingsId by remember { mutableStateOf<String?>(null) }
    var editingClassification by remember { mutableStateOf<TaskClassification?>(null) }
    Column(modifier.fillMaxSize().background(AmanBgLight).padding(16.dp)) {
        Text("إعدادات التشغيل والمهام", fontSize = 18.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(10.dp), modifier=Modifier.weight(1f)) {
            items(providers, key={it.id}) { provider ->
                val current=byProvider[provider.id]
                val providerClassifications = classifications.filter { it.taskSettingsId == current?.id }
                var first by remember(current?.id) { mutableStateOf(current?.firstTaskEnabled ?: false) }
                var reschedule by remember(current?.id) { mutableStateOf(current?.manualRescheduleEnabled ?: true) }
                var interval by remember(current?.id) { mutableStateOf(current?.intervalDays?.toString().orEmpty()) }
                var visibleBefore by remember(current?.id) { mutableStateOf(current?.visibilityDaysBefore?.toString() ?: "30") }
                val initialAmount = amountSettings.firstOrNull { it.providerId == provider.id && it.taskType == "initial_activation" }
                val recurringAmount = amountSettings.firstOrNull { it.providerId == provider.id && it.taskType == "recurring" }
                var initialAmountText by remember(initialAmount?.id) { mutableStateOf(initialAmount?.amount?.toString().orEmpty()) }
                var recurringAmountText by remember(recurringAmount?.id) { mutableStateOf(recurringAmount?.amount?.toString().orEmpty()) }
                Card(colors=CardDefaults.cardColors(containerColor=SurfaceWhite), modifier=Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement=Arrangement.spacedBy(6.dp)) {
                        Text(provider.nameAr, fontSize=14.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                        Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(first,{first=it}); Text("إنشاء المهمة الأولى عند بدء الحماية") }
                        Row(verticalAlignment=Alignment.CenterVertically) { Checkbox(reschedule,{reschedule=it}); Text("السماح بإعادة الجدولة اليدوية") }
                        OutlinedTextField(value=interval,onValueChange={interval=it.filter(Char::isDigit)},label={Text("مدة الخمول بين الدورات بالأيام")},singleLine=true,modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(value=visibleBefore,onValueChange={visibleBefore=it.filter(Char::isDigit)},label={Text("ظهور المهمة قبل الموعد بالأيام (الافتراضي 30)")},singleLine=true,modifier=Modifier.fillMaxWidth())
                        Text("مبالغ التشغيل حسب نوع المهمة", fontSize=12.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                        OutlinedTextField(value=initialAmountText,onValueChange={initialAmountText=it.filter { c -> c.isDigit() || c == '.' }},label={Text("مبلغ المهمة الأولى")},singleLine=true,modifier=Modifier.fillMaxWidth())
                        OutlinedTextField(value=recurringAmountText,onValueChange={recurringAmountText=it.filter { c -> c.isDigit() || c == '.' }},label={Text("مبلغ المهمة الدورية")},singleLine=true,modifier=Modifier.fillMaxWidth())
                        Button(onClick={
                            val parsed = interval.toIntOrNull()
                            if (parsed == null || parsed <= 0) viewModel.showMessage("الفاصل الدوري يجب أن يكون رقمًا موجبًا", true)
                            else {
                                viewModel.updateTaskSettings(provider.id,first,reschedule,parsed,visibleBefore.toIntOrNull() ?: 30,true)
                                initialAmountText.toDoubleOrNull()?.takeIf { it >= 0 }?.let { value -> viewModel.updateTaskAmountSetting(TaskAmountSetting(initialAmount?.id,provider.id,"initial_activation",value,"YER",true)) }
                                recurringAmountText.toDoubleOrNull()?.takeIf { it >= 0 }?.let { value -> viewModel.updateTaskAmountSetting(TaskAmountSetting(recurringAmount?.id,provider.id,"recurring",value,"YER",true)) }
                            }
                        },colors=ButtonDefaults.buttonColors(containerColor=AmanTealDark),modifier=Modifier.fillMaxWidth()) { Text("حفظ إعدادات الشركة",color=Color.White) }
                        HorizontalDivider()
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text("تصنيفات هذه الشركة", fontSize=13.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                            }
                            if (current != null) {
                                IconButton(onClick={ classificationSettingsId = current.id; editingClassification = null }) {
                                    Icon(Icons.Default.Add, contentDescription="إضافة تصنيف", tint=AmanTealDark)
                                }
                            }
                        }
                        if (current == null) {
                            Text("احفظ الفاصل الدوري أولًا لتتمكن من إضافة تصنيفات لهذه الشركة.", fontSize=10.sp, color=TextSecondary)
                        } else if (providerClassifications.isEmpty()) {
                            Text("لا توجد تصنيفات مضافة بعد.", fontSize=11.sp, color=TextSecondary)
                        } else {
                            providerClassifications.sortedBy { it.sortOrder }.forEach { c ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(c.name, fontSize=12.sp, fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                                        Text("من ${c.minDaysRemaining ?: "بلا حد أدنى"} إلى ${c.maxDaysRemaining ?: "بلا حد أعلى"} يوم متبقٍ", fontSize=10.sp, color=TextSecondary)
                                    }
                                    IconButton(onClick={ classificationSettingsId = c.taskSettingsId; editingClassification = c }) {
                                        Icon(Icons.Default.Edit, contentDescription="تعديل التصنيف", tint=AmanTealDark)
                                    }
                                    Switch(checked=c.isActive,onCheckedChange={viewModel.updateTaskClassification(c.id,c.taskSettingsId,c.name,c.minDaysRemaining,c.maxDaysRemaining,c.sortOrder,it)})
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(8.dp))
        Text("إعدادات الإشعارات",fontSize=15.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
        notifications.forEach { n ->
            var enabled by remember(n.id) { mutableStateOf(n.enabled) }
            var days by remember(n.id) { mutableStateOf(n.daysBefore?.toString().orEmpty()) }
            Card(colors=CardDefaults.cardColors(containerColor=SurfaceWhite),modifier=Modifier.fillMaxWidth().padding(vertical=4.dp)) {
                Column(Modifier.padding(10.dp)) {
                    Text(n.type,fontSize=12.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                    Row(verticalAlignment=Alignment.CenterVertically){Switch(enabled,{enabled=it});Text(if(enabled) "مفعل" else "معطل",fontSize=11.sp)}
                    if(n.daysBefore != null){ OutlinedTextField(days,{days=it.filter(Char::isDigit)},label={Text("قبل الاستحقاق بالأيام")},singleLine=true,modifier=Modifier.fillMaxWidth()) }
                    TextButton(onClick={viewModel.updateNotificationSetting(n.copy(enabled=enabled,daysBefore=days.toIntOrNull()))}){Text("حفظ")}
                }
            }
        }
    }
    val settingsId = classificationSettingsId
    if (settingsId != null) {
        TaskClassificationDialog(
            existing = editingClassification,
            taskSettingsId = settingsId,
            onDismiss = { classificationSettingsId = null; editingClassification = null },
            onSave = { id, name, min, max ->
                viewModel.updateTaskClassification(id, settingsId, name, min, max, editingClassification?.sortOrder ?: (classifications.count { it.taskSettingsId == settingsId } + 1), true)
                classificationSettingsId = null
                editingClassification = null
            }
        )
    }
}

@Composable
private fun TaskClassificationDialog(
    existing: TaskClassification?,
    taskSettingsId: String,
    onDismiss: () -> Unit,
    onSave: (String?, String, Int?, Int?) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var minDays by remember(existing?.id) { mutableStateOf(existing?.minDaysRemaining?.toString().orEmpty()) }
    var maxDays by remember(existing?.id) { mutableStateOf(existing?.maxDaysRemaining?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "إضافة تصنيف مهمة" else "تعديل تصنيف المهمة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label={Text("اسم التصنيف")}, singleLine=true)
                OutlinedTextField(minDays, { minDays = it.filter(Char::isDigit) }, label={Text("الحد الأدنى للأيام المتبقية")}, singleLine=true)
                OutlinedTextField(maxDays, { maxDays = it.filter(Char::isDigit) }, label={Text("الحد الأعلى للأيام المتبقية")}, singleLine=true)
            }
        },
        confirmButton = {
            Button(onClick={
                val min = minDays.toIntOrNull()
                val max = maxDays.toIntOrNull()
                if (name.trim().isBlank() || (min != null && max != null && min > max)) return@Button
                onSave(existing?.id, name.trim(), min, max)
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick=onDismiss) { Text("إلغاء") } }
    )
}

@Composable
fun AdminEmployeesScreen(viewModel: AmanViewModel, modifier: Modifier = Modifier) {
    val employees by viewModel.employees.collectAsState()
    val roles by viewModel.roles.collectAsState()
    var selected by remember { mutableStateOf<EmployeeAccount?>(null) }
    Column(modifier.fillMaxSize().background(AmanBgLight).padding(16.dp)) {
        Text("الموظفون والأدوار",fontSize=18.sp,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Spacer(Modifier.height(12.dp))
        LazyColumn(verticalArrangement=Arrangement.spacedBy(8.dp)) {
            items(employees,key={it.id}) { e ->
                Card(colors=CardDefaults.cardColors(containerColor=SurfaceWhite),modifier=Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Default.AdminPanelSettings,null,tint=AmanTealDark)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)){Text(e.fullName,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold);Text(e.email,fontSize=11.sp,color=TextSecondary);Text("${e.roleName ?: "بدون دور"} — ${e.status}",fontSize=10.sp,color=TextSecondary)}
                        OutlinedButton(onClick={selected=e}){Text("إدارة")}
                    }
                }
            }
        }
    }
    selected?.let { e ->
        var roleId by remember(e.id) { mutableStateOf(e.roleId ?: roles.firstOrNull()?.first.orEmpty()) }
        var status by remember(e.id) { mutableStateOf(e.status) }
        AlertDialog(onDismissRequest={selected=null},title={Text("إدارة الموظف")},text={
            Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
                Text(e.fullName,fontWeight=androidx.compose.ui.text.font.FontWeight.Bold)
                Text("الدور الحالي: ${e.roleName ?: "بدون دور"}",fontSize=11.sp,color=TextSecondary)
                roles.forEach { (id,name) -> Row(verticalAlignment=Alignment.CenterVertically){RadioButton(roleId==id,{roleId=id});Text(name)} }
                Text("الحالة",fontSize=12.sp)
                Row{ listOf("active" to "نشط","suspended" to "موقوف","disabled" to "معطل").forEach{(v,l)->TextButton(onClick={status=v}){Text(if(status==v) "✓ $l" else l)}} }
            }
        },confirmButton={Button(onClick={viewModel.setEmployeeRole(e.id,roleId);viewModel.setEmployeeStatus(e.id,status);selected=null}){Text("حفظ")}},dismissButton={TextButton(onClick={selected=null}){Text("إلغاء")}})
    }
}
