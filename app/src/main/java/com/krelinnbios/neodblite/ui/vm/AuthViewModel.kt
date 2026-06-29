package com.krelinnbios.neodblite.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.krelinnbios.neodblite.data.model.NeoUser
import com.krelinnbios.neodblite.global.App
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val user: NeoUser) : AuthState
}

class AuthViewModel : ViewModel() {
    private val container = App.container

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    /** 登录页提示信息（错误/进行中）。 */
    private val _loginMessage = MutableStateFlow<String?>(null)
    val loginMessage: StateFlow<String?> = _loginMessage.asStateFlow()

    val currentHost: String get() = container.authStore.cachedHost

    init {
        viewModelScope.launch {
            container.bootstrap()
            refresh()
        }
    }

    fun refresh() {
        viewModelScope.launch {
            if (container.authStore.cachedToken.isNullOrBlank()) {
                _authState.value = AuthState.LoggedOut
                return@launch
            }
            container.repository.me()
                .onSuccess { _authState.value = AuthState.LoggedIn(it) }
                .onFailure {
                    // token 失效或网络异常：保守地退回登录态，让用户重新授权。
                    _authState.value = AuthState.LoggedOut
                }
        }
    }

    /** 准备授权 URL，成功回调交给 UI 打开浏览器。 */
    fun beginLogin(host: String, onAuthorizeUrl: (String) -> Unit) {
        _loginMessage.value = "正在准备授权…"
        viewModelScope.launch {
            container.authRepository.beginLogin(host)
                .onSuccess {
                    _loginMessage.value = null
                    onAuthorizeUrl(it)
                }
                .onFailure { _loginMessage.value = "无法连接该实例：${it.message ?: "未知错误"}" }
        }
    }

    /** 处理 OAuth 回调中的授权 code。 */
    fun handleAuthCode(code: String) {
        _authState.value = AuthState.Loading
        _loginMessage.value = "正在登录…"
        viewModelScope.launch {
            container.authRepository.completeLogin(code)
                .onSuccess {
                    _loginMessage.value = null
                    refresh()
                }
                .onFailure {
                    _loginMessage.value = "登录失败：${it.message ?: "未知错误"}"
                    _authState.value = AuthState.LoggedOut
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            container.authRepository.logout()
            _authState.value = AuthState.LoggedOut
        }
    }
}
