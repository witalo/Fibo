package com.example.fibo.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.fibo.model.ICompany
import com.example.fibo.model.ISubsidiary
import com.example.fibo.model.IUser
import com.example.fibo.model.IUserData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
// Extensión para DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

class PreferencesManager(context: Context) {
    private val dataStore = context.dataStore
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
        val COMPANY_ID = intPreferencesKey("company_id")
        val COMPANY_DOC = stringPreferencesKey("company_doc")
        val COMPANY_NAME = stringPreferencesKey("company_name")
        val SUBSIDIARY_ID = intPreferencesKey("subsidiary_id")
        val SUBSIDIARY_SERIAL = stringPreferencesKey("subsidiary_serial")
        val SUBSIDIARY_NAME = stringPreferencesKey("subsidiary_name")
        val SUBSIDIARY_ADDRESS = stringPreferencesKey("subsidiary_address")
        val SUBSIDIARY_TOKEN = stringPreferencesKey("subsidiary_token")
        val USER_ID = intPreferencesKey("user_id")
    }

    // Estado de autenticación como StateFlow para acceso inmediato
    val isLoggedIn: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[IS_LOGGED_IN] ?: false }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    // Datos de la compañía
    val companyData: StateFlow<ICompany?> = dataStore.data
        .map { preferences ->
            if (preferences[COMPANY_ID] != null) {
                ICompany(
                    id = preferences[COMPANY_ID]!!,
                    doc = preferences[COMPANY_DOC] ?: "",
                    businessName = preferences[COMPANY_NAME] ?: ""
                )
            } else {
                null
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Datos de la subsidiaria
    val subsidiaryData: StateFlow<ISubsidiary?> = dataStore.data
        .map { preferences ->
            if (preferences[SUBSIDIARY_ID] != null) {
                ISubsidiary(
                    id = preferences[SUBSIDIARY_ID]!!.toInt(),
                    serial = preferences[SUBSIDIARY_SERIAL] ?: "",
                    name = preferences[SUBSIDIARY_NAME] ?: "",
                    address = preferences[SUBSIDIARY_ADDRESS] ?: "",
                    token = preferences[SUBSIDIARY_TOKEN] ?: ""
                )
            } else {
                null
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Datos del usuario
    val userData: StateFlow<IUser?> = dataStore.data
        .map { preferences ->
            if (preferences[USER_ID] != null) {
                IUser(id = preferences[USER_ID]!!.toInt())
            } else {
                null
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    suspend fun saveUserData(userData: IUserData) {
        dataStore.edit { preferences ->
            preferences[IS_LOGGED_IN] = userData.success
            userData.company?.let { company ->
                preferences[COMPANY_ID] = company.id
                preferences[COMPANY_DOC] = company.doc
                preferences[COMPANY_NAME] = company.businessName
            }
            userData.subsidiary?.let { subsidiary ->
                preferences[SUBSIDIARY_ID] = subsidiary.id
                preferences[SUBSIDIARY_SERIAL] = subsidiary.serial
                preferences[SUBSIDIARY_NAME] = subsidiary.name
                preferences[SUBSIDIARY_ADDRESS] = subsidiary.address
                preferences[SUBSIDIARY_TOKEN] = subsidiary.token
            }
            userData.user?.let { user ->
                preferences[USER_ID] = user.id
            }
        }
    }

    suspend fun clearUserData() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Función para obtener todos los datos como IUserData
    val currentUserData: StateFlow<IUserData?> = dataStore.data
        .map { preferences ->
            if (preferences[IS_LOGGED_IN] == true) {
                IUserData(
                    success = true,
                    message = "Datos cargados desde preferencias",
                    company = preferences[COMPANY_ID]?.let {
                        ICompany(
                            id = it,
                            doc = preferences[COMPANY_DOC] ?: "",
                            businessName = preferences[COMPANY_NAME] ?: ""
                        )
                    },
                    subsidiary = preferences[SUBSIDIARY_ID]?.let {
                        ISubsidiary(
                            id = it.toInt(),
                            serial = preferences[SUBSIDIARY_SERIAL] ?: "",
                            name = preferences[SUBSIDIARY_NAME] ?: "",
                            address = preferences[SUBSIDIARY_ADDRESS] ?: "",
                            token = preferences[SUBSIDIARY_TOKEN] ?: ""
                        )
                    },
                    user = preferences[USER_ID]?.let {
                        IUser(id = it.toInt())
                    }
                )
            } else {
                null
            }
        }
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )
}
//val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")
//
//class PreferencesManager(context: Context) {
//    private val dataStore = context.dataStore
//    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
//
//    companion object {
//        val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
//        val COMPANY_ID = intPreferencesKey("company_id")
//        val COMPANY_DOC = stringPreferencesKey("company_doc")
//        val COMPANY_NAME = stringPreferencesKey("company_name")
//        val SUBSIDIARY_ID = intPreferencesKey("subsidiary_id")
//        val SUBSIDIARY_SERIAL = stringPreferencesKey("subsidiary_serial")
//        val SUBSIDIARY_NAME = stringPreferencesKey("subsidiary_name")
//        val SUBSIDIARY_ADDRESS = stringPreferencesKey("subsidiary_address")
//        val SUBSIDIARY_TOKEN = stringPreferencesKey("subsidiary_token")
//        val USER_ID = intPreferencesKey("user_id")
//    }
//
//    // Estado de autenticación como StateFlow para acceso inmediato
//    val isLoggedIn: StateFlow<Boolean> = dataStore.data
//        .map { preferences -> preferences[IS_LOGGED_IN] ?: false }
//        .stateIn(
//            scope = coroutineScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = false
//        )
//
//    // Datos de la compañía
//    val companyData: StateFlow<ICompany?> = dataStore.data
//        .map { preferences ->
//            if (preferences[COMPANY_ID] != null) {
//                ICompany(
//                    id = preferences[COMPANY_ID]!!,
//                    doc = preferences[COMPANY_DOC] ?: "",
//                    businessName = preferences[COMPANY_NAME] ?: ""
//                )
//            } else {
//                null
//            }
//        }
//        .stateIn(
//            scope = coroutineScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = null
//        )
//
//    // Datos de la subsidiaria
//    val subsidiaryData: StateFlow<ISubsidiary?> = dataStore.data
//        .map { preferences ->
//            if (preferences[SUBSIDIARY_ID] != null) {
//                ISubsidiary(
//                    id = preferences[SUBSIDIARY_ID]!!.toInt(),
//                    serial = preferences[SUBSIDIARY_SERIAL] ?: "",
//                    name = preferences[SUBSIDIARY_NAME] ?: "",
//                    address = preferences[SUBSIDIARY_ADDRESS] ?: "",
//                    token = preferences[SUBSIDIARY_TOKEN] ?: ""
//                )
//            } else {
//                null
//            }
//        }
//        .stateIn(
//            scope = coroutineScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = null
//        )
//
//    // Datos del usuario
//    val userData: StateFlow<IUser?> = dataStore.data
//        .map { preferences ->
//            if (preferences[USER_ID] != null) {
//                IUser(id = preferences[USER_ID]!!.toInt())
//            } else {
//                null
//            }
//        }
//        .stateIn(
//            scope = coroutineScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = null
//        )
//
//    suspend fun saveUserData(userData: IUserData) {
//        dataStore.edit { preferences ->
//            preferences[IS_LOGGED_IN] = userData.success
//            userData.company?.let { company ->
//                preferences[COMPANY_ID] = company.id
//                preferences[COMPANY_DOC] = company.doc
//                preferences[COMPANY_NAME] = company.businessName
//            }
//            userData.subsidiary?.let { subsidiary ->
//                preferences[SUBSIDIARY_ID] = subsidiary.id
//                preferences[SUBSIDIARY_SERIAL] = subsidiary.serial
//                preferences[SUBSIDIARY_NAME] = subsidiary.name
//                preferences[SUBSIDIARY_ADDRESS] = subsidiary.address
//                preferences[SUBSIDIARY_TOKEN] = subsidiary.token
//            }
//            userData.user?.let { user ->
//                preferences[USER_ID] = user.id
//            }
//        }
//    }
//
//    suspend fun clearUserData() {
//        dataStore.edit { preferences ->
//            preferences.clear()
//        }
//    }
//
//    // Función para obtener todos los datos como IUserData
//    val currentUserData: StateFlow<IUserData?> = dataStore.data
//        .map { preferences ->
//            if (preferences[IS_LOGGED_IN] == true) {
//                IUserData(
//                    success = true,
//                    message = "Datos cargados desde preferencias",
//                    company = preferences[COMPANY_ID]?.let {
//                        ICompany(
//                            id = it,
//                            doc = preferences[COMPANY_DOC] ?: "",
//                            businessName = preferences[COMPANY_NAME] ?: ""
//                        )
//                    },
//                    subsidiary = preferences[SUBSIDIARY_ID]?.let {
//                        ISubsidiary(
//                            id = it.toInt(),
//                            serial = preferences[SUBSIDIARY_SERIAL] ?: "",
//                            name = preferences[SUBSIDIARY_NAME] ?: "",
//                            address = preferences[SUBSIDIARY_ADDRESS] ?: "",
//                            token = preferences[SUBSIDIARY_TOKEN] ?: ""
//                        )
//                    },
//                    user = preferences[USER_ID]?.let {
//                        IUser(id = it.toInt())
//                    }
//                )
//            } else {
//                null
//            }
//        }
//        .stateIn(
//            scope = coroutineScope,
//            started = SharingStarted.WhileSubscribed(5000),
//            initialValue = null
//        )
//}