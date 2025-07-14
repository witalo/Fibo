package com.example.fibo.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.IGuide
import com.example.fibo.model.IGuideResponse
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GuideListViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _guides = MutableStateFlow<List<IGuide>>(emptyList())
    val guides: StateFlow<List<IGuide>> = _guides.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _totalPages = MutableStateFlow(0)
    val totalPages: StateFlow<Int> = _totalPages.asStateFlow()

    private val _totalSales = MutableStateFlow(0)
    val totalSales: StateFlow<Int> = _totalSales.asStateFlow()

    private val _currentPage = MutableStateFlow(1)
    val currentPage: StateFlow<Int> = _currentPage.asStateFlow()

    private val _startDate = MutableStateFlow(getCurrentDate())
    val startDate: StateFlow<String> = _startDate.asStateFlow()

    private val _endDate = MutableStateFlow(getCurrentDate())
    val endDate: StateFlow<String> = _endDate.asStateFlow()

    private val _documentType = MutableStateFlow("NA")
    val documentType: StateFlow<String> = _documentType.asStateFlow()

    private val pageSize = 50

    init {
        loadGuides()
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun loadGuides() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                val subsidiaryId = preferencesManager.getSubsidiaryId()
                if (subsidiaryId != null) {
                    val result = operationRepository.getAllGuides(
                        subsidiaryId = subsidiaryId,
                        startDate = _startDate.value,
                        endDate = _endDate.value,
                        documentType = _documentType.value,
                        page = _currentPage.value,
                        pageSize = pageSize
                    )
                    
                    result.fold(
                        onSuccess = { response ->
                            _guides.value = response.guides
                            _totalPages.value = response.totalNumberOfPages
                            _totalSales.value = response.totalNumberOfSales
                        },
                        onFailure = { e ->
                            _error.value = e.message ?: "Error al cargar guías"
                        }
                    )
                } else {
                    _error.value = "No se encontró la información de la sede"
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateStartDate(date: String) {
        _startDate.value = date
        _currentPage.value = 1
        loadGuides()
    }

    fun updateEndDate(date: String) {
        _endDate.value = date
        _currentPage.value = 1
        loadGuides()
    }

    fun updateDocumentType(type: String) {
        _documentType.value = type
        _currentPage.value = 1
        loadGuides()
    }

    fun goToPage(page: Int) {
        if (page in 1.._totalPages.value) {
            _currentPage.value = page
            loadGuides()
        }
    }

    fun nextPage() {
        if (_currentPage.value < _totalPages.value) {
            _currentPage.value++
            loadGuides()
        }
    }

    fun previousPage() {
        if (_currentPage.value > 1) {
            _currentPage.value--
            loadGuides()
        }
    }

    fun clearError() {
        _error.value = null
    }
} 