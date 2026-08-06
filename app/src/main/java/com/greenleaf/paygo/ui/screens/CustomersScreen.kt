package com.greenleaf.paygo.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenleaf.paygo.data.db.entity.CustomerEntity
import com.greenleaf.paygo.ui.vm.CustomersViewModel

/** Contacts area: a Customers ledger (sms-cust-id based) plus raw conversations. */
@Composable
fun ContactsScreen() {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Customers", "Conversations")
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> CustomersTab()
            1 -> GroupsScreen()
        }
    }
}

@Composable
private fun CustomersTab(vm: CustomersViewModel = viewModel()) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<CustomerEntity?>(null) }

    if (customers.isEmpty()) {
        EmptyState(
            "No customers yet.\nWhen a payment arrives, the customer is assigned an " +
                "sms-cust-id and asked to reply with their details."
        )
    } else {
        LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
            items(customers, key = { it.custId }) { c ->
                Card(
                    Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { selected = c }
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(c.custId, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(c.name ?: "(name not yet provided)", style = MaterialTheme.typography.bodyLarge)
                        c.phoneNumber?.let { KeyValueRow("Phone", it) }
                        c.location?.let { KeyValueRow("Location", it) }
                        c.productType?.let { KeyValueRow("Product", it) }
                        KeyValueRow("Total paid (TZS)", Format.money(c.totalPaid))
                        KeyValueRow("Payments", c.paymentCount.toString())
                    }
                }
            }
        }
    }

    selected?.let { c -> CustomerDetailDialog(vm, c) { selected = null } }
}

@Composable
private fun CustomerDetailDialog(vm: CustomersViewModel, c: CustomerEntity, onDismiss: () -> Unit) {
    val messages by vm.messagesFor(c.custId).collectAsState(initial = emptyList())
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(c.name ?: "") }
    var location by remember { mutableStateOf(c.location ?: "") }
    var product by remember { mutableStateOf(c.productType ?: "") }
    var phone by remember { mutableStateOf(c.phoneNumber ?: "") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                Modifier.padding(16.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(c.custId, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Divider(Modifier.padding(vertical = 8.dp))

                if (editing) {
                    LabeledField("Name", name) { name = it }
                    LabeledField("Phone", phone) { phone = it }
                    LabeledField("Location", location) { location = it }
                    LabeledField("Product type", product) { product = it }
                } else {
                    KeyValueRow("Name", c.name ?: "-")
                    KeyValueRow("Phone", c.phoneNumber ?: "-")
                    KeyValueRow("Location", c.location ?: "-")
                    KeyValueRow("Product", c.productType ?: "-")
                }
                KeyValueRow("Total paid (TZS)", Format.money(c.totalPaid))
                KeyValueRow("Last amount", c.lastAmount?.let { Format.money(it) } ?: "-")

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    if (editing) {
                        TextButton(onClick = { editing = false }) { Text("Cancel") }
                        TextButton(onClick = {
                            vm.save(
                                c.copy(
                                    name = name.ifBlank { null },
                                    phoneNumber = phone.ifBlank { null },
                                    location = location.ifBlank { null },
                                    productType = product.ifBlank { null },
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                            editing = false
                        }) { Text("Save") }
                    } else {
                        TextButton(onClick = { editing = true }) { Text("Edit details") }
                    }
                }

                Divider(Modifier.padding(vertical = 8.dp))
                Text("Messages", style = MaterialTheme.typography.titleSmall)
                LazyColumn(Modifier.heightIn(max = 240.dp)) {
                    items(messages, key = { it.id }) { m ->
                        Column(Modifier.padding(vertical = 6.dp)) {
                            Text(Format.date(m.timestamp), style = MaterialTheme.typography.labelSmall)
                            Text(m.body, style = MaterialTheme.typography.bodyMedium)
                            Divider(Modifier.padding(top = 6.dp))
                        }
                    }
                }
                TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) { Text("Close") }
            }
        }
    }
}
