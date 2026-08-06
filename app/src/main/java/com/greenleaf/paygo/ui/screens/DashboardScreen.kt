package com.greenleaf.paygo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenleaf.paygo.ui.vm.DashboardViewModel

@Composable
fun DashboardScreen(vm: DashboardViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val messages by vm.messageCount.collectAsStateWithLifecycle()
    val txns by vm.transactionCount.collectAsStateWithLifecycle()
    val total by vm.totalReceived.collectAsStateWithLifecycle()
    val events by vm.recentEvents.collectAsStateWithLifecycle()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Paygo SMS Assistant", style = MaterialTheme.typography.headlineSmall)
        Text(
            if (settings.masterEnabled) "Active — watching incoming SMS"
            else "Paused — enable to start processing",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )

        SectionCard("Controls") {
            ToggleRow("Master switch (process SMS)", settings.masterEnabled) { vm.setMasterEnabled(it) }
            ToggleRow("Auto-reply to senders", settings.autoReplyEnabled) { vm.setAutoReply(it) }
        }

        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("SMS captured", messages.toString(), Modifier.weight(1f))
            StatTile("Payments", txns.toString(), Modifier.weight(1f))
        }
        Row(Modifier.padding(top = 12.dp)) {
            StatTile("Total received (TZS)", Format.money(total), Modifier.weight(1f))
        }

        SectionCard("Recent activity") {
            if (events.isEmpty()) {
                Text("No replies or forwards yet.", style = MaterialTheme.typography.bodySmall)
            } else {
                events.take(15).forEach { e ->
                    Divider(Modifier.padding(vertical = 4.dp))
                    Text(
                        "${e.action} → ${e.target ?: "-"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${Format.date(e.timestamp)} • ${if (e.success) "OK" else "FAILED"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (e.success) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
