package com.example.core.network

import android.net.Uri
import com.example.data.model.AdminRole
import com.example.data.model.CustomerAccount
import com.example.data.model.User
import com.example.data.model.UserType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class SupabaseAuthResponse(
    val accessToken: String,
    val refreshToken: String?,
    val user: User
)

object SupabaseClient {
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    data class RestResponse(val code: Int, val body: String, val successful: Boolean)

    suspend fun checkHealth(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = rest("GET", "/rest/v1/telecom_providers", query = mapOf("select" to "id", "limit" to "1"), accessToken = com.example.core.auth.SessionManager.getAccessToken())
            response.successful
        } catch (_: Exception) { false }
    }

    fun normalizePhoneForAuth(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return when {
            raw.startsWith("+") -> raw.trim()
            raw.startsWith("00") -> "+" + raw.substring(2).trim()
            digits.length == 9 && (digits.startsWith("7") || digits.startsWith("1")) -> "+967" + digits
            digits.length == 10 && digits.startsWith("0") -> "+967" + digits.substring(1)
            digits.length == 12 && digits.startsWith("967") -> "+" + digits
            else -> if (digits.isNotBlank()) "+967" + digits else raw.trim()
        }
    }

    fun extractLocalPhone(raw: String): String {
        val digits = raw.filter { it.isDigit() }
        return if (digits.length >= 9) digits.takeLast(9) else digits
    }

    suspend fun signUp(email: String, pass: String, fullName: String): NetworkResult<SupabaseAuthResponse> =
        withContext(Dispatchers.IO) {
            try {
                val body = JSONObject().apply {
                    put("email", email.trim().lowercase())
                    put("password", pass)
                    put("data", JSONObject().apply {
                        put("full_name", fullName.trim())
                        put("user_type", "customer")
                    })
                }
                val response = rest("POST", "/auth/v1/signup", body, authenticated = false)
                if (!response.successful) {
                    val respBody = response.body.lowercase()
                    if (response.code == 422 || response.code == 400 || response.code == 409 ||
                        respBody.contains("already registered") || respBody.contains("already exists") ||
                        respBody.contains("duplicate") || respBody.contains("email")) {
                        return@withContext NetworkResult.Error(409, "البريد الإلكتروني مسجل مسبقاً في أمان. يمكنك تسجيل الدخول أو استعادة الحساب.")
                    }
                    return@withContext errorResult(response, "فشل إنشاء الحساب")
                }
                val json = JSONObject(response.body)
                var access = json.optString("access_token")
                var refresh = json.optString("refresh_token").ifBlank { null }
                val userObj = json.optJSONObject("user")
                val userId = userObj?.optString("id").orEmpty()

                if (access.isBlank()) {
                    val autoLogin = signIn(email, pass)
                    if (autoLogin is NetworkResult.Success) {
                        return@withContext autoLogin
                    }
                }

                if (access.isNotBlank()) {
                    val actor = currentActor(access)
                    val resolvedUser = if (actor is NetworkResult.Success) {
                        actor.data
                    } else {
                        User(
                            id = userId,
                            email = email.trim().lowercase(),
                            fullName = fullName.trim(),
                            phone = null,
                            userType = UserType.CUSTOMER,
                            status = "active"
                        )
                    }
                    return@withContext NetworkResult.Success(SupabaseAuthResponse(access, refresh, resolvedUser))
                }

                NetworkResult.Error(502, "تم إنشاء الحساب ولكن تعذر فتح الجلسة تلقائياً. يرجى تسجيل الدخول.")
            } catch (e: IOException) {
                NetworkResult.NetworkFailure(e)
            } catch (e: Exception) {
                NetworkResult.Unknown("حدث خطأ أثناء إنشاء الحساب")
            }
        }

    suspend fun signIn(email: String, pass: String): NetworkResult<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("email", email.trim().lowercase())
                put("password", pass)
            }
            val response = rest("POST", "/auth/v1/token?grant_type=password", body, authenticated = false)
            if (!response.successful) return@withContext errorResult(response, "البريد الإلكتروني أو كلمة المرور غير صحيحة")
            val json = JSONObject(response.body)
            val access = json.optString("access_token")
            val refresh = json.optString("refresh_token").ifBlank { null }
            if (access.isBlank()) return@withContext NetworkResult.Error(502, "لم يتم استلام جلسة صالحة من الخادم")
            val userObj = json.optJSONObject("user") ?: return@withContext NetworkResult.Error(502, "لم يتم استلام بيانات المستخدم")
            val actor = currentActor(access)
            if (actor !is NetworkResult.Success) return@withContext actor.mapError()
            NetworkResult.Success(SupabaseAuthResponse(access, refresh, actor.data))
        } catch (e: IOException) {
            NetworkResult.NetworkFailure(e)
        } catch (e: Exception) {
            NetworkResult.Unknown("حدث خطأ أثناء تسجيل الدخول")
        }
    }

    suspend fun recoverPassword(email: String): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("email", email.trim().lowercase()) }
            val response = rest("POST", "/auth/v1/recover", body, authenticated = false)
            if (response.successful) NetworkResult.Success(Unit)
            else errorResult(response, "تعذر إرسال رابط استعادة الحساب")
        } catch (e: IOException) { NetworkResult.NetworkFailure(e) }
        catch (_: Exception) { NetworkResult.Unknown("تعذر إرسال طلب استعادة الحساب") }
    }

    suspend fun signOut(token: String? = null): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = rest("POST", "/auth/v1/logout", JSONObject(), token)
            if (response.successful) NetworkResult.Success(Unit) else errorResult(response, "تعذر إنهاء الجلسة")
        } catch (e: IOException) { NetworkResult.NetworkFailure(e) }
        catch (_: Exception) { NetworkResult.Unknown("تعذر إنهاء الجلسة") }
    }

    suspend fun refreshSession(refreshToken: String): NetworkResult<SupabaseAuthResponse> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("refresh_token", refreshToken) }
            val response = rest("POST", "/auth/v1/token?grant_type=refresh_token", body, authenticated = false)
            if (!response.successful) return@withContext errorResult(response, "انتهت جلسة الدخول، يرجى تسجيل الدخول من جديد")
            val json = JSONObject(response.body)
            val access = json.optString("access_token")
            val refresh = json.optString("refresh_token").ifBlank { refreshToken }
            val userObj = json.optJSONObject("user") ?: return@withContext NetworkResult.Error(502, "جلسة غير صالحة")
            if (access.isBlank()) return@withContext NetworkResult.Error(502, "جلسة غير صالحة")
            val actor = currentActor(access)
            if (actor !is NetworkResult.Success) return@withContext actor.mapError()
            NetworkResult.Success(SupabaseAuthResponse(access, refresh, actor.data))
        } catch (e: IOException) { NetworkResult.NetworkFailure(e) }
        catch (_: Exception) { NetworkResult.Unknown("تعذر تحديث الجلسة") }
    }

    suspend fun currentActor(accessToken: String? = null): NetworkResult<User> = withContext(Dispatchers.IO) {
        try {
            val response = rest("POST", "/rest/v1/rpc/get_current_actor", JSONObject(), accessToken)
            if (!response.successful) return@withContext errorResult(response, "تعذر التحقق من حساب أمان")
            val raw = response.body.trim()
            if (raw.isBlank()) return@withContext NetworkResult.Error(502, "استجاب الخادم دون بيانات الحساب")

            val json = try {
                if (raw.startsWith("[")) {
                    val array = JSONArray(raw)
                    if (array.length() == 0) null else array.optJSONObject(0)
                } else {
                    JSONObject(raw)
                }
            } catch (_: Exception) {
                null
            } ?: return@withContext NetworkResult.Error(502, "تعذر قراءة بيانات حساب أمان")

            val type = json.optString("user_type").trim().lowercase()
            val role = adminRoleFrom(json)
            val resolvedType = when {
                type == "admin" -> UserType.ADMIN
                type == "customer" -> UserType.CUSTOMER
                role != null -> UserType.ADMIN
                else -> null
            } ?: return@withContext NetworkResult.Error(403, "الحساب غير مصرح له باستخدام أمان")

            val status = json.optString("status", "active").trim().lowercase()
            if (status.isNotBlank() && status != "active") {
                return@withContext NetworkResult.Error(403, "حساب أمان غير نشط")
            }

            val user = User(
                id = json.optString("id").trim(),
                email = json.optString("email").trim(),
                fullName = json.optString("full_name").ifBlank { "مستخدم أمان" },
                phone = json.optString("phone").ifBlank { null },
                userType = resolvedType,
                role = if (resolvedType == UserType.ADMIN) role else null,
                status = status.ifBlank { "active" }
            )
            if (user.id.isBlank()) NetworkResult.Error(502, "تعذر تحديد هوية المستخدم") else NetworkResult.Success(user)
        } catch (e: IOException) { NetworkResult.NetworkFailure(e) }
        catch (_: Exception) { NetworkResult.Unknown("تعذر التحقق من حساب أمان") }
    }

    private fun adminRoleFrom(json: JSONObject): AdminRole? {
        val raw = json.optString("role_code").ifBlank { json.optString("role_name") }
        val normalized = raw.trim().lowercase().replace('-', '_').replace(' ', '_')
        return AdminRole.fromCode(normalized) ?: when (normalized) {
            "super_admin", "superadmin" -> AdminRole.SUPER_ADMIN
            "finance_officer", "financeofficer" -> AdminRole.FINANCE_OFFICER
            "operations_officer", "operation_officer", "operationsofficer" -> AdminRole.OPERATIONS_OFFICER
            "support" -> AdminRole.SUPPORT
            else -> null
        }
    }

    suspend fun updateMyProfile(name: String, phone: String?, token: String? = null): NetworkResult<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply { put("p_full_name", name.trim()); put("p_phone", phone?.trim()?.ifBlank { null } ?: JSONObject.NULL) }
            val response = rest("POST", "/rest/v1/rpc/update_my_profile", body, token)
            if (response.successful) NetworkResult.Success(Unit) else errorResult(response, "تعذر حفظ بيانات الحساب")
        } catch (e: IOException) { NetworkResult.NetworkFailure(e) } catch (_: Exception) { NetworkResult.Unknown("تعذر حفظ بيانات الحساب") }
    }

    suspend fun get(table: String, query: Map<String, String> = emptyMap(), token: String? = null): NetworkResult<JSONArray> = withContext(Dispatchers.IO) {
        try {
            val response = restWithRefresh("GET", "/rest/v1/$table", query = query, accessToken = token)
            if (response.successful) NetworkResult.Success(JSONArray(if (response.body.isBlank()) "[]" else response.body)) else errorResult(response, "تعذر جلب البيانات")
        } catch (e: IOException) { NetworkResult.NetworkFailure(e) }
        catch (_: Exception) { NetworkResult.Unknown("تعذر جلب البيانات") }
    }

    suspend fun rpc(name: String, body: JSONObject = JSONObject(), token: String? = null): NetworkResult<JSONObject> = withContext(Dispatchers.IO) {
        try {
            val response = restWithRefresh("POST", "/rest/v1/rpc/$name", body, token)
            if (!response.successful) return@withContext errorResult(response, "فشلت العملية")
            val raw = response.body.trim()
            val json = when {
                raw.isBlank() -> JSONObject()
                raw.startsWith("[") -> JSONObject().put("data", JSONArray(raw))
                raw.startsWith("{") -> JSONObject(raw)
                raw.equals("null", ignoreCase = true) -> JSONObject().put("data", JSONObject.NULL)
                else -> JSONObject().put("data", raw.trim().trim('"'))
            }
            NetworkResult.Success(json)
        } catch (e: IOException) { NetworkResult.NetworkFailure(e) }
        catch (_: Exception) { NetworkResult.Unknown("فشلت العملية") }
    }

    suspend fun patch(table: String, filters: Map<String, String>, body: JSONObject, token: String? = null): NetworkResult<JSONArray> = withContext(Dispatchers.IO) {
        try {
            val response = restWithRefresh("PATCH", "/rest/v1/$table", body, token, query = filters + mapOf("select" to "*"))
            if (response.successful) NetworkResult.Success(JSONArray(if (response.body.isBlank()) "[]" else response.body)) else errorResult(response, "فشل تحديث البيانات")
        } catch (e: IOException) { NetworkResult.NetworkFailure(e) }
        catch (_: Exception) { NetworkResult.Unknown("فشل تحديث البيانات") }
    }

    private suspend fun restWithRefresh(
        method: String,
        path: String,
        body: JSONObject? = null,
        accessToken: String? = null,
        query: Map<String, String> = emptyMap()
    ): RestResponse {
        val first = rest(method, path, body, accessToken, query = query)
        if (first.code != 401) return first

        val refreshToken = com.example.core.auth.SessionManager.getRefreshToken() ?: return first
        val refreshed = refreshSession(refreshToken)
        if (refreshed !is NetworkResult.Success) return first
        com.example.core.auth.SessionManager.saveSession(
            refreshed.data.accessToken,
            refreshed.data.refreshToken,
            refreshed.data.user
        )
        return rest(method, path, body, refreshed.data.accessToken, query = query)
    }

    private fun rest(method: String, path: String, body: JSONObject? = null, accessToken: String? = null, authenticated: Boolean = true, query: Map<String, String> = emptyMap()): RestResponse {
        if (SupabaseConfig.supabaseUrl.isBlank() || SupabaseConfig.supabaseAnonKey.isBlank()) {
            return RestResponse(503, "{\"message\":\"إعدادات الاتصال بقاعدة البيانات غير موجودة في نسخة التطبيق\"}", false)
        }
        val base = (SupabaseConfig.supabaseUrl.trimEnd('/') + path).toUriWithQuery(query)
        val builder = Request.Builder().url(base).addHeader("apikey", SupabaseConfig.supabaseAnonKey)
            .addHeader("Accept", "application/json")
        val token = accessToken ?: if (authenticated) com.example.core.auth.SessionManager.getAccessToken() else null
        if (!token.isNullOrBlank()) builder.addHeader("Authorization", "Bearer $token")
        if (body != null) builder.addHeader("Content-Type", "application/json").method(method, body.toString().toRequestBody(jsonMediaType))
        else builder.method(method, null)
        client.newCall(builder.build()).execute().use { response ->
            return RestResponse(response.code, response.body?.string().orEmpty(), response.isSuccessful)
        }
    }

    private fun String.toUriWithQuery(query: Map<String, String>): String {
        if (query.isEmpty()) return this
        val b = Uri.parse(this).buildUpon()
        query.forEach { (k,v) -> b.appendQueryParameter(k,v) }
        return b.build().toString()
    }

    private fun errorResult(response: RestResponse, fallback: String): NetworkResult.Error = NetworkResult.Error(response.code, parseErrorMessage(response.body, fallback), response.body.takeIf { it.isNotBlank() })

    private fun parseErrorMessage(body: String, fallback: String): String = try {
        val j = JSONObject(body)
        val rawMsg = when {
            j.optString("message").isNotBlank() -> j.optString("message")
            j.optString("msg").isNotBlank() -> j.optString("msg")
            j.optString("error_description").isNotBlank() -> j.optString("error_description")
            else -> fallback
        }
        when {
            rawMsg.contains("إعدادات الاتصال بقاعدة البيانات", ignoreCase = true) -> "نسخة التطبيق غير مرتبطة بقاعدة البيانات. أعد تثبيت النسخة المرتبطة بـ Supabase."
            rawMsg.contains("Invalid login credentials", ignoreCase = true) -> "رقم الهاتف أو كلمة المرور غير صحيحة"
            rawMsg.contains("already registered", ignoreCase = true) || rawMsg.contains("already exists", ignoreCase = true) -> "رقم الهاتف مسجل مسبقاً في أمان. يمكنك تسجيل الدخول أو استعادة حسابك."
            rawMsg.contains("Invalid phone", ignoreCase = true) -> "صيغة رقم الهاتف غير صالحة"
            rawMsg.contains("Password should be", ignoreCase = true) -> "كلمة المرور يجب أن تكون من 6 خانات على الأقل"
            rawMsg.contains("Email not confirmed", ignoreCase = true) -> "يجب تأكيد البريد الإلكتروني قبل تسجيل الدخول"
            rawMsg.contains("User already registered", ignoreCase = true) -> "البريد الإلكتروني مسجل مسبقاً. استخدم تسجيل الدخول أو استعادة كلمة المرور."
            rawMsg.contains("Failed to fetch", ignoreCase = true) -> "تعذر الوصول إلى خادم Supabase. تحقق من الإنترنت وحاول مرة أخرى."
            else -> rawMsg
        }
    } catch (_: Exception) { fallback }

    private fun <T> NetworkResult<T>.mapError(): NetworkResult<Nothing> = when (this) {
        is NetworkResult.Error -> this
        is NetworkResult.NetworkFailure -> this
        is NetworkResult.Unknown -> this
        is NetworkResult.Success -> NetworkResult.Unknown("تعذر التحقق من الحساب")
    }
}
