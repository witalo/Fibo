package com.example.fibo.ui.screens.person
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.repository.PersonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Result

@HiltViewModel
class NewPersonViewModel @Inject constructor(
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewPersonUiState())
    val uiState: StateFlow<NewPersonUiState> = _uiState.asStateFlow()

    fun onNamesChanged(names: String) {
        _uiState.value = _uiState.value.copy(
            names = names,
            namesError = if (names.isBlank()) "Los nombres son obligatorios" else ""
        )
        validateForm()
    }

    fun onShortNameChanged(shortName: String) {
        _uiState.value = _uiState.value.copy(
            shortName = shortName,
            shortNameError = if (shortName.isBlank()) "La razón comercial es obligatoria" else ""
        )
        validateForm()
    }

    fun onCodeChanged(code: String) {
        _uiState.value = _uiState.value.copy(code = code)
    }

    fun onDocumentTypeChanged(documentType: String) {
        _uiState.value = _uiState.value.copy(
            documentType = documentType,
            documentTypeError = if (documentType.isBlank()) "El tipo de documento es obligatorio" else ""
        )
        validateForm()
    }

    fun onDocumentNumberChanged(documentNumber: String) {
        val currentState = _uiState.value
        val documentType = currentState.documentType

        // Validar longitud según tipo de documento
        val isValidLength = when (documentType) {
            "1" -> documentNumber.length == 8  // DNI: 8 dígitos
            "6" -> documentNumber.length == 11 // RUC: 11 dígitos
            else -> documentNumber.isNotBlank() // Otros documentos
        }

        val errorMessage = when {
            documentNumber.isBlank() -> "El número de documento es obligatorio"
            !isValidLength -> when (documentType) {
                "1" -> "El DNI debe tener 8 dígitos"
                "6" -> "El RUC debe tener 11 dígitos"
                else -> "Número de documento inválido"
            }
            else -> ""
        }

        _uiState.value = _uiState.value.copy(
            documentNumber = documentNumber,
            documentNumberError = errorMessage
        )
        validateForm()
    }

    fun onPhoneChanged(phone: String) {
        _uiState.value = _uiState.value.copy(
            phone = phone,
            phoneError = "" // Ya no es obligatorio
        )
        validateForm()
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = "" // Ya no es obligatorio
        )
        validateForm()
    }

    fun onAddressChanged(address: String) {
        _uiState.value = _uiState.value.copy(
            address = address,
            addressError = if (address.isBlank()) "La dirección es obligatoria" else ""
        )
        validateForm()
    }

    fun onIsClientChanged(isClient: Boolean) {
        _uiState.value = _uiState.value.copy(isClient = isClient)
        validateForm()
    }

    fun onIsSupplierChanged(isSupplier: Boolean) {
        _uiState.value = _uiState.value.copy(isSupplier = isSupplier)
        validateForm()
    }

    fun onIsDriverChanged(isDriver: Boolean) {
        _uiState.value = _uiState.value.copy(isDriver = isDriver)
        validateForm()
    }

    fun onIsEnabledChanged(isEnabled: Boolean) {
        _uiState.value = _uiState.value.copy(isEnabled = isEnabled)
    }

    fun onDriverLicenseChanged(driverLicense: String) {
        _uiState.value = _uiState.value.copy(
            driverLicense = driverLicense,
            driverLicenseError = if (uiState.value.isDriver && driverLicense.isBlank()) {
                "La licencia de conducir es obligatoria para conductores"
            } else ""
        )
        validateForm()
    }


    fun extractPersonData() {
        val documentNumber = uiState.value.documentNumber
        if (documentNumber.isBlank()) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExtracting = true)

            try {
                val result = personRepository.getSntPerson(documentNumber)
                result.onSuccess { personData ->
                    _uiState.value = _uiState.value.copy(
                        names = personData.names ?: "",
                        address = personData.address ?: "",
                        driverLicense = personData.driverLicense ?: ""
                    )
                }.onFailure { error ->
                    // Mostrar error en snackbar o manejar como prefieras
                    println("Error al extraer datos: ${error.message}")
                }
            } catch (e: Exception) {
                println("Error al extraer datos: ${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isExtracting = false)
            }
        }
    }

    fun createPerson(subsidiaryId: Int?) {
        if (subsidiaryId == null) {
            _uiState.value = _uiState.value.copy(
                personResult = Result.failure(Exception("ID de subsidiaria no válido"))
            )
            return
        }
        // Evitar múltiples llamadas si ya está cargando
        if (uiState.value.isLoading) {
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                val result = personRepository.createPerson(
                    names = uiState.value.names,
                    shortName = uiState.value.shortName,
                    code = uiState.value.code,
                    phone = uiState.value.phone.ifBlank { "" }, // Ahora opcional
                    email = uiState.value.email.ifBlank { "" }, // Ahora opcional
                    address = uiState.value.address,
                    country = "PE",
                    districtId = "040601", // Por defecto como solicitaste
                    documentType = uiState.value.documentType,
                    documentNumber = uiState.value.documentNumber,
                    isEnabled = uiState.value.isEnabled,
                    isSupplier = uiState.value.isSupplier,
                    isClient = uiState.value.isClient,
                    isDriver = uiState.value.isDriver,
                    driverLicense = uiState.value.driverLicense,
                    subsidiaryId = subsidiaryId,
                    economicActivityMain = 0
                )

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    personResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    personResult = Result.failure(e)
                )
            }
        }
    }

    fun resetPersonResult() {
        _uiState.value = _uiState.value.copy(personResult = null)
    }

    private fun validateForm() {
        val currentState = _uiState.value
        val isValid = currentState.names.isNotBlank() &&
                currentState.documentType.isNotBlank() &&
                currentState.documentNumber.isNotBlank() &&
//                currentState.phone.isNotBlank() &&
//                currentState.email.isNotBlank() &&
                currentState.address.isNotBlank() &&
                (currentState.isClient || currentState.isSupplier || currentState.isDriver) &&
                // Validar licencia de conducir solo si es conductor
                (!currentState.isDriver || currentState.driverLicense.isNotBlank())

        _uiState.value = currentState.copy(isFormValid = isValid)
    }
}

data class NewPersonUiState(
    val names: String = "",
    val shortName: String = "",
    val code: String = "",
    val documentType: String = "",
    val documentNumber: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val isClient: Boolean = false,
    val isSupplier: Boolean = false,
    val isDriver: Boolean = false,
    val isEnabled: Boolean = true,
    val driverLicense: String = "",
    val isLoading: Boolean = false,
    val isExtracting: Boolean = false,
    val isFormValid: Boolean = false,
    val personResult: Result<String>? = null,
    // Errores de validación
    val namesError: String = "",
    val shortNameError: String = "",
    val documentTypeError: String = "",
    val documentNumberError: String = "",
    val phoneError: String = "",
    val emailError: String = "",
    val addressError: String = "",
    val driverLicenseError: String = "" // Agregar este campo
)