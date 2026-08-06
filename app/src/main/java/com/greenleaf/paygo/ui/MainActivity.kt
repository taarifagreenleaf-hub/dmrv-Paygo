package com.greenleaf.paygo.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.greenleaf.paygo.ui.nav.Dest
import com.greenleaf.paygo.ui.screens.ContactsScreen
import com.greenleaf.paygo.ui.screens.DashboardScreen
import com.greenleaf.paygo.ui.screens.SettingsScreen
import com.greenleaf.paygo.ui.screens.TrainingScreen
import com.greenleaf.paygo.ui.screens.TransactionsScreen
import com.greenleaf.paygo.ui.theme.PaygoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PaygoTheme {
                RequestPermissions()
                PaygoApp()
            }
        }
    }
}

@Composable
private fun RequestPermissions() {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { /* result handled by the OS; app degrades gracefully if denied */ }

    LaunchedEffect(Unit) {
        val perms = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            perms += Manifest.permission.POST_NOTIFICATIONS
        }
        launcher.launch(perms.toTypedArray())
    }
}

@Composable
private fun PaygoApp() {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            val backStack by navController.currentBackStackEntryAsState()
            val current = backStack?.destination
            NavigationBar {
                Dest.bottomBar.forEach { dest ->
                    NavigationBarItem(
                        selected = current?.hierarchy?.any { it.route == dest.route } == true,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Dashboard.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Dest.Dashboard.route) { DashboardScreen() }
            composable(Dest.Transactions.route) { TransactionsScreen() }
            composable(Dest.Groups.route) { ContactsScreen() }
            composable(Dest.Training.route) { TrainingScreen() }
            composable(Dest.Settings.route) { SettingsScreen() }
        }
    }
}
