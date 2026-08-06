package com.greenleaf.paygo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenleaf.paygo.data.db.entity.ContactGroupEntity
import com.greenleaf.paygo.data.db.entity.CustomerEntity
import com.greenleaf.paygo.ui.vm.GroupsViewModel
import com.greenleaf.paygo.util.InfoJson

@Composable
fun GroupsScreen(vm: GroupsViewModel = viewModel()) {
    val groups by vm.groups.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<ContactGroupEntity?>(null) }

    if (groups.isEmpty()) {
        EmptyState("No contacts yet.\nMessages get grouped by sender/payee as they arrive.")
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
            items(groups, key = { it.groupKey }) { group ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = group }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(group.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("${group.messageCount} messages", style = MaterialTheme.typography.bodySmall)
                        if (group.totalReceived > 0) {
                            Text(
                                "Total received: TZS ${Format.money(group.totalReceived)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text("Last: ${Format.date(group.lastMessageAt)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
    }

    selected?.let { group ->
        GroupDetailDialog(vm, group) { selected = null }
    }
}

@Composable
private fun GroupDetailDialog(vm: GroupsViewModel, group: ContactGroupEntity, onDismiss: () -> Unit) {
    val messages by vm.messagesFor(group.groupKey).collectAsState(initial = emptyList())
    val customers by vm.customers.collectAsStateWithLifecycle()
    var assigningMessageId by remember { mutableStateOf<Long?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text(group.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Divider(Modifier.padding(vertical = 8.dp))
                LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(messages, key = { it.id }) { m ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(Format.date(m.timestamp), style = MaterialTheme.typography.labelSmall)
                            Text(m.body, style = MaterialTheme.typography.bodyMedium)
                            val captured = InfoJson.decode(m.extractedInfo)
                            if (captured.isNotEmpty()) {
                                Text(
                                    "Captured: " + captured.entries.joinToString(", ") { "${it.key}=${it.value}" },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (m.customerId != null) {
                                Text("Linked: ${m.customerId}", style = MaterialTheme.typography.labelMedium)
                            } else {
                                TextButton(onClick = { assigningMessageId = m.id }) {
                                    Text("Assign to customer")
                                }
                            }
                            Divider(Modifier.padding(top = 6.dp))
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }

    assigningMessageId?.let { msgId ->
        CustomerPickerDialog(
            customers = customers,
            onPick = { custId -> vm.assign(msgId, custId); assigningMessageId = null },
            onDismiss = { assigningMessageId = null }
        )
    }
}

@Composable
private fun CustomerPickerDialog(
    customers: List<CustomerEntity>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(Modifier.padding(16.dp)) {
                Text("Pick a customer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Divider(Modifier.padding(vertical = 8.dp))
                if (customers.isEmpty()) {
                    Text("No customers yet — one is created on the first payment.")
                } else {
                    LazyColumn(Modifier.heightIn(max = 360.dp)) {
                        items(customers, key = { it.custId }) { c ->
                            Column(Modifier.fillMaxWidth().clickable { onPick(c.custId) }.padding(vertical = 8.dp)) {
                                Text("${c.custId} — ${c.name ?: "(no name)"}", style = MaterialTheme.typography.bodyLarge)
                                c.phoneNumber?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                                Divider(Modifier.padding(top = 6.dp))
                            }
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Cancel") }
            }
        }
    }
}
