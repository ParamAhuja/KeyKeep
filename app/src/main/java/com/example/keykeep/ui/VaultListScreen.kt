package com.example.keykeep.ui

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.SettingsSystemDaydream
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.keykeep.data.Credential
import com.example.keykeep.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultListScreen(
    viewModel: MainViewModel,
    onNavigateToAdd: () -> Unit,
    onNavigateToEdit: (Int) -> Unit
) {
    val credentials by viewModel.appDatabase?.credentialDao()?.getAllCredentials()
        ?.collectAsState(initial = emptyList()) ?: remember { mutableStateOf(emptyList()) }

    val coroutineScope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    val filteredCredentials = remember(credentials, searchQuery) {
        if (searchQuery.isBlank()) credentials
        else credentials.filter { it.serviceName.contains(searchQuery, ignoreCase = true) }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val appTheme by viewModel.appTheme.collectAsState()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(280.dp)) {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Settings",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge
                )
                HorizontalDivider()

                Text(
                    "Theme",
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.SettingsSystemDaydream, contentDescription = null) },
                    label = { Text("System Default") },
                    selected = appTheme == MainViewModel.AppTheme.SYSTEM,
                    onClick = { viewModel.setTheme(MainViewModel.AppTheme.SYSTEM) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.LightMode, contentDescription = null) },
                    label = { Text("Light Theme") },
                    selected = appTheme == MainViewModel.AppTheme.LIGHT,
                    onClick = { viewModel.setTheme(MainViewModel.AppTheme.LIGHT) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Filled.DarkMode, contentDescription = null) },
                    label = { Text("Dark Theme") },
                    selected = appTheme == MainViewModel.AppTheme.DARK,
                    onClick = { viewModel.setTheme(MainViewModel.AppTheme.DARK) },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            if (it.length <= 100) {
                                searchQuery = it
                            }
                        },
                        placeholder = { Text("Search vault...") },
                        leadingIcon = {
                            IconButton(onClick = { coroutineScope.launch { drawerState.open() } }) {
                                Icon(Icons.Filled.Menu, contentDescription = "Open Menu")
                            }
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Clear Search")
                                }
                            } else {
                                Icon(Icons.Filled.Search, contentDescription = "Search")
                            }
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        ),
                        shape = MaterialTheme.shapes.extraLarge,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(onClick = onNavigateToAdd) {
                    Icon(Icons.Filled.Add, contentDescription = "Add Password")
                }
            }
        ) { paddingValues ->
            if (filteredCredentials.isEmpty() && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No credentials found for \"$searchQuery\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (filteredCredentials.isEmpty() && credentials.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Your vault is empty. Add a password!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredCredentials) { credential ->
                        CredentialCard(
                            credential = credential,
                            onEdit = { onNavigateToEdit(credential.id) },
                            onDelete = {
                                coroutineScope.launch {
                                    viewModel.appDatabase?.credentialDao()?.deleteCredential(credential)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CredentialCard(
    credential: Credential,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboard = LocalClipboard.current
    var passwordVisible by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {

            // Service Name Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Service: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = credential.serviceName,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Username Row
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
                Text(
                    text = "User/Email: ",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = credential.username,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (credential.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${credential.notes}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {

                // Visible, bounded container for the scrollable password
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            shape = MaterialTheme.shapes.small
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = MaterialTheme.shapes.small
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val scrollState = rememberScrollState()

                    Text(
                        text = if (passwordVisible) credential.password else "••••••••••••",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = FontFamily.Monospace
                        ),
                        modifier = Modifier.horizontalScroll(scrollState),
                        maxLines = 1
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide" else "Show",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = {
                        coroutineScope.launch {
                            val clipData = ClipData.newPlainText("password", credential.password)
                            clipData.description.extras = PersistableBundle().apply {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
                                } else {
                                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                                }
                            }
                            clipboard.setClipEntry(ClipEntry(clipData))
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = onEdit) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Credential") },
            text = { Text("Are you sure you want to delete the password for ${credential.serviceName}? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}