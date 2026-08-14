package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Poi
import com.example.data.model.Street
import com.example.data.model.StreetJunction
import com.example.data.repository.TransitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransitViewModel(
    private val transitRepository: TransitRepository
) : ViewModel() {

    val allPois: StateFlow<List<Poi>> = transitRepository.allPois
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStreets: StateFlow<List<Street>> = transitRepository.allStreets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allJunctions: StateFlow<List<StreetJunction>> = transitRepository.allJunctions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val poiCount: StateFlow<Int> = transitRepository.poiCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val streetCount: StateFlow<Int> = transitRepository.streetCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val junctionCount: StateFlow<Int> = transitRepository.junctionCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedPoi = MutableStateFlow<Poi?>(null)
    val selectedPoi: StateFlow<Poi?> = _selectedPoi.asStateFlow()

    private val _selectedStreet = MutableStateFlow<Street?>(null)
    val selectedStreet: StateFlow<Street?> = _selectedStreet.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    private val _statusIsError = MutableStateFlow(false)
    val statusIsError: StateFlow<Boolean> = _statusIsError.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Filtered POIs based on search query
    val filteredPois: StateFlow<List<Poi>> = combine(allPois, _searchQuery) { pois, query ->
        if (query.isBlank()) {
            pois
        } else {
            pois.filter { poi ->
                poi.name.contains(query, ignoreCase = true) ||
                poi.alternativeNames.any { it.contains(query, ignoreCase = true) } ||
                poi.localizedNames.values.any { it.contains(query, ignoreCase = true) } ||
                (poi.address?.contains(query, ignoreCase = true) == true) ||
                (poi.type?.contains(query, ignoreCase = true) == true)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            runCatching {
                transitRepository.seedInitialDataIfEmpty()
                // A partial/empty database is never considered a successful
                // startup. Rebuild the complete sample atomically if needed.
                if (!transitRepository.isLocalDatasetReady()) {
                    transitRepository.restoreDefaultData()
                }
                check(transitRepository.isLocalDatasetReady()) {
                    "Local dataset is still empty after restore"
                }
            }.onFailure { error ->
                _statusIsError.value = true
                _statusMessage.value = "تعذر تحميل البيانات المحلية: ${error.localizedMessage ?: "خطأ في قاعدة البيانات المشفرة"}"
            }

            // Cloud sync is optional for displaying the verified local data,
            // but a denied/failed sync must never be presented as success.
            val cloudResult = runCatching { transitRepository.syncWithCloudFirestore() }
                .getOrElse { Result.failure(it) }
            if (cloudResult.isFailure) {
                _statusIsError.value = true
                _statusMessage.value = "فشلت المزامنة السحابية: ${cloudResult.exceptionOrNull()?.localizedMessage ?: "صلاحيات Firestore غير كافية"}"
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun selectPoi(poi: Poi?) {
        _selectedPoi.value = poi
        if (poi != null) {
            _selectedStreet.value = null
        }
    }

    fun selectStreet(street: Street?) {
        _selectedStreet.value = street
        if (street != null) {
            _selectedPoi.value = null
        }
    }

    fun syncFromCloud() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                val result = transitRepository.syncWithCloudFirestore()
                if (result.isSuccess) {
                    val found = result.getOrDefault(false)
                    _statusIsError.value = false
                    _statusMessage.value = if (found) {
                        "تمت المزامنة مع قاعدة البيانات السحابية بنجاح!"
                    } else {
                        "لم يتم العثور على مجموعة بيانات سحابية؛ ما زالت البيانات المحلية مستخدمة"
                    }
                } else {
                    _statusIsError.value = true
                    _statusMessage.value = "فشلت المزامنة: ${result.exceptionOrNull()?.localizedMessage ?: "صلاحيات Firestore غير كافية"}"
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun importCustomJson(jsonString: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = transitRepository.importJson(jsonString)
            try {
                if (result.isSuccess) {
                    val dataset = result.getOrNull()
                    _statusIsError.value = false
                    _statusMessage.value = "تم استيراد ${dataset?.pois?.size ?: 0} نقطة و ${dataset?.streets?.size ?: 0} مسار وتشفيرها محلياً بـ SQLCipher!"
                } else {
                    _statusIsError.value = true
                    _statusMessage.value = "فشل تحليل الـ JSON: ${result.exceptionOrNull()?.localizedMessage ?: "بيانات غير صالحة"}"
                }
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
        _statusIsError.value = false
    }

    fun resetToDefaultData() {
        viewModelScope.launch {
            _isSyncing.value = true
            try {
                transitRepository.restoreDefaultData()
                check(transitRepository.isLocalDatasetReady()) {
                    "Local dataset is still empty after restore"
                }
                _statusIsError.value = false
                _statusMessage.value = "تمت استعادة البيانات النموذجية لقاعدة البيانات المشفرة"
            } catch (error: Throwable) {
                _statusIsError.value = true
                _statusMessage.value = "فشلت استعادة البيانات المحلية: ${error.localizedMessage ?: "خطأ في قاعدة البيانات المشفرة"}"
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
