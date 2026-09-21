package com.example.keykeep.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.example.keykeep.data.Credential
import com.example.keykeep.viewmodel.MainViewModel
// eye button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.text.input.VisualTransformation

enum class PasswordStrength(val score: Int, val label: String) {
    WEAK(1, "Weak"),
    FAIR(2, "Fair"),
    GOOD(3, "Good"),
    STRONG(4, "Strong")
}

fun evaluatePasswordStrength(password: String): PasswordStrength {
    if (password.isBlank()) return PasswordStrength.WEAK

    var score = 0
    if (password.length >= 8) score++
    if (password.any { it.isUpperCase() }) score++
    if (password.any { it.isDigit() }) score++
    if (password.any { !it.isLetterOrDigit() }) score++

    return when (score) {
        0, 1 -> PasswordStrength.WEAK
        2 -> PasswordStrength.FAIR
        3 -> PasswordStrength.GOOD
        else -> PasswordStrength.STRONG
    }
}

fun generateSecurePassword(length: Int = 16): String {
    val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%^&*()_+"
    val secureRandom = java.security.SecureRandom()
    val sb = StringBuilder(length)
    for (i in 0 until length) {
        val randomInt = secureRandom.nextInt(allowedChars.length)
        sb.append(allowedChars[randomInt])
    }
    return sb.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCredentialScreen(
    viewModel: MainViewModel,
    credentialId: Int, // Added argument for edit mode
    onNavigateBack: () -> Unit
) {
    var serviceName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    // Error states for mandatory fields
    var serviceNameError by remember { mutableStateOf(false) }
    var usernameError by remember { mutableStateOf(false) }
    var passwordError by remember { mutableStateOf(false) }

    val isEditing = credentialId != -1
    var hasLoadedData by remember { mutableStateOf(false) }

    var passwordVisible by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    // Fetch existing credential if in edit mode
    LaunchedEffect(credentialId) {
        if (isEditing) {
            val existingCredential = viewModel.appDatabase?.credentialDao()?.getCredentialById(credentialId)
            existingCredential?.let {
                serviceName = it.serviceName
                username = it.username
                password = it.password
                notes = it.notes
            }
            hasLoadedData = true
        } else {
            hasLoadedData = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Password" else "Add New Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Cancel")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (!hasLoadedData) {
            Box(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = serviceName,
                    onValueChange = {
                        if (it.length <= 50) {
                            serviceName = it
                            serviceNameError = false
                        }
                    },
                    label = { Text("Service (e.g., Google, Bank)") },
                    isError = serviceNameError,
                    supportingText = {
                        if (serviceNameError) {
                            Text("Service name is required")
                        } else {
                            Text("${serviceName.length}/50")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        if (it.length <= 254) {
                            username = it
                            usernameError = false
                        }
                    },
                    label = { Text("Username / Email") },
                    isError = usernameError,
                    supportingText = {
                        if (usernameError) {
                            Text("Username or Email is required")
                        } else {
                            Text("${username.length}/254")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                val strength = evaluatePasswordStrength(password)
                val strengthColor = when (strength) {
                    PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
                    PasswordStrength.FAIR -> MaterialTheme.colorScheme.tertiary
                    PasswordStrength.GOOD -> MaterialTheme.colorScheme.primary
                    PasswordStrength.STRONG -> MaterialTheme.colorScheme.secondary
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        if (it.length <= 256) {
                            password = it
                            passwordError = false
                        }
                    },
                    label = { Text("Password") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    // Switch between dots and plain text based on the state
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    // Add the eye icon button to the right side of the field
                    trailingIcon = {
                        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                        val description = if (passwordVisible) "Hide password" else "Show password"

                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(imageVector = image, contentDescription = description)
                        }
                    },
                    isError = passwordError,
                    supportingText = {
                        if (passwordError) {
                            Text("Password is required")
                        } else {
                            Text("${password.length}/256")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Password Strength Indicator
                if (password.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { strength.score / 4f },
                            modifier = Modifier.weight(1f).padding(end = 8.dp),
                            color = strengthColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Text(
                            text = strength.label,
                            color = strengthColor,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Button(
                    onClick = {
                        password = generateSecurePassword()
                        passwordError = false
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Generate Secure Password")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = {
                        if (it.length <= 2000) {
                            notes = it
                        }
                    },
                    label = { Text("Notes (Optional)") },
                    supportingText = { Text("${notes.length}/2000") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val isServiceValid = serviceName.isNotBlank()
                            val isUsernameValid = username.isNotBlank()
                            val isPasswordValid = password.isNotBlank()

                            serviceNameError = !isServiceValid
                            usernameError = !isUsernameValid
                            passwordError = !isPasswordValid

                            if (isServiceValid && isUsernameValid && isPasswordValid) {
                                val credentialData = Credential(
                                    id = if (isEditing) credentialId else 0,
                                    serviceName = serviceName.trim(),
                                    username = username.trim(),
                                    password = password,
                                    notes = notes.trim()
                                )
                                coroutineScope.launch {
                                    if (isEditing) {
                                        viewModel.appDatabase?.credentialDao()?.updateCredential(credentialData)
                                    } else {
                                        viewModel.appDatabase?.credentialDao()?.insertCredential(credentialData)
                                    }
                                    
                                    // Securely wipe sensitive in-memory variables immediately
                                    password = ""
                                    notes = ""
                                    serviceName = ""
                                    username = ""
                                    
                                    onNavigateBack()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(if (isEditing) "Update" else "Save")
                    }
                }
            }
        }
    }
}