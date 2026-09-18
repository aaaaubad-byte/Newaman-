package com.example.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.security.AppLockManager
import com.example.core.security.BiometricAuthenticator
import com.example.ui.theme.AmanBgLight
import com.example.ui.theme.AmanDarkSlate
import com.example.ui.theme.AmanTealDark
import com.example.ui.theme.AmanTealLight
import com.example.ui.theme.AmanTealPrimary
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.StatusActiveBg
import com.example.ui.theme.StatusActiveText
import com.example.ui.theme.SurfaceWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AmanViewModel

// ========================================================
// 1. Settings Screen (الإعدادات)
// ========================================================
@Composable
fun CustomerSettingsScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("إعدادات التطبيق", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
        Spacer(modifier = Modifier.height(12.dp))

        // Language & Localization Card
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("اللغة والعرض", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("لغة الواجهة", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text("العربية (الافتراضية)", fontSize = 11.sp, color = TextSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .background(AmanTealLight.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text("العربية", color = AmanTealDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}


@Composable
fun CustomerMoreScreen(onNavigate:(String)->Unit,modifier:Modifier=Modifier){
    Column(modifier.fillMaxSize().background(AmanBgLight).padding(16.dp).verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(10.dp)){
        Spacer(Modifier.height(8.dp)); Text("المزيد",fontSize=18.sp,fontWeight=FontWeight.Bold,color=AmanDarkSlate)
        listOf("settings" to "الإعدادات","security" to "الأمان","help" to "المساعدة","terms" to "الشروط والأحكام","about" to "عن أمان").forEach{(route,label)->
            Card(colors=CardDefaults.cardColors(containerColor=SurfaceWhite),modifier=Modifier.fillMaxWidth()){Button(onClick={onNavigate(route)},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.buttonColors(containerColor=Color.Transparent),contentPadding=PaddingValues(14.dp)){Text(label,color=TextPrimary)}}
        }
    }
}

// ========================================================
// 2. Security Screen (الأمان)
// ========================================================
@Composable
fun CustomerSecurityScreen(
    viewModel: AmanViewModel,
    modifier: Modifier = Modifier
) {
    var isAppLockEnabled by remember { mutableStateOf(AppLockManager.isLockEnabled()) }
    var isBiometricEnabled by remember { mutableStateOf(AppLockManager.isBiometricEnabled()) }
    var showSetPinDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("الأمان وقفل التطبيق", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
        Spacer(modifier = Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("رمز PIN لقفل التطبيق", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isAppLockEnabled) "مفعّل - يتم طلب الرمز المكون من 4 أرقام عند فتح التطبيق" else "معطّل - يمكنك تفعيله لحماية خصوصية بياناتك وأرقامك",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                    Switch(
                        checked = isAppLockEnabled,
                        onCheckedChange = { enable ->
                            if (enable) {
                                showSetPinDialog = true
                            } else {
                                viewModel.configureAppLock(false)
                                isAppLockEnabled = false
                            }
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = AmanTealDark, checkedTrackColor = AmanTealLight)
                    )
                }

                if (isAppLockEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { showSetPinDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = AmanTealLight.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("تغيير رمز PIN", color = AmanTealDark, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("فتح التطبيق بالبصمة", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("يتطلب PIN مفعّلًا وبصمة مسجلة على الجهاز", fontSize = 11.sp, color = TextSecondary)
                }
                Switch(
                    checked = isBiometricEnabled,
                    onCheckedChange = { enabled ->
                        val allowed = enabled && BiometricAuthenticator.canAuthenticate(context)
                        if (enabled && !allowed) {
                            viewModel.showMessage("البصمة غير متاحة على هذا الجهاز", true)
                        } else if (AppLockManager.setBiometricEnabled(enabled)) {
                            isBiometricEnabled = enabled
                        } else {
                            viewModel.showMessage("فعّل PIN أولًا قبل استخدام البصمة", true)
                        }
                    },
                    enabled = isAppLockEnabled,
                    colors = SwitchDefaults.colors(checkedThumbColor = AmanTealDark, checkedTrackColor = AmanTealLight)
                )
            }
        }

        // Security Info Banner
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = AmanTealDark, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("معايير الأمان والتشفير", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Text(
                    text = "نحافظ على خصوصية بياناتك ونستخدم وسائل حماية مناسبة للحساب والتطبيق.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showSetPinDialog) {
        var newPin by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSetPinDialog = false },
            title = { Text("تعيين رمز PIN جديد", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("أدخل رمز PIN مكون من 4 أرقام:", fontSize = 12.sp, color = TextSecondary)
                    OutlinedTextField(
                        value = newPin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPin.length == 4) {
                            viewModel.configureAppLock(true, newPin)
                            isAppLockEnabled = true
                            showSetPinDialog = false
                        }
                    },
                    enabled = newPin.length == 4,
                    colors = ButtonDefaults.buttonColors(containerColor = AmanTealDark)
                ) {
                    Text("حفظ الرمز", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSetPinDialog = false }) {
                    Text("إلغاء", color = TextSecondary)
                }
            }
        )
    }
}

// ========================================================
// 3. Terms & Conditions Screen (الشروط والأحكام)
// ========================================================
@Composable
fun TermsAndConditionsScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("الشروط والأحكام", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
        Spacer(modifier = Modifier.height(6.dp))
        Text("وثيقة سياسة تقديم واستخدام خدمات حماية أرقام الهاتف في أمان", fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TermSection(
                    title = "1. طبيعة ونطاق الخدمة",
                    content = "منصة «أمان» تقدم خدمة آلية ومتابعة دورية لتسديد الحد الأدنى المطلوب على أرقام الهواتف التابعة لشركات الاتصالات العاملة في الجمهورية اليمنية لمنع دخول الرقم في حالة الخمول أو المصادرة وإعادة الطرح للبيع."
                )
                HorizontalDivider(color = BorderSubtle)
                TermSection(
                    title = "2. استمرارية الحماية",
                    content = "تعمل أمان على متابعة الحماية وفق السياسات المعتمدة، ويظهر للعميل فقط ما يتعلق بحالة الحماية ومدة الحماية."
                )
                HorizontalDivider(color = BorderSubtle)
                TermSection(
                    title = "3. رسوم الحماية السنوية",
                    content = "تُدفع رسوم الحماية المحددة في النظام لكل رقم، وتبدأ مدة الحماية من تاريخ التفعيل المسجل في النظام."
                )
                HorizontalDivider(color = BorderSubtle)
                TermSection(
                    title = "4. مسؤولية بيانات الرقم",
                    content = "العميل مسؤول عن إدخال رقم هاتف صحيح يخصه. في حال إدخال رقم خاطئ أو لا يتبع المشغل المكتشف، لا تتحمل المنصة مسؤولية العمليات المنفذة على الرقم الخاطئ."
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ========================================================
// 4. Privacy Policy Screen (سياسة الخصوصية)
// ========================================================
@Composable
fun PrivacyPolicyScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = Modifier.height(14.dp))
        Text("سياسة الخصوصية وسرية البيانات", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
        Spacer(modifier = Modifier.height(6.dp))
        Text("التزامنا بحماية أمان وخصوصية بيانات المستخدمين", fontSize = 11.sp, color = TextSecondary)
        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                TermSection(
                    title = "1. سرية أرقام الهواتف",
                    content = "نحافظ على خصوصية أرقامك ولا نستخدم بياناتك إلا لتقديم الخدمة وإدارة طلباتك."
                )
                HorizontalDivider(color = BorderSubtle)
                TermSection(
                    title = "2. المعاملات المالية وأدوات الدفع",
                    content = "تتعامل أمان مع أرقام الحوالات والمراجع المالية بغرض تدقيق ومطابقة طلب الحماية الخاص بك فقط. لا نقوم بتخزين أي كلمات مرور أو بيانات حساسة للمحافظ الإلكترونية للعملاء."
                )
                HorizontalDivider(color = BorderSubtle)
                TermSection(
                    title = "3. أمان الحساب والوصول",
                    content = "يتم التحقق من هوية صاحب الحساب برمز التوثيق المشفر ورمز PIN عند تفعيله. في حال الاشتباه بأي اختراق لجهازك يرجى تحديث بيانات المرور فوراً من شاشة الحساب."
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
    }
}

// ========================================================
// 5. About AMAN Screen (عن أمان)
// ========================================================
@Composable
fun AboutAmanScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AmanBgLight)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Shield squircle icon
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(AmanTealDark)
                .border(2.dp, AmanTealPrimary.copy(alpha = 0.5f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(42.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))
        Text("أمان | AMAN", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
        Text("الإصدار 1.0.0 (النسخة الرسمية)", fontSize = 12.sp, color = AmanTealDark, fontWeight = FontWeight.SemiBold)

        Spacer(modifier = Modifier.height(20.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("عن المنصة", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                Text(
                    text = "أمان خدمة تساعدك على حماية أرقام هاتفك ومتابعة حالة حمايتك وطلباتك بسهولة.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = BorderSubtle)
                Spacer(modifier = Modifier.height(4.dp))
                Text("مميزات أمان:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                BulletPoint("حماية رقمك خلال مدة الحماية")
                BulletPoint("عرض واضح لحالة الحماية والحماية")
                BulletPoint("اختيار وسائل الدفع المتاحة")
                BulletPoint("إشعارات واضحة عن حالة طلبك")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Trust footer
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = StatusActiveText, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("خدمة مصممة لتكون بسيطة وواضحة", fontSize = 11.sp, color = TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TermSection(title: String, content: String) {
    Column {
        Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = content, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
    }
}

@Composable
private fun BulletPoint(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(AmanTealDark)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 11.sp, color = TextSecondary)
    }
}

// ========================================================
// Support & Help
// ========================================================
@Composable
fun CustomerHelpScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(AmanBgLight).padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("المساعدة", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
        Text("إجابات مختصرة على أكثر الأسئلة شيوعًا حول استخدام أمان.", fontSize = 12.sp, color = TextSecondary)
        val items = listOf(
            "هل إضافة الرقم تفعّل الحماية؟" to "لا. إضافة الرقم تحفظه في حسابك فقط ويظل غير محمي حتى تختار تفعيل الحماية وتُرسل طلب التفعيل.",
            "متى تبدأ الحماية؟" to "تبدأ فقط بعد قبول طلب التفعيل من أمان، وتظهر لك مدة الحماية وتاريخا البداية والانتهاء.",
            "كيف أتابع طلبي؟" to "من قسم الطلبات يمكنك معرفة حالة كل طلب وأي سبب للرفض عند وجوده.",
            "هل تنفذ أمان الدفع التشغيلي تلقائيًا؟" to "لا. التنفيذ التشغيلي يتم خارجيًا بواسطة الجهة المصرح لها، وأمان يتابع الحالة ويسجل النتيجة فقط."
        )
        items.forEach { (q,a) ->
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(q, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(a, fontSize = 12.sp, color = TextSecondary, lineHeight = 18.sp)
                }
            }
        }
    }
}

@Composable
fun CustomerSupportScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(AmanBgLight).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("الدعم", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AmanDarkSlate)
        Card(colors = CardDefaults.cardColors(containerColor = SurfaceWhite), shape = RoundedCornerShape(12.dp), border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("تحتاج مساعدة؟", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("إذا واجهت مشكلة في إضافة رقم أو إرسال طلب حماية، جهّز رقم الهاتف ورقم مرجع الطلب إن وجد، ثم تواصل مع دعم أمان عبر قناة الدعم المعتمدة.", fontSize = 12.sp, color = TextSecondary, lineHeight = 19.sp)
                Text("ملاحظة: لا ترسل كلمات المرور أو رموز الدخول أو بيانات سرية للمحافظ.", fontSize = 11.sp, color = StatusActiveText)
            }
        }
    }
}
