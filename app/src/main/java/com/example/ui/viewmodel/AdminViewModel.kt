package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.UserVipProfile
import com.example.data.repository.AuthRepository
import com.example.data.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class UserFilter {
    ALL,
    PENDING_ONLY,
    VIP_ONLY
}

class AdminViewModel(
    private val authRepository: AuthRepository,
    private val transitRepository: TransitRepository
) : ViewModel() {

    val allUsers: StateFlow<List<UserVipProfile>> = authRepository.getAllUsersFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(UserFilter.ALL)
    val selectedFilter: StateFlow<UserFilter> = _selectedFilter.asStateFlow()

    private val _adminActionMessage = MutableStateFlow<String?>(null)
    val adminActionMessage: StateFlow<String?> = _adminActionMessage.asStateFlow()

    private val _isUploadingDataset = MutableStateFlow(false)
    val isUploadingDataset: StateFlow<Boolean> = _isUploadingDataset.asStateFlow()

    val filteredUsers: StateFlow<List<UserVipProfile>> = combine(
        allUsers,
        _searchQuery,
        _selectedFilter
    ) { users, query, filter ->
        users.filter { user ->
            val matchesQuery = query.isBlank() ||
                    user.email.contains(query, ignoreCase = true) ||
                    user.displayName.contains(query, ignoreCase = true) ||
                    user.uid.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                UserFilter.ALL -> true
                UserFilter.PENDING_ONLY -> !user.isVip
                UserFilter.VIP_ONLY -> user.isVip
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: UserFilter) {
        _selectedFilter.value = filter
    }

    fun toggleUserVip(user: UserVipProfile, enableVip: Boolean) {
        viewModelScope.launch {
            val result = authRepository.setVipStatus(
                uid = user.uid,
                isVip = enableVip,
                customMessage = if (enableVip) "تم تفعيل حسابك من قبل الأدمن بنجاح" else "الحساب قيد المراجعة"
            )
            if (result.isSuccess) {
                _adminActionMessage.value = if (enableVip) {
                    "تم تفعيل صلاحية VIP للمستخدم (${user.email}) فورياً!"
                } else {
                    "تم إلغاء صلاحية VIP عن المستخدم (${user.email})"
                }
            } else {
                _adminActionMessage.value = "فشل تحديث حالة المستخدم: ${result.exceptionOrNull()?.localizedMessage}"
            }
        }
    }

    fun uploadNewDatasetToCloud(jsonString: String) {
        viewModelScope.launch {
            _isUploadingDataset.value = true
            val result = transitRepository.uploadDatasetToCloud(jsonString)
            if (result.isSuccess) {
                _adminActionMessage.value = "تم رفع قاعدة بيانات النقل الجديدة إلى السحابة بنجاح ومزامنتها لجميع مستخدمي الـ VIP!"
            } else {
                _adminActionMessage.value = "فشل رفع قاعدة البيانات: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _isUploadingDataset.value = false
        }
    }

    fun clearActionMessage() {
        _adminActionMessage.value = null
    }
}
