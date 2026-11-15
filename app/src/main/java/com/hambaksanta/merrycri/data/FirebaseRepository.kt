// data/FirebaseRepository.kt
package com.hambaksanta.merrycri.data

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.database.ValueEventListener
import com.hambaksanta.merrycri.core.character.Outfit
import com.hambaksanta.merrycri.data.model.DbPath
import com.hambaksanta.merrycri.data.model.Presence
import com.hambaksanta.merrycri.data.model.SnowstormEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf

/**
 * Firebase 읽기/쓰기는 전부 여기서만 한다. 담당: B
 * google-services.json 이 없으면 isAvailable = false 이고, 모든 함수가 조용히 아무것도 안 한다 (오프라인 모드).
 * 구독 함수는 전부 Flow 로 돌려주고, Flow 가 끝나면(awaitClose) 리스너를 해제한다.
 *
 * TODO(B, 단계 1~2): tree/ornaments, gifts·demo/gifts, lounge/fire(runTransaction), snowmen 함수 추가
 */
class FirebaseRepository(context: Context) {
    val isAvailable: Boolean = FirebaseApp.getApps(context).isNotEmpty()

    private val _authState = MutableStateFlow<AuthState>(
        if (isAvailable) AuthState.Loading else AuthState.Offline,
    )
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    var uid: String? = null
        private set

    private val db: DatabaseReference? =
        if (isAvailable) FirebaseDatabase.getInstance().reference else null

    fun signIn(onDone: (Boolean) -> Unit = {}) {
        if (!isAvailable) {
            _authState.value = AuthState.Offline
            onDone(false)
            return
        }

        _authState.value = AuthState.Loading
        val auth = FirebaseAuth.getInstance()
        auth.currentUser?.let {
            uid = it.uid
            _authState.value = AuthState.SignedIn
            onDone(true)
            return
        }

        auth.signInAnonymously().addOnCompleteListener { task ->
            uid = auth.currentUser?.uid
            if (task.isSuccessful && uid != null) {
                _authState.value = AuthState.SignedIn
                onDone(true)
            } else {
                Log.w(TAG, "anonymous sign-in failed", task.exception)
                _authState.value = AuthState.Error
                onDone(false)
            }
        }
    }

    /** users/{uid} */
    fun saveUser(nick: String, outfit: Outfit) {
        val u = uid ?: return
        db?.child(DbPath.USERS)?.child(u)?.setValue(mapOf("nick" to nick, "outfit" to outfit))
    }

    /**
     * 공간에 들어갈 때. rooms/{spaceId}/presence/{uid}
     * 앱이 꺼지거나 연결이 끊기면 서버가 onDisconnect 로 자동으로 지운다.
     */
    fun enterRoom(spaceId: String, p: Presence) {
        val u = uid ?: return
        val ref = db?.child(DbPath.presence(spaceId))?.child(u) ?: return
        ref.onDisconnect().removeValue()
        ref.setValue(
            mapOf(
                "nick" to p.nick, "outfit" to p.outfit,
                "x" to p.x, "y" to p.y, "tx" to p.tx, "ty" to p.ty,
                "moveStartAt" to ServerValue.TIMESTAMP,
                "holding" to p.holding, "bites" to p.bites, "roasted" to p.roasted,
                "updatedAt" to ServerValue.TIMESTAMP,
            ).filterValues { it != null }
        )
    }

    /** 공간을 나갈 때. 다른 공간으로 갈 때는 leaveRoom → enterRoom 순서. */
    fun leaveRoom(spaceId: String) {
        val u = uid ?: return
        val ref = db?.child(DbPath.presence(spaceId))?.child(u) ?: return
        ref.onDisconnect().cancel()
        ref.removeValue()
    }

    /** 걷기 시작: 출발점·목적지만 보낸다. moveStartAt 은 서버 시간. */
    fun startMove(spaceId: String, fromX: Double, fromY: Double, toX: Double, toY: Double) {
        updatePresence(
            spaceId,
            mapOf("x" to fromX, "y" to fromY, "tx" to toX, "ty" to toY, "moveStartAt" to ServerValue.TIMESTAMP),
        )
    }

    fun updatePresence(spaceId: String, fields: Map<String, Any?>) {
        val u = uid ?: return
        db?.child(DbPath.presence(spaceId))?.child(u)
            ?.updateChildren(fields + ("updatedAt" to ServerValue.TIMESTAMP))
    }

    /** 같은 공간에 있는 사람만 구독한다. uid 는 snapshot.key 로 채운다. */
    fun observeRoom(spaceId: String): Flow<List<Presence>> {
        val root = db ?: return flowOf(emptyList())
        return callbackFlow {
            val ref = root.child(DbPath.presence(spaceId))
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.children.mapNotNull { child ->
                        child.getValue(Presence::class.java)?.also { it.uid = child.key.orEmpty() }
                    })
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.w(TAG, "presence listen failed: ${error.message}")
                    trySend(emptyList())
                }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
    }

    /** events/snowstorm 덮어쓰기 (흔들기 감지 또는 디버그 버튼) */
    fun sendSnowstorm() {
        val u = uid ?: return
        db?.child(DbPath.SNOWSTORM)?.setValue(mapOf("by" to u, "at" to ServerValue.TIMESTAMP))
    }

    fun observeSnowstorm(): Flow<SnowstormEvent?> {
        val root = db ?: return flowOf(null)
        return callbackFlow {
            val ref = root.child(DbPath.SNOWSTORM)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend(snapshot.getValue(SnowstormEvent::class.java))
                }
                override fun onCancelled(error: DatabaseError) { trySend(null) }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
    }

    /** 서버 시간 - 내 폰 시간 (ms). serverNow = System.currentTimeMillis() + offset */
    fun observeServerTimeOffset(): Flow<Long> {
        if (!isAvailable) return flowOf(0L)
        return callbackFlow {
            val ref = FirebaseDatabase.getInstance().getReference(DbPath.SERVER_TIME_OFFSET)
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    trySend((snapshot.value as? Number)?.toLong() ?: 0L)
                }
                override fun onCancelled(error: DatabaseError) { trySend(0L) }
            }
            ref.addValueEventListener(listener)
            awaitClose { ref.removeEventListener(listener) }
        }
    }

    private companion object { const val TAG = "FirebaseRepository" }
}
