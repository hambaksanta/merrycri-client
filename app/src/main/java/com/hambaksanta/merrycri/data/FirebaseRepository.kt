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
import com.google.firebase.database.ValueEventListener
import com.hambaksanta.merrycri.data.model.FriendProfile
import com.hambaksanta.merrycri.data.model.Letter
import com.hambaksanta.merrycri.data.model.Ornament
import com.hambaksanta.merrycri.data.model.OrnamentType
import com.hambaksanta.merrycri.data.model.Score
import com.hambaksanta.merrycri.data.model.UserProfile
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/** v2 Firebase 진입점. 화면은 이 클래스의 공개 함수로만 DB를 사용한다. 담당: B */
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

    /** uid로 사용자 정보를 읽는다. 존재하지 않는 uid면 null이다. */
    suspend fun getUser(userId: String): UserProfile? =
        requireDatabase().child(USERS).child(userId).get().awaitResult().toUserProfile()

    /** 6자리 친구 코드로 친구를 찾아 양쪽 친구 목록을 한 번에 저장한다. */
    suspend fun addFriendByCode(friendCode: String): FriendProfile {
        val code = friendCode.trim().uppercase()
        require(code.length == FRIEND_CODE_LENGTH) { "friend code must be 6 characters" }

        val userId = requireUid()
        val root = requireDatabase()
        val friendId = root.child(FRIEND_CODES).child(code).get().awaitResult()
            .getValue(String::class.java)
            ?: throw NoSuchElementException("friend code not found")
        require(friendId != userId) { "cannot add self" }
        val friend = getUser(friendId) ?: throw NoSuchElementException("friend not found")

        root.updateChildren(
            mapOf(
                "$FRIENDS/$userId/$friendId" to ServerValue.TIMESTAMP,
                "$FRIENDS/$friendId/$userId" to ServerValue.TIMESTAMP,
            ),
        ).awaitResult()

        val friendedAt = root.child(FRIENDS).child(userId).child(friendId)
            .get().awaitResult().getValue(Long::class.java) ?: 0L
        return FriendProfile(friendId, friend.nickname, friend.friendCode, friendedAt)
    }

    /** 내 친구 목록을 사용자 프로필과 합쳐 실시간으로 전달한다. */
    fun observeFriends(): Flow<List<FriendProfile>> = callbackFlow {
        val userId = requireUid()
        val root = requireDatabase()
        val friendsRef = root.child(FRIENDS).child(userId)
        var mappingJob: Job? = null
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                mappingJob?.cancel()
                mappingJob = launch {
                    val friends = snapshot.children.mapNotNull { friendSnapshot ->
                        val friendId = friendSnapshot.key ?: return@mapNotNull null
                        val profile = root.child(USERS).child(friendId).get().awaitResult()
                            .toUserProfile() ?: return@mapNotNull null
                        FriendProfile(
                            uid = friendId,
                            nickname = profile.nickname,
                            friendCode = profile.friendCode,
                            friendedAt = friendSnapshot.getValue(Long::class.java) ?: 0L,
                        )
                    }.sortedByDescending { it.friendedAt }
                    trySend(friends)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        friendsRef.addValueEventListener(listener)
        awaitClose {
            mappingJob?.cancel()
            friendsRef.removeEventListener(listener)
        }
    }

    /** 트리 주인의 장식 목록을 실시간으로 전달한다. */
    fun observeOrnaments(ownerUid: String): Flow<List<Ornament>> = observeList(
        requireDatabase().child(ORNAMENTS).child(ownerUid),
    ) { snapshot ->
        val writerUid = snapshot.key ?: return@observeList null
        val type = snapshot.child("type").getValue(String::class.java)
            ?.let(OrnamentType::fromWireValue) ?: return@observeList null
        val slot = snapshot.child("slot").getValue(Long::class.java)?.toInt()
            ?: return@observeList null
        Ornament(
            writerUid = writerUid,
            type = type,
            slot = slot,
            createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
        )
    }

    /** 장식과 편지를 원자적으로 저장한다. 한 트리에는 사용자당 한 번만 성공한다. */
    suspend fun writeLetter(
        ownerUid: String,
        ornamentType: OrnamentType,
        slot: Int,
        text: String,
    ) {
        val writerUid = requireUid()
        require(ownerUid != writerUid) { "cannot write to own tree" }
        require(slot in MIN_ORNAMENT_SLOT..MAX_ORNAMENT_SLOT) { "invalid ornament slot" }
        val cleanText = text.trim()
        require(cleanText.isNotEmpty() && cleanText.length <= MAX_LETTER_LENGTH) {
            "letter must be 1..300 characters"
        }
        val nickname = requireNotNull(getCurrentUser()).nickname

        requireDatabase().updateChildren(
            mapOf(
                "$ORNAMENTS/$ownerUid/$writerUid" to mapOf(
                    "type" to ornamentType.wireValue,
                    "slot" to slot,
                    "createdAt" to ServerValue.TIMESTAMP,
                ),
                "$LETTERS/$ownerUid/$writerUid" to mapOf(
                    "nickname" to nickname,
                    "text" to cleanText,
                    "createdAt" to ServerValue.TIMESTAMP,
                ),
            ),
        ).awaitResult()
    }

    /** 로그인 사용자가 해당 트리에 이미 편지를 썼는지 확인한다. */
    suspend fun hasWrittenLetter(ownerUid: String): Boolean = requireDatabase()
        .child(ORNAMENTS).child(ownerUid).child(requireUid()).get().awaitResult().exists()

    /** 공개일 이후 내 트리에 도착한 편지를 실시간으로 전달한다. */
    fun observeMyLetters(): Flow<List<Letter>> {
        val ownerUid = requireUid()
        return observeList(requireDatabase().child(LETTERS).child(ownerUid)) { snapshot ->
            val writerUid = snapshot.key ?: return@observeList null
            Letter(
                writerUid = writerUid,
                nickname = snapshot.child("nickname").getValue(String::class.java)
                    ?: return@observeList null,
                text = snapshot.child("text").getValue(String::class.java)
                    ?: return@observeList null,
                createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: 0L,
            )
        }
    }

    /** 편지를 쓴 트리의 최고점을 더 높은 경우에만 저장한다. */
    suspend fun saveBestScore(ownerUid: String, score: Int): Boolean {
        require(score >= 0) { "score must not be negative" }
        val writerUid = requireUid()
        val nickname = requireNotNull(getCurrentUser()).nickname
        val scoreRef = requireDatabase().child(SCORES).child(ownerUid).child(writerUid)
        return suspendCancellableCoroutine { continuation ->
            scoreRef.runTransaction(
                object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val oldBest = currentData.child("best").getValue(Long::class.java)?.toInt() ?: -1
                        if (score <= oldBest) return Transaction.abort()
                        currentData.value = mapOf(
                            "nickname" to nickname,
                            "best" to score,
                            "updatedAt" to ServerValue.TIMESTAMP,
                        )
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
    }

    /** 해당 트리의 점수를 높은 순서로 실시간 전달한다. */
    fun observeScores(ownerUid: String): Flow<List<Score>> = observeList(
        requireDatabase().child(SCORES).child(ownerUid),
    ) { snapshot ->
        val writerUid = snapshot.key ?: return@observeList null
        Score(
            writerUid = writerUid,
            nickname = snapshot.child("nickname").getValue(String::class.java)
                ?: return@observeList null,
            best = snapshot.child("best").getValue(Long::class.java)?.toInt()
                ?: return@observeList null,
            updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: 0L,
        )
    }.sortedByDescendingScore()

    /** 서버 시간 오프셋을 반영해 편지 공개 가능 여부를 계산한다. */
    suspend fun canOpenLetters(): Boolean {
        val root = requireDatabase()
        val openAt = root.child(CONFIG).child(OPEN_AT).get().awaitResult()
            .getValue(Long::class.java) ?: return false
        val offset = root.root.child(".info/serverTimeOffset").get().awaitResult()
            .getValue(Long::class.java) ?: 0L
        return System.currentTimeMillis() + offset >= openAt
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

    private fun <T> observeList(
        reference: DatabaseReference,
        mapper: (DataSnapshot) -> T?,
    ): Flow<List<T>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                trySend(snapshot.children.mapNotNull(mapper))
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        reference.addValueEventListener(listener)
        awaitClose { reference.removeEventListener(listener) }
    }

    private fun Flow<List<Score>>.sortedByDescendingScore(): Flow<List<Score>> =
        map { scores -> scores.sortedByDescending(Score::best) }

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
        const val FRIENDS = "friends"
        const val ORNAMENTS = "ornaments"
        const val LETTERS = "letters"
        const val SCORES = "scores"
        const val CONFIG = "config"
        const val OPEN_AT = "openAt"
        const val FRIEND_CODE_LENGTH = 6
        const val MAX_CODE_ATTEMPTS = 20
        const val MIN_ORNAMENT_SLOT = 0
        const val MAX_ORNAMENT_SLOT = 14
        const val MAX_LETTER_LENGTH = 300
        const val FRIEND_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val random = SecureRandom()
    }
}
