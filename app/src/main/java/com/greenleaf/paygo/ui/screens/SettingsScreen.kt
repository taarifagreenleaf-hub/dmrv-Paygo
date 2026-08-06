package com.greenleaf.paygo.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenleaf.paygo.data.prefs.WhatsAppMode
import com.greenleaf.paygo.util.AccessibilityUtil
import com.greenleaf.paygo.ui.vm.ExportType
import com.greenleaf.paygo.ui.vm.SettingsViewModel

@Composable
fun SettingsScreen(vm: SettingsViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {

        SectionCard("Forwarding") {
            SettingSwitch("Forward incoming messages", settings.forwardEnabled) {
                vm.update { s -> s.copy(forwardEnabled = it) }
            }
            SettingSwitch("Only forward payments", settings.forwardOnlyPayments) {
                vm.update { s -> s.copy(forwardOnlyPayments = it) }
            }
            LabeledField("Forward to numbers (SMS, comma separated)", settings.forwardSmsNumbers) {
                vm.update { s -> s.copy(forwardSmsNumbers = it) }
            }
            LabeledField("Forward to WhatsApp number (e.g. +2557...)", settings.forwardWhatsAppNumber) {
                vm.update { s -> s.copy(forwardWhatsAppNumber = it) }
            }
        }

        SectionCard("WhatsApp delivery") {
            Text("How replies/forwards are sent to WhatsApp:", style = MaterialTheme.typography.bodySmall)
            Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WhatsAppMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.whatsAppMode == mode.name,
                        onClick = { vm.update { s -> s.copy(whatsAppMode = mode.name) } },
                        label = { Text(mode.name) }
                    )
                }
            }
            when (WhatsAppMode.valueOf(settings.whatsAppMode)) {
                WhatsAppMode.INTENT -> Text(
                    "Opens WhatsApp with the message pre-filled; you tap send.",
                    style = MaterialTheme.typography.bodySmall
                )
                WhatsAppMode.ACCESSIBILITY -> {
                    Text(
                        "Hands-free: Paygo taps WhatsApp's send button for you. " +
                            "SMS replies are already sent automatically through your " +
                            "operator — no action needed there.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    AccessibilityStatusRow(context)
                }
                WhatsAppMode.CLOUD_API -> {
                    LabeledField("Cloud API token", settings.cloudApiToken) {
                        vm.update { s -> s.copy(cloudApiToken = it) }
                    }
                    LabeledField("Cloud API phone number id", settings.cloudApiPhoneNumberId) {
                        vm.update { s -> s.copy(cloudApiPhoneNumberId = it) }
                    }
                }
            }
        }

        SectionCard("Export & download") {
            Text("Save your data as a file you can share or download.", style = MaterialTheme.typography.bodySmall)
            ExportButton("Payments (CSV)") { vm.export(ExportType.TRANSACTIONS_CSV) { f -> share(context, f) } }
            ExportButton("Messages (CSV)") { vm.export(ExportType.MESSAGES_CSV) { f -> share(context, f) } }
            ExportButton("Full backup (JSON)") { vm.export(ExportType.BACKUP_JSON) { f -> share(context, f) } }
        }

        SectionCard("Permissions checklist") {
            Text(
                "For full automation grant: SMS (receive/read/send), Notifications, " +
                    "disable battery optimisation, and (optional) the Accessibility service " +
                    "for WhatsApp auto-send.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun AccessibilityStatusRow(context: android.content.Context) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var enabled by remember { mutableStateOf(AccessibilityUtil.isServiceEnabled(context)) }

    // Re-check whenever the user comes back from the system settings screen.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = AccessibilityUtil.isServiceEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Row(
        Modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (enabled) "Auto-send service: ON" else "Auto-send service: OFF",
            color = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
        Button(onClick = { AccessibilityUtil.openSettings(context) }) {
            Text(if (enabled) "Manage" else "Enable")
        }
    }
    if (!enabled) {
        Text(
            "Tap Enable, then turn on \"Paygo WhatsApp Auto-Reply\" in the list.",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun ExportButton(label: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
        Text(label)
    }
}

private fun share(
    context: android.content.Context,
    file: java.io.File
) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = if (file.extension == "json") "application/json" else "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Share ${file.name}"))
}
