// data/FirebaseRepository.kt
package com.hambaksanta.merrycri.data

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.ServerValue
import com.google.firebase.database.Transaction
import com.hambaksanta.merrycri.data.model.UserProfile
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine

/** v2 Firebase 진입점. 인증과 사용자 프로필·친구 코드 생성을 담당한다. 담당: B */
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

    /** 로그인한 사용자의 프로필을 읽는다. 아직 생성 전이면 null이다. */
    suspend fun getCurrentUser(): UserProfile? {
        val userId = requireUid()
        val snapshot = requireDatabase().child(USERS).child(userId).get().awaitResult()
        return snapshot.toUserProfile()
    }

    /** 친구 코드를 트랜잭션으로 선점한 뒤 사용자 프로필을 생성한다. */
    suspend fun createUser(nickname: String): UserProfile {
        val cleanNickname = nickname.trim()
        require(cleanNickname.isNotEmpty()) { "nickname must not be blank" }
        getCurrentUser()?.let { return it }

        val userId = requireUid()
        val root = requireDatabase()
        repeat(MAX_CODE_ATTEMPTS) {
            val code = generateFriendCode()
            if (!reserveFriendCode(root, userId, code)) return@repeat

            return try {
                val userRef = root.child(USERS).child(userId)
                userRef.setValue(
                    mapOf(
                        "nickname" to cleanNickname,
                        "friendCode" to code,
                        "createdAt" to ServerValue.TIMESTAMP,
                    ),
                ).awaitResult()
                requireNotNull(userRef.get().awaitResult().toUserProfile())
            } catch (error: Throwable) {
                runCatching { root.child(FRIEND_CODES).child(code).removeValue().awaitResult() }
                throw error
            }
        }
        throw IllegalStateException("친구 코드를 발급하지 못했습니다.")
    }

    private suspend fun reserveFriendCode(
        root: DatabaseReference,
        userId: String,
        code: String,
    ): Boolean = suspendCancellableCoroutine { continuation ->
        root.child(FRIEND_CODES).child(code).runTransaction(
            object : Transaction.Handler {
                override fun doTransaction(currentData: MutableData): Transaction.Result {
                    if (currentData.value != null) return Transaction.abort()
                    currentData.value = userId
                    return Transaction.success(currentData)
                }

                override fun onComplete(
                    error: DatabaseError?,
                    committed: Boolean,
                    currentData: DataSnapshot?,
                ) {
                    if (!continuation.isActive) return
                    if (error != null) continuation.resumeWithException(error.toException())
                    else continuation.resume(committed)
                }
            },
        )
    }

    private fun requireUid(): String = uid ?: FirebaseAuth.getInstance().currentUser?.uid
        ?: throw IllegalStateException("로그인이 필요합니다.")

    private fun requireDatabase(): DatabaseReference = if (isAvailable) {
        FirebaseDatabase.getInstance().reference
    } else {
        throw IllegalStateException("Firebase를 사용할 수 없습니다.")
    }

    private fun generateFriendCode(): String = buildString(FRIEND_CODE_LENGTH) {
        repeat(FRIEND_CODE_LENGTH) {
            append(FRIEND_CODE_ALPHABET[random.nextInt(FRIEND_CODE_ALPHABET.length)])
        }
    }

    private fun DataSnapshot.toUserProfile(): UserProfile? {
        if (!exists()) return null
        val nickname = child("nickname").getValue(String::class.java) ?: return null
        val friendCode = child("friendCode").getValue(String::class.java) ?: return null
        val createdAt = child("createdAt").getValue(Long::class.java) ?: 0L
        return UserProfile(nickname, friendCode, createdAt)
    }

    private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
        addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) continuation.resume(task.result)
            else continuation.resumeWithException(
                task.exception ?: IllegalStateException("Firebase 요청 실패"),
            )
        }
    }

    private companion object {
        const val TAG = "FirebaseRepository"
        const val USERS = "users"
        const val FRIEND_CODES = "friendCodes"
        const val FRIEND_CODE_LENGTH = 6
        const val MAX_CODE_ATTEMPTS = 20
        const val FRIEND_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
    }
}
