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
            try {
                transitRepository.seedInitialDataIfEmpty()
            } catch (error: Throwable) {
                // Startup data is optional; a local database/provider failure
                // must not crash the authentication screen.
                _statusMessage.value = "تعذر تحميل البيانات المحلية"
            }

            // Cloud sync is also optional during startup. The repository
            // returns a Result for expected network failures, while this guard
            // protects against provider/runtime failures as well.
            try {
                transitRepository.syncWithCloudFirestore()
            } catch (error: Throwable) {
                _statusMessage.value = "تعذر مزامنة البيانات السحابية"
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
            val result = transitRepository.syncWithCloudFirestore()
            if (result.isSuccess) {
                val found = result.getOrDefault(false)
                _statusMessage.value = if (found) "تمت المزامنة مع قاعدة البيانات السحابية بنجاح!" else "البيانات المحلية محدثة بالفعل"
            } else {
                _statusMessage.value = "فشلت المزامنة: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _isSyncing.value = false
        }
    }

    fun importCustomJson(jsonString: String) {
        viewModelScope.launch {
            _isSyncing.value = true
            val result = transitRepository.importJson(jsonString)
            if (result.isSuccess) {
                val dataset = result.getOrNull()
                _statusMessage.value = "تم استيراد ${dataset?.pois?.size ?: 0} نقطة و ${dataset?.streets?.size ?: 0} مسار وتشفيرها محلياً بـ SQLCipher!"
            } else {
                _statusMessage.value = "فشل تحليل الـ JSON: ${result.exceptionOrNull()?.localizedMessage}"
            }
            _isSyncing.value = false
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    fun resetToDefaultData() {
        viewModelScope.launch {
            _isSyncing.value = true
            transitRepository.clearDatabase()
            transitRepository.seedInitialDataIfEmpty()
            _statusMessage.value = "تمت استعادة البيانات النموذجية لقاعدة البيانات المشفرة"
            _isSyncing.value = false
        }
    }
}
