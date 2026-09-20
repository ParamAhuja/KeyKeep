package com.example.keykeep.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.keykeep.security.CryptoManager
import com.example.keykeep.data.AppDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val cryptoManager = CryptoManager(application)

    // Define the UI states
    enum class AuthState { LOCKED, UNLOCKED, FIRST_OPEN }

    private val _authState = MutableStateFlow(AuthState.LOCKED)
    val authState: StateFlow<AuthState> = _authState

    var appDatabase: AppDatabase? = null
        private set

    init {
        checkFirstLaunch()
    }

    private fun checkFirstLaunch() {
        val prefs = getApplication<Application>().getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("encrypted_db_key")) {
            _authState.value = AuthState.FIRST_OPEN
        }
    }

    fun handleAuthenticationSuccess() {
        try {
            val dbKey = if (_authState.value == AuthState.FIRST_OPEN) {
                cryptoManager.initializeAndSaveDatabaseKey()
            } else {
                cryptoManager.getSavedDatabaseKey()
                    ?: throw IllegalStateException("Key not found")
            }

            // Initialize SQLCipher Room Database securely
            appDatabase = AppDatabase.getDatabase(getApplication(), dbKey)
            _authState.value = AuthState.UNLOCKED

        } catch (e: Exception) {
            // In a production app, handle Keystore exceptions here
            e.printStackTrace()
        }
    }
    fun lockVault() {
        // Destroy the global Room instance so a new one is forced on unlock
        AppDatabase.destroyInstance()
        appDatabase = null

        if (_authState.value != AuthState.FIRST_OPEN) {
            _authState.value = AuthState.LOCKED
        }
    }
}

