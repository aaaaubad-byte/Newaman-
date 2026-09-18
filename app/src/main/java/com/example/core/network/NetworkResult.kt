package com.example.core.network

sealed class NetworkResult<out T> {
    data class Success<out T>(val data: T) : NetworkResult<T>()
    data class Error(val code: Int, val messageAr: String, val details: String? = null) : NetworkResult<Nothing>()
    data class NetworkFailure(val exception: Throwable, val messageAr: String = "تعذر الاتصال بالخادم، يرجى التحقق من اتصال الإنترنت") : NetworkResult<Nothing>()
    data class Unknown(val messageAr: String = "حدث خطأ غير متوقع، يرجى إعادة المحاولة") : NetworkResult<Nothing>()

    val isSuccess: Boolean get() = this is Success

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (messageAr: String) -> Unit): NetworkResult<T> {
        when (this) {
            is Error -> action(messageAr)
            is NetworkFailure -> action(messageAr)
            is Unknown -> action(messageAr)
            is Success -> Unit
        }
        return this
    }
}
