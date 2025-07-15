package com.example.fibo.viewmodels

import android.util.Log
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
                Log.d("ProductViewModel", "Iniciando carga de productos...")
                
                val subsidiaryData = preferencesManager.subsidiaryData.first()
                Log.d("ProductViewModel", "Subsidiary data: $subsidiaryData")
                
                val subsidiaryId = subsidiaryData?.id ?: throw Exception("Subsidiary ID no encontrado")
                Log.d("ProductViewModel", "Subsidiary ID: $subsidiaryId")

                Log.d("ProductViewModel", "Llamando getAllProductsBySubsidiaryId...")
                allProducts = productRepository.getAllProductsBySubsidiaryId(subsidiaryId, true)
                Log.d("ProductViewModel", "Productos obtenidos: ${allProducts.size}")
                
                if (allProducts.isNotEmpty()) {
                    Log.d("ProductViewModel", "Primer producto: ${allProducts.first()}")
                } else {
                    Log.w("ProductViewModel", "No se obtuvieron productos de la API")
                }

                // Primero actualizar el estado básico
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    totalProducts = allProducts.size,
                    error = null
                )
                Log.d("ProductViewModel", "Estado básico actualizado - isLoading: false")

                // Luego aplicar filtros y paginación
                updateFilteredProducts()
                Log.d("ProductViewModel", "Productos filtrados actualizados")
                
                Log.d("ProductViewModel", "Estado final - isLoading: ${_uiState.value.isLoading}, products.size: ${_uiState.value.products.size}, error: ${_uiState.value.error}")
                
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error al cargar productos: ${e.message}", e)
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
        Log.d("ProductViewModel", "updateFilteredProducts - query: '$query', length: ${query.length}")
        Log.d("ProductViewModel", "allProducts.size: ${allProducts.size}")
        
        val filteredProducts = if (query.isEmpty() || (query.length < 3 && !query.contains(" "))) {
            Log.d("ProductViewModel", "Usando todos los productos (sin filtro)")
            allProducts
        } else {
            Log.d("ProductViewModel", "Aplicando filtro de búsqueda")
            filterProducts(allProducts, query)
        }

        Log.d("ProductViewModel", "filteredProducts.size: ${filteredProducts.size}")
        Log.d("ProductViewModel", "currentPage: ${currentPage.value}, pageSize: $pageSize")

        val startIndex = currentPage.value * pageSize
        val endIndex = minOf(startIndex + pageSize, filteredProducts.size)
        val paginatedProducts = if (startIndex < filteredProducts.size) {
            filteredProducts.subList(startIndex, endIndex)
        } else {
            emptyList()
        }

        Log.d("ProductViewModel", "startIndex: $startIndex, endIndex: $endIndex")
        Log.d("ProductViewModel", "paginatedProducts.size: ${paginatedProducts.size}")

        _uiState.value = _uiState.value.copy(
            products = paginatedProducts,
            filteredTotal = filteredProducts.size,
            hasMorePages = endIndex < filteredProducts.size,
            hasPreviousPages = currentPage.value > 0,
            currentPage = currentPage.value + 1,
            totalPages = kotlin.math.ceil(filteredProducts.size.toDouble() / pageSize).toInt()
        )
        
        Log.d("ProductViewModel", "Estado UI actualizado - products.size: ${_uiState.value.products.size}")
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

    // Método de debug para probar directamente la API
    fun testDirectApi() {
        viewModelScope.launch {
            try {
                Log.d("ProductViewModel", "=== TEST DIRECTO API ===")
                
                // Test 1: Verificar token
                val token = preferencesManager.getAuthToken()
                Log.d("ProductViewModel", "Token disponible: ${!token.isNullOrEmpty()}")
                if (!token.isNullOrEmpty()) {
                    Log.d("ProductViewModel", "Token (primeros 20 chars): ${token.take(20)}")
                }
                
                // Test 2: Verificar subsidiary
                val subsidiary = preferencesManager.getSubsidiaryId()
                Log.d("ProductViewModel", "Subsidiary ID: $subsidiary")
                
                // Test 3: Llamada directa al repositorio
                if (subsidiary != null) {
                    Log.d("ProductViewModel", "Llamando directamente al repositorio...")
                    val products = productRepository.getAllProductsBySubsidiaryId(subsidiary, true)
                    Log.d("ProductViewModel", "Productos obtenidos en test: ${products.size}")
                    
                    if (products.isNotEmpty()) {
                        Log.d("ProductViewModel", "Productos encontrados:")
                        products.take(3).forEach { product ->
                            Log.d("ProductViewModel", "- ${product.code}: ${product.name} (Stock: ${product.stock})")
                        }
                        
                        // Actualizar UI para mostrar productos
                        allProducts = products
                        updateFilteredProducts()
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            totalProducts = allProducts.size,
                            error = null
                        )
                        Log.d("ProductViewModel", "UI actualizada con productos del test")
                    }
                } else {
                    Log.e("ProductViewModel", "No hay subsidiary ID disponible")
                }
                
            } catch (e: Exception) {
                Log.e("ProductViewModel", "Error en test directo: ${e.message}", e)
            }
        }
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