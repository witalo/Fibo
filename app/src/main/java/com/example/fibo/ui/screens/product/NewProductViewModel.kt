package com.example.fibo.ui.screens.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.*
import com.example.fibo.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Result

@HiltViewModel
class NewProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val preferencesManager: PreferencesManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewProductUiState())
    val uiState: StateFlow<NewProductUiState> = _uiState.asStateFlow()

    private val _units = MutableStateFlow<List<IUnit>>(emptyList())
    val units: StateFlow<List<IUnit>> = _units.asStateFlow()

    private val _typeAffectations = MutableStateFlow<List<ITypeAffectation>>(emptyList())
    val typeAffectations: StateFlow<List<ITypeAffectation>> = _typeAffectations.asStateFlow()

    // ✅ Tipos de precio disponibles
    private val priceTypes = listOf(
        PriceType(1, "Costo de Compra Unitario"),
        PriceType(2, "Costo de Compra al por Mayor"),
        PriceType(3, "Precio Unitario de Venta"),
        PriceType(4, "Precio al por Mayor de Venta")
    )

    // ✅ Tipos de producto disponibles
    private val productTypes = listOf(
        ProductType("01", "PRODUCTO"),
        ProductType("02", "REGALO"),
        ProductType("03", "SERVICIO"),
        ProductType("NA", "NO APLICA")
    )

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                val unitsDeferred = async { productRepository.getUnits() }
                val typeAffectationsDeferred = async { productRepository.getTypeAffectations() }

                _units.value = unitsDeferred.await()
                _typeAffectations.value = typeAffectationsDeferred.await()

                // ✅ Cargar el primer tipo de afectación por defecto
                val firstTypeAffectation = _typeAffectations.value.firstOrNull()
                if (firstTypeAffectation != null) {
                    _uiState.value = _uiState.value.copy(typeAffectation = firstTypeAffectation)
                    validateForm()
                }
            } catch (e: Exception) {
                // Manejar errores si es necesario
            }
        }
    }

    fun loadProductForEdit(productId: Int) {
        viewModelScope.launch {
            try {
                val product = productRepository.getProductById(productId)
                product?.let {
                    _uiState.value = _uiState.value.copy(
                        name = it.name,
                        code = it.code,
                        barcode = it.barcode,
                        observation = it.observation,
                        activeType = it.activeType,
                        stockMin = it.stockMin,
                        stockMax = it.stockMax,
                        typeAffectation = it.typeAffectation,
                        subjectPerception = it.subjectPerception,
                        active = it.available,
                        productTariffs = it.productTariffs
                    )
                    validateForm()
                }
            } catch (e: Exception) {
                // Manejar errores
            }
        }
    }
    fun onActiveTypeChanged(activeType: String) {
        _uiState.value = _uiState.value.copy(activeType = activeType)
    }
    // ✅ Función para calcular precio sin IGV automáticamente
    fun onPriceWithIgvChanged(index: Int, priceWithIgv: Double) {
        val currentTariffs = _uiState.value.productTariffs.toMutableList()
        val tariff = currentTariffs[index]

        val igvPercentage = getIgvPercentage()
        val priceWithoutIgv = priceWithIgv / (1 + igvPercentage)

        val updatedTariff = tariff.copy(
            priceWithIgv = priceWithIgv,
            priceWithoutIgv = priceWithoutIgv
        )

        currentTariffs[index] = updatedTariff
        _uiState.value = _uiState.value.copy(productTariffs = currentTariffs)
        validateForm()
    }

    // ✅ Función para calcular precio con IGV automáticamente
    fun onPriceWithoutIgvChanged(index: Int, priceWithoutIgv: Double) {
        val currentTariffs = _uiState.value.productTariffs.toMutableList()
        val tariff = currentTariffs[index]

        val igvPercentage = getIgvPercentage()
        val priceWithIgv = priceWithoutIgv * (1 + igvPercentage)

        val updatedTariff = tariff.copy(
            priceWithoutIgv = priceWithoutIgv,
            priceWithIgv = priceWithIgv
        )

        currentTariffs[index] = updatedTariff
        _uiState.value = _uiState.value.copy(productTariffs = currentTariffs)
        validateForm()
    }

    private fun getIgvPercentage(): Double {
        return (preferencesManager.companyData.value?.percentageIgv ?: 18.0) / 100.0
    }

    fun onNameChanged(name: String) {
        _uiState.value = _uiState.value.copy(
            name = name,
            nameError = if (name.isBlank()) "El nombre es obligatorio" else ""
        )
        validateForm()
    }

    fun onCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(code = code)
    }

    fun onBarcodeChanged(barcode: String) {
        _uiState.value = _uiState.value.copy(barcode = barcode)
    }

    fun onObservationChanged(observation: String) {
        _uiState.value = _uiState.value.copy(observation = observation)
    }

    fun onStockMinChanged(stockMin: Int) {
        _uiState.value = _uiState.value.copy(stockMin = stockMin)
    }

    fun onStockMaxChanged(stockMax: Int) {
        _uiState.value = _uiState.value.copy(stockMax = stockMax)
    }

    fun onTypeAffectationChanged(typeAffectation: ITypeAffectation?) {
        _uiState.value = _uiState.value.copy(typeAffectation = typeAffectation)
        validateForm()
    }

    fun onSubjectPerceptionChanged(subjectPerception: Boolean) {
        _uiState.value = _uiState.value.copy(subjectPerception = subjectPerception)
    }

    fun onActiveChanged(active: Boolean) {
        _uiState.value = _uiState.value.copy(active = active)
    }

    // ✅ Función corregida para agregar tarifa con unidad
    fun addProductTariff(priceType: Int, unit: IUnit?) {
        try {
            val currentTariffs = _uiState.value.productTariffs.toMutableList()

            // Verificar que no haya más de 4 tarifas
            if (currentTariffs.size >= 4) return

            // Verificar que no se duplique el tipo de precio
            if (currentTariffs.any { it.typePrice == priceType }) return

            // ✅ Crear tarifa con valores por defecto seguros
            val newTariff = IProductTariff(
                id = 0, // ID temporal para nuevas tarifas
                product = null, // ✅ Campo opcional
                unit = unit,
                typeTrade = "", // ✅ Campo requerido
                typePrice = priceType,
                priceWithIgv = 0.0,
                priceWithoutIgv = 0.0,
                quantityMinimum = 1.0
            )

            currentTariffs.add(newTariff)
            _uiState.value = _uiState.value.copy(productTariffs = currentTariffs)
            validateForm()
        } catch (e: Exception) {
            // ✅ Log del error para debugging
            println("Error en addProductTariff: ${e.message}")
            e.printStackTrace()
        }
    }



    fun removeProductTariff(index: Int) {
        val currentTariffs = _uiState.value.productTariffs.toMutableList()
        currentTariffs.removeAt(index)
        _uiState.value = _uiState.value.copy(productTariffs = currentTariffs)
        validateForm()
    }

    fun updateProductTariff(index: Int, field: String, value: Any) {
        val currentTariffs = _uiState.value.productTariffs.toMutableList()
        val tariff = currentTariffs[index]

        val updatedTariff = when (field) {
            "priceWithIgv" -> tariff.copy(priceWithIgv = value as Double)
            "priceWithoutIgv" -> tariff.copy(priceWithoutIgv = value as Double)
            "quantityMinimum" -> tariff.copy(quantityMinimum = value as Double)
            else -> tariff
        }

        currentTariffs[index] = updatedTariff
        _uiState.value = _uiState.value.copy(productTariffs = currentTariffs)
        validateForm()
    }
    // ✅ Función para actualizar unidad de una tarifa
    fun updateTariffUnit(index: Int, unit: IUnit?) {
        val currentTariffs = _uiState.value.productTariffs.toMutableList()
        val tariff = currentTariffs[index]

        val updatedTariff = tariff.copy(unit = unit)
        currentTariffs[index] = updatedTariff
        _uiState.value = _uiState.value.copy(productTariffs = currentTariffs)
        validateForm()
    }
    fun createProduct(subsidiaryId: Int?) {
        if (subsidiaryId == null) {
            _uiState.value = _uiState.value.copy(
                productResult = Result.failure(Exception("ID de subsidiaria no válido"))
            )
            return
        }

        if (uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val result = productRepository.createProduct(
                    name = uiState.value.name,
                    activeType = uiState.value.activeType,
                    code = uiState.value.code.takeIf { it.isNotBlank() },
                    barcode = uiState.value.barcode.takeIf { it.isNotBlank() },
                    observation = uiState.value.observation.takeIf { it.isNotBlank() },
                    stockMin = uiState.value.stockMin.takeIf { it > 0 },
                    stockMax = uiState.value.stockMax.takeIf { it > 0 },
                    typeAffectation = uiState.value.typeAffectation,
                    subjectPerception = uiState.value.subjectPerception,
                    available = uiState.value.active,
                    productTariffs = uiState.value.productTariffs,
                    subsidiaryId = subsidiaryId
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    productResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    productResult = Result.failure(e)
                )
            }
        }
    }

    fun updateProduct(productId: Int, subsidiaryId: Int?) {
        if (subsidiaryId == null) {
            _uiState.value = _uiState.value.copy(
                productResult = Result.failure(Exception("ID de subsidiaria no válido"))
            )
            return
        }

        if (uiState.value.isLoading) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val result = productRepository.updateProduct(
                    productId = productId,
                    name = uiState.value.name,
                    activeType = uiState.value.activeType,
                    code = uiState.value.code.takeIf { it.isNotBlank() },
                    barcode = uiState.value.barcode.takeIf { it.isNotBlank() },
                    observation = uiState.value.observation.takeIf { it.isNotBlank() },
                    stockMin = uiState.value.stockMin.takeIf { it > 0 },
                    stockMax = uiState.value.stockMax.takeIf { it > 0 },
                    typeAffectation = uiState.value.typeAffectation,
                    subjectPerception = uiState.value.subjectPerception,
                    available = uiState.value.active,
                    productTariffs = uiState.value.productTariffs
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    productResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    productResult = Result.failure(e)
                )
            }
        }
    }

    fun resetProductResult() {
        _uiState.value = _uiState.value.copy(productResult = null)
    }

    fun onTypeAffectationExpandedChange(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isTypeAffectationExpanded = expanded)
    }

    private fun validateForm() {
        val currentState = _uiState.value
        val isValid = currentState.name.isNotBlank() &&
                currentState.activeType.isNotBlank() &&
                currentState.typeAffectation != null &&
                currentState.productTariffs.isNotEmpty() &&
                currentState.productTariffs.all { it.priceWithIgv > 0 && it.unit != null }

        _uiState.value = currentState.copy(isFormValid = isValid)
    }

    // ✅ Obtener tipos de precio disponibles
    fun getAvailablePriceTypes(): List<PriceType> {
        val usedTypes = _uiState.value.productTariffs.map { it.typePrice }
        return priceTypes.filter { !usedTypes.contains(it.id) }
    }

    // ✅ Obtener tipos de producto disponibles
    fun getProductTypes(): List<ProductType> = productTypes
}

// ✅ Estado actualizado
data class NewProductUiState(
    val name: String = "",
    val activeType: String = "01", // Por defecto PRODUCTO
    val code: String = "",
    val barcode: String = "",
    val observation: String = "",
    val stockMin: Int = 0,
    val stockMax: Int = 0,
    val typeAffectation: ITypeAffectation? = null,
    val subjectPerception: Boolean = false,
    val active: Boolean = true,
    val productTariffs: List<IProductTariff> = emptyList(),
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    val productResult: Result<String>? = null,
    val nameError: String = "",
    val isTypeAffectationExpanded: Boolean = false
)

// ✅ Clase para tipos de precio
data class PriceType(
    val id: Int,
    val name: String
)

// ✅ Clase para tipos de producto
data class ProductType(
    val id: String,
    val name: String
)