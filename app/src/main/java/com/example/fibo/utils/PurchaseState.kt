package com.example.fibo.utils

import com.example.fibo.model.IOperation

sealed class PurchaseState {
    object Loading : PurchaseState()
    object WaitingForUser : PurchaseState()
    data class Success(val data: List<IOperation>) : PurchaseState()
    data class Error(val message: String) : PurchaseState()
} 