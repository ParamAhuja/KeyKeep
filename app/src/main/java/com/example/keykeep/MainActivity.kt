package com.example.keykeep

import android.os.Bundle
import android.view.WindowManager
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.compose.material3.Surface
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
// Ensure you import your app's specific R class
import com.example.keykeep.R
import com.example.keykeep.ui.theme.KeyKeepTheme


class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Block screenshots and recents menu snapshots
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )

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
            val appTheme = viewModel.appTheme.collectAsState().value

            val isDarkTheme = when (appTheme) {
                MainViewModel.AppTheme.SYSTEM -> isSystemInDarkTheme()
                MainViewModel.AppTheme.DARK -> true
                MainViewModel.AppTheme.LIGHT -> false
            }

            KeyKeepTheme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
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

                        NavHost(
                            navController = navController,
                            startDestination = "vault_list",
                            enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
                            exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) },
                            popEnterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) },
                            popExitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) }
                        ) {
                            composable("vault_list") {
                                VaultListScreen(
                                    viewModel = viewModel,
                                    onNavigateToAdd = { navController.navigate("add_edit_credential/-1") },
                                    onNavigateToEdit = { credentialId ->
                                        navController.navigate("add_edit_credential/$credentialId")}
                                )
                            }
                            composable(
                                route = "add_edit_credential/{credentialId}",
                                arguments = listOf(navArgument("credentialId") {
                                    type = NavType.IntType
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
                    // Removed the Toast that shows authentication error
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