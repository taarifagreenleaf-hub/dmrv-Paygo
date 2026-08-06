package com.greenleaf.paygo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.greenleaf.paygo.data.db.entity.ParsingRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseRuleEntity

@Composable
fun LabeledField(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@Composable
private fun ToggleLine(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
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
fun ParsingRuleDialog(
    rule: ParsingRuleEntity,
    onDismiss: () -> Unit,
    onSave: (ParsingRuleEntity) -> Unit,
    onDelete: (ParsingRuleEntity) -> Unit
) {
    var name by remember { mutableStateOf(rule.name) }
    var provider by remember { mutableStateOf(rule.provider) }
    var enabled by remember { mutableStateOf(rule.enabled) }
    var sender by remember { mutableStateOf(rule.senderPattern) }
    var body by remember { mutableStateOf(rule.bodyPattern) }
    var amount by remember { mutableStateOf(rule.amountRegex ?: "") }
    var nameRx by remember { mutableStateOf(rule.nameRegex ?: "") }
    var number by remember { mutableStateOf(rule.numberRegex ?: "") }
    var reference by remember { mutableStateOf(rule.referenceRegex ?: "") }
    var balance by remember { mutableStateOf(rule.balanceRegex ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
                Text("Read rule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LabeledField("Name", name) { name = it }
                LabeledField("Provider slug (mpesa, mixx, ...)", provider) { provider = it }
                ToggleLine("Enabled", enabled) { enabled = it }
                Text("Regex patterns", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
                LabeledField("Sender matches", sender) { sender = it }
                LabeledField("Body must contain", body) { body = it }
                LabeledField("Amount regex (group 1)", amount) { amount = it }
                LabeledField("Name regex (group 1)", nameRx) { nameRx = it }
                LabeledField("Number regex (group 1)", number) { number = it }
                LabeledField("Reference regex (group 1)", reference) { reference = it }
                LabeledField("Balance regex (group 1)", balance) { balance = it }

                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    if (rule.id != 0L) {
                        TextButton(onClick = { onDelete(rule) }) { Text("Delete") }
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        onSave(
                            rule.copy(
                                name = name, provider = provider.trim().lowercase(), enabled = enabled,
                                senderPattern = sender, bodyPattern = body,
                                amountRegex = amount.ifBlank { null },
                                nameRegex = nameRx.ifBlank { null },
                                numberRegex = number.ifBlank { null },
                                referenceRegex = reference.ifBlank { null },
                                balanceRegex = balance.ifBlank { null }
                            )
                        )
                    }) { Text("Save") }
                }
            }
        }
    }
}

@Composable
fun ResponseRuleDialog(
    rule: ResponseRuleEntity,
    onDismiss: () -> Unit,
    onSave: (ResponseRuleEntity) -> Unit,
    onDelete: (ResponseRuleEntity) -> Unit
) {
    var name by remember { mutableStateOf(rule.name) }
    var enabled by remember { mutableStateOf(rule.enabled) }
    var trigger by remember { mutableStateOf(rule.triggerType) }
    var providerFilter by remember { mutableStateOf(rule.providerFilter ?: "") }
    var keywords by remember { mutableStateOf(rule.keywords ?: "") }
    var template by remember { mutableStateOf(rule.template) }
    var viaSms by remember { mutableStateOf(rule.replyViaSms) }
    var viaWa by remember { mutableStateOf(rule.replyViaWhatsApp) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp).heightIn(max = 560.dp).verticalScroll(rememberScrollState())) {
                Text("Reply rule", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                LabeledField("Name", name) { name = it }
                ToggleLine("Enabled", enabled) { enabled = it }
                LabeledField("Trigger (PAYMENT / NORMAL / KEYWORD)", trigger) { trigger = it.uppercase() }
                LabeledField("Provider filter (blank = any)", providerFilter) { providerFilter = it }
                LabeledField("Keywords (comma separated, for KEYWORD)", keywords) { keywords = it }
                LabeledField("Reply template", template, singleLine = false) { template = it }
                Text(
                    "Placeholders: {name} {amount} {currency} {provider} {reference} {number} {balance} {date}",
                    style = MaterialTheme.typography.bodySmall
                )
                ToggleLine("Reply via SMS", viaSms) { viaSms = it }
                ToggleLine("Reply via WhatsApp", viaWa) { viaWa = it }

                Row(Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
                    if (rule.id != 0L) {
                        TextButton(onClick = { onDelete(rule) }) { Text("Delete") }
                    }
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    TextButton(onClick = {
                        onSave(
                            rule.copy(
                                name = name, enabled = enabled, triggerType = trigger,
                                providerFilter = providerFilter.trim().lowercase().ifBlank { null },
                                keywords = keywords.ifBlank { null },
                                template = template, replyViaSms = viaSms, replyViaWhatsApp = viaWa
                            )
                        )
                    }) { Text("Save") }
                }
            }
        }
    }
}
