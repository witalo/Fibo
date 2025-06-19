package com.example.fibo.viewmodels

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.IOperation
import com.example.fibo.model.IPerson
import com.example.fibo.model.ISubsidiary
import com.example.fibo.repository.OperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject


@HiltViewModel
class GuideViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _invoiceState = MutableStateFlow<InvoiceState>(InvoiceState.WaitingForUser)
    val invoiceState: StateFlow<InvoiceState> = _invoiceState.asStateFlow()

    // Store the userId when available
    private val _currentUserId = MutableStateFlow<Int?>(null)
    val subsidiaryData: StateFlow<ISubsidiary?> = preferencesManager.subsidiaryData

    // Estado para el diálogo de PDF
    private val _showPdfDialog = MutableStateFlow(false)
    val showPdfDialog: StateFlow<Boolean> = _showPdfDialog
    private val _currentInvoiceId = MutableStateFlow(0)
    val currentInvoiceId: StateFlow<Int> = _currentInvoiceId

    // Añade esto para manejar eventos de refresco
    private val _refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    private val _showCancelDialog = MutableStateFlow(false)
    val showCancelDialog: StateFlow<Boolean> = _showCancelDialog.asStateFlow()

    private val _currentOperationId = MutableStateFlow(0)
    val currentOperationId: StateFlow<Int> = _currentOperationId.asStateFlow()

    // Nuevos estados para la búsqueda de clientes
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<IPerson>>(emptyList())
    val searchResults: StateFlow<List<IPerson>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedClient = MutableStateFlow<IPerson?>(null)
    val selectedClient: StateFlow<IPerson?> = _selectedClient.asStateFlow()
    // Nuevos estados para la búsqueda de clientes

    fun triggerRefresh() {
        viewModelScope.launch {
            _refreshTrigger.emit(Unit)
        }
    }
    init {
        // 1. Collector para datos de usuario
        viewModelScope.launch {
            preferencesManager.userData.collect { user ->
                val previousUserId = _currentUserId.value
                _currentUserId.value = user?.id

                // Si el ID cambió y ahora tenemos un usuario válido, cargamos facturas
                if (previousUserId != user?.id && user?.id != null) {
                    Log.d("italo", "Usuario autenticado con ID: ${user.id}")
                    loadInvoices(_selectedDate.value)
                } else if (user?.id == null) {
                    // Si no hay usuario, actualizamos el estado
                    _invoiceState.value = InvoiceState.WaitingForUser
                    Log.d("italo", "Esperando por un usuario válido")
                }
            }
        }

        // 2. Collector independiente para eventos de refresco
        viewModelScope.launch {
            _refreshTrigger.collect {
                _currentUserId.value?.let {
                    loadInvoices(_selectedDate.value)
                }
            }
        }

        // 3. Observamos cambios en la fecha seleccionada
        viewModelScope.launch {
            _selectedDate.collect { date ->
                // Solo cargamos si tenemos un usuario
                _currentUserId.value?.let {
                    loadInvoices(date)
                }
            }
        }
    }

    fun updateSelectedDate(date: String) {
        Log.d("italo", "Fecha seleccionada actualizada: $date")
        _selectedDate.value = date
        // No llamamos a loadInvoices aquí, ya que el collector de _selectedDate se encargará
    }
    fun loadInvoices(date: String) {
        viewModelScope.launch {
            _invoiceState.value = InvoiceState.Loading

            val userId = _currentUserId.value
            if (userId == null) {
                Log.d("italo", "Intento de cargar facturas sin usuario autenticado")
                _invoiceState.value = InvoiceState.WaitingForUser
                return@launch
            }

            try {
                Log.d("italo", "Cargando operaciones para userId: $userId y fecha: $date")
                val invoices = operationRepository.getOperationByDate(date, userId)
                Log.d("italo", "Operaciones cargadas: ${invoices.size}")
                _invoiceState.value = InvoiceState.Success(invoices)
            } catch (e: Exception) {
                Log.e("italo", "Error al cargar operaciones", e)
                _invoiceState.value = InvoiceState.Error(
                    e.message ?: "Error al cargar las operaciones"
                )
            }
        }
    }

    sealed class InvoiceState {
        object WaitingForUser : InvoiceState() // Nuevo estado para esperar autenticación
        object Loading : InvoiceState()
        data class Success(val data: List<IOperation>) : InvoiceState()
        data class Error(val message: String) : InvoiceState()
    }

    // Resto de funciones sin cambios...

    // Función para mostrar el diálogo de PDF
    private var lastDialogCloseTime = 0L
    fun showPdfDialog(invoiceId: Int) {
        // Verificar si ha pasado suficiente tiempo desde el último cierre
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDialogCloseTime < 1500) {
            // Si no ha pasado suficiente tiempo, no hacer nada
            return
        }
        _currentInvoiceId.value = invoiceId
        _showPdfDialog.value = true
    }

    // Función para cerrar el diálogo
    fun closePdfDialog() {
        _showPdfDialog.value = false
        // Registrar el tiempo de cierre
        lastDialogCloseTime = System.currentTimeMillis()
    }

    fun showCancelDialog(operationId: Int) {
        _currentOperationId.value = operationId
        _showCancelDialog.value = true
    }

    fun closeCancelDialog() {
        _showCancelDialog.value = false
    }
    fun cancelOperation(operationId: Int, operationType: String, emitDate: String) {
        viewModelScope.launch {
            try {
                // Parsear la fecha de emisión
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val emitDateObj = dateFormat.parse(emitDate) ?: throw Exception("Fecha inválida")
                val currentDate = Date()

                // Calcular días de diferencia
                val diffInMillis = currentDate.time - emitDateObj.time
                val diffInDays = TimeUnit.MILLISECONDS.toDays(diffInMillis)

                // Validar según tipo de comprobante
                when (operationType) {
                    "01" -> { // Factura
                        if (diffInDays > 3) {
                            Toast.makeText(
                                context,
                                "No se puede anular facturas con más de 3 días de emisión",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                    }
                    "03" -> { // Boleta
                        if (diffInDays > 5) {
                            Toast.makeText(
                                context,
                                "No se puede anular boletas con más de 5 días de emisión",
                                Toast.LENGTH_LONG
                            ).show()
                            return@launch
                        }
                    }
                    else -> {
                        Toast.makeText(
                            context,
                            "Tipo de comprobante no soportado para anulación",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@launch
                    }
                }

                // Si pasa las validaciones, proceder con la anulación
                val currentDateStr = dateFormat.format(currentDate)
                val result = operationRepository.cancelInvoice(operationId, currentDateStr)
                result.fold(
                    onSuccess = { message ->
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        closeCancelDialog()
                        loadInvoices(selectedDate.value)
                    },
                    onFailure = { error ->
                        Toast.makeText(
                            context,
                            error.message ?: "Error al anular el comprobante",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Error: ${e.message ?: "No se pudo validar la fecha"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
//    fun cancelOperation(operationId: Int) {
//        viewModelScope.launch {
//            try {
//
//                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
//                val result = operationRepository.cancelInvoice(operationId, currentDate)
//                result.fold(
//                    onSuccess = { message ->
//                        // Mostrar mensaje de éxito
//                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
//                        closeCancelDialog()
//                        loadInvoices(selectedDate.value)
//                    },
//                    onFailure = { error ->
//                        // Mostrar mensaje de error
//                        Toast.makeText(context, error.message ?: "Error al anular el comprobante", Toast.LENGTH_SHORT).show()
//                    }
//                )
//            } catch (e: Exception) {
//                Toast.makeText(context, e.message ?: "Error al anular el comprobante", Toast.LENGTH_SHORT).show()
//            }
//        }
//    }

    // Función para buscar clientes
    fun searchClients(query: String) {
        _searchQuery.value = query
        if (query.length >= 3) { // Solo buscar si hay al menos 3 caracteres
            viewModelScope.launch {
                _isSearching.value = true
                try {
                    val results = operationRepository.searchPersons(query)
                    _searchResults.value = results
                } catch (e: Exception) {
                    _searchResults.value = emptyList()
                    // Podrías manejar el error aquí si lo deseas
                } finally {
                    _isSearching.value = false
                }
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    // Función para seleccionar un cliente y cargar sus cotizaciones
    fun selectClient(client: IPerson) {
        _selectedClient.value = client
        val userId = _currentUserId.value
        if (userId != null) {
            loadInvoicesByPersonAndUser(client.id, userId)
        }
    }

    // Función para cargar cotizaciones por cliente
    fun loadInvoicesByPersonAndUser(personId: Int, userId: Int) {
        viewModelScope.launch {
            _invoiceState.value = InvoiceState.Loading
            val types = listOf("01", "03") // Tipo para cotizaciones
            try {
                val invoices = operationRepository.getOperationsByPersonAndUser(
                    personId = personId,
                    userId = userId,
                    types = types
                )
                _invoiceState.value = InvoiceState.Success(invoices)
            } catch (e: Exception) {
                _invoiceState.value = InvoiceState.Error(
                    e.message ?: "Error al cargar los comprobantes del cliente"
                )
            }
        }
    }

    // Función para limpiar la búsqueda y volver a las cotizaciones por fecha
    fun clearClientSearch() {
        _selectedClient.value = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
        // Volver a cargar por fecha
        val userId = _currentUserId.value
        val date = selectedDate.value
        if (userId != null) {
            loadInvoices(date)
        }
    }
}