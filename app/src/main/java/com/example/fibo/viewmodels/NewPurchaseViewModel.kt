package com.example.fibo.ui.screens.purchase.viewmodel

import androidx.lifecycle.ViewModel
import com.example.fibo.model.IProductOperation
import com.example.fibo.model.ISupplier
import com.example.fibo.model.PaymentMethod
import com.example.fibo.repository.OperationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import kotlin.Result

@HiltViewModel
class NewPurchaseViewModel @Inject constructor(
    private val operationRepository: OperationRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(NewPurchaseUiState())
    val uiState: StateFlow<NewPurchaseUiState> = _uiState.asStateFlow()

}

data class NewPurchaseUiState(
    val supplier: ISupplier? = null,
    val documentType: String = "",
    val emitDate: String = "",
    val products: List<IProductOperation> = emptyList(),
    val paymentMethod: PaymentMethod? = null,
    val isLoading: Boolean = false,
    val isFormValid: Boolean = false,
    val purchaseResult: Result<String>? = null
)