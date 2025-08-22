package com.example.fibo.ui.screens.person

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.IPerson
import com.example.fibo.repository.PersonRepository
import com.example.fibo.utils.PersonState
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PersonViewModel @Inject constructor(
    private val personRepository: PersonRepository,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _personState = MutableStateFlow<PersonState>(PersonState.WaitingForUser)
    val personState: StateFlow<PersonState> = _personState.asStateFlow()

    val userData = preferencesManager.userData
    val subsidiaryData = preferencesManager.subsidiaryData
    val companyData = preferencesManager.companyData

    // Estados para el filtro de tipos de persona
    private val _selectedTypes = MutableStateFlow(setOf("CLIENT", "SUPPLIER", "DRIVER"))
    val selectedTypes: StateFlow<Set<String>> = _selectedTypes.asStateFlow()

    // Estados para la búsqueda
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<IPerson>>(emptyList())
    val searchResults: StateFlow<List<IPerson>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _selectedPerson = MutableStateFlow<IPerson?>(null)
    val selectedPerson: StateFlow<IPerson?> = _selectedPerson.asStateFlow()

    init {
        // Cargar personas cuando se inicializa el ViewModel
        viewModelScope.launch {
            preferencesManager.subsidiaryData.collect { subsidiary ->
                if (subsidiary != null) {
                    loadPersons(subsidiary.id, getCurrentTypes())
                }
            }
        }

        // Observar cambios en los tipos seleccionados
        viewModelScope.launch {
            selectedTypes.collect { types ->
                subsidiaryData.value?.let { subsidiary ->
                    if (types.isNotEmpty()) {
                        loadPersons(subsidiary.id, types.joinToString(","))
                    }
                }
            }
        }
    }

    fun loadPersons(subsidiaryId: Int, types: String) {
        viewModelScope.launch {
            _personState.value = PersonState.Loading
            try {
                val persons = personRepository.getAllPersonsBySubsidiaryAndType(subsidiaryId, types)
                _personState.value = PersonState.Success(persons)
            } catch (e: Exception) {
                _personState.value = PersonState.Error(
                    e.message ?: "Error al cargar los datos de personas"
                )
            }
        }
    }

    // Función para actualizar los tipos seleccionados
    fun updateSelectedTypes(types: Set<String>) {
        _selectedTypes.value = types
    }

    // Función para alternar un tipo específico
    fun togglePersonType(type: String) {
        val currentTypes = _selectedTypes.value.toMutableSet()
        if (currentTypes.contains(type)) {
            currentTypes.remove(type)
        } else {
            currentTypes.add(type)
        }
        _selectedTypes.value = currentTypes
    }

    fun getCurrentTypes(): String {
        return _selectedTypes.value.joinToString(",")
    }

    // Función para buscar personas
    fun searchPersons(query: String) {
        _searchQuery.value = query
        if (query.length >= 2) {
            viewModelScope.launch {
                _isSearching.value = true
                try {
                    val currentPersons = (_personState.value as? PersonState.Success)?.data ?: emptyList()
                    val filtered = currentPersons.filter { person ->
                        person.names?.contains(query, ignoreCase = true) == true ||
                                person.documentNumber?.contains(query, ignoreCase = true) == true
                    }
                    _searchResults.value = filtered
                } catch (e: Exception) {
                    _searchResults.value = emptyList()
                } finally {
                    _isSearching.value = false
                }
            }
        } else {
            _searchResults.value = emptyList()
        }
    }

    // Función para seleccionar una persona
    fun selectPerson(person: IPerson) {
        _selectedPerson.value = person
    }

    // Función para limpiar la selección
    fun clearPersonSelection() {
        _selectedPerson.value = null
        _searchQuery.value = ""
        _searchResults.value = emptyList()
    }

    // Función para recargar personas después de crear/editar
    fun refreshPersons() {
        subsidiaryData.value?.let { subsidiary ->
            loadPersons(subsidiary.id, getCurrentTypes())
        }
    }
}