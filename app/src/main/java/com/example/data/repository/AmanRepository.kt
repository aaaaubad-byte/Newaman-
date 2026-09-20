package com.example.data.repository

import com.example.core.auth.SessionManager
import com.example.core.network.NetworkResult
import com.example.core.network.SupabaseClient
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/** Single source of truth for the Android client. Business decisions remain in Supabase RPC/RLS. */
object AmanRepository {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()
    private val _systemSettings = MutableStateFlow(SystemSettings())
    val systemSettings: StateFlow<SystemSettings> = _systemSettings.asStateFlow()
    private val _telecomProviders = MutableStateFlow<List<TelecomProvider>>(emptyList())
    val telecomProviders: StateFlow<List<TelecomProvider>> = _telecomProviders.asStateFlow()
    private val _paymentWallets = MutableStateFlow<List<PaymentWallet>>(emptyList())
    private val _packages = MutableStateFlow<List<ProtectionPackage>>(emptyList())
    val packages: StateFlow<List<ProtectionPackage>> = _packages.asStateFlow()
    val paymentWallets: StateFlow<List<PaymentWallet>> = _paymentWallets.asStateFlow()
    private val _customerNumbers = MutableStateFlow<List<CustomerNumber>>(emptyList())
    val customerNumbers: StateFlow<List<CustomerNumber>> = _customerNumbers.asStateFlow()
    private val _protectionRequests = MutableStateFlow<List<ProtectionRequest>>(emptyList())
    val protectionRequests: StateFlow<List<ProtectionRequest>> = _protectionRequests.asStateFlow()
    private val _protections = MutableStateFlow<List<Protection>>(emptyList())
    val protections: StateFlow<List<Protection>> = _protections.asStateFlow()
    private val _paymentTasks = MutableStateFlow<List<PaymentTask>>(emptyList())
    val paymentTasks: StateFlow<List<PaymentTask>> = _paymentTasks.asStateFlow()
    private val _transactions = MutableStateFlow<List<FinancialTransaction>>(emptyList())
    val transactions: StateFlow<List<FinancialTransaction>> = _transactions.asStateFlow()
    private val _notifications = MutableStateFlow<List<NotificationItem>>(emptyList())
    val notifications: StateFlow<List<NotificationItem>> = _notifications.asStateFlow()
    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()
    private val _stateTransitionLogs = MutableStateFlow<List<StateTransitionLog>>(emptyList())
    val stateTransitionLogs: StateFlow<List<StateTransitionLog>> = _stateTransitionLogs.asStateFlow()
    private val _customers = MutableStateFlow<List<CustomerAccount>>(emptyList())
    val customers: StateFlow<List<CustomerAccount>> = _customers.asStateFlow()
    private val _taskSettings = MutableStateFlow<List<TaskSettings>>(emptyList())
    val taskSettings: StateFlow<List<TaskSettings>> = _taskSettings.asStateFlow()
    private val _notificationSettings = MutableStateFlow<List<NotificationSetting>>(emptyList())
    private val _taskClassifications = MutableStateFlow<List<TaskClassification>>(emptyList())
    val taskClassifications: StateFlow<List<TaskClassification>> = _taskClassifications.asStateFlow()
    val notificationSettings: StateFlow<List<NotificationSetting>> = _notificationSettings.asStateFlow()
    private val _employees = MutableStateFlow<List<EmployeeAccount>>(emptyList())
    val employees: StateFlow<List<EmployeeAccount>> = _employees.asStateFlow()
    private val _roles = MutableStateFlow<List<Pair<String,String>>>(emptyList())
    val roles: StateFlow<List<Pair<String,String>>> = _roles.asStateFlow()
    private val _roleDetails = MutableStateFlow<List<Role>>(emptyList())
    val roleDetails: StateFlow<List<Role>> = _roleDetails.asStateFlow()
    private val _permissions = MutableStateFlow<List<PermissionItem>>(emptyList())
    val permissions: StateFlow<List<PermissionItem>> = _permissions.asStateFlow()
    private val _isBackendConnected = MutableStateFlow(false)
    val isBackendConnected: StateFlow<Boolean> = _isBackendConnected.asStateFlow()

    private fun token() = SessionManager.getAccessToken()

    fun onUserAuthenticated(user: User) { _currentUser.value = user; scope.launch { refreshAll() } }
    fun onUserLoggedOut() { _currentUser.value = null; clearData() }

    suspend fun refreshAll() {
        val user = _currentUser.value ?: SessionManager.getCurrentUser() ?: return
        val health = SupabaseClient.checkHealth()
        _isBackendConnected.value = health
        if (!health) return
        try {
            loadCatalog(user.userType)
            if (user.userType == UserType.CUSTOMER) loadCustomer(user.id) else loadAdmin()
        } catch (_: Exception) { _isBackendConnected.value = false }
    }

    private suspend fun loadCatalog(userType: UserType) {
        val providerQuery = mutableMapOf("select" to "*", "order" to "sort_order.asc")
        if (userType == UserType.CUSTOMER) providerQuery["is_visible_to_customer"] = "eq.true"
        val providers = SupabaseClient.get("telecom_providers", providerQuery, token()).getOrNull()
        if (providers != null) {
            val prefixRows = SupabaseClient.get("provider_prefixes", mapOf("select" to "*", "is_active" to "eq.true"), token()).getOrNull() ?: JSONArray()
            _telecomProviders.value = providers.toProviderList(prefixRows)
        }
        val methods = SupabaseClient.get("payment_methods", mapOf("select" to "*", "order" to "sort_order.asc", "is_active" to "eq.true"), token()).getOrNull()
        if (methods != null) _paymentWallets.value = methods.toPaymentMethodList()
        val packageQuery = mutableMapOf("select" to "*", "order" to "sort_order.asc", "is_active" to "eq.true")
        if (userType == UserType.CUSTOMER) packageQuery["is_visible_to_customer"] = "eq.true"
        val packages = SupabaseClient.get("packages", packageQuery, token()).getOrNull()
        if (packages != null) _packages.value = packages.toPackageList()

        if (userType == UserType.ADMIN) {
            // Settings are key/value records in V1.0, never hard-coded into UI logic.
            val rows = SupabaseClient.get("system_settings", mapOf("select" to "*"), token()).getOrNull() ?: JSONArray()
            _systemSettings.value = rows.toSettings()
        } else {
            val cfg = SupabaseClient.get("packages", mapOf("select" to "id", "limit" to "1"), token()).getOrNull()
            // Customer-facing package price/duration comes from packages, not a global hard-coded value.
            if (cfg != null) {
            val first = SupabaseClient.get("packages", mapOf("select" to "price,duration_days", "is_active" to "eq.true", "order" to "sort_order.asc", "limit" to "1"), token()).getOrNull()?.optJSONObject(0)
                if (first != null) _systemSettings.value = SystemSettings(currency = _systemSettings.value.currency.ifBlank { "YER" })
            }
        }
    }

    private suspend fun loadCustomer(customerId: String) {
        // customer_my_numbers exposes created_at as its canonical timestamp.
        val numbers = SupabaseClient.get("customer_my_numbers", mapOf("select" to "*", "customer_id" to "eq.$customerId", "order" to "created_at.desc"), token()).getOrNull()?.toCustomerNumbers() ?: emptyList()
        _customerNumbers.value = numbers
        val requests = SupabaseClient.get("protection_requests", mapOf(
            "select" to "*,customer_numbers(customer_id,phone_numbers(number,provider_id,telecom_providers(name_ar,name_en))),packages(name,duration_days,price),payment_methods(name)",
            "customer_id" to "eq.$customerId", "order" to "created_at.desc"
        ), token()).getOrNull()?.toProtectionRequests() ?: emptyList()
        val numberMap = numbers.associate { it.id to it.phoneNumber }
        _protectionRequests.value = requests.map { it.copy(phoneNumber = numberMap[it.numberId] ?: it.phoneNumber) }
        val protected = SupabaseClient.get("customer_my_protections", mapOf("select" to "*", "order" to "created_at.desc"), token()).getOrNull()?.toProtections() ?: emptyList()
        _protections.value = protected
        val activeIds = protected.filter { it.status == "active" && it.daysRemaining > 0 }.map { it.numberId }.toSet()
        _customerNumbers.value = numbers.map { it.copy(isProtected = it.id in activeIds) }
        val notifications = SupabaseClient.get("client_notifications", mapOf("select" to "*", "customer_id" to "eq.$customerId", "order" to "created_at.desc"), token()).getOrNull()?.toNotifications() ?: emptyList()
        _notifications.value = notifications
        SupabaseClient.currentActor(token()).getOrNull()?.let { _currentUser.value = it }
    }

    private suspend fun loadAdmin() {
        val loadedCustomers = SupabaseClient.get("users", mapOf("select" to "*", "user_type" to "eq.customer", "order" to "created_at.desc"), token()).getOrNull()?.toCustomers() ?: emptyList()
        val numbers = SupabaseClient.get("customer_my_numbers", mapOf("select" to "*", "order" to "created_at.desc"), token()).getOrNull()?.toCustomerNumbers() ?: emptyList()
        val protected = SupabaseClient.get("admin_all_protections", mapOf("select" to "*", "order" to "created_at.desc"), token()).getOrNull()?.toProtections() ?: emptyList()
        val activeIds = protected.filter { it.status == "active" && it.daysRemaining > 0 }.map { it.numberId }.toSet()
        _customerNumbers.value = numbers.map { it.copy(isProtected = it.id in activeIds) }
        _protections.value = protected
        _customers.value = loadedCustomers.map { c -> c.copy(registeredNumbersCount = numbers.count { it.customerId == c.id }, activeProtectionsCount = protected.count { it.customerId == c.id && it.status == "active" }) }
        val adminRequests = SupabaseClient.get("protection_requests", mapOf("select" to "*,customer_numbers(customer_id,phone_numbers(number,provider_id,telecom_providers(name_ar,name_en))),packages(name,duration_days,price),payment_methods(name)", "order" to "created_at.desc"), token()).getOrNull()?.toProtectionRequests() ?: emptyList()
        val adminNumberMap = numbers.associate { it.id to it.phoneNumber }
        _protectionRequests.value = adminRequests.map { it.copy(phoneNumber = adminNumberMap[it.numberId] ?: it.phoneNumber) }
        val tasks = SupabaseClient.get("admin_payment_tasks", mapOf("select" to "*", "order" to "due_at.asc"), token()).getOrNull()?.toPaymentTasks() ?: emptyList()
        _paymentTasks.value = tasks
        _transactions.value = SupabaseClient.get("financial_transactions", mapOf("select" to "*", "order" to "created_at.desc"), token()).getOrNull()?.toTransactions() ?: emptyList()
        _notifications.value = SupabaseClient.get("admin_notifications", mapOf("select" to "*", "order" to "created_at.desc"), token()).getOrNull()?.toNotifications() ?: emptyList()
        _auditLogs.value = SupabaseClient.get("audit_logs", mapOf("select" to "*", "order" to "created_at.desc"), token()).getOrNull()?.toAuditLogs() ?: emptyList()
        _taskSettings.value = SupabaseClient.get("task_settings", mapOf("select" to "*", "provider_id" to "not.is.null", "order" to "updated_at.asc"), token()).getOrNull()?.toTaskSettings() ?: emptyList()
        _notificationSettings.value = SupabaseClient.get("notification_settings", mapOf("select" to "*", "order" to "type.asc"), token()).getOrNull()?.toNotificationSettings() ?: emptyList()
        val admins = SupabaseClient.get("users", mapOf("select" to "id,full_name,email,status,role_id,roles(name)", "user_type" to "eq.admin", "order" to "created_at.asc"), token()).getOrNull()
        _employees.value = admins?.toEmployees() ?: emptyList()
        val rolesRows = SupabaseClient.get("roles", mapOf("select" to "id,name,description,is_active", "order" to "name.asc"), token()).getOrNull()
        _roles.value = rolesRows?.let { arr -> List(arr.length()) { i -> val j=arr.getJSONObject(i); j.getString("id") to j.getString("name") } } ?: emptyList()
        val permissionRows = SupabaseClient.get("permissions", mapOf("select" to "id,code,name,description", "order" to "code.asc"), token()).getOrNull()
        _permissions.value = permissionRows?.let { arr -> List(arr.length()) { i -> val j=arr.getJSONObject(i); PermissionItem(j.getString("id"), j.text("code"), j.text("name"), j.optString("description").ifBlank { null }) } } ?: emptyList()
        val rolePermissionRows = SupabaseClient.get("role_permissions", mapOf("select" to "role_id,permissions(code)"), token()).getOrNull()
        _roleDetails.value = rolesRows?.let { arr -> List(arr.length()) { i ->
            val j = arr.getJSONObject(i); val roleId = j.getString("id")
            val codes = rolePermissionRows?.let { rp -> List(rp.length()) { x -> rp.optJSONObject(x) }.filterNotNull().filter { it.optString("role_id") == roleId }.mapNotNull { it.optJSONObject("permissions")?.optString("code")?.ifBlank { null } } } ?: emptyList()
            Role(roleId, j.text("name"), j.optString("description").ifBlank { null }, j.optBoolean("is_active", true), codes, 0)
        } } ?: emptyList()
        val classRows = SupabaseClient.get("task_time_classifications", mapOf("select" to "*", "order" to "sort_order.asc"), token()).getOrNull()
        _taskClassifications.value = classRows?.let { arr -> List(arr.length()){ i -> val j=arr.getJSONObject(i); TaskClassification(j.getString("id"),j.optString("task_settings_id").ifBlank { j.optString("provider_id") },j.optString("name").ifBlank { j.optString("classification") },if(j.isNull("min_days_remaining"))null else j.optInt("min_days_remaining"),if(j.isNull("max_days_remaining"))null else j.optInt("max_days_remaining"),j.optInt("sort_order"),j.optBoolean("is_active",true)) } } ?: emptyList()
        _stateTransitionLogs.value = emptyList()
    }

    private suspend fun reloadCustomerNumbersFromDatabase(customerId: String): Result<List<CustomerNumber>> {
        return when (val response = SupabaseClient.get(
            "customer_my_numbers",
            mapOf(
                "select" to "*",
                "customer_id" to "eq.$customerId",
                "order" to "created_at.desc"
            ),
            token()
        )) {
            is NetworkResult.Success -> {
                val numbers = response.data.toCustomerNumbers()
                _customerNumbers.value = numbers
                Result.success(numbers)
            }
            is NetworkResult.Error -> Result.failure(Exception(response.messageAr))
            is NetworkResult.NetworkFailure -> Result.failure(response.exception)
            is NetworkResult.Unknown -> Result.failure(Exception(response.messageAr))
        }
    }

    suspend fun addCustomerNumber(phoneNumber: String): Result<CustomerNumber> = when (val result = SupabaseClient.rpc("add_customer_number", JSONObject().put("p_number", phoneNumber.trim()), token())) {
        is NetworkResult.Success -> {
            // The database view is the only source of truth after the RPC.
            // Do not synthesize or prepend a local row when this read fails.
            val customerId = _currentUser.value?.id
                ?: SessionManager.getCurrentUser()?.id
                ?: return Result.failure(Exception("تعذر تحديد حساب العميل"))
            val refreshed = reloadCustomerNumbersFromDatabase(customerId)
                .getOrElse { return Result.failure(Exception("تمت الإضافة، لكن تعذر تحديث قائمة الأرقام من قاعدة البيانات: ${it.message}")) }
            val normalizedInput = SupabaseClient.extractLocalPhone(phoneNumber)
            refreshed.firstOrNull { SupabaseClient.extractLocalPhone(it.phoneNumber) == normalizedInput }
                ?.let { Result.success(it) }
                ?: Result.failure(Exception("تمت الإضافة، لكن الرقم لم يظهر في قائمة قاعدة البيانات"))
        }
        is NetworkResult.Error -> Result.failure(Exception(result.messageAr)); is NetworkResult.NetworkFailure -> Result.failure(Exception(result.messageAr)); is NetworkResult.Unknown -> Result.failure(Exception(result.messageAr))
    }

    suspend fun submitProtectionRequest(numberId: String, packageId: String, paymentMethodId: String, paymentReference: String, idempotencyKey: String = UUID.randomUUID().toString()): Result<Unit> {
        if (paymentReference.trim().isBlank()) return Result.failure(Exception("رقم المرجع مطلوب"))
        val body = JSONObject().apply { put("p_number_id", numberId); put("p_package_id", packageId); put("p_wallet_id", paymentMethodId); put("p_transfer_ref", paymentReference.trim()); put("p_idempotency_key", idempotencyKey) }
        return rpcUnit("submit_protection_request", body).also { if (it.isSuccess) refreshAll() }
    }

    suspend fun approveProtectionRequest(requestId: String): Result<Protection> = when (val result = SupabaseClient.rpc("verify_and_approve_protection_request", JSONObject().put("p_request_id", requestId), token())) {
        is NetworkResult.Success -> { refreshAll(); _protections.value.firstOrNull { it.requestId == requestId }?.let { Result.success(it) } ?: Result.failure(Exception("تمت الموافقة ولكن تعذر تحديث الحماية")) }
        is NetworkResult.Error -> Result.failure(Exception(result.messageAr)); is NetworkResult.NetworkFailure -> Result.failure(Exception(result.messageAr)); is NetworkResult.Unknown -> Result.failure(Exception(result.messageAr))
    }

    suspend fun rejectProtectionRequest(requestId: String, reason: String): Result<Unit> {
        val cleanReason = reason.trim()
        if (cleanReason.isBlank()) return Result.failure(Exception("سبب الرفض مطلوب"))
        return rpcUnit("reject_protection_request", JSONObject().apply {
            put("p_request_id", requestId)
            put("p_rejection_reason", cleanReason)
        }).also { if (it.isSuccess) refreshAll() }
    }

    suspend fun submitRenewalRequest(protectionId: String, packageId: String, paymentMethodId: String, paymentReference: String): Result<Unit> = try {
        if (paymentReference.trim().isBlank()) throw IllegalArgumentException("رقم المرجع مطلوب")
        val r = SupabaseClient.rpc("submit_renewal_request", JSONObject().apply {
            put("p_protection_id", protectionId); put("p_package_id", packageId)
            put("p_wallet_id", paymentMethodId); put("p_transfer_ref", paymentReference.trim())
        }, token())
        if (r is NetworkResult.Success) { refreshAll(); Result.success(Unit) }
        else Result.failure(Exception((r as? NetworkResult.Error)?.messageAr ?: "فشل إرسال طلب التجديد"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun approveRenewalRequest(requestId:String):Result<Unit> = rpcUnit("approve_renewal_request",JSONObject().put("p_request_id",requestId))

    suspend fun resubmitRejectedRequest(requestId:String,packageId:String,paymentMethodId:String,paymentReference:String):Result<Unit> = try {
        if(paymentReference.trim().isBlank()) throw IllegalArgumentException("رقم المرجع مطلوب")
        val r=SupabaseClient.rpc("resubmit_rejected_request",JSONObject().apply{put("p_request_id",requestId);put("p_package_id",packageId);put("p_wallet_id",paymentMethodId);put("p_transfer_ref",paymentReference.trim())},token())
        if(r is NetworkResult.Success){refreshAll();Result.success(Unit)} else Result.failure(Exception((r as? NetworkResult.Error)?.messageAr ?: "فشل إعادة تقديم الطلب"))
    } catch(e:Exception){Result.failure(e)}

    suspend fun reschedulePaymentTask(taskId: String, newDueAt: String, reason: String? = null): Result<Unit> = try {
        val r = SupabaseClient.rpc("reschedule_payment_task", JSONObject().apply {
            put("p_task_id", taskId)
            put("p_new_due_date", newDueAt)
            if (!reason.isNullOrBlank()) put("p_reason", reason.trim())
        }, token())
        if (r is NetworkResult.Success) { refreshAll(); Result.success(Unit) }
        else Result.failure(Exception((r as? NetworkResult.Error)?.messageAr ?: "فشل إعادة جدولة المهمة"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun completePaymentTask(taskId: String, telecomReference: String? = null): Result<PaymentTask> {
        val result = SupabaseClient.rpc("complete_payment_task", JSONObject().apply {
            put("p_task_id", taskId)
            put("p_telecom_ref", telecomReference?.trim().takeUnless { it.isNullOrBlank() } ?: JSONObject.NULL)
        }, token())
        return when (result) {
            is NetworkResult.Success -> { refreshAll(); _paymentTasks.value.firstOrNull { it.id == taskId }?.let { Result.success(it) } ?: Result.failure(Exception("تم التنفيذ ولكن تعذر تحديث المهمة")) }
            is NetworkResult.Error -> Result.failure(Exception(result.messageAr)); is NetworkResult.NetworkFailure -> Result.failure(Exception(result.messageAr)); is NetworkResult.Unknown -> Result.failure(Exception(result.messageAr))
        }
    }

    fun getFinancialSummary(): FinancialSummary { val i=_transactions.value.filter{it.type=="income"}.sumOf{it.amount}; val e=_transactions.value.filter{it.type=="expense"}.sumOf{it.amount}; return FinancialSummary(i,e,i-e,_systemSettings.value.currency) }

    suspend fun setTelecomProviderActive(id:String,active:Boolean):Result<Unit> = rpcUnit("admin_upsert_provider", JSONObject().apply { put("p_id",id); val p=_telecomProviders.value.firstOrNull{it.id==id}; put("p_name",p?.nameAr ?: ""); put("p_code",p?.code ?: ""); put("p_number_length",p?.numberLength ?: 9); put("p_is_active",active); put("p_sort_order",p?.sortOrder ?: 0) })
    suspend fun upsertPackage(id: String?, providerId: String, name: String, durationDays: Int, price: Double, currency: String, active: Boolean, sortOrder: Int): Result<Unit> = try {
        val r = SupabaseClient.rpc("admin_upsert_package", JSONObject().apply {
            put("p_id", id); put("p_provider_id", providerId); put("p_name", name.trim()); put("p_duration_days", durationDays)
            put("p_price", price); put("p_currency", currency.trim()); put("p_is_active", active); put("p_sort_order", sortOrder)
        }, token())
        if (r is NetworkResult.Success) { refreshAll(); Result.success(Unit) }
        else Result.failure(Exception((r as? NetworkResult.Error)?.messageAr ?: "فشل حفظ الباقة"))
    } catch (e: Exception) { Result.failure(e) }

    suspend fun addTelecomProvider(nameAr:String,nameEn:String,prefixes:List<String>,sortOrder:Int,numberLength:Int=9):Result<Unit> = try {
        val r=SupabaseClient.rpc("admin_upsert_provider",JSONObject().apply{put("p_id",JSONObject.NULL);put("p_name",nameAr.trim());put("p_code",nameEn.trim());put("p_number_length",numberLength);put("p_is_active",true);put("p_sort_order",sortOrder)},token())
        if(r is NetworkResult.Success){ val id=r.data.optString("id").ifBlank{r.data.optString("result")}.ifBlank{r.data.optString("data")}; if(id.isNotBlank()) prefixes.forEach{SupabaseClient.rpc("admin_upsert_provider_prefix",JSONObject().apply{put("p_id",JSONObject.NULL);put("p_provider_id",id);put("p_prefix",it);put("p_is_active",true)},token())}; refreshAll(); Result.success(Unit) } else Result.failure(Exception((r as? NetworkResult.Error)?.messageAr ?: "فشل إضافة الشركة"))
    } catch(e:Exception){Result.failure(e)}

    suspend fun updateTelecomProvider(id:String,nameAr:String,nameEn:String,prefixes:List<String>,sortOrder:Int,numberLength:Int=9):Result<Unit> { val p=_telecomProviders.value.firstOrNull{it.id==id}; val r=SupabaseClient.rpc("admin_upsert_provider",JSONObject().apply{put("p_id",id);put("p_name",nameAr.trim());put("p_code",p?.code?:nameEn.trim());put("p_number_length",numberLength);put("p_is_active",p?.isActive?:true);put("p_sort_order",sortOrder)},token()); if(r is NetworkResult.Success){ val sync=SupabaseClient.rpc("admin_replace_provider_prefixes",JSONObject().apply{put("p_provider_id",id);put("p_prefixes",JSONArray(prefixes.map{it.trim()}.filter{it.isNotBlank()}))},token()); if(sync !is NetworkResult.Success) return Result.failure(Exception((sync as? NetworkResult.Error)?.messageAr ?: "فشل مزامنة البادئات")); refreshAll(); return Result.success(Unit)}; return Result.failure(Exception((r as? NetworkResult.Error)?.messageAr ?: "فشل تحديث الشركة")) }
    suspend fun setPaymentWalletActive(id:String,active:Boolean):Result<Unit> = updatePaymentMethodActive(id,active)
    suspend fun upsertPaymentMethod(id:String?,type:String,name:String,recipient:String,account:String,instructions:String,active:Boolean=true,sortOrder:Int=0):Result<Unit> = rpcUnit("admin_upsert_payment_method",JSONObject().apply{put("p_id",id ?: JSONObject.NULL);put("p_type",type);put("p_name",name.trim());put("p_recipient",recipient.trim());put("p_account",account.trim());put("p_instructions",instructions.trim());put("p_is_active",active);put("p_sort_order",sortOrder)})
    suspend fun updatePaymentWallet(id:String,nameAr:String,nameEn:String,account:String?,beneficiary:String?,instructions:String,sortOrder:Int):Result<Unit> { val old=_paymentWallets.value.firstOrNull{it.id==id}; return rpcUnit("admin_upsert_payment_method",JSONObject().apply{put("p_id",id);put("p_type",old?.code?.takeIf{it in setOf("bank","wallet","transfer")}?:"wallet");put("p_name",nameAr.trim());put("p_recipient",beneficiary?.trim());put("p_account",account?.trim());put("p_instructions",instructions.trim());put("p_is_active",old?.isActive?:true);put("p_sort_order",sortOrder)}) }
    private suspend fun updatePaymentMethodActive(id:String,active:Boolean):Result<Unit> { val m=_paymentWallets.value.firstOrNull{it.id==id}?:return Result.failure(Exception("وسيلة الدفع غير موجودة")); return rpcUnit("admin_upsert_payment_method",JSONObject().apply{put("p_id",id);put("p_type",m.code);put("p_name",m.nameAr);put("p_recipient",m.beneficiaryName);put("p_account",m.beneficiaryAccount);put("p_instructions",m.instructionsAr);put("p_is_active",active);put("p_sort_order",m.sortOrder)}) }
    suspend fun updateSystemSettings(newSettings:SystemSettings):Result<Unit> = try {
        val pairs=listOf(
            "currency" to JSONObject().put("value",newSettings.currency),
            "timezone" to JSONObject().put("value",newSettings.timezone),
            "date_format" to JSONObject().put("value",newSettings.dateFormat),
            "time_format" to JSONObject().put("value",newSettings.timeFormat),
            "maintenance_mode" to JSONObject().put("value",newSettings.maintenanceMode)
        )
        for((k,v) in pairs){
            val r=SupabaseClient.rpc("admin_set_system_setting",JSONObject().apply{put("p_key",k);put("p_value",v)},token())
            if(r !is NetworkResult.Success) throw IllegalStateException((r as? NetworkResult.Error)?.messageAr ?: "فشل حفظ الإعدادات")
        }
        refreshAll(); Result.success(Unit)
    } catch(e:Exception){Result.failure(e)}
    suspend fun updateMyProfile(name:String,email:String?):Result<Unit> {
        val r=SupabaseClient.updateMyProfile(name,email,token())
        return when(r){ is NetworkResult.Success->{refreshAll();Result.success(Unit)}; is NetworkResult.Error->Result.failure(Exception(r.messageAr)); is NetworkResult.NetworkFailure->Result.failure(Exception(r.messageAr)); is NetworkResult.Unknown->Result.failure(Exception(r.messageAr)) }
    }

    suspend fun markNotificationAsRead(id:String){ val table=if(_currentUser.value?.userType==UserType.CUSTOMER)"client_notifications" else "admin_notifications"; val filters=if(table=="client_notifications") mapOf("id" to "eq.$id","customer_id" to "eq.${_currentUser.value?.id}") else mapOf("id" to "eq.$id"); SupabaseClient.patch(table,filters,JSONObject().put("is_read",true),token()); refreshAll() }
    suspend fun markAllNotificationsRead(){ val table=if(_currentUser.value?.userType==UserType.CUSTOMER)"client_notifications" else "admin_notifications"; val filters=if(table=="client_notifications") mapOf("customer_id" to "eq.${_currentUser.value?.id}","is_read" to "eq.false") else mapOf("is_read" to "eq.false"); SupabaseClient.patch(table,filters,JSONObject().put("is_read",true),token()); refreshAll() }
    suspend fun verifyPaymentReceipt(requestId:String,note:String):Result<Unit> {
        val cleanNote = note.trim()
        return rpcUnit("verify_request_payment", JSONObject().apply {
            put("p_request_id", requestId)
            put("p_verified", true)
            put("p_note", cleanNote.ifBlank { JSONObject.NULL })
        })
    }

    suspend fun updateTaskSettings(providerId:String,firstTask:Boolean,reschedule:Boolean,intervalDays:Int?,visibilityDaysBefore:Int,active:Boolean):Result<Unit> = rpcUnit("admin_upsert_task_settings", JSONObject().apply { put("p_provider_id",providerId);put("p_first_task_enabled",firstTask);put("p_manual_reschedule_enabled",reschedule);put("p_interval_days",intervalDays ?: JSONObject.NULL);put("p_visibility_days_before",visibilityDaysBefore);put("p_is_active",active) })
    suspend fun updateTaskClassification(id:String?,taskSettingsId:String,name:String,minDays:Int?,maxDays:Int?,sortOrder:Int,active:Boolean):Result<Unit> = rpcUnit("admin_upsert_task_classification",JSONObject().apply{put("p_id",id ?: JSONObject.NULL);put("p_task_settings_id",taskSettingsId);put("p_name",name.trim());put("p_min_days",minDays ?: JSONObject.NULL);put("p_max_days",maxDays ?: JSONObject.NULL);put("p_sort_order",sortOrder);put("p_is_active",active)})

    suspend fun updateNotificationSetting(setting:NotificationSetting):Result<Unit> = rpcUnit("admin_upsert_notification_setting", JSONObject().apply { put("p_id",setting.id);put("p_type",setting.type);put("p_recipient",setting.recipient);put("p_enabled",setting.enabled);put("p_days_before",setting.daysBefore ?: JSONObject.NULL) })
    suspend fun setEmployeeRole(userId:String,roleId:String):Result<Unit> = rpcUnit("admin_set_user_role",JSONObject().apply{put("p_user_id",userId);put("p_role_id",roleId)})
    suspend fun setEmployeeStatus(userId:String,status:String):Result<Unit> = rpcUnit("admin_set_user_status",JSONObject().apply{put("p_user_id",userId);put("p_status",status)})
    suspend fun replaceRolePermissions(roleId:String, permissionCodes:List<String>):Result<Unit> = rpcUnit("admin_replace_role_permissions", JSONObject().apply { put("p_role_id", roleId); put("p_permission_codes", JSONArray(permissionCodes)) })

    private suspend fun rpcUnit(name:String,body:JSONObject):Result<Unit> = when(val r=SupabaseClient.rpc(name,body,token())){is NetworkResult.Success->{refreshAll();Result.success(Unit)};is NetworkResult.Error->Result.failure(Exception(r.messageAr));is NetworkResult.NetworkFailure->Result.failure(Exception(r.messageAr));is NetworkResult.Unknown->Result.failure(Exception(r.messageAr))}
    fun detectProvider(phoneNumber:String):TelecomProvider?{val n=phoneNumber.filter(Char::isDigit);return _telecomProviders.value.firstOrNull{p->p.prefixes.any{n.startsWith(it)}}}
    private fun clearData(){_customerNumbers.value=emptyList();_protectionRequests.value=emptyList();_protections.value=emptyList();_paymentTasks.value=emptyList();_transactions.value=emptyList();_notifications.value=emptyList();_auditLogs.value=emptyList();_stateTransitionLogs.value=emptyList();_customers.value=emptyList();_packages.value=emptyList();_taskSettings.value=emptyList();_notificationSettings.value=emptyList();_employees.value=emptyList();_roles.value=emptyList();_isBackendConnected.value=false}
    private fun nowIso()=SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX",Locale.US).format(Date())

    private fun JSONArray.optJSONObject(i:Int):JSONObject?=if(i in 0 until length()) optJSONObject(i) else null
    private inline fun <T> JSONArray.safeMap(mapper:(JSONObject)->T):List<T> = buildList { for(i in 0 until length()) { val item=optJSONObject(i) ?: continue; runCatching { add(mapper(item)) } } }
    private fun JSONObject.text(k:String)=optString(k).ifBlank{""}
    private fun JSONObject.obj(k:String)=optJSONObject(k)
    private fun JSONObject.arr(k:String)=optJSONArray(k)?:JSONArray()
    private fun JSONObject.date(k:String)=text(k).take(10)
    private fun JSONArray.toProviderList(prefixRows:JSONArray)=safeMap { j ->
        val id = j.getString("id")
        val nameAr = j.text("name_ar").ifBlank { j.text("name") }
        val nameEn = j.text("name_en").ifBlank { j.text("name") }.ifBlank { nameAr }
        val pref = List(prefixRows.length()) { x -> prefixRows.optJSONObject(x) }
            .filter { it?.optString("provider_id") == id }
            .mapNotNull { it?.optString("prefix")?.ifBlank { null } }
        TelecomProvider(id, nameAr, nameEn, j.text("code"), pref, j.optInt("number_length", 9), 0xFF00695C, j.optBoolean("is_active", true), j.optInt("sort_order", 0), j.optBoolean("is_visible_to_customer", true), j.optString("logo_url").ifBlank { null })
    }
    private fun JSONArray.toPackageList()=safeMap { j ->ProtectionPackage(j.getString("id"),j.getString("provider_id"),j.text("name"),j.optInt("duration_days"),j.optDouble("price"),j.optString("currency").ifBlank { "YER" },j.optBoolean("is_active",true),j.optInt("sort_order",0),j.optString("description").ifBlank { null },j.optBoolean("is_visible_to_customer",true))}
    private fun JSONArray.toPaymentMethodList()=safeMap { j ->PaymentWallet(j.getString("id"),j.text("name"),j.text("name"),j.text("type"),j.text("account_number"),j.optString("account_name").ifBlank { j.optString("recipient_name") },j.optString("instructions"),j.optBoolean("is_active",true),j.optInt("sort_order",0))}
    private fun JSONArray.toCustomerNumbers() = safeMap { j ->
        // customer_my_numbers uses the canonical view columns (number,
        // provider_name_ar, created_at). Keep the legacy aliases so older
        // deployments continue to work as well.
        val number = j.optString("number")
            .ifBlank { j.optString("display_number") }
            .ifBlank { j.optString("normalized_number") }
        val providerName = j.optString("provider_name_ar")
            .ifBlank { j.optString("provider_name") }
            .ifBlank { j.optString("provider_name_en") }
        val createdAt = j.optString("created_at")
            .ifBlank { j.optString("added_at") }
        val protectionStatus = j.optString("protection_status").lowercase()
        CustomerNumber(
            id = j.optString("id").ifBlank { j.optString("phone_number_id") },
            customerId = j.optString("customer_id"),
            phoneNumber = number,
            providerId = j.optString("provider_id"),
            providerNameAr = providerName,
            status = if (j.has("is_active")) {
                if (j.optBoolean("is_active", true)) "active" else "inactive"
            } else j.optString("status", "active"),
            isProtected = j.optBoolean("is_protected") ||
                j.optString("active_protection_id").isNotBlank() ||
                protectionStatus == "active",
            createdAt = createdAt
        )
    }
    private fun JSONArray.toCustomers()=safeMap { j ->CustomerAccount(j.getString("id"),j.text("full_name"),j.text("email"),j.optString("phone"),j.optString("status")=="suspended",0,0,j.date("created_at"))}
    private fun JSONArray.toProtectionRequests()=safeMap { j ->
        val cn=j.obj("customer_numbers"); val pn=cn?.obj("phone_numbers"); val tp=pn?.obj("telecom_providers"); val pm=j.obj("payment_methods"); val pkg=j.obj("packages")
        val status=runCatching { RequestStatus.valueOf(j.optString("status","pending").substringBefore('_').uppercase()) }.getOrDefault(RequestStatus.PENDING)
        ProtectionRequest(id=j.optString("id"),customerId=j.optString("customer_id").ifBlank { cn?.optString("customer_id").orEmpty() },customerName=j.optString("customer_name"),numberId=j.optString("customer_number_id").ifBlank { j.optString("phone_number_id") },phoneNumber=j.optString("phone_number").ifBlank { pn?.optString("number").orEmpty() },providerId=j.optString("provider_id").ifBlank { pn?.optString("provider_id").orEmpty() },providerNameAr=tp?.optString("name_ar").orEmpty().ifBlank { j.optString("provider_name_snapshot") },walletId=j.optString("payment_method_id"),walletNameAr=pm?.optString("name").orEmpty().ifBlank { j.optString("payment_method_name_snapshot") },paymentReference=j.optString("transfer_reference").ifBlank { j.optString("payment_reference") },priceSnapshot=j.optDouble("price_snapshot",j.optDouble("package_price_snapshot",pkg?.optDouble("price",0.0) ?: 0.0)),durationDaysSnapshot=j.optInt("duration_days_snapshot",j.optInt("package_duration_days_snapshot",pkg?.optInt("duration_days",0) ?: 0)),currencySnapshot=j.optString("currency_snapshot").ifBlank { j.optString("package_currency_snapshot").ifBlank { "YER" } },paymentStatus=j.optString("payment_status","pending"),requestType=j.optString("request_type","new"),previousProtectionId=j.optString("previous_protection_id").ifBlank{null},status=status,rejectionReason=j.optString("rejection_reason").ifBlank{null},verifiedAt=j.optString("reviewed_at").ifBlank { j.optString("payment_verified_at").ifBlank { null } },createdAt=j.date("created_at"))
    }
    private fun JSONArray.toProtections()=safeMap { j ->
        val cn=j.obj("customer_numbers"); val pn=cn?.obj("phone_numbers"); val tp=pn?.obj("telecom_providers"); val pkg=j.obj("packages")
        Protection(id=j.optString("id"),requestId=j.optString("request_id"),customerId=j.optString("customer_id").ifBlank { cn?.optString("customer_id").orEmpty() },numberId=j.optString("customer_number_id").ifBlank { j.optString("phone_number_id") },phoneNumber=j.optString("number").ifBlank { pn?.optString("number").orEmpty() }.ifBlank { j.optString("phone_number") },providerNameAr=j.optString("provider_name").ifBlank { j.optString("provider_name_snapshot") }.ifBlank { tp?.optString("name_ar").orEmpty() },status=j.optString("status","active"),startDate=j.optString("starts_at").ifBlank { j.optString("start_at") },endDate=j.optString("expires_at").ifBlank { j.optString("end_at") },durationDaysSnapshot=j.optInt("package_duration_days_snapshot",pkg?.optInt("duration_days",0) ?: 0),priceSnapshot=j.optDouble("package_price_snapshot",pkg?.optDouble("price",0.0) ?: 0.0))
    }
    private fun JSONArray.toPaymentTasks()=safeMap { j ->
        val stored=TaskStatus.fromValue(j.optString("status"))
        PaymentTask(id=j.optString("id"), protectionId=j.optString("protection_id"), numberId=j.optString("customer_number_id").ifBlank { j.optString("number_id") }, phoneNumber=j.optString("number").ifBlank { j.optString("phone_number") }, providerId=j.optString("provider_id"), providerNameAr=j.optString("provider_name").ifBlank { j.optString("provider_name_ar") }, cycleNumber=j.optInt("cycle_number",1), dueDate=j.date("due_at"), amountSnapshot=j.optDouble("amount_snapshot",0.0), status=stored, completedAt=j.optString("completed_at").ifBlank{null}, paymentReference=j.optString("telecom_reference").ifBlank{null}, notes=j.optString("rescheduled_reason").ifBlank{null}, taskType=j.optString("task_type","recurring"), telecomDueAt=j.optString("telecom_due_at").ifBlank{null}, daysRemaining=if(j.has("days_remaining")&&!j.isNull("days_remaining")) j.optInt("days_remaining") else null, classificationName=j.optString("classification_name").ifBlank{null})
    }
    private fun JSONArray.toTransactions()=safeMap { j ->FinancialTransaction(j.getString("id"),j.text("tx_type"),j.optDouble("amount"),j.text("currency"),j.optString("description",j.text("source_type")),j.optString("reference").ifBlank{null},j.text("source_type"),j.optString("source_id").ifBlank{null},j.date("created_at"))}
    private fun JSONArray.toNotifications()=safeMap { j -> val relatedType=j.optString("related_entity_type").ifBlank { "" }; val route=when(relatedType){"protection_request"->"requests";"payment_task"->"tasks";"protection"->"protections";"audit_log"->"audit";else->null}; NotificationItem(j.getString("id"),null,j.text("title"),j.text("message"),j.optBoolean("is_read"),j.date("created_at"),route,j.text("type"),relatedType.ifBlank { null },j.optString("related_entity_id").ifBlank { null })}
    private fun JSONArray.toAuditLogs()=safeMap { j ->AuditLog(j.getString("id"),j.text("action"),j.text("entity_type"),j.optString("entity_id").ifBlank{null},"",j.optString("actor_id"),j.optString("metadata",j.optString("after_data")),j.date("created_at"),j.optString("before_data").ifBlank { null },j.optString("after_data").ifBlank { null },j.optString("metadata").ifBlank { null })}
    private fun JSONArray.toTaskSettings()=safeMap { j ->TaskSettings(j.getString("id"),j.getString("provider_id"),j.optBoolean("first_task_enabled",true),j.optBoolean("manual_reschedule_enabled",true),if(j.isNull("default_interval_days"))null else j.optInt("default_interval_days"),j.optBoolean("is_active",true),j.optInt("visibility_days_before",30))}
    private fun JSONArray.toNotificationSettings()=safeMap { j ->NotificationSetting(j.getString("id"),j.text("type"),j.text("recipient"),j.optBoolean("enabled",true),if(j.isNull("days_before"))null else j.optInt("days_before"))}
    private fun JSONArray.toEmployees()=safeMap { j ->EmployeeAccount(j.getString("id"),j.text("full_name"),j.text("email"),j.text("status"),j.optString("role_id").ifBlank{null},j.obj("roles")?.text("name"))}
    private fun JSONArray.toSettings():SystemSettings{
        var currency="YER"; var timezone="Asia/Aden"; var dateFormat="yyyy-MM-dd"; var timeFormat="24h"; var maintenance=false
        for(i in 0 until length()){ val j=optJSONObject(i) ?: continue; val key=j.text("key"); val v=j.obj("value")?.opt("value") ?: j.opt("value")
            when(key){
                "currency" -> currency=v?.toString()?.trim('\"')?.ifBlank{"YER"} ?: "YER"
                "timezone" -> timezone=v?.toString()?.trim('\"')?.ifBlank{"Asia/Aden"} ?: "Asia/Aden"
                "date_format" -> dateFormat=v?.toString()?.trim('\"')?.ifBlank{"yyyy-MM-dd"} ?: "yyyy-MM-dd"
                "time_format" -> timeFormat=v?.toString()?.trim('\"')?.ifBlank{"24h"} ?: "24h"
                "maintenance_mode" -> maintenance=(v?.toString()?.lowercase()=="true")
            }
        }
        return SystemSettings(currency,timezone,dateFormat,timeFormat,maintenance)
    }
}
