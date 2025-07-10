package com.example.fibo.viewmodels

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.CreateSaleMutation
import com.example.fibo.GuideModesQuery
import com.example.fibo.GuideReasonsQuery
import com.example.fibo.SerialsQuery
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.*
import com.example.fibo.repository.OperationRepository
import com.example.fibo.type.Date
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class GuideViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Estado para la guía
    private val _guideState = MutableStateFlow(GuideState())
    val guideState: StateFlow<GuideState> = _guideState.asStateFlow()

    // Estado para el proceso de guardado
    private val _isSaving = MutableStateFlow(false)
    val isSaving: StateFlow<Boolean> = _isSaving.asStateFlow()

    // Estado para errores
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Estados para los datos de configuración
    private val _guideModeTypes = MutableStateFlow<List<IGuideModeType>>(emptyList())
    val guideModeTypes: List<IGuideModeType> get() = _guideModeTypes.value

    private val _guideReasonTypes = MutableStateFlow<List<IGuideReasonType>>(emptyList())
    val guideReasonTypes: List<IGuideReasonType> get() = _guideReasonTypes.value

    private val _serialAssigneds = MutableStateFlow<List<ISerialAssigned>>(emptyList())
    val serialAssigneds: List<ISerialAssigned> get() = _serialAssigneds.value

    private val _isLoadingData = MutableStateFlow(false)
    val isLoadingData: Boolean get() = _isLoadingData.value

    init {
        loadConfigurationData()
    }

    private fun loadConfigurationData() {
        viewModelScope.launch {
            _isLoadingData.value = true
            try {
                // Cargar modos de guía
                operationRepository.getGuideModes().fold(
                    onSuccess = { response ->
                        _guideModeTypes.value = response.allGuideModes!!.map {
                            IGuideModeType(code = it?.code!!, name = it.name!!)
                        }
                    },
                    onFailure = { e ->
                        _error.value = "Error al cargar modos de guía: ${e.message}"
                    }
                )

                // Cargar razones de guía
                operationRepository.getGuideReasons().fold(
                    onSuccess = { response ->
                        _guideReasonTypes.value = response.allGuideReasons!!.map {
                            IGuideReasonType(code = it?.code!!, name = it.name!!)
                        }
                    },
                    onFailure = { e ->
                        _error.value = "Error al cargar razones de guía: ${e.message}"
                    }
                )

                // Cargar series
                val subsidiaryId = preferencesManager.subsidiaryData.value?.id // Obtener el ID de la sucursal desde el DataStorei
                if (subsidiaryId != null) {
                    operationRepository.getSerials(subsidiaryId).fold(
                        onSuccess = { response ->
                            _serialAssigneds.value = response.allSerials!!.map {
                                ISerialAssigned(
                                    documentType = it?.documentType.toString(),
                                    documentTypeReadable = it?.documentTypeReadable!!,
                                    serial = it.serial!!,
                                    isGeneratedViaApi = it.isGeneratedViaApi
                                )
                            }
                        },
                        onFailure = { e ->
                            _error.value = "Error al cargar series: ${e.message}"
                        }
                    )
                }
            } finally {
                _isLoadingData.value = false
            }
        }
    }

    // Función para actualizar campos individuales
    fun updateField(field: String, value: Any) {
        _guideState.update { currentState ->
            currentState.copy().apply {
                when (field) {
                    "clientId" -> clientId = value as Int
                    "clientName" -> clientName = value as String
                    "documentType" -> documentType = value as String
                    "serial" -> serial = value as String
                    "correlative" -> correlative = value as Int
                    "emitDate" -> emitDate = value as String
                    "guideModeTransfer" -> guideModeTransfer = value as String
                    "guideReasonTransfer" -> guideReasonTransfer = value as String
                    // ... otros campos según sea necesario
                }
            }
        }

        // Lógica especial para cuando cambia el tipo de documento
        if (field == "documentType") {
            handleDocumentTypeChange(value as String)
        }
    }

    private fun handleDocumentTypeChange(documentType: String) {
        if (documentType == "31") {
            // Para guía de transportista, establecer modo de transporte a "02"
            _guideState.update { currentState ->
                currentState.copy(
                    guideModeTransfer = "02",
                    clientId = 0,
                    clientName = ""
                )
            }
        }
    }

    // Función para agregar un producto
    fun addProduct(product: IOperationDetail) {
        _guideState.update { currentState ->
            val newProducts = currentState.operationDetailSet.toMutableList()
            newProducts.add(product)
            currentState.copy(operationDetailSet = newProducts)
        }
    }

    // Función para remover un producto
    fun removeProduct(index: Int) {
        _guideState.update { currentState ->
            val newProducts = currentState.operationDetailSet.toMutableList()
            newProducts.removeAt(index)
            currentState.copy(operationDetailSet = newProducts)
        }
    }

    // Función para guardar la guía
    fun saveGuide() {
        viewModelScope.launch {
            _isSaving.value = true
            _error.value = null
            
            try {
                val state = _guideState.value
                
                val input = CreateSaleMutation(
                    clientId = state.clientId,
                    documentType = state.documentType,
                    serial = state.serial,
                    correlative = state.correlative,
                    emitDate = Date(state.emitDate),
                    guideModeTransfer = state.guideModeTransfer,
                    guideReasonTransfer = state.guideReasonTransfer,
                    productIdSet = state.operationDetailSet.map { it.tariff.productId },
                    descriptionSet = state.operationDetailSet.map { it.description },
                    quantitySet = state.operationDetailSet.map { it.quantity.toDouble() },
                    relatedDocumentsSerialSet = state.relatedDocuments.map { it.serial ?: "" },
                    relatedDocumentsDocumentTypeSet = state.relatedDocuments.map { it.documentType ?: "" },
                    relatedDocumentsCorrelativeSet = state.relatedDocuments.map { it.correlative ?: 0 },
                    transferDate = Date(state.transferDate),
                    totalWeight = state.totalWeight.toDouble(),
                    weightMeasurementUnitCode = state.weightMeasurementUnitCode,
                    quantityPackages = state.quantityPackages.toDouble(),
                    transportationCompanyDocumentType = state.transportationCompanyDocumentType,
                    transportationCompanyDocumentNumber = state.transportationCompanyDocumentNumber,
                    transportationCompanyNames = state.transportationCompanyNames,
                    transportationCompanyMtcRegistrationNumber = state.transportationCompanyMtcRegistrationNumber,
                    mainVehicleLicensePlate = state.mainVehicleLicensePlate.uppercase(),
                    othersVehiclesLicensePlateSet = state.othersVehicles.map { it.licensePlate?.uppercase() ?: "" },
                    mainDriverDocumentType = state.mainDriverDocumentType,
                    mainDriverDocumentNumber = state.mainDriverDocumentNumber,
                    mainDriverDriverLicense = state.mainDriverDriverLicense.uppercase(),
                    mainDriverNames = state.mainDriverNames,
                    othersDriversDocumentTypeSet = state.othersDrivers.map { it.documentType ?: "" },
                    othersDriversDocumentNumberSet = state.othersDrivers.map { it.documentNumber ?: "" },
                    othersDriversDriverLicenseSet = state.othersDrivers.map { it.driverLicense?.uppercase() ?: "" },
                    othersDriversNamesSet = state.othersDrivers.map { it.names ?: "" },
                    receiverDocumentType = state.receiverDocumentType,
                    receiverDocumentNumber = state.receiverDocumentNumber,
                    receiverNames = state.receiverNames,
                    guideOriginDistrictId = state.guideOriginDistrictId,
                    guideOriginAddress = state.guideOriginAddress,
                    guideOriginSerial = state.guideOriginSerial,
                    guideArrivalDistrictId = state.guideArrivalDistrictId,
                    guideArrivalAddress = state.guideArrivalAddress,
                    guideArrivalSerial = state.guideArrivalSerial,
                    observation = state.observation
                )

                val result = operationRepository.createSale(input)
                result.fold(
                    onSuccess = { response ->
                        val createSaleData = response.createSale
                        if (createSaleData?.error == null) {
                            // Éxito
                            resetState()
                            Toast.makeText(context, createSaleData?.message ?: "Guía creada con éxito", Toast.LENGTH_SHORT).show()
                        } else {
                            // Error del servidor
                            _error.value = createSaleData.error.toString()
                        }
                    },
                    onFailure = { e ->
                        _error.value = e.message ?: "Error al crear la guía"
                    }
                )
            } catch (e: Exception) {
                _error.value = e.message ?: "Error desconocido"
            } finally {
                _isSaving.value = false
            }
        }
    }

    // Función para limpiar el error
    fun clearError() {
        _error.value = null
    }

    private fun resetState() {
        _guideState.value = GuideState()
    }

    // Clase para manejar el estado de la guía
    data class GuideState(
        var clientId: Int = 0,
        var clientName: String = "",
        var documentType: String = "09",
        var serial: String = "",
        var correlative: Int = 0,
        var emitDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date()),
        var guideModeTransfer: String = "01",
        var guideReasonTransfer: String = "01",
        var operationDetailSet: List<IOperationDetail> = listOf(),
        var relatedDocuments: List<IRelatedDocument> = listOf(),
        var transferDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(java.util.Date()),
        var totalWeight: Double = 0.0,
        var weightMeasurementUnitCode: String = "KGM",
        var quantityPackages: Double = 0.0,
        var transportationCompanyDocumentType: String = "6",
        var transportationCompanyDocumentNumber: String = "",
        var transportationCompanyNames: String = "",
        var transportationCompanyMtcRegistrationNumber: String = "",
        var mainVehicleLicensePlate: String = "",
        var othersVehicles: List<IVehicle> = listOf(),
        var mainDriverDocumentType: String = "1",
        var mainDriverDocumentNumber: String = "",
        var mainDriverDriverLicense: String = "",
        var mainDriverNames: String = "",
        var othersDrivers: List<IPerson> = listOf(),
        var receiverDocumentType: String = "1",
        var receiverDocumentNumber: String = "",
        var receiverNames: String = "",
        var guideOriginDistrictId: String = "",
        var guideOriginAddress: String = "",
        var guideOriginSerial: String = "",
        var guideArrivalDistrictId: String = "",
        var guideArrivalAddress: String = "",
        var guideArrivalSerial: String = "",
        var observation: String = ""
    )
}