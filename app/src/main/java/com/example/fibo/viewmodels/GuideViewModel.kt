package com.example.fibo.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.CreateSaleMutation
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.*
import com.example.fibo.repository.OperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

    // Estado para mensaje de éxito
    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    // Estados para los datos de configuración
    private val _guideModeTypes = MutableStateFlow<List<IGuideModeType>>(emptyList())
    val guideModeTypes: StateFlow<List<IGuideModeType>> = _guideModeTypes.asStateFlow()

    private val _guideReasonTypes = MutableStateFlow<List<IGuideReasonType>>(emptyList())
    val guideReasonTypes: StateFlow<List<IGuideReasonType>> = _guideReasonTypes.asStateFlow()

    private val _serialAssigneds = MutableStateFlow<List<ISerialAssigned>>(emptyList())
    val serialAssigneds: StateFlow<List<ISerialAssigned>> = _serialAssigneds.asStateFlow()

    private val _isLoadingData = MutableStateFlow(false)
    val isLoadingData: StateFlow<Boolean> = _isLoadingData.asStateFlow()

    // Estados para la búsqueda de clientes
    private val _searchResults = MutableStateFlow<List<IPerson>>(emptyList())
    val searchResults: StateFlow<List<IPerson>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _searchError = MutableStateFlow<String?>(null)
    val searchError: StateFlow<String?> = _searchError.asStateFlow()

    // Estado para los productos
    private val _products = MutableStateFlow<List<IProductOperation>>(emptyList())
    val products: StateFlow<List<IProductOperation>> = _products.asStateFlow()

    private val _isLoadingProducts = MutableStateFlow(false)
    val isLoadingProducts: StateFlow<Boolean> = _isLoadingProducts.asStateFlow()

    // Estados para búsqueda de ubicaciones geográficas
    private val _originSearchResults = MutableStateFlow<List<IGeographicLocation>>(emptyList())
    val originSearchResults: StateFlow<List<IGeographicLocation>> = _originSearchResults.asStateFlow()

    private val _arrivalSearchResults = MutableStateFlow<List<IGeographicLocation>>(emptyList())
    val arrivalSearchResults: StateFlow<List<IGeographicLocation>> = _arrivalSearchResults.asStateFlow()

    private val _isSearchingOrigin = MutableStateFlow(false)
    val isSearchingOrigin: StateFlow<Boolean> = _isSearchingOrigin.asStateFlow()

    private val _isSearchingArrival = MutableStateFlow(false)
    val isSearchingArrival: StateFlow<Boolean> = _isSearchingArrival.asStateFlow()

    // Estados para tipos de documento
    private val _documentTypes = MutableStateFlow<List<IDocumentType>>(emptyList())
    val documentTypes: StateFlow<List<IDocumentType>> = _documentTypes.asStateFlow()

    init {
        Log.d("GuideViewModel", "Initializing and loading configuration data")
        loadConfigurationData()
        loadProducts() // Cargar productos al inicializar
    }

    private fun loadConfigurationData() {
        viewModelScope.launch {
            _isLoadingData.value = true
            try {
                withContext(Dispatchers.IO) {
                    // Cargar modos de guía
                    val guideModeResult = operationRepository.getGuideModes()
                    withContext(Dispatchers.Main) {
                        guideModeResult.fold(
                            onSuccess = { response ->
                                _guideModeTypes.value = response.allGuideModes!!.map {
                                    IGuideModeType(code = it?.code!!, name = it.name!!)
                                }
                            },
                            onFailure = { e ->
                                _error.value = "Error al cargar modos de guía: ${e.message}"
                            }
                        )
                    }

                    // Cargar razones de guía
                    val guideReasonResult = operationRepository.getGuideReasons()
                    withContext(Dispatchers.Main) {
                        guideReasonResult.fold(
                            onSuccess = { response ->
                                _guideReasonTypes.value = response.allGuideReasons!!.map {
                                    IGuideReasonType(code = it?.code!!, name = it.name!!)
                                }
                            },
                            onFailure = { e ->
                                _error.value = "Error al cargar razones de guía: ${e.message}"
                            }
                        )
                    }

                    // Cargar series
                    val subsidiaryId = preferencesManager.getSubsidiaryId()
                    if (subsidiaryId != null) {
                        val serialsResult = operationRepository.getSerials(subsidiaryId)
                        withContext(Dispatchers.Main) {
                            serialsResult.fold(
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
                    }

                    // Cargar tipos de documento
                    val documentTypesResult = operationRepository.getDocumentTypes()
                    withContext(Dispatchers.Main) {
                        documentTypesResult.fold(
                            onSuccess = { response ->
                                _documentTypes.value = response.allDocumentTypes!!.map {
                                    IDocumentType(
                                        code = it?.code!!,
                                        name = it.name!!
                                    )
                                }
                            },
                            onFailure = { e ->
                                _error.value = "Error al cargar tipos de documento: ${e.message}"
                            }
                        )
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = "Error al cargar datos: ${e.message}"
                }
            } finally {
                withContext(Dispatchers.Main) {
                    _isLoadingData.value = false
                }
            }
        }
    }

    private fun loadProducts() {
        viewModelScope.launch {
            _isLoadingProducts.value = true
            try {
                val subsidiaryId = preferencesManager.getSubsidiaryId()
                if (subsidiaryId != null) {
                    val productsList = operationRepository.getAllProductsBySubsidiaryId(
                        subsidiaryId = subsidiaryId,
                        available = true
                    )
                    _products.value = productsList
                }
            } catch (e: Exception) {
                _error.value = "Error al cargar productos: ${e.message}"
            } finally {
                _isLoadingProducts.value = false
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
                    "transferDate" -> transferDate = value as String
                    "totalWeight" -> totalWeight = value as Double
                    "weightMeasurementUnitCode" -> weightMeasurementUnitCode = value as String
                    "quantityPackages" -> quantityPackages = value as Double
                    "transportationCompanyDocumentType" -> transportationCompanyDocumentType = value as String
                    "transportationCompanyDocumentNumber" -> transportationCompanyDocumentNumber = value as String
                    "transportationCompanyNames" -> transportationCompanyNames = value as String
                    "transportationCompanyMtcRegistrationNumber" -> transportationCompanyMtcRegistrationNumber = value as String
                    "mainVehicleLicensePlate" -> mainVehicleLicensePlate = value as String
                    "mainDriverDocumentType" -> mainDriverDocumentType = value as String
                    "mainDriverDocumentNumber" -> mainDriverDocumentNumber = value as String
                    "mainDriverDriverLicense" -> mainDriverDriverLicense = value as String
                    "mainDriverNames" -> mainDriverNames = value as String
                    "receiverDocumentType" -> receiverDocumentType = value as String
                    "receiverDocumentNumber" -> receiverDocumentNumber = value as String
                    "receiverNames" -> receiverNames = value as String
                    "guideOriginDistrictId" -> guideOriginDistrictId = value as String
                    "guideOriginAddress" -> guideOriginAddress = value as String
                    "guideOriginSerial" -> guideOriginSerial = value as String
                    "guideArrivalDistrictId" -> guideArrivalDistrictId = value as String
                    "guideArrivalAddress" -> guideArrivalAddress = value as String
                    "guideArrivalSerial" -> guideArrivalSerial = value as String
                    "observation" -> observation = value as String

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
            // Para guía de transportista, establecer modo de transporte a "01" (TRANSPORTE PÚBLICO)
            _guideState.update { currentState ->
                currentState.copy(
                    guideModeTransfer = "01",
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
            // Verificar que el índice es válido
            if (index >= 0 && index < newProducts.size) {
                newProducts.removeAt(index)
                currentState.copy(operationDetailSet = newProducts)
            } else {
                currentState
            }
        }
    }

    // Función para actualizar un producto
    fun updateProduct(index: Int, field: String, value: Any) {
        _guideState.update { currentState ->
            // Verificar que el índice es válido
            if (index < 0 || index >= currentState.operationDetailSet.size) {
                return@update currentState
            }

            val newProducts = currentState.operationDetailSet.toMutableList()
            val product = newProducts[index]
            
            val updatedProduct = when (field) {
                "productName" -> {
                    val productName = value as String
                    val selectedProduct = _products.value.find { p ->
                        val productString = "${p.code?.let { "$it " } ?: ""}${p.name.trim()} ${p.minimumUnitName}".trim()
                        productString == productName
                    }
                    
                    if (selectedProduct != null) {
                        product.copy(
                            tariff = product.tariff.copy(
                                productId = selectedProduct.id,
                                productName = selectedProduct.name,
                                productCode = selectedProduct.code ?: ""
                            )
                        )
                    } else {
                        product.copy(
                            tariff = product.tariff.copy(
                                productName = productName
                            )
                        )
                    }
                }
                "description" -> {
                    product.copy(
                        description = value as String
                    )
                }
                "quantity" -> {
                    val quantity = (value as String).toDoubleOrNull() ?: product.quantity
                    product.copy(
                        quantity = quantity
                    )
                }
                else -> product
            }
            
            newProducts[index] = updatedProduct
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
                
                // Ejecutar validaciones
                val validationError = validateGuide(state)
                if (validationError != null) {
                    _error.value = validationError
                    _isSaving.value = false
                    return@launch
                }
                
                val input = CreateSaleMutation(
                    clientId = state.clientId,
                    documentType = state.documentType,
                    serial = state.serial,
                    correlative = state.correlative,
                    emitDate = state.emitDate,
                    guideModeTransfer = state.guideModeTransfer,
                    guideReasonTransfer = state.guideReasonTransfer,
                    productIdSet = state.operationDetailSet.map { it.tariff.productId },
                    descriptionSet = state.operationDetailSet.map { it.description },
                    quantitySet = state.operationDetailSet.map { it.quantity.toDouble() },
                    relatedDocumentsSerialSet = state.relatedDocuments.map { it.serial ?: "" },
                    relatedDocumentsDocumentTypeSet = state.relatedDocuments.map { it.documentType ?: "" },
                    relatedDocumentsCorrelativeSet = state.relatedDocuments.map { it.correlative ?: 0 },
                    transferDate = state.transferDate,
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
                Log.d("CreateSaleMutation", input.toString())

                val result = operationRepository.createSale(input)
                Log.d("CreateSaleMutation", result.toString())
                result.fold(
                    onSuccess = { response ->
                        val createSaleData = response.createSale
                        if (createSaleData?.error == false) {
                            // Éxito
                            resetState()
                            _successMessage.value = createSaleData.message ?: "Guía creada con éxito"
                        } else {
                            // Error del servidor
                            _error.value = createSaleData?.message ?: "Error al crear la guía"
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

    // Función para limpiar el mensaje de éxito
    fun clearSuccessMessage() {
        _successMessage.value = null
    }

    private fun resetState() {
        _guideState.value = GuideState()
    }

    fun searchClients(query: String) {
        if (query.length < 3) {
            _searchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearching.value = true
            _searchError.value = null
            try {
                withContext(Dispatchers.IO) {
                    operationRepository.searchClientByParameter(
                        search = query,
                        isClient = true,
                        documentType = null,
                        operationDocumentType = _guideState.value.documentType
                    ).fold(
                        onSuccess = { response ->
                            withContext(Dispatchers.Main) {
                                _searchResults.value = response.searchClientByParameter?.map { client ->
                                    IPerson(
                                        id = client?.id?.toInt() ?: 0,
                                        names = client?.names ?: "",
                                        documentType = client?.documentType ?: "",
                                        documentNumber = client?.documentNumber ?: ""
                                    )
                                } ?: emptyList()
                            }
                        },
                        onFailure = { e ->
                            withContext(Dispatchers.Main) {
                                _searchError.value = "Error al buscar clientes: ${e.message}"
                                _searchResults.value = emptyList()
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _searchError.value = "Error al buscar clientes: ${e.message}"
                    _searchResults.value = emptyList()
                }
            } finally {
                _isSearching.value = false
            }
        }
    }

    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _searchError.value = null
    }

    // Funciones para manejar vehículos secundarios
    fun addOtherVehicle() {
        Log.d("GuideViewModel", "addOtherVehicle called")
        _guideState.update { currentState ->
            val newVehicles = currentState.othersVehicles.toMutableList()
            if (newVehicles.size < 2) {
                newVehicles.add(IVehicle(licensePlate = "", vehicleType = ""))
                Log.d("GuideViewModel", "Added vehicle, new size: ${newVehicles.size}")
                currentState.copy(othersVehicles = newVehicles)
            } else {
                Log.d("GuideViewModel", "Max vehicles reached")
                currentState
            }
        }
    }

    fun removeOtherVehicle(index: Int) {
        _guideState.update { currentState ->
            val newVehicles = currentState.othersVehicles.toMutableList()
            if (index >= 0 && index < newVehicles.size) {
                newVehicles.removeAt(index)
                currentState.copy(othersVehicles = newVehicles)
            } else {
                currentState
            }
        }
    }

    fun updateOtherVehicle(index: Int, licensePlate: String) {
        _guideState.update { currentState ->
            if (index < 0 || index >= currentState.othersVehicles.size) {
                return@update currentState
            }
            
            val newVehicles = currentState.othersVehicles.toMutableList()
            newVehicles[index] = newVehicles[index].copy(licensePlate = licensePlate)
            currentState.copy(othersVehicles = newVehicles)
        }
    }

    // Funciones para manejar conductores secundarios
    fun addOtherDriver() {
        Log.d("GuideViewModel", "addOtherDriver called")
        _guideState.update { currentState ->
            val newDrivers = currentState.othersDrivers.toMutableList()
            if (newDrivers.size < 2) {
                newDrivers.add(
                    IPerson(
                        id = 0,
                        names = "",
                        documentType = "1",
                        documentNumber = "",
                        driverLicense = ""
                    )
                )
                Log.d("GuideViewModel", "Added driver, new size: ${newDrivers.size}")
                currentState.copy(othersDrivers = newDrivers)
            } else {
                Log.d("GuideViewModel", "Max drivers reached")
                currentState
            }
        }
    }

    fun removeOtherDriver(index: Int) {
        _guideState.update { currentState ->
            val newDrivers = currentState.othersDrivers.toMutableList()
            if (index >= 0 && index < newDrivers.size) {
                newDrivers.removeAt(index)
                currentState.copy(othersDrivers = newDrivers)
            } else {
                currentState
            }
        }
    }

    fun updateOtherDriver(index: Int, field: String, value: Any) {
        _guideState.update { currentState ->
            if (index < 0 || index >= currentState.othersDrivers.size) {
                return@update currentState
            }
            
            val newDrivers = currentState.othersDrivers.toMutableList()
            val driver = newDrivers[index]
            
            val updatedDriver = when (field) {
                "documentType" -> driver.copy(documentType = value as String)
                "documentNumber" -> driver.copy(documentNumber = value as String)
                "names" -> driver.copy(names = value as String)
                "driverLicense" -> driver.copy(driverLicense = value as String)
                else -> driver
            }
            
            newDrivers[index] = updatedDriver
            currentState.copy(othersDrivers = newDrivers)
        }
    }

    // Función para agregar un documento relacionado
    fun addRelatedDocument() {
        _guideState.update { currentState ->
            val newDocuments = currentState.relatedDocuments.toMutableList()
            newDocuments.add(
                IRelatedDocument(
                    documentType = "01",
                    serial = "",
                    correlative = 0
                )
            )
            currentState.copy(relatedDocuments = newDocuments)
        }
    }

    // Función para remover un documento relacionado
    fun removeRelatedDocument(index: Int) {
        _guideState.update { currentState ->
            val newDocuments = currentState.relatedDocuments.toMutableList()
            newDocuments.removeAt(index)
            currentState.copy(relatedDocuments = newDocuments)
        }
    }

    // Función para actualizar un documento relacionado
    fun updateRelatedDocument(index: Int, field: String, value: Any) {
        _guideState.update { currentState ->
            val newDocuments = currentState.relatedDocuments.toMutableList()
            val document = newDocuments[index]
            newDocuments[index] = when (field) {
                "documentType" -> document.copy(documentType = value as String)
                "serial" -> document.copy(serial = value as String)
                "correlative" -> document.copy(correlative = value as Int)
                else -> document
            }
            currentState.copy(relatedDocuments = newDocuments)
        }
    }

    // Funciones para búsqueda de ubicaciones geográficas
    fun searchOriginLocation(query: String) {
        if (query.length < 3) {
            _originSearchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearchingOrigin.value = true
            try {
                withContext(Dispatchers.IO) {
                    operationRepository.searchGeographicLocation(query).fold(
                        onSuccess = { locations ->
                            withContext(Dispatchers.Main) {
                                _originSearchResults.value = locations
                            }
                        },
                        onFailure = { e ->
                            withContext(Dispatchers.Main) {
                                _originSearchResults.value = emptyList()
                                Log.e("GuideViewModel", "Error searching origin location: ${e.message}")
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _originSearchResults.value = emptyList()
                    Log.e("GuideViewModel", "Exception searching origin location: ${e.message}")
                }
            } finally {
                _isSearchingOrigin.value = false
            }
        }
    }

    fun searchArrivalLocation(query: String) {
        if (query.length < 3) {
            _arrivalSearchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearchingArrival.value = true
            try {
                withContext(Dispatchers.IO) {
                    operationRepository.searchGeographicLocation(query).fold(
                        onSuccess = { locations ->
                            withContext(Dispatchers.Main) {
                                _arrivalSearchResults.value = locations
                            }
                        },
                        onFailure = { e ->
                            withContext(Dispatchers.Main) {
                                _arrivalSearchResults.value = emptyList()
                                Log.e("GuideViewModel", "Error searching arrival location: ${e.message}")
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _arrivalSearchResults.value = emptyList()
                    Log.e("GuideViewModel", "Exception searching arrival location: ${e.message}")
                }
            } finally {
                _isSearchingArrival.value = false
            }
        }
    }

    fun clearOriginSearchResults() {
        _originSearchResults.value = emptyList()
    }

    fun clearArrivalSearchResults() {
        _arrivalSearchResults.value = emptyList()
    }

    // Funciones para consultar datos de personas
    fun searchPersonData(document: String, onSuccess: (IPerson) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    operationRepository.getSntPerson(document).fold(
                        onSuccess = { person ->
                            withContext(Dispatchers.Main) {
                                onSuccess(person)
                            }
                        },
                        onFailure = { e ->
                            withContext(Dispatchers.Main) {
                                onError(e.message ?: "Error al consultar datos")
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError(e.message ?: "Error al consultar datos")
                }
            }
        }
    }

    // Estados para búsqueda de conductores por nombre
    private val _driverSearchResults = MutableStateFlow<List<IPerson>>(emptyList())
    val driverSearchResults: StateFlow<List<IPerson>> = _driverSearchResults.asStateFlow()

    private val _isSearchingDrivers = MutableStateFlow(false)
    val isSearchingDrivers: StateFlow<Boolean> = _isSearchingDrivers.asStateFlow()

    // Función para buscar conductores por nombre
    fun searchDriversByName(query: String, documentType: String) {
        if (query.length < 3) {
            _driverSearchResults.value = emptyList()
            return
        }

        viewModelScope.launch {
            _isSearchingDrivers.value = true
            try {
                withContext(Dispatchers.IO) {
                    operationRepository.searchClientByParameter(
                        search = query,
                        isClient = false,
                        documentType = documentType,
                        isDriver = true
                    ).fold(
                        onSuccess = { response ->
                            withContext(Dispatchers.Main) {
                                _driverSearchResults.value = response.searchClientByParameter?.map { driver ->
                                    IPerson(
                                        id = driver?.id?.toInt() ?: 0,
                                        names = driver?.names ?: "",
                                        documentType = driver?.documentType ?: "",
                                        documentNumber = driver?.documentNumber ?: "",
                                        driverLicense = "" // Se llenará con búsqueda por documento
                                    )
                                } ?: emptyList()
                            }
                        },
                        onFailure = { e ->
                            withContext(Dispatchers.Main) {
                                _driverSearchResults.value = emptyList()
                                Log.e("GuideViewModel", "Error searching drivers: ${e.message}")
                            }
                        }
                    )
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _driverSearchResults.value = emptyList()
                    Log.e("GuideViewModel", "Exception searching drivers: ${e.message}")
                }
            } finally {
                _isSearchingDrivers.value = false
            }
        }
    }

    fun clearDriverSearchResults() {
        _driverSearchResults.value = emptyList()
    }

    // Función de validación
    private fun validateGuide(guide: GuideState): String? {
        // Validación de cliente (excepto para guía de transportista)
        if (guide.documentType != "31" && guide.clientId == 0) {
            return "La guía debe tener un cliente."
        }

        // Validación de serie
        if (guide.serial.length != 4) {
            return "La guía debe tener una serie."
        }

        // Validación de productos
        if (guide.operationDetailSet.isEmpty()) {
            return "Debe agregar al menos un producto a la guía."
        }

        val invalidItems = guide.operationDetailSet.filter { item ->
            item.tariff.productId == 0 || item.quantity <= 0
        }

        if (invalidItems.isNotEmpty()) {
            return "Todos los productos deben tener una cantidad mayor a 0 y un producto seleccionado."
        }

        // Validación de peso total
        if (guide.totalWeight == 0.0) {
            return "La guía debe tener un peso total."
        }

        when (guide.documentType) {
            "09" -> {
                // Guía de remitente
                if (guide.quantityPackages == 0.0) {
                    return "La guía debe tener un número de bultos."
                }

                if (guide.guideModeTransfer == "NA") {
                    return "La guía debe tener un tipo."
                }

                when (guide.guideModeTransfer) {
                    "01" -> {
                        // TRANSPORTE PÚBLICO
                        if (guide.transportationCompanyDocumentNumber.length != 11) {
                            return "La guía debe tener un RUC de transportista válido."
                        }
                        if (guide.transportationCompanyNames.isEmpty()) {
                            return "La guía debe tener una razón social transportista válido."
                        }
                    }
                    "02" -> {
                        // TRANSPORTE PRIVADO
                        return validatePrivateTransport(guide)
                    }
                }

                if (guide.guideReasonTransfer == "NA") {
                    return "La guía debe tener un motivo de traslado."
                }
            }
            "31" -> {
                // Guía de transportista
                val privateTransportError = validatePrivateTransport(guide)
                if (privateTransportError != null) return privateTransportError

                if (guide.receiverDocumentNumber.isEmpty()) {
                    return "La guía debe tener un número de documento de destinatario válido."
                }
                if (guide.receiverNames.isEmpty()) {
                    return "La guía debe tener un nombre de destinatario válido."
                }
            }
        }

        // Validaciones de ubicación
        if (guide.guideOriginDistrictId.isEmpty()) {
            return "La guía debe tener un ubigeo como punto de partida."
        }
        if (guide.guideOriginAddress.isEmpty()) {
            return "La guía debe tener una dirección como punto de partida."
        }
        if (guide.guideArrivalDistrictId.isEmpty()) {
            return "La guía debe tener un ubigeo como punto de llegada."
        }
        if (guide.guideArrivalAddress.isEmpty()) {
            return "La guía debe tener una dirección como punto de llegada."
        }

        // Validación de series para motivo de traslado "04"
        if (guide.guideReasonTransfer == "04") {
            if (guide.guideOriginSerial.isEmpty() || guide.guideArrivalSerial.isEmpty()) {
                return "Los puntos de partida y llegada deben tener un código de establecimiento."
            }
        }

        return null
    }

    private fun validatePrivateTransport(guide: GuideState): String? {
        if (guide.mainVehicleLicensePlate.isEmpty()) {
            return "La guía debe tener la placa de un vehículo principal válido."
        }

        val invalidOtherVehicles = guide.othersVehicles.filter { vehicle ->
            vehicle.licensePlate.isNullOrEmpty()
        }

        if (invalidOtherVehicles.isNotEmpty()) {
            return "Todos los vehículos deben tener una placa."
        }

        // Validación de conductor principal
        when (guide.mainDriverDocumentType) {
            "1" -> {
                if (guide.mainDriverDocumentNumber.length != 8) {
                    return "La guía debe tener número DNI de un conductor principal válido."
                }
            }
            "6" -> {
                if (guide.mainDriverDocumentNumber.length != 11) {
                    return "La guía debe tener número RUC de un conductor principal válido."
                }
            }
        }

        if (guide.mainDriverNames.isEmpty()) {
            return "La guía debe tener nombres y apellidos de un conductor principal válido."
        }
        if (guide.mainDriverDriverLicense.isEmpty()) {
            return "La guía debe tener una licencia de un conductor principal válido."
        }

        // Validación de conductores secundarios
        val invalidOtherDrivers = guide.othersDrivers.filter { driver ->
            driver.documentNumber.isNullOrEmpty() || 
            driver.names.isNullOrEmpty() || 
            driver.driverLicense.isNullOrEmpty()
        }

        if (invalidOtherDrivers.isNotEmpty()) {
            return "Todos los conductores deben tener un número de documento, un nombre y una licencia de conducir."
        }

        // Validaciones de formato para placas y licencias (solo para modo "02")
        if (guide.guideModeTransfer == "02" || guide.documentType == "31") {
            // Validar placa del vehículo principal
            val mainVehicleLicensePlate = guide.mainVehicleLicensePlate
            if (!isValidLicensePlate(mainVehicleLicensePlate)) {
                return "La placa del vehículo principal debe tener entre 6 y 7 caracteres, no debe tener espacios y solo puede contener un guion."
            }

            // Validar placas de otros vehículos
            val invalidVehiclePlates = guide.othersVehicles.filter { vehicle ->
                !isValidLicensePlate(vehicle.licensePlate ?: "")
            }

            if (invalidVehiclePlates.isNotEmpty()) {
                return "Las placas de los vehículos deben tener entre 6 y 7 caracteres, no deben tener espacios y solo pueden contener un guion."
            }

            // Validar licencia del conductor principal
            val mainDriverLicense = guide.mainDriverDriverLicense
            if (!isValidDriverLicense(mainDriverLicense)) {
                return "La licencia del conductor principal debe tener entre 8 y 12 caracteres, no debe tener espacios y solo puede contener un guion."
            }

            // Validar licencias de otros conductores
            val invalidDriverLicenses = guide.othersDrivers.filter { driver ->
                !isValidDriverLicense(driver.driverLicense ?: "")
            }

            if (invalidDriverLicenses.isNotEmpty()) {
                return "Las licencias de los conductores deben tener entre 8 y 12 caracteres, no deben tener espacios y solo pueden contener un guion."
            }
        }

        return null
    }

    private fun isValidLicensePlate(licensePlate: String): Boolean {
        if (licensePlate.isEmpty()) return false
        val pattern = Regex("^\\S+(-\\S+)?$")
        return pattern.matches(licensePlate) && licensePlate.length in 6..7
    }

    private fun isValidDriverLicense(driverLicense: String): Boolean {
        if (driverLicense.isEmpty()) return false
        val pattern = Regex("^\\S+(-\\S+)?$")
        return pattern.matches(driverLicense) && driverLicense.length in 8..12
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
        var guideOriginSerial: String = "0000",
        var guideArrivalDistrictId: String = "",
        var guideArrivalAddress: String = "",
        var guideArrivalSerial: String = "0000",
        var observation: String = ""
    )
}