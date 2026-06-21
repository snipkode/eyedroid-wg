package com.eyedroid.vpn.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eyedroid.vpn.data.repository.AuthRepository
import com.eyedroid.vpn.data.session.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class LoginViewModel(
    private val auth: AuthRepository,
    val session: SessionManager
) : ViewModel() {

    sealed class State {
        object Idle : State()
        object Loading : State()
        object Success : State()
        data class Error(val msg: String) : State()
    }

    private val _state = MutableStateFlow<State>(State.Idle)
    val state: StateFlow<State> = _state

    fun login(username: String, password: String) = viewModelScope.launch {
        _state.value = State.Loading
        auth.login(username, password)
            .onSuccess { _state.value = State.Success }
            .onFailure { _state.value = State.Error(it.message ?: "Login failed") }
    }

    fun checkSession(onValid: () -> Unit, onInvalid: () -> Unit) = viewModelScope.launch {
        if (session.isLoggedIn && auth.validateSession()) onValid() else onInvalid()
    }
}
