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

@HiltViewModel
class PersonDetailViewModel @Inject constructor(
    private val personRepository: PersonRepository
) : ViewModel() {

    private val _personState = MutableStateFlow<PersonDetailState>(PersonDetailState.Loading)
    val personState: StateFlow<PersonDetailState> = _personState.asStateFlow()

    fun loadPerson(personId: Int) {
        viewModelScope.launch {
            _personState.value = PersonDetailState.Loading
            try {
                val person = personRepository.getPersonById(personId)
                _personState.value = PersonDetailState.Success(person)
            } catch (e: Exception) {
                _personState.value = PersonDetailState.Error(
                    e.message ?: "Error al cargar los datos de la persona"
                )
            }
        }
    }
}

sealed class PersonDetailState {
    object Loading : PersonDetailState()
    data class Success(val person: IPerson) : PersonDetailState()
    data class Error(val message: String) : PersonDetailState()
}
