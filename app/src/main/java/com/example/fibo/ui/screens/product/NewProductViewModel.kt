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
    // ✅ Agregar estados para unidades y tipos de afectación
    private val _units = MutableStateFlow<List<IUnit>>(emptyList())
    val units: StateFlow<List<IUnit>> = _units.asStateFlow()

    private val _typeAffectations = MutableStateFlow<List<ITypeAffectation>>(emptyList())
    val typeAffectations: StateFlow<List<ITypeAffectation>> = _typeAffectations.asStateFlow()

    // ✅ Cargar datos al inicializar
    init {
        loadInitialData()
    }
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Cargar unidades y tipos de afectación en paralelo
                val unitsDeferred = async { productRepository.getUnits() }
                val typeAffectationsDeferred = async { productRepository.getTypeAffectations() }

                _units.value = unitsDeferred.await()
                _typeAffectations.value = typeAffectationsDeferred.await()
            } catch (e: Exception) {
                // Manejar errores si es necesario
            }
        }
    }
    // ✅ Función para cargar producto existente (para editar)
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
                        stockMin = it.stockMin,
                        stockMax = it.stockMax,
                        minimumUnit = it.minimumUnit,
                        maximumUnit = it.maximumUnit,
                        typeAffectation = it.typeAffectation,
                        subjectPerception = it.subjectPerception,
                        productTariffs = it.productTariffs
                    )
                    validateForm()
                }
            } catch (e: Exception) {
                // Manejar errores
            }
        }
    }
    // ✅ Función para calcular precio sin IGV automáticamente
    fun onPriceWithIgvChanged(index: Int, priceWithIgv: Double) {
        val currentTariffs = _uiState.value.productTariffs.toMutableList()
        val tariff = currentTariffs[index]

        // Obtener porcentaje de IGV
        val igvPercentage = getIgvPercentage()

        // Calcular precio sin IGV
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

        // Obtener porcentaje de IGV
        val igvPercentage = getIgvPercentage()

        // Calcular precio con IGV
        val priceWithIgv = priceWithoutIgv * (1 + igvPercentage)

        val updatedTariff = tariff.copy(
            priceWithoutIgv = priceWithoutIgv,
            priceWithIgv = priceWithIgv
        )

        currentTariffs[index] = updatedTariff
        _uiState.value = _uiState.value.copy(productTariffs = currentTariffs)
        validateForm()
    }
    // ✅ Función para obtener el porcentaje de IGV desde el StateFlow (más eficiente)
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

    fun onMinimumUnitChanged(unit: IUnit?) {
        _uiState.value = _uiState.value.copy(minimumUnit = unit)
    }

    fun onMaximumUnitChanged(unit: IUnit?) {
        _uiState.value = _uiState.value.copy(maximumUnit = unit)
    }

    fun onTypeAffectationChanged(typeAffectation: ITypeAffectation?) {
        _uiState.value = _uiState.value.copy(typeAffectation = typeAffectation)
    }

    fun onSubjectPerceptionChanged(subjectPerception: Boolean) {
        _uiState.value = _uiState.value.copy(subjectPerception = subjectPerception)
    }

    fun addProductTariff() {
        val currentTariffs = _uiState.value.productTariffs.toMutableList()
        currentTariffs.add(
            IProductTariff(
                typePrice = 3, // Precio de venta por defecto
                priceWithIgv = 0.0,
                priceWithoutIgv = 0.0,
                quantityMinimum = 1.0
            )
        )
        _uiState.value = _uiState.value.copy(productTariffs = currentTariffs)
        validateForm()
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
            "typePrice" -> tariff.copy(typePrice = value as Int)
            else -> tariff
        }

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
                    code = uiState.value.code,
                    barcode = uiState.value.barcode,
                    observation = uiState.value.observation,
                    stockMin = uiState.value.stockMin,
                    stockMax = uiState.value.stockMax,
                    minimumUnit = uiState.value.minimumUnit,
                    maximumUnit = uiState.value.maximumUnit,
                    typeAffectation = uiState.value.typeAffectation,
                    subjectPerception = uiState.value.subjectPerception,
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
                    code = uiState.value.code,
                    barcode = uiState.value.barcode,
                    observation = uiState.value.observation,
                    stockMin = uiState.value.stockMin,
                    stockMax = uiState.value.stockMax,
                    minimumUnit = uiState.value.minimumUnit,
                    maximumUnit = uiState.value.maximumUnit,
                    typeAffectation = uiState.value.typeAffectation,
                    subjectPerception = uiState.value.subjectPerception,
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
    fun onMinimumUnitExpandedChange(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isMinimumUnitExpanded = expanded)
    }

    fun onMaximumUnitExpandedChange(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isMaximumUnitExpanded = expanded)
    }

    fun onTypeAffectationExpandedChange(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isTypeAffectationExpanded = expanded)
    }

    private fun validateForm() {
        val currentState = _uiState.value
        val isValid = currentState.name.isNotBlank() &&
                currentState.productTariffs.isNotEmpty() &&
                currentState.productTariffs.all { it.priceWithIgv > 0 }

        _uiState.value = currentState.copy(isFormValid = isValid)
    }
}

// ✅ Actualizar el estado para incluir los nuevos campos
data class NewProductUiState(
    val name: String = "",
    val code: String = "",
    val barcode: String = "",
    val observation: String = "-",
    val stockMin: Int = 0,
    val stockMax: Int = 0,
    val minimumUnit: IUnit? = null,
    val maximumUnit: IUnit? = null,
    val typeAffectation: ITypeAffectation? = null,
    val subjectPerception: Boolean = false,
    val productTariffs: List<IProductTariff> = emptyList(),
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    val productResult: Result<String>? = null,
    val nameError: String = "",
    // ✅ Agregar campos para manejar selección
    val isMinimumUnitExpanded: Boolean = false,
    val isMaximumUnitExpanded: Boolean = false,
    val isTypeAffectationExpanded: Boolean = false
)