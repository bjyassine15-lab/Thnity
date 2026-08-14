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
    }

    private fun listenToUserDocument(uid: String, email: String) {
        userDocListener?.remove()

        // Read local SQLCipher cache first for instant UI response
        appScope.launch(Dispatchers.IO) {
            val cached = vipCacheDao.getCachedProfile(uid)
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

        // Attach Real-time Firestore Listener
        userDocListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // If network fails, rely on cached or emit error
                    appScope.launch(Dispatchers.IO) {
                        val cached = vipCacheDao.getCachedProfile(uid)
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

                    // Real-time State Transition!
                    if (profile.isVip || profile.isAdmin) {
                        _authState.value = AuthState.Authenticated(profile, isVip = profile.isVip)
                    } else {
                        _authState.value = AuthState.PendingVipApproval(profile)
                    }
                } else {
                    // Document doesn't exist yet, initialize it
                    val defaultName = auth.currentUser?.displayName ?: email.substringBefore("@")
                    val isAdmin = email.contains("admin", ignoreCase = true)
                    val initialProfile = UserVipProfile(
                        uid = uid,
                        email = email,
                        displayName = defaultName,
                        isVip = isAdmin, // Admins get VIP automatically
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
    }

    private fun parseUserProfile(snapshot: DocumentSnapshot, uid: String, defaultEmail: String): UserVipProfile {
        val email = snapshot.getString("email") ?: defaultEmail
        val displayName = snapshot.getString("displayName") ?: email.substringBefore("@")
        val isVip = snapshot.getBoolean("isVip") ?: false
        val isAdmin = snapshot.getBoolean("isAdmin") ?: email.contains("admin", ignoreCase = true)
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

    suspend fun loginWithEmail(email: String, pass: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _authState.value = AuthState.Loading
            auth.signInWithEmailAndPassword(email, pass).await()
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

            val isAdmin = email.contains("admin", ignoreCase = true)
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
