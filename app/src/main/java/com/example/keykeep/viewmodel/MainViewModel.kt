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
    enum class AppTheme { SYSTEM, LIGHT, DARK }

    private val _authState = MutableStateFlow(AuthState.LOCKED)
    val authState: StateFlow<AuthState> = _authState

    private val _appTheme = MutableStateFlow(AppTheme.SYSTEM)
    val appTheme: StateFlow<AppTheme> = _appTheme

    var appDatabase: AppDatabase? = null
        private set

    init {
        checkFirstLaunch()
        loadTheme()
    }

    private fun checkFirstLaunch() {
        val prefs = getApplication<Application>().getSharedPreferences("secure_prefs", Context.MODE_PRIVATE)
        if (!prefs.contains("encrypted_db_key")) {
            _authState.value = AuthState.FIRST_OPEN
        }
    }

    private fun loadTheme() {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val themeName = prefs.getString("app_theme", AppTheme.SYSTEM.name) ?: AppTheme.SYSTEM.name
        _appTheme.value = AppTheme.valueOf(themeName)
    }

    fun setTheme(theme: AppTheme) {
        val prefs = getApplication<Application>().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("app_theme", theme.name).apply()
        _appTheme.value = theme
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

