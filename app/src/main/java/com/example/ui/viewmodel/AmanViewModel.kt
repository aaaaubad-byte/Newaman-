package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.auth.SessionManager
import com.example.core.network.NetworkResult
import com.example.core.network.SupabaseClient
import com.example.core.security.AppLockManager
import com.example.data.model.AuditLog
import com.example.data.model.CustomerAccount
import com.example.data.model.CustomerNumber
import com.example.data.model.FinancialSummary
import com.example.data.model.FinancialTransaction
import com.example.data.model.NotificationItem
import com.example.data.model.PaymentTask
import com.example.data.model.PaymentWallet
import com.example.data.model.Protection
import com.example.data.model.ProtectionRequest
import com.example.data.model.ProtectionPackage
import com.example.data.model.StateTransitionLog
import com.example.data.model.TaskSettings
import com.example.data.model.TaskClassification
import com.example.data.model.NotificationSetting
import com.example.data.model.EmployeeAccount
import com.example.data.model.SystemSettings
import com.example.data.model.TelecomProvider
import com.example.data.model.User
import com.example.data.model.UserType
import com.example.data.repository.AmanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

data class UiMessage(
    val message: String,
    val isError: Boolean = false
)

class AmanViewModel : ViewModel() {

    // Authentication & Current User
    val currentUser: StateFlow<User?> = AmanRepository.currentUser

    private val _isLoggedIn = MutableStateFlow(SessionManager.hasValidSession())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    // A saved token is only a local hint. Keep the splash visible until the
    // token/user session has been verified with Supabase.
    private val _isInitializing = MutableStateFlow(SessionManager.hasValidSession())
    val isInitializing: StateFlow<Boolean> = _isInitializing.asStateFlow()

    // App Lock PIN Verification state
    private val _isAppLocked = MutableStateFlow(AppLockManager.isLockEnabled() && AppLockManager.hasPinSet())
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

        private val _currentRoute = MutableStateFlow(
        if (SessionManager.hasValidSession()) {
            val user = SessionManager.getCurrentUser()
            if (user?.userType == UserType.ADMIN) "overview" else "home"
        } else "login"
    )
    val currentRoute: StateFlow<String> = _currentRoute.asStateFlow()

    val isBackendConnected: StateFlow<Boolean> = AmanRepository.isBackendConnected
    val dataLoadError: StateFlow<String?> = AmanRepository.dataLoadError

    // Data streams from repository
    val telecomProviders: StateFlow<List<TelecomProvider>> = AmanRepository.telecomProviders
    val paymentWallets: StateFlow<List<PaymentWallet>> = AmanRepository.paymentWallets
    val packages: StateFlow<List<ProtectionPackage>> = AmanRepository.packages
    val customerNumbers: StateFlow<List<CustomerNumber>> = AmanRepository.customerNumbers
    val protectionRequests: StateFlow<List<ProtectionRequest>> = AmanRepository.protectionRequests
    val protections: StateFlow<List<Protection>> = AmanRepository.protections
    val paymentTasks: StateFlow<List<PaymentTask>> = AmanRepository.paymentTasks
    val transactions: StateFlow<List<FinancialTransaction>> = AmanRepository.transactions
    val notifications: StateFlow<List<NotificationItem>> = AmanRepository.notifications
    val taskSettings: StateFlow<List<TaskSettings>> = AmanRepository.taskSettings
    val taskAmountSettings: StateFlow<List<TaskAmountSetting>> = AmanRepository.taskAmountSettings
    val notificationSettings: StateFlow<List<NotificationSetting>> = AmanRepository.notificationSettings
    val taskClassifications: StateFlow<List<TaskClassification>> = AmanRepository.taskClassifications
    val employees: StateFlow<List<EmployeeAccount>> = AmanRepository.employees
    val roles: StateFlow<List<Pair<String,String>>> = AmanRepository.roles
    val roleDetails = AmanRepository.roleDetails
    val permissions = AmanRepository.permissions
    val auditLogs: StateFlow<List<AuditLog>> = AmanRepository.auditLogs
    val stateTransitionLogs: StateFlow<List<StateTransitionLog>> = AmanRepository.stateTransitionLogs
    val customers: StateFlow<List<CustomerAccount>> = AmanRepository.customers
    val systemSettings: StateFlow<SystemSettings> = AmanRepository.systemSettings

    val financialSummary: StateFlow<FinancialSummary> = transactions.map {
        AmanRepository.getFinancialSummary()
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        AmanRepository.getFinancialSummary()
    )

    private val _uiMessage = MutableStateFlow<UiMessage?>(null)
    val uiMessage: StateFlow<UiMessage?> = _uiMessage.asStateFlow()

    // Keep the same idempotency key while a single protection submission is retried.
    private var protectionSubmissionSignature: String? = null
    private var protectionSubmissionKey: String? = null

    init {
        if (SessionManager.hasValidSession()) {
            val user = SessionManager.getCurrentUser()
            if (user != null) {
                _isLoggedIn.value = true
                viewModelScope.launch {
                    try {
                        val actor = SupabaseClient.currentActor(SessionManager.getAccessToken())
                        if (actor is NetworkResult.Success) {
                            val access = SessionManager.getAccessToken()
                            if (!access.isNullOrBlank()) {
                                SessionManager.saveSession(access, SessionManager.getRefreshToken(), actor.data)
                                AmanRepository.onUserAuthenticated(actor.data)
                            }
                        } else {
                            val refresh = SessionManager.getRefreshToken()
                            if (!refresh.isNullOrBlank()) {
                                val refreshed = SupabaseClient.refreshSession(refresh)
                                if (refreshed is NetworkResult.Success) {
                                    SessionManager.saveSession(refreshed.data.accessToken, refreshed.data.refreshToken, refreshed.data.user)
                                    AmanRepository.onUserAuthenticated(refreshed.data.user)
                                } else logout()
                            } else logout()
                        }
                    } finally {
                        _isInitializing.value = false
                    }
                }
            }
        } else {
            _isInitializing.value = false
        }
    }

    fun navigateTo(route: String) {
        val user = currentUser.value ?: SessionManager.getCurrentUser() ?: return
        val customerRoutes = setOf("home","phones","requests","protections","notifications","account","settings","security","help","terms","about")
        val adminRoutes = setOf("overview","customers","phones","requests","protections","tasks","notifications","settings","packages","task_settings","employees","roles","telecom","wallets","audit","account")
        if ((user.userType == UserType.CUSTOMER && route in customerRoutes) || (user.userType == UserType.ADMIN && route in adminRoutes)) {
            _currentRoute.value = route
        }
    }

    fun clearUiMessage() {
        _uiMessage.value = null
    }

    fun showMessage(message: String, isError: Boolean = false) {
        _uiMessage.value = UiMessage(message, isError)
    }

    fun retryConnection() {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            when (val actor = SupabaseClient.currentActor(SessionManager.getAccessToken())) {
                is NetworkResult.Success -> {
                    val access = SessionManager.getAccessToken()
                    if (access.isNullOrBlank()) {
                        logout()
                    } else {
                        SessionManager.saveSession(access, SessionManager.getRefreshToken(), actor.data)
                        AmanRepository.onUserAuthenticated(actor.data)
                        _isLoggedIn.value = true
                        _uiMessage.value = UiMessage("تمت استعادة الاتصال وتحديث البيانات")
                    }
                }
                is NetworkResult.Error -> {
                    if (actor.code == 401 || actor.code == 403) logout()
                    else _uiMessage.value = UiMessage(actor.messageAr, isError = true)
                }
                is NetworkResult.NetworkFailure -> _uiMessage.value = UiMessage(actor.messageAr, isError = true)
                is NetworkResult.Unknown -> _uiMessage.value = UiMessage(actor.messageAr, isError = true)
            }
            _isLoading.value = false
        }
    }

    // -------------------------------------------------------------
    // Real Authentication
    // -------------------------------------------------------------
    fun login(identifier: String, pass: String) {
        val cleanId = identifier.trim()
        val cleanPass = pass

        if (cleanId.isEmpty() || cleanPass.isEmpty()) {
            _uiMessage.value = UiMessage("يرجى إدخال البريد الإلكتروني وكلمة المرور", isError = true)
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanId).matches()) {
            _uiMessage.value = UiMessage("يرجى إدخال بريد إلكتروني صحيح", isError = true)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = SupabaseClient.signIn(cleanId, cleanPass)
            _isLoading.value = false

            when (result) {
                is NetworkResult.Success -> {
                    try {
                        val auth = result.data
                        SessionManager.saveSession(auth.accessToken, auth.refreshToken, auth.user)
                        AmanRepository.onUserAuthenticated(auth.user)
                        _isLoggedIn.value = true
                        _currentRoute.value = if (auth.user.userType == UserType.ADMIN) "overview" else "home"
                        _uiMessage.value = UiMessage("أهلاً بك، ${auth.user.fullName}")
                    } catch (e: Exception) {
                        _uiMessage.value = UiMessage("تم الاتصال بالخادم لكن تعذر فتح الجلسة: ${e.message ?: "خطأ غير معروف"}", isError = true)
                    }
                }
                is NetworkResult.Error -> {
                    _uiMessage.value = UiMessage(result.messageAr, isError = true)
                }
                is NetworkResult.NetworkFailure -> {
                    _uiMessage.value = UiMessage("تعذر الاتصال بالخادم، يرجى التأكد من اتصال الإنترنت وإعادة المحاولة", isError = true)
                }
                is NetworkResult.Unknown -> {
                    _uiMessage.value = UiMessage(result.messageAr, isError = true)
                }
            }
        }
    }

    fun register(fullName: String, email: String, pass: String) {
        val cleanName = fullName.trim()
        val cleanEmail = email.trim().lowercase()
        val cleanPass = pass

        if (cleanName.isEmpty() || cleanEmail.isEmpty() || cleanPass.isEmpty()) {
            _uiMessage.value = UiMessage("يرجى ملء جميع الحقول المطلوبة", isError = true)
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches()) {
            _uiMessage.value = UiMessage("يرجى إدخال بريد إلكتروني صحيح", isError = true)
            return
        }

        if (cleanPass.length < 6) {
            _uiMessage.value = UiMessage("كلمة المرور يجب أن تكون 6 خانات على الأقل", isError = true)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = SupabaseClient.signUp(cleanEmail, cleanPass, cleanName)
            _isLoading.value = false

            when (result) {
                is NetworkResult.Success -> {
                    try {
                        val auth = result.data
                        SessionManager.saveSession(auth.accessToken, auth.refreshToken, auth.user)
                        AmanRepository.onUserAuthenticated(auth.user)
                        _isLoggedIn.value = true
                        _currentRoute.value = if (auth.user.userType == UserType.ADMIN) "overview" else "home"
                        _uiMessage.value = UiMessage("تم إنشاء الحساب بنجاح، مرحباً بك في أمان")
                    } catch (e: Exception) {
                        _uiMessage.value = UiMessage("تم إنشاء الحساب لكن تعذر فتح الجلسة: ${e.message ?: "خطأ غير معروف"}", isError = true)
                    }
                }
                is NetworkResult.Error -> {
                    _uiMessage.value = UiMessage(result.messageAr, isError = true)
                }
                is NetworkResult.NetworkFailure -> {
                    _uiMessage.value = UiMessage("تعذر الاتصال بالخادم، يرجى التأكد من الاتصال بالإنترنت وإعادة المحاولة", isError = true)
                }
                is NetworkResult.Unknown -> {
                    _uiMessage.value = UiMessage(result.messageAr, isError = true)
                }
            }
        }
    }

    fun recoverPassword(identifier: String, onSuccess: () -> Unit) {
        val cleanId = identifier.trim().lowercase()
        if (cleanId.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(cleanId).matches()) {
            _uiMessage.value = UiMessage("يرجى إدخال بريد إلكتروني صحيح", isError = true)
            return
        }

        viewModelScope.launch {
            _isLoading.value = true
            val result = SupabaseClient.recoverPassword(cleanId)
            _isLoading.value = false

            when (result) {
                is NetworkResult.Success -> {
                    _uiMessage.value = UiMessage("تم إرسال طلب استعادة الحساب بنجاح")
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    _uiMessage.value = UiMessage(result.messageAr, isError = true)
                }
                is NetworkResult.NetworkFailure -> {
                    _uiMessage.value = UiMessage("تعذر الاتصال بالخادم، يرجى المحاولة لاحقاً", isError = true)
                }
                is NetworkResult.Unknown -> {
                    _uiMessage.value = UiMessage(result.messageAr, isError = true)
                }
            }
        }
    }

    fun logout() {
        val access = SessionManager.getAccessToken()
        viewModelScope.launch {
            if (!access.isNullOrBlank()) SupabaseClient.signOut(access)
            SessionManager.clearSession()
            AmanRepository.onUserLoggedOut()
            _isLoggedIn.value = false
            _isInitializing.value = false
            _currentRoute.value = "login"
            _uiMessage.value = UiMessage("تم تسجيل الخروج بنجاح")
        }
    }

    // -------------------------------------------------------------
    // App Lock PIN Management
    // -------------------------------------------------------------
    fun unlockWithPin(pin: String): Boolean {
        if (AppLockManager.verifyPin(pin)) {
            _isAppLocked.value = false
            return true
        } else {
            _uiMessage.value = UiMessage("رمز PIN غير صحيح", isError = true)
            return false
        }
    }

    fun unlockWithBiometric() {
        if (AppLockManager.isBiometricEnabled() && AppLockManager.hasPinSet()) {
            _isAppLocked.value = false
        }
    }

    fun configureAppLock(enabled: Boolean, pin: String? = null) {
        val success = AppLockManager.setLockEnabled(enabled, pin)
        if (success) {
            _uiMessage.value = UiMessage(if (enabled) "تم تفعيل قفل التطبيق بنجاح" else "تم تعطيل قفل التطبيق")
        } else {
            _uiMessage.value = UiMessage("يرجى إدخال رمز PIN صالح مكون من 4 أرقام", isError = true)
        }
    }

    // -------------------------------------------------------------
    // Customer Actions
    // -------------------------------------------------------------
    fun addCustomerNumber(phoneNumber: String, onSuccess: (CustomerNumber) -> Unit = {}) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = AmanRepository.addCustomerNumber(phoneNumber)
                result.onSuccess {
                    _uiMessage.value = UiMessage("تمت إضافة الرقم بنجاح (${it.phoneNumber} - ${it.providerNameAr})")
                    onSuccess(it)
                }.onFailure {
                    _uiMessage.value = UiMessage(it.message ?: "فشلت إضافة الرقم", isError = true)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitProtectionRequest(
        numberId: String,
        packageId: String,
        walletId: String,
        paymentReference: String,
        onSuccess: () -> Unit = {}
    ) {
        if (_isLoading.value) return
        val signature = "$numberId|$packageId|$walletId|${paymentReference.trim()}"
        if (signature != protectionSubmissionSignature) {
            protectionSubmissionSignature = signature
            protectionSubmissionKey = UUID.randomUUID().toString()
        }
        val idempotencyKey = protectionSubmissionKey ?: UUID.randomUUID().toString()
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = AmanRepository.submitProtectionRequest(numberId, packageId, walletId, paymentReference, idempotencyKey)
                result.onSuccess {
                    protectionSubmissionSignature = null
                    protectionSubmissionKey = null
                    _uiMessage.value = UiMessage("تم إرسال طلب الحماية بنجاح وهو الآن قيد المراجعة")
                    onSuccess()
                }.onFailure {
                    _uiMessage.value = UiMessage(it.message ?: "فشل تقديم طلب الحماية", isError = true)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    // -------------------------------------------------------------
    // Admin Actions
    // -------------------------------------------------------------
    fun approveProtectionRequest(requestId: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = AmanRepository.approveProtectionRequest(requestId)
                result.onSuccess {
                    _uiMessage.value = UiMessage("تمت الموافقة على الطلب وتفعيل الحماية بنجاح")
                }.onFailure {
                    _uiMessage.value = UiMessage(it.message ?: "فشلت الموافقة على الطلب", isError = true)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun rejectProtectionRequest(requestId: String, reason: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = AmanRepository.rejectProtectionRequest(requestId, reason)
                result.onSuccess {
                    _uiMessage.value = UiMessage("تم رفض طلب الحماية وإشعار العميل")
                }.onFailure {
                    _uiMessage.value = UiMessage(it.message ?: "فشل رفض الطلب", isError = true)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun submitRenewalRequest(protectionId: String, packageId: String, paymentMethodId: String, reference: String, onSuccess: () -> Unit = {}) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = AmanRepository.submitRenewalRequest(protectionId, packageId, paymentMethodId, reference)
                result.onSuccess { _uiMessage.value = UiMessage("تم إرسال طلب التجديد للمراجعة"); onSuccess() }
                    .onFailure { _uiMessage.value = UiMessage(it.message ?: "فشل إرسال طلب التجديد", true) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun reschedulePaymentTask(taskId: String, newDueAt: String, reason: String? = null) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = AmanRepository.reschedulePaymentTask(taskId, newDueAt, reason)
                result.onSuccess { _uiMessage.value = UiMessage("تمت إعادة جدولة المهمة الحالية فقط") }
                    .onFailure { _uiMessage.value = UiMessage(it.message ?: "فشل إعادة جدولة المهمة", true) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun completePaymentTask(taskId: String, telecomReference: String? = null) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = AmanRepository.completePaymentTask(taskId, telecomReference)
                result.onSuccess {
                    _uiMessage.value = UiMessage("تم تأكيد سداد المهمة وتسجيل المصروف التشغيلي بنجاح")
                }.onFailure {
                    _uiMessage.value = UiMessage(it.message ?: "فشل تأكيد سداد المهمة", isError = true)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun cancelPaymentTask(taskId: String, reason: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = AmanRepository.cancelPaymentTask(taskId, reason)
                result.onSuccess {
                    _uiMessage.value = UiMessage("تم إلغاء المهمة بنجاح")
                }.onFailure {
                    _uiMessage.value = UiMessage(it.message ?: "فشل إلغاء المهمة", isError = true)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun upsertPackage(id: String?, providerId: String, name: String, durationDays: Int, price: Double, currency: String, active: Boolean, sortOrder: Int) {
        viewModelScope.launch { AmanRepository.upsertPackage(id, providerId, name, durationDays, price, currency, active, sortOrder).onFailure { _uiMessage.value = UiMessage(it.message ?: "فشل حفظ الباقة", true) } }
    }

    fun addTelecomProvider(nameAr:String,nameEn:String,prefixes:List<String>,sortOrder:Int,numberLength:Int){viewModelScope.launch{AmanRepository.addTelecomProvider(nameAr,nameEn,prefixes,sortOrder,numberLength).onFailure{_uiMessage.value=UiMessage(it.message ?: "فشل إضافة الشركة",true)}}}
        fun updateTelecomProvider(id: String, nameAr: String, nameEn: String, prefixes: List<String>, sortOrder: Int, numberLength: Int = 9) {
        viewModelScope.launch { AmanRepository.updateTelecomProvider(id,nameAr,nameEn,prefixes,sortOrder,numberLength).onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل تحديث الشركة",true) } }
    }

    fun upsertPaymentMethod(id:String?,type:String,name:String,recipient:String,account:String,instructions:String,active:Boolean=true,sortOrder:Int=0){viewModelScope.launch{AmanRepository.upsertPaymentMethod(id,type,name,recipient,account,instructions,active,sortOrder).onFailure{_uiMessage.value=UiMessage(it.message ?: "فشل حفظ وسيلة الدفع",true)}}}
        fun updatePaymentWallet(id: String, nameAr: String, nameEn: String, account: String?, beneficiary: String?, instructions: String, sortOrder: Int) {
        viewModelScope.launch { AmanRepository.updatePaymentWallet(id,nameAr,nameEn,account,beneficiary,instructions,sortOrder).onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل تحديث المحفظة",true) } }
    }

    fun setTelecomProviderActive(id: String, active: Boolean) {
        viewModelScope.launch { AmanRepository.setTelecomProviderActive(id, active).onFailure { _uiMessage.value = UiMessage(it.message ?: "فشل تحديث شركة الاتصالات", true) } }
    }

    fun setPaymentWalletActive(id: String, active: Boolean) {
        viewModelScope.launch { AmanRepository.setPaymentWalletActive(id, active).onFailure { _uiMessage.value = UiMessage(it.message ?: "فشل تحديث محفظة الدفع", true) } }
    }

    fun updateSettings(newSettings: SystemSettings) {
        viewModelScope.launch {
            val result = AmanRepository.updateSystemSettings(newSettings)
            result.onSuccess {
                _uiMessage.value = UiMessage("تم حفظ إعدادات وسياسات النظام بنجاح")
            }.onFailure {
                _uiMessage.value = UiMessage(it.message ?: "فشل حفظ الإعدادات", isError = true)
            }
        }
    }


    fun updateTaskSettings(providerId:String, firstTask:Boolean, reschedule:Boolean, intervalDays:Int?, visibilityDaysBefore:Int=30, active:Boolean=true) { viewModelScope.launch { AmanRepository.updateTaskSettings(providerId,firstTask,reschedule,intervalDays,visibilityDaysBefore,active).onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل حفظ إعدادات المهام",true) } } }
    fun updateTaskAmountSetting(setting: TaskAmountSetting) { viewModelScope.launch { AmanRepository.updateTaskAmountSetting(setting).onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل حفظ مبلغ المهمة",true) } } }
    fun updateTaskClassification(id:String?,taskSettingsId:String,name:String,minDays:Int?,maxDays:Int?,sortOrder:Int,active:Boolean){ viewModelScope.launch { AmanRepository.updateTaskClassification(id,taskSettingsId,name,minDays,maxDays,sortOrder,active).onFailure{_uiMessage.value=UiMessage(it.message ?: "فشل حفظ تصنيف المهمة",true)} } }
        fun updateNotificationSetting(setting:NotificationSetting) { viewModelScope.launch { AmanRepository.updateNotificationSetting(setting).onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل حفظ إعداد الإشعار",true) } } }
    fun setEmployeeRole(userId:String,roleId:String) { viewModelScope.launch { AmanRepository.setEmployeeRole(userId,roleId).onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل تغيير الدور",true) } } }
    fun setEmployeeStatus(userId:String,status:String) { viewModelScope.launch { AmanRepository.setEmployeeStatus(userId,status).onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل تغيير الحالة",true) } } }
    fun replaceRolePermissions(roleId:String, permissionCodes:List<String>) { viewModelScope.launch { AmanRepository.replaceRolePermissions(roleId, permissionCodes).onSuccess { _uiMessage.value=UiMessage("تم حفظ صلاحيات الدور وتسجيل العملية") }.onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل حفظ الصلاحيات",true) } } }
    fun updateMyProfile(name:String,email:String?,onSuccess:()->Unit={}) { viewModelScope.launch { AmanRepository.updateMyProfile(name,email).onSuccess { _uiMessage.value=UiMessage("تم حفظ بيانات الحساب"); onSuccess() }.onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل حفظ بيانات الحساب",true) } } }

    fun markNotificationAsRead(id: String) {
        viewModelScope.launch { AmanRepository.markNotificationAsRead(id) }
    }

    fun markNotificationRead(id: String) {
        viewModelScope.launch { AmanRepository.markNotificationAsRead(id) }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { AmanRepository.markAllNotificationsRead() }
    }

    fun approveRequest(requestId: String) {
        approveProtectionRequest(requestId)
    }

    fun rejectRequest(requestId: String, reason: String) {
        rejectProtectionRequest(requestId, reason)
    }

    fun verifyPayment(requestId: String, note: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val res = AmanRepository.verifyPaymentReceipt(requestId, note)
                res.onSuccess { _uiMessage.value = UiMessage("تم التحقق من إشعار السداد بنجاح") }
                    .onFailure { _uiMessage.value = UiMessage(it.message ?: "فشل التحقق من السداد", isError = true) }
            } finally {
                _isLoading.value = false
            }
        }
    }
    fun approveRenewalRequest(requestId: String) {
        if (_isLoading.value) return
        viewModelScope.launch {
            _isLoading.value = true
            try {
                AmanRepository.approveRenewalRequest(requestId)
                    .onSuccess { _uiMessage.value = UiMessage("تم قبول طلب التجديد وبدء الحماية الجديدة") }
                    .onFailure { _uiMessage.value = UiMessage(it.message ?: "فشل قبول طلب التجديد", true) }
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun resubmitRejectedRequest(requestId:String, packageId:String, paymentMethodId:String, reference:String, onSuccess:()->Unit={}) {
        viewModelScope.launch {
            AmanRepository.resubmitRejectedRequest(requestId,packageId,paymentMethodId,reference)
                .onSuccess { _uiMessage.value=UiMessage("تمت إعادة تقديم الطلب للمراجعة"); onSuccess() }
                .onFailure { _uiMessage.value=UiMessage(it.message ?: "فشل إعادة تقديم الطلب",true) }
        }
    }

}
