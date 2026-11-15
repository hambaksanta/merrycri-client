// data/AuthState.kt
package com.hambaksanta.merrycri.data

/** 앱 시작 시 Firebase 익명 로그인 진행 상태. */
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedIn : AuthState
    data object Offline : AuthState
    data object Error : AuthState
}
