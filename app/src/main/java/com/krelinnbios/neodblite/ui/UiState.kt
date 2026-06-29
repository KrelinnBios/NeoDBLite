package com.krelinnbios.neodblite.ui

/** 通用加载状态。 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

fun Throwable.friendlyMessage(): String =
    message?.takeIf { it.isNotBlank() } ?: "网络异常，请稍后重试"
