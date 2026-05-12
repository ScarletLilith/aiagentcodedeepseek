package com.example.agentmemory

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Rule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val subsystemManager = remember { LinuxSubsystemManager(context) }
    val installationState by subsystemManager.installationState.collectAsState(initial = LinuxSubsystemManager.InstallationState.Idle)
    val scope = rememberCoroutineScope()
    var currentRoute by remember { mutableStateOf("home") }

    LaunchedEffect(Unit) {
        subsystemManager.setupSubsystemIfNeeded()
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = currentRoute == "home",
                    onClick = {
                        currentRoute = "home"
                        navController.navigate("home")
                    },
                    icon = { Icon(Icons.Default.Build, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = {
                        currentRoute = "settings"
                        navController.navigate("settings")
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
                NavigationBarItem(
                    selected = currentRoute == "verify",
                    onClick = {
                        currentRoute = "verify"
                        navController.navigate("verify")
                    },
                    icon = { Icon(Icons.Default.Verified, contentDescription = "Verify") },
                    label = { Text("Verify") }
                )
                NavigationBarItem(
                    selected = currentRoute == "editor",
                    onClick = {
                        currentRoute = "editor"
                        navController.navigate("editor")
                    },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
                    label = { Text("Editor") }
                )
                NavigationBarItem(
                    selected = currentRoute == "rules",
                    onClick = {
                        currentRoute = "rules"
                        navController.navigate("rules")
                    },
                    icon = { Icon(Icons.Default.Rule, contentDescription = "Rules") },
                    label = { Text("Rules") }
                )
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            NavHost(
                navController = navController,
                startDestination = "home"
            ) {
                composable("home") {
                    HomeScreen(
                        installationState = installationState,
                        onRetry = {
                            scope.launch {
                                subsystemManager.setupSubsystemIfNeeded()
                            }
                        }
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("verify") {
                    VerifyScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("editor") {
                    EditorScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
                composable("rules") {
                    RulesScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreen(
    installationState: LinuxSubsystemManager.InstallationState,
    onRetry: () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
    ) {
        Text(
            "AgentMemory",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "AI Memory System for Android",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        when (installationState) {
            is LinuxSubsystemManager.InstallationState.Idle -> {
                Text("Checking system...")
            }
            is LinuxSubsystemManager.InstallationState.Extracting -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(context.getString(R.string.extracting))
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
            is LinuxSubsystemManager.InstallationState.Installing -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(context.getString(R.string.installing))
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(installationState.message)
                    }
                }
            }
            is LinuxSubsystemManager.InstallationState.Complete -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("✅ System Ready!", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Linux subsystem and MemPalace are running.")
                    }
                }
            }
            is LinuxSubsystemManager.InstallationState.Error -> {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("❌ Error", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(installationState.message)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = onRetry) {
                            Text("Retry")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Use navigation bar below to explore features",
            style = MaterialTheme.typography.bodySmall
        )
    }
}
