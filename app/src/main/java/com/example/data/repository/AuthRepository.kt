package com.example.data.repository

import com.example.data.local.CachedVipProfileEntity
import com.example.data.local.VipCacheDao
import com.example.data.model.UserVipProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val profile: UserVipProfile, val isVip: Boolean) : AuthState()
    data class PendingVipApproval(val profile: UserVipProfile) : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthRepository(
    private val vipCacheDao: VipCacheDao,
    private val appScope: CoroutineScope
) {
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var userDocListener: ListenerRegistration? = null

    init {
        setupAuthListener()
    }

    private fun setupAuthListener() {
        try {
            auth.addAuthStateListener { firebaseAuth ->
                val currentUser = firebaseAuth.currentUser
                if (currentUser == null) {
                    userDocListener?.remove()
                    userDocListener = null
                    _authState.value = AuthState.Unauthenticated
                } else {
                    listenToUserDocument(currentUser.uid, currentUser.email ?: "")
                }
            }
        } catch (error: Throwable) {
            _authState.value = AuthState.Error("تعذر تهيئة خدمة المصادقة")
        }
    }

    private suspend fun readCachedProfile(uid: String): CachedVipProfileEntity? =
        runCatching { vipCacheDao.getCachedProfile(uid) }.getOrNull()

    private fun listenToUserDocument(uid: String, email: String) {
        userDocListener?.remove()

        // Read local SQLCipher cache first for instant UI response
        appScope.launch(Dispatchers.IO) {
            val cached = readCachedProfile(uid)
            if (cached != null && _authState.value is AuthState.Loading) {
                val profile = UserVipProfile(
                    uid = cached.uid,
                    email = cached.email,
                    displayName = cached.displayName,
                    isVip = cached.isVip,
                    isAdmin = cached.isAdmin,
                    statusMessage = cached.statusMessage
                )
                if (profile.isVip || profile.isAdmin) {
                    _authState.value = AuthState.Authenticated(profile, isVip = profile.isVip)
                } else {
                    _authState.value = AuthState.PendingVipApproval(profile)
                }
            }
        }

        // Attach the real-time listener, but keep provider failures inside the
        // auth state machine instead of allowing them to terminate the process.
        try {
            userDocListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // A successful Firebase Auth result is enough to route the user.
                    // Firestore must not send the app back to the login screen later.
                    if (_authState.value is AuthState.Authenticated || _authState.value is AuthState.PendingVipApproval) {
                        return@addSnapshotListener
                    }
                    // If network fails, rely on cached or emit error
                    appScope.launch(Dispatchers.IO) {
                        val cached = readCachedProfile(uid)
                        if (cached != null) {
                            val profile = UserVipProfile(
                                uid = cached.uid,
                                email = cached.email,
                                displayName = cached.displayName,
                                isVip = cached.isVip,
                                isAdmin = cached.isAdmin,
                                statusMessage = cached.statusMessage
                            )
                            if (profile.isVip || profile.isAdmin) {
                                _authState.value = AuthState.Authenticated(profile, isVip = profile.isVip)
                            } else {
                                _authState.value = AuthState.PendingVipApproval(profile)
                            }
                        } else {
                            _authState.value = AuthState.Error(error.localizedMessage ?: "حدث خطأ في الاتصال بالسيرفر")
                        }
                    }
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val profile = parseUserProfile(snapshot, uid, email)

                    // Persist to encrypted local DB
                    appScope.launch(Dispatchers.IO) {
                        runCatching {
                            vipCacheDao.saveCachedProfile(
                                CachedVipProfileEntity(
                                    uid = profile.uid,
                                    email = profile.email,
                                    displayName = profile.displayName,
                                    isVip = profile.isVip,
                                    isAdmin = profile.isAdmin,
                                    statusMessage = profile.statusMessage,
                                    lastCheckedTimestamp = System.currentTimeMillis()
                                )
                            )
                        }
                    }

                    // Real-time State Transition!
                    if (profile.isVip || profile.isAdmin) {
                        _authState.value = AuthState.Authenticated(profile, isVip = profile.isVip)
                    } else {
                        _authState.value = AuthState.PendingVipApproval(profile)
                    }
                } else {
                    // Document doesn't exist yet, initialize it
                    val defaultName = auth.currentUser?.displayName ?: email.substringBefore("@")
                    // Missing profiles are never granted privileges from an email address.
                    // Admin/VIP status must come from a trusted Firestore profile.
                    val isAdmin = false
                    val initialProfile = UserVipProfile(
                        uid = uid,
                        email = email,
                        displayName = defaultName,
                        isVip = isAdmin, // Privileges remain disabled until provisioned in Firestore
                        isAdmin = isAdmin,
                        statusMessage = if (isAdmin) "حساب مسؤول النظام" else "في انتظار موافقة مسؤول النظام",
                        registeredAt = System.currentTimeMillis()
                    )

                    firestore.collection("users").document(uid).set(
                        hashMapOf(
                            "uid" to initialProfile.uid,
                            "email" to initialProfile.email,
                            "displayName" to initialProfile.displayName,
                            "isVip" to initialProfile.isVip,
                            "isAdmin" to initialProfile.isAdmin,
                            "statusMessage" to initialProfile.statusMessage,
                            "registeredAt" to initialProfile.registeredAt
                        )
                    )

                    if (initialProfile.isVip) {
                        _authState.value = AuthState.Authenticated(initialProfile, isVip = true)
                    } else {
                        _authState.value = AuthState.PendingVipApproval(initialProfile)
                    }
                }
            }
        } catch (error: Throwable) {
            _authState.value = AuthState.Error("تعذر الاتصال بخدمة المستخدمين")
        }
    }

    private fun parseUserProfile(snapshot: DocumentSnapshot, uid: String, defaultEmail: String): UserVipProfile {
        val email = snapshot.getString("email") ?: defaultEmail
        val displayName = snapshot.getString("displayName") ?: email.substringBefore("@")
        val isVip = snapshot.getBoolean("isVip") ?: false
        // Never infer administrator privileges from an email address.
        val isAdmin = snapshot.getBoolean("isAdmin") ?: false
        val statusMessage = snapshot.getString("statusMessage")
            ?: if (isVip) "حساب VIP مفعّل" else "في انتظار موافقة مسؤول النظام"
        val registeredAt = snapshot.getLong("registeredAt") ?: System.currentTimeMillis()
        val vipActivatedAt = snapshot.getLong("vipActivatedAt")

        return UserVipProfile(
            uid = uid,
            email = email,
            displayName = displayName,
            isVip = isVip,
            isAdmin = isAdmin,
            statusMessage = statusMessage,
            registeredAt = registeredAt,
            vipActivatedAt = vipActivatedAt
        )
    }

    private fun fallbackProfile(uid: String, email: String): UserVipProfile {
        // Fallback profiles are deliberately non-privileged.
        // Admin/VIP status must come from a trusted Firestore profile.
        val isAdmin = false
        return UserVipProfile(
            uid = uid,
            email = email,
            displayName = email.substringBefore("@"),
            isVip = isAdmin,
            isAdmin = isAdmin,
            statusMessage = if (isAdmin) "حساب مسؤول النظام" else "في انتظار موافقة مسؤول النظام",
            registeredAt = System.currentTimeMillis()
        )
    }

    private fun publishProfile(profile: UserVipProfile) {
        _authState.value = if (profile.isVip || profile.isAdmin) {
            AuthState.Authenticated(profile, isVip = profile.isVip)
        } else {
            AuthState.PendingVipApproval(profile)
        }
    }

    private suspend fun resolveProfileAfterLogin(uid: String, email: String): UserVipProfile {
        val snapshot = withTimeoutOrNull(8_000L) {
            runCatching {
                firestore.collection("users").document(uid).get().await()
            }.getOrNull()
        }

        if (snapshot != null && snapshot.exists()) {
            return parseUserProfile(snapshot, uid, email)
        }

        val profile = fallbackProfile(uid, email)
        // Profile creation must never block or prevent the verified Firebase user
        // from entering the app when Firestore is unavailable or its rules reject it.
        withTimeoutOrNull(4_000L) {
            runCatching {
                firestore.collection("users").document(uid).set(
                    hashMapOf(
                        "uid" to profile.uid,
                        "email" to profile.email,
                        "displayName" to profile.displayName,
                        "isVip" to profile.isVip,
                        "isAdmin" to profile.isAdmin,
                        "statusMessage" to profile.statusMessage,
                        "registeredAt" to profile.registeredAt
                    )
                ).await()
            }
        }
        return profile
    }

    suspend fun loginWithEmail(email: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val authResult = withTimeoutOrNull(15_000L) {
                auth.signInWithEmailAndPassword(email, pass).await()
            } ?: throw IllegalStateException("انتهت مهلة الاتصال بخدمة المصادقة")

            val user = authResult.user ?: throw IllegalStateException("لم تُرجع Firebase مستخدماً صالحاً")
            val profile = resolveProfileAfterLogin(user.uid, user.email ?: email)
            publishProfile(profile)
            Result.success(Unit)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "فشل تسجيل الدخول")
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, pass: String, name: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            val authResult = auth.createUserWithEmailAndPassword(email, pass).await()
            val user = authResult.user ?: throw IllegalStateException("User creation failed")

            // New registrations are always non-privileged. An existing admin
            // profile must be provisioned by a trusted Firestore/admin workflow.
            val isAdmin = false
            val profile = hashMapOf(
                "uid" to user.uid,
                "email" to email,
                "displayName" to name.ifBlank { email.substringBefore("@") },
                "isVip" to isAdmin,
                "isAdmin" to isAdmin,
                "statusMessage" to if (isAdmin) "حساب مسؤول النظام" else "في انتظار موافقة مسؤول النظام",
                "registeredAt" to System.currentTimeMillis()
            )

            firestore.collection("users").document(user.uid).set(profile).await()
            Result.success(Unit)
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "فشل إنشاء الحساب")
            Result.failure(e)
        }
    }

    // Real-time Flow of ALL registered users for the Admin Panel
    fun getAllUsersFlow(): Flow<List<UserVipProfile>> = callbackFlow {
        val subscription = firestore.collection("users")
            .orderBy("registeredAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val users = snapshot.documents.map { doc ->
                        parseUserProfile(doc, doc.id, doc.getString("email") ?: "")
                    }
                    trySend(users)
                }
            }

        awaitClose { subscription.remove() }
    }

    // Admin updates a user's VIP status instantly in Firestore
    suspend fun setVipStatus(uid: String, isVip: Boolean, customMessage: String? = null): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updates = hashMapOf<String, Any>(
                "isVip" to isVip,
                "statusMessage" to (customMessage ?: if (isVip) "تم تفعيل الحساب بنجاح من قبل المسؤول (VIP)" else "الحساب قيد المراجعة")
            )
            if (isVip) {
                updates["vipActivatedAt"] = System.currentTimeMillis()
            }
            firestore.collection("users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Quick VIP Simulator for Local / Offline Sandbox testing
    suspend fun simulateVipActivationLocally(approved: Boolean) = withContext(Dispatchers.IO) {
        val current = _authState.value
        if (current is AuthState.PendingVipApproval) {
            val updated = current.profile.copy(
                isVip = approved,
                statusMessage = if (approved) "تم التفعيل عبر المحاكي السريع" else "في انتظار الموافقة"
            )
            vipCacheDao.saveCachedProfile(
                CachedVipProfileEntity(
                    uid = updated.uid,
                    email = updated.email,
                    displayName = updated.displayName,
                    isVip = updated.isVip,
                    isAdmin = updated.isAdmin,
                    statusMessage = updated.statusMessage,
                    lastCheckedTimestamp = System.currentTimeMillis()
                )
            )
            if (approved) {
                _authState.value = AuthState.Authenticated(updated, isVip = true)
            }
        }
    }

    fun signOut() {
        userDocListener?.remove()
        userDocListener = null
        auth.signOut()
        _authState.value = AuthState.Unauthenticated
    }
}
