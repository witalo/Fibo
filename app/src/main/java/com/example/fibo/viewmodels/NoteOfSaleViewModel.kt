package com.example.fibo.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.ISubsidiary
import com.example.fibo.repository.OperationRepository
import com.example.fibo.utils.NoteOfSaleState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject


@HiltViewModel
class NoteOfSaleViewModel @Inject constructor(
    private val operationRepository: OperationRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {
    private val _selectedDate = MutableStateFlow(
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    )
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    private val _noteOfSaleState = MutableStateFlow<NoteOfSaleState>(NoteOfSaleState.WaitingForUser)
    val noteOfSaleState: StateFlow<NoteOfSaleState> = _noteOfSaleState.asStateFlow()

    val userData = preferencesManager.userData
    val subsidiaryData: StateFlow<ISubsidiary?> = preferencesManager.subsidiaryData


    init {
        // Cargar notas de salida cuando se inicializa el ViewModel y hay un usuario válido
        viewModelScope.launch {
            // Observar cambios en userData y cargar notas de salida cuando tenemos un usuario
            preferencesManager.userData.collect { user ->
                if (user != null) {
                    // Aquí pasamos el ID del usuario junto con la fecha
                    loadNoteOfSale(_selectedDate.value, user.id)
                } else {
                    _noteOfSaleState.value = NoteOfSaleState.WaitingForUser
                }
            }
        }
    }

    fun loadNoteOfSale(date: String, userId: Int) {
        viewModelScope.launch {
            _noteOfSaleState.value = NoteOfSaleState.Loading
            val types = listOf("48")
            try {
                val notesOfSale =
                    operationRepository.getOperationsByDateAndUserId(date, userId, types)
                _noteOfSaleState.value = NoteOfSaleState.Success(notesOfSale)
            } catch (e: Exception) {
                _noteOfSaleState.value = NoteOfSaleState.Error(
                    e.message ?: "Error al cargar las notas de salida"
                )
            }
        }
    }

    // Método corregido para pasar también el userId
    fun updateDate(date: String) {
        _selectedDate.value = date
        // Obtenemos el usuario actual
        val user = userData.value

        // Si tenemos un usuario, cargamos las notas de salida con la nueva fecha
        if (user != null) {
            loadNoteOfSale(date, user.id)
        } else {
            _noteOfSaleState.value = NoteOfSaleState.WaitingForUser
        }
    }
}