package com.example.fibo.ui.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.model.IProduct
import com.example.fibo.repository.ProductRepository
import com.example.fibo.utils.ProductSortOrder
import com.example.fibo.utils.ProductUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductUiState())
    val uiState: StateFlow<ProductUiState> = _uiState.asStateFlow()

    private val _products = MutableStateFlow<List<IProduct>>(emptyList())
    private val _searchQuery = MutableStateFlow("")

    init {
        // Lanzar la corrutina para observar los flujos
        viewModelScope.launch {
            // Combinar productos, búsqueda y filtros para filtrar
            combine(
                _products,
                _searchQuery,
                _uiState.map { it.sortOrder },
                _uiState.map { it.filterAvailable }
            ) { products, query, sortOrder, filterAvailable ->
                println("DEBUG: Iniciando filtrado - Total productos: ${products.size}")
                println("DEBUG: Filtros activos - Disponibilidad: $filterAvailable")
                
                var filteredProducts = products

                // Aplicar filtros
                if (filterAvailable != null) {
                    val beforeFilter = filteredProducts.size
                    println("DEBUG: Aplicando filtro de disponibilidad: $filterAvailable")
                    println("DEBUG: Productos antes del filtro: $beforeFilter")
                    
                    // Log de todos los productos con su estado de disponibilidad
                    filteredProducts.forEach { product ->
                        println("DEBUG: Producto '${product.name}' - available: ${product.available}")
                    }
                    
                    filteredProducts = filteredProducts.filter { it.available == filterAvailable }
                    val afterFilter = filteredProducts.size
                    
                    println("DEBUG: Filtro disponibilidad $filterAvailable aplicado, antes: $beforeFilter, después: $afterFilter")
                    println("DEBUG: Productos disponibles en total: ${products.count { it.available }}")
                    println("DEBUG: Productos no disponibles en total: ${products.count { !it.available }}")
                    
                    // Log de productos filtrados
                    filteredProducts.forEach { product ->
                        println("DEBUG: Producto filtrado '${product.name}' - available: ${product.available}")
                    }
                }

                // Aplicar búsqueda
                if (query.isNotBlank()) {
                    filteredProducts = filteredProducts.filter { product ->
                        product.name.contains(query, ignoreCase = true) ||
                                product.code.contains(query, ignoreCase = true) ||
                                product.barcode.contains(query, ignoreCase = true)
                    }
                }

                // Aplicar ordenamiento
                filteredProducts = when (sortOrder) {
                    ProductSortOrder.NAME_ASC -> filteredProducts.sortedBy { it.name }
                    ProductSortOrder.NAME_DESC -> filteredProducts.sortedByDescending { it.name }
                    ProductSortOrder.CODE_ASC -> filteredProducts.sortedBy { it.code }
                    ProductSortOrder.CODE_DESC -> filteredProducts.sortedByDescending { it.code }
                    ProductSortOrder.STOCK_ASC -> filteredProducts.sortedBy { it.totalStock }
                    ProductSortOrder.STOCK_DESC -> filteredProducts.sortedByDescending { it.totalStock }
                    ProductSortOrder.DATE_CREATED_ASC -> filteredProducts.sortedBy { it.id }
                    ProductSortOrder.DATE_CREATED_DESC -> filteredProducts.sortedByDescending { it.id }
                }

                println("DEBUG: Filtrado completado - Productos finales: ${filteredProducts.size}")
                filteredProducts
            }.collect { filteredProducts ->
                _uiState.value = _uiState.value.copy(
                    filteredProducts = filteredProducts
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _uiState.value = _uiState.value.copy(
            searchQuery = query,
            isSearching = query.isNotBlank()
        )
    }

    fun clearSearch() {
        _searchQuery.value = ""
        _uiState.value = _uiState.value.copy(
            searchQuery = "",
            isSearching = false
        )
    }

    fun loadProducts(subsidiaryId: Int?) {
        if (subsidiaryId == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val products = productRepository.getProductsBySubsidiary(subsidiaryId)
                _products.value = products
                _uiState.value = _uiState.value.copy(
                    products = products,
                    isLoading = false,
                    currentPage = 1,
                    hasMoreProducts = products.size >= 20 // Asumiendo paginación de 20
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error al cargar productos"
                )
            }
        }
    }

    fun refreshProducts(subsidiaryId: Int?) {
        if (subsidiaryId == null) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true, error = null)

            try {
                val products = productRepository.getProductsBySubsidiary(subsidiaryId)
                _products.value = products
                _uiState.value = _uiState.value.copy(
                    products = products,
                    isRefreshing = false,
                    currentPage = 1,
                    hasMoreProducts = products.size >= 20
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRefreshing = false,
                    error = e.message ?: "Error al actualizar productos"
                )
            }
        }
    }

    fun loadMoreProducts(subsidiaryId: Int?) {
        if (subsidiaryId == null || !_uiState.value.hasMoreProducts) return

        viewModelScope.launch {
            val currentPage = _uiState.value.currentPage
            _uiState.value = _uiState.value.copy(currentPage = currentPage + 1)

            try {
                // Aquí podrías implementar paginación real si tu backend la soporta
                // Por ahora solo simulamos
                _uiState.value = _uiState.value.copy(hasMoreProducts = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    currentPage = currentPage,
                    error = e.message ?: "Error al cargar más productos"
                )
            }
        }
    }

    fun getProductById(productId: Int, onSuccess: (IProduct) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val product = productRepository.getProductById(productId)
                if (product != null) {
                    onSuccess(product)
                } else {
                    onError("Producto no encontrado")
                }
            } catch (e: Exception) {
                onError(e.message ?: "Error al cargar el producto")
            }
        }
    }

    fun setFilterAvailable(available: Boolean?) {
        println("DEBUG: setFilterAvailable llamado con: $available")
        _uiState.value = _uiState.value.copy(
            filterAvailable = available
        )
    }

    fun clearFilters() {
        println("DEBUG: clearFilters llamado")
        _uiState.value = _uiState.value.copy(
            filterAvailable = null
        )
    }

    // Función para establecer ordenamiento
    fun setSortOrder(sortOrder: ProductSortOrder) {
        _uiState.value = _uiState.value.copy(
            sortOrder = sortOrder
        )
    }

    // Función para seleccionar producto
    fun selectProduct(product: IProduct) {
        _uiState.value = _uiState.value.copy(
            selectedProduct = product
        )
    }

    // Funciones para el diálogo de eliminación
    fun showDeleteDialog(product: IProduct) {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = true,
            productToDelete = product
        )
    }

    fun hideDeleteDialog() {
        _uiState.value = _uiState.value.copy(
            showDeleteDialog = false,
            productToDelete = null
        )
    }

    fun deleteProduct(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val product = uiState.value.productToDelete ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isDeleting = true)

            try {
                // Aquí llamarías a tu repositorio para eliminar el producto
                // productRepository.deleteProduct(product.id)

                // Simular eliminación exitosa
                val updatedProducts = uiState.value.products.filter { it.id != product.id }
                _products.value = updatedProducts
                _uiState.value = _uiState.value.copy(
                    products = updatedProducts,
                    isDeleting = false,
                    showDeleteDialog = false,
                    productToDelete = null,
                    successMessage = "Producto eliminado exitosamente"
                )
                onSuccess()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isDeleting = false,
                    error = "Error al eliminar producto: ${e.message}"
                )
                onError(e.message ?: "Error desconocido")
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun clearSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun clearAllMessages() {
        _uiState.value = _uiState.value.copy(
            error = null,
            successMessage = null
        )
    }
}