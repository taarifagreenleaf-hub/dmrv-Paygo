package com.greenleaf.paygo.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Dashboard : Dest("dashboard", "Home", Icons.Filled.Dashboard)
    data object Transactions : Dest("transactions", "Payments", Icons.Filled.Payments)
    data object Groups : Dest("groups", "Contacts", Icons.Filled.Groups)
    data object Training : Dest("training", "Training", Icons.Filled.School)
    data object Settings : Dest("settings", "Settings", Icons.Filled.Settings)

    companion object {
        val bottomBar = listOf(Dashboard, Transactions, Groups, Training, Settings)
    }
}
