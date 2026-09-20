package com.example.keykeep

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.keykeep.ui.AddCredentialScreen
import com.example.keykeep.ui.VaultListScreen
import com.example.keykeep.viewmodel.MainViewModel

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.dp
// img:
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
// Ensure you import your app's specific R class
import com.example.keykeep.R


class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        System.loadLibrary("sqlcipher")
        // 1. Create the observer
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                // Triggered when the user switches apps or goes to the home screen
                viewModel.lockVault()
            }
        }

        // 2. Attach it to the entire application process
        ProcessLifecycleOwner.get().lifecycle.addObserver(lifecycleObserver)
        setContent {
            val authState = viewModel.authState.collectAsState().value

            when (authState) {
                MainViewModel.AuthState.FIRST_OPEN -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Button(onClick = { showBiometricPrompt() }) {
                            Text("Setup Secure Vault")
                        }
                    }
                }
                MainViewModel.AuthState.LOCKED -> {

                    // Automatically trigger the prompt when the screen appears
                    LaunchedEffect(Unit) {
                        showBiometricPrompt()
                    }

                    // Fallback UI in case the user taps outside and dismisses the biometric dialog
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Replaced the Lock Icon with your custom Image
                        Image(
                            painter = painterResource(id = R.drawable.app_logo), // Matches your filename
                            contentDescription = "KeyKeep Logo",
                            modifier = Modifier.size(120.dp) // Adjust size as necessary
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Vault is Locked",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(onClick = {
                            showBiometricPrompt()
                        }) {
                            Text("Unlock Vault")
                        }
                    }
                }
                MainViewModel.AuthState.UNLOCKED -> {
                    // Set up the Navigation Graph
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "vault_list") {
                        composable("vault_list") {
                            VaultListScreen(
                                viewModel = viewModel,
                                onNavigateToAdd = { navController.navigate("add_edit_credential/-1") },
                                onNavigateToEdit = { credentialId ->
                                    navController.navigate("add_edit_credential/$credentialId")
                                }
                            )
                        }

                        // Update the route to accept an ID. -1 means "Add New"
                        composable(
                            route = "add_edit_credential/{credentialId}",
                            arguments = listOf(androidx.navigation.navArgument("credentialId") {
                                type = androidx.navigation.NavType.IntType
                            })
                        ) { backStackEntry ->
                            val credentialId = backStackEntry.arguments?.getInt("credentialId") ?: -1
                            AddCredentialScreen(
                                viewModel = viewModel,
                                credentialId = credentialId, // Pass the ID to the screen
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    viewModel.handleAuthenticationSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock KeyKeep")
            .setSubtitle("Confirm your identity to decrypt your vault")
            .setAllowedAuthenticators(BIOMETRIC_STRONG or DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }
}