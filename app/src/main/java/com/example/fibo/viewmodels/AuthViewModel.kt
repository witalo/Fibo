package com.example.fibo.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apollographql.apollo3.ApolloClient
import com.apollographql.apollo3.api.ApolloResponse
import com.example.fibo.QrScanMutation
import com.example.fibo.model.IUserData
import com.example.fibo.datastore.PreferencesManager
import com.example.fibo.model.ICompany
import com.example.fibo.model.ISubsidiary
import com.example.fibo.model.IUser
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val apolloClient: ApolloClient,
    private val preferencesManager: PreferencesManager,
    @ApplicationContext private val context: Context
) : ViewModel() {

    // Estados
    private val _scanResult = MutableStateFlow<ApolloResponse<QrScanMutation.Data>?>(null)
    val scanResult: StateFlow<ApolloResponse<QrScanMutation.Data>?> = _scanResult.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Limpiar estados al iniciar
        clearStates()
    }

    fun clearStates() {
        _scanResult.value = null
        _error.value = null
        _isLoading.value = false
    }

    fun scanQr(token: String, username: String, description: String) {
        if (isLoading.value) return

        _isLoading.value = true
        clearError()

        viewModelScope.launch {
            try {
                val response = apolloClient.mutation(
                    QrScanMutation(
                        token = token,
                        username = username,
                        description = description
                    )
                ).execute()

                _scanResult.value = response

                if (response.data?.qrScan?.success == true) {
                    Log.d("Italo scan", response.data?.qrScan.toString())
                    val userData = IUserData(
                        success = response.data!!.qrScan?.success!!,
                        message = response.data!!.qrScan?.message ?: "",
                        company = response.data!!.qrScan?.company?.let {
                            ICompany(
                                id = it.id!!,
                                doc = it.doc!!,
                                businessName = it.businessName!!,
                                logo = it.logo!!,
                                percentageIgv = it.percentageIgv?.toDouble()!!,
                                isEnabled = it.isEnabled,
                                withStock = it.withStock,
                                appMobil = it.app,
                                disableContinuePay = it.disableContinuePay
                            )
                        },
                        subsidiary = response.data?.qrScan?.subsidiary?.let {
                            ISubsidiary(
                                id = it.id.toInt(),
                                serial = it.serial!!,
                                name = it.name!!,
                                address = it.address!!,
                                token = it.token!!
                            )
                        },
                        user = response.data?.qrScan?.user?.let {
                            IUser(id = it.id.toInt())
                        }
                    )
                    preferencesManager.saveUserData(userData)
                } else {
                    setError(response.data?.qrScan?.message ?: "Error desconocido")
                }
            } catch (e: Exception) {
                setError(e.message ?: "Error de conexión")
                Log.d("Italo scan", e.message.toString())
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            preferencesManager.clearUserData()
            clearStates()
        }
    }

    private fun setError(message: String) {
        _error.value = message
    }

    private fun clearError() {
        _error.value = null
    }
}