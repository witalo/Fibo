package com.example.fibo.ui.screens.person

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.model.IPerson
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
            shortNameError = if (shortName.isBlank()) "El nombre corto es obligatorio" else ""
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
        _uiState.value = _uiState.value.copy(
            documentNumber = documentNumber,
            documentNumberError = if (documentNumber.isBlank()) "El número de documento es obligatorio" else ""
        )
        validateForm()
    }

    fun onPhoneChanged(phone: String) {
        _uiState.value = _uiState.value.copy(
            phone = phone,
            phoneError = if (phone.isBlank()) "El teléfono es obligatorio" else ""
        )
        validateForm()
    }

    fun onEmailChanged(email: String) {
        _uiState.value = _uiState.value.copy(
            email = email,
            emailError = if (email.isBlank()) "El email es obligatorio" else ""
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

    fun onEconomicActivityMainChanged(economicActivityMain: Int) {
        _uiState.value = _uiState.value.copy(economicActivityMain = economicActivityMain)
    }

    fun onDriverLicenseChanged(driverLicense: String) {
        _uiState.value = _uiState.value.copy(driverLicense = driverLicense)
    }

    fun createPerson(subsidiaryId: Int?) {
        if (subsidiaryId == null) {
            _uiState.value = _uiState.value.copy(
                personResult = Result.failure(Exception("ID de subsidiaria no válido"))
            )
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val result = personRepository.createPerson(
                    names = uiState.value.names,
                    shortName = uiState.value.shortName,
                    code = uiState.value.code,
                    phone = uiState.value.phone,
                    email = uiState.value.email,
                    address = uiState.value.address,
                    country = "Perú", // Por defecto
                    districtId = "1", // Por defecto
                    documentType = uiState.value.documentType,
                    documentNumber = uiState.value.documentNumber,
                    isEnabled = true,
                    isSupplier = uiState.value.isSupplier,
                    isClient = uiState.value.isClient,
                    economicActivityMain = uiState.value.economicActivityMain,
                    driverLicense = uiState.value.driverLicense,
                    subsidiaryId = subsidiaryId
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
                currentState.shortName.isNotBlank() &&
                currentState.documentType.isNotBlank() &&
                currentState.documentNumber.isNotBlank() &&
                currentState.phone.isNotBlank() &&
                currentState.email.isNotBlank() &&
                currentState.address.isNotBlank() &&
                (currentState.isClient || currentState.isSupplier)

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
    val economicActivityMain: Int = 0,
    val driverLicense: String = "",
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    val personResult: Result<String>? = null,
    // Errores de validación
    val namesError: String = "",
    val shortNameError: String = "",
    val documentTypeError: String = "",
    val documentNumberError: String = "",
    val phoneError: String = "",
    val emailError: String = "",
    val addressError: String = ""
)