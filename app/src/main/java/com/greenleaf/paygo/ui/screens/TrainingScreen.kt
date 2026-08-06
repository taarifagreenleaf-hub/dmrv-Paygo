package com.greenleaf.paygo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenleaf.paygo.data.db.entity.ParsingRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseTrigger
import com.greenleaf.paygo.data.db.entity.TxnDirection
import com.greenleaf.paygo.parser.ParseResult
import com.greenleaf.paygo.ui.vm.TrainingViewModel
import kotlinx.coroutines.launch

@Composable
fun TrainingScreen(vm: TrainingViewModel = viewModel()) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Read rules", "Reply rules", "Test")

    var editingParse by remember { mutableStateOf<ParsingRuleEntity?>(null) }
    var editingResponse by remember { mutableStateOf<ResponseRuleEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            when (tab) {
                0 -> ExtendedFloatingActionButton(
                    text = { Text("Read rule") },
                    icon = { Icon(Icons.Filled.Add, null) },
                    onClick = { editingParse = blankParsingRule() }
                )
                1 -> ExtendedFloatingActionButton(
                    text = { Text("Reply rule") },
                    icon = { Icon(Icons.Filled.Add, null) },
                    onClick = { editingResponse = blankResponseRule() }
                )
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { i, title ->
                    Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
                }
            }
            when (tab) {
                0 -> ParsingRulesTab(vm, onEdit = { editingParse = it })
                1 -> ResponseRulesTab(vm, onEdit = { editingResponse = it })
                2 -> TestTab(vm)
            }
        }
    }

    editingParse?.let { rule ->
        ParsingRuleDialog(
            rule = rule,
            onDismiss = { editingParse = null },
            onSave = { vm.saveParsingRule(it); editingParse = null },
            onDelete = { vm.deleteParsingRule(it); editingParse = null }
        )
    }
    editingResponse?.let { rule ->
        ResponseRuleDialog(
            rule = rule,
            onDismiss = { editingResponse = null },
            onSave = { vm.saveResponseRule(it); editingResponse = null },
            onDelete = { vm.deleteResponseRule(it); editingResponse = null }
        )
    }
}

@Composable
private fun ParsingRulesTab(vm: TrainingViewModel, onEdit: (ParsingRuleEntity) -> Unit) {
    val rules by vm.parsingRules.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text(
            "These rules tell the app how to read a payment SMS for each provider. " +
                "Tap to edit the regex patterns.",
            style = MaterialTheme.typography.bodySmall
        )
        rules.forEach { rule ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(rule.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "provider=${rule.provider} • ${if (rule.enabled) "enabled" else "disabled"}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("sender ~ ${rule.senderPattern}", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { onEdit(rule) }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
private fun ResponseRulesTab(vm: TrainingViewModel, onEdit: (ResponseRuleEntity) -> Unit) {
    val rules by vm.responseRules.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(12.dp)) {
        Text(
            "These rules tell the app what to reply. Use placeholders like {name}, " +
                "{amount}, {provider}, {reference} in the template.",
            style = MaterialTheme.typography.bodySmall
        )
        rules.forEach { rule ->
            Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text(rule.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "trigger=${rule.triggerType} • ${if (rule.enabled) "enabled" else "disabled"} • " +
                            "sms=${rule.replyViaSms} wa=${rule.replyViaWhatsApp}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text("\"${rule.template}\"", style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { onEdit(rule) }, modifier = Modifier.padding(top = 8.dp)) {
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
private fun TestTab(vm: TrainingViewModel) {
    var sender by remember { mutableStateOf("M-PESA") }
    var body by remember {
        mutableStateOf("ABC1234567 Confirmed. You have received Tsh 50,000.00 from JOHN DOE 255712345678 on 6/8/26. New M-PESA balance is Tsh 120,000.00")
    }
    var result by remember { mutableStateOf<ParseResult?>(null) }
    var tested by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Paste a sample SMS to check how the app reads it.", style = MaterialTheme.typography.bodyMedium)
        LabeledField("Sender", sender) { sender = it }
        LabeledField("Message body", body, singleLine = false) { body = it }
        Button(
            onClick = {
                scope.launch { result = vm.testParse(sender, body); tested = true }
            },
            modifier = Modifier.padding(top = 12.dp)
        ) { Text("Run test") }

        if (tested) {
            Card(Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    val r = result
                    if (r == null || !r.isPayment) {
                        Text("No payment matched.", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Adjust a read rule's sender/body/amount regex so it matches this message.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Text("Matched: ${r.matchedRuleName}", style = MaterialTheme.typography.titleMedium)
                        KeyValueRow("Provider", r.provider)
                        KeyValueRow("Amount", r.amount?.let { Format.money(it) } ?: "-")
                        KeyValueRow("Name", r.counterpartyName ?: "-")
                        KeyValueRow("Number", r.counterpartyNumber ?: "-")
                        KeyValueRow("Reference", r.reference ?: "-")
                        KeyValueRow("Balance", r.balanceAfter?.let { Format.money(it) } ?: "-")
                    }
                }
            }
        }
    }
}

private fun blankParsingRule() = ParsingRuleEntity(
    name = "New provider",
    provider = "custom",
    senderPattern = "",
    bodyPattern = "(?i)(received|umepokea)",
    direction = TxnDirection.RECEIVED.name,
    amountRegex = "(?:Tsh|TZS)\\.?\\s*([\\d.,]+)",
    numberRegex = "(\\+?255\\d{9}|0\\d{9})"
)

private fun blankResponseRule() = ResponseRuleEntity(
    name = "New reply",
    triggerType = ResponseTrigger.PAYMENT.name,
    template = "Asante {name}, tumepokea {currency} {amount}."
)
