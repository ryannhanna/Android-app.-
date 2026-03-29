package com.smartfilemanager.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.rememberNavController
import com.smartfilemanager.app.data.preferences.ThemePreferenceStore
import com.smartfilemanager.app.ui.navigation.AppNavHost
import com.smartfilemanager.app.ui.navigation.BottomNavBar
import com.smartfilemanager.app.ui.screen.PermissionScreen
import com.smartfilemanager.app.ui.theme.AppTheme
import com.smartfilemanager.app.ui.theme.SmartFileManagerTheme
import com.smartfilemanager.app.ui.viewmodel.SettingsViewModel
import com.smartfilemanager.app.util.PermissionUtil

class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val context = LocalContext.current
            val appTheme by ThemePreferenceStore.getTheme(context)
                .collectAsState(initial = AppTheme.SYSTEM)

            SmartFileManagerTheme(appTheme = appTheme) {
                var permissionGranted by remember {
                    mutableStateOf(PermissionUtil.isGranted(this@MainActivity))
                }

                if (!permissionGranted) {
                    PermissionScreen(onPermissionGranted = { permissionGranted = true })
                } else {
                    val navController = rememberNavController()

                    Scaffold(
                        topBar = {
                            TopAppBar(
                                title = { Text("SmartFileManager") },
                                actions = {
                                    IconButton(onClick = { navController.navigate("settings") }) {
                                        Icon(
                                            imageVector = Icons.Filled.Settings,
                                            contentDescription = "Settings"
                                        )
                                    }
                                }
                            )
                        },
                        bottomBar = {
                            BottomNavBar(navController = navController)
                        }
                    ) { innerPadding ->
                        AppNavHost(
                            navController = navController,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
