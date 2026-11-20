// data/FirebaseRepository.kt
package com.hambaksanta.merrycri.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** v2 Firebase 진입점. 현재는 앱 시작 시 익명 로그인만 담당한다. 담당: B */
class FirebaseRepository(context: Context) {
    val isAvailable: Boolean = FirebaseApp.getApps(context).isNotEmpty()

    private val _authState = MutableStateFlow<AuthState>(
        if (isAvailable) AuthState.Loading else AuthState.Offline,
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var uid: String? = null
        private set

    fun signIn(onDone: (Boolean) -> Unit = {}) {
        if (!isAvailable) {
            _authState.value = AuthState.Offline
            onDone(false)
            return
        }

        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let { user ->
            uid = user.uid
            _authState.value = AuthState.SignedIn
            onDone(true)
            return
        }

        _authState.value = AuthState.Loading
        auth.signInAnonymously().addOnCompleteListener { task ->
            uid = auth.currentUser?.uid
            val success = task.isSuccessful && uid != null
            _authState.value = if (success) AuthState.SignedIn else AuthState.Error
            if (!success) Log.w(TAG, "anonymous sign-in failed", task.exception)
            onDone(success)
        }
    }

    private companion object {
        const val TAG = "FirebaseRepository"
    }
}
