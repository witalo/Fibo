package com.example.fibo.viewmodels

import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import com.example.fibo.datastore.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.example.fibo.model.IOperation
import com.example.fibo.model.ISubsidiary
import com.example.fibo.repository.OperationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Date
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*
import android.widget.Toast
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
@HiltViewModel
class HomeViewModel @Inject constructor(
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

    fun cancelOperation(operationId: Int) {
        viewModelScope.launch {
            try {
                val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                val result = operationRepository.cancelInvoice(operationId, currentDate)
                result.fold(
                    onSuccess = { message ->
                        // Mostrar mensaje de éxito
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                        closeCancelDialog()
                        loadInvoices(selectedDate.value)
                    },
                    onFailure = { error ->
                        // Mostrar mensaje de error
                        Toast.makeText(context, error.message ?: "Error al anular el comprobante", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(context, e.message ?: "Error al anular el comprobante", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
//@HiltViewModel
//class HomeViewModel @Inject constructor(
//    private val operationRepository: OperationRepository,
//    private val preferencesManager: PreferencesManager,
//    @ApplicationContext private val context: Context
//) : ViewModel() {
//    private val _selectedDate = MutableStateFlow(
//        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
//    )
//    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()
//
//    private val _invoiceState = MutableStateFlow<InvoiceState>(InvoiceState.Loading)
//    val invoiceState: StateFlow<InvoiceState> = _invoiceState.asStateFlow()
//
//    // Store the userId when available
//    private var currentUserId: Int? = null
//    val subsidiaryData: StateFlow<ISubsidiary?> = preferencesManager.subsidiaryData
//
//    // Estado para el diálogo de PDF
//    private val _showPdfDialog = MutableStateFlow(false)
//    val showPdfDialog: StateFlow<Boolean> = _showPdfDialog
//    private val _currentInvoiceId = MutableStateFlow(0)
//    val currentInvoiceId: StateFlow<Int> = _currentInvoiceId
//
//    // Añade esto para manejar eventos de refresco
//    private val _refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
//
//    private val _showCancelDialog = MutableStateFlow(false)
//    val showCancelDialog: StateFlow<Boolean> = _showCancelDialog.asStateFlow()
//
//    private val _currentOperationId = MutableStateFlow(0)
//    val currentOperationId: StateFlow<Int> = _currentOperationId.asStateFlow()
//
//    fun triggerRefresh() {
//        viewModelScope.launch {
//            _refreshTrigger.emit(Unit)
//        }
//    }
//
//    init {
//        // 1. Collector para datos de usuario
//        viewModelScope.launch {
//            preferencesManager.userData.collect { user ->
//                currentUserId = user?.id
//                user?.id?.let {
//                    // Carga facturas cuando tenemos un usuario válido
//                    loadInvoices(_selectedDate.value)
//                }
//            }
//        }
//
//        // 2. Collector independiente para eventos de refresco
//        viewModelScope.launch {
//            _refreshTrigger.collect {
//                currentUserId?.let {
//                    loadInvoices(_selectedDate.value)
//                }
//            }
//        }
//    }
//
//    fun updateSelectedDate(date: String) {
//        Log.d("italo", "Fecha: $date")
//        _selectedDate.value = date
//        loadInvoices(date)
//    }
//
//    fun loadInvoices(date: String) {
//        viewModelScope.launch {
//            _invoiceState.value = InvoiceState.Loading
//            Log.d("italo", "Usuario: $currentUserId")
//            val userId = currentUserId
//            if (userId == null) {
//                _invoiceState.value = InvoiceState.Error("Usuario no autenticado")
//                return@launch
//            }
//
//            try {
//                Log.d("italo", "Cargando operaciones para userId: $userId y fecha: $date")
//                val invoices = operationRepository.getOperationByDate(date, userId)
//                Log.d("italo", "Operaciones cargadas: $invoices")
//                _invoiceState.value = InvoiceState.Success(invoices)
//            } catch (e: Exception) {
//                Log.e("italo", "Error al cargar operaciones", e)
//                _invoiceState.value = InvoiceState.Error(
//                    e.message ?: "Error al cargar las operaciones"
//                )
//            }
//        }
//    }
//
//    sealed class InvoiceState {
//        object Loading : InvoiceState()
//        data class Success(val data: List<IOperation>) : InvoiceState()
//        data class Error(val message: String) : InvoiceState()
//    }
//    // Función para mostrar el diálogo de PDF
//    fun showPdfDialog(invoiceId: Int) {
//        _currentInvoiceId.value = invoiceId
//        _showPdfDialog.value = true
//    }
//
//    // Función para cerrar el diálogo
//    fun closePdfDialog() {
//        _showPdfDialog.value = false
//    }
//
//    fun showCancelDialog(operationId: Int) {
//        _currentOperationId.value = operationId
//        _showCancelDialog.value = true
//    }
//
//    fun closeCancelDialog() {
//        _showCancelDialog.value = false
//    }
//
//    fun cancelOperation(operationId: Int) {
//        viewModelScope.launch {
//            try {
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
//}
