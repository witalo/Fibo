package com.example.fibo.viewmodels

import com.example.fibo.model.IProduct
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.repository.OperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: OperationRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var allProducts = listOf<IProduct>()
    private val currentPage = MutableStateFlow(0)
    private val pageSize = 50

    init {
        loadProducts()
    }

    fun loadProducts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val subsidiaryData = preferencesManager.subsidiaryData.first()
                val subsidiaryId = subsidiaryData?.id ?: throw Exception("Subsidiary ID no encontrado")

                allProducts = productRepository.getAllProductsBySubsidiaryId(subsidiaryId, true)
                updateFilteredProducts()

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalProducts = allProducts.size
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar productos"
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
        currentPage.value = 0
        updateFilteredProducts()
    }

    fun loadNextPage() {
        if (!_uiState.value.hasMorePages) return
        currentPage.value += 1
        updateFilteredProducts()
    }

    fun loadPreviousPage() {
        if (currentPage.value <= 0) return
        currentPage.value -= 1
        updateFilteredProducts()
    }

    private fun updateFilteredProducts() {
        val query = _searchQuery.value.trim()
        val filteredProducts = if (query.length < 3 && !query.contains(" ")) {
            allProducts
        } else {
            filterProducts(allProducts, query)
        }

        val startIndex = currentPage.value * pageSize
        val endIndex = minOf(startIndex + pageSize, filteredProducts.size)
        val paginatedProducts = if (startIndex < filteredProducts.size) {
            filteredProducts.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        _uiState.value = _uiState.value.copy(
            products = paginatedProducts,
            filteredTotal = filteredProducts.size,
            hasMorePages = endIndex < filteredProducts.size,
            hasPreviousPages = currentPage.value > 0,
            currentPage = currentPage.value + 1,
            totalPages = kotlin.math.ceil(filteredProducts.size.toDouble() / pageSize).toInt()
        )
    }

    private fun filterProducts(products: List<IProduct>, query: String): List<IProduct> {
        val queryWords = query.lowercase().split(" ").filter { it.isNotBlank() }

        return products.filter { product ->
            val searchText = "${product.code} ${product.name}".lowercase()
            queryWords.all { word -> searchText.contains(word) }
        }
    }

    fun onAddProductClick() {
        // TODO: Implementar navegación a pantalla de agregar producto
    }

    fun retryLoad() {
        loadProducts()
    }
}

// UI State
data class ProductUiState(
    val isLoading: Boolean = false,
    val products: List<IProduct> = emptyList(),
    val totalProducts: Int = 0,
    val filteredTotal: Int = 0,
    val error: String? = null,
    val hasMorePages: Boolean = false,
    val hasPreviousPages: Boolean = false,
    val currentPage: Int = 1,
    val totalPages: Int = 0
)