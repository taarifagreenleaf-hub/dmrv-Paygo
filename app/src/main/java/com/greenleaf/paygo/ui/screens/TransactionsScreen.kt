package com.greenleaf.paygo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.greenleaf.paygo.data.db.entity.TransactionEntity
import com.greenleaf.paygo.ui.vm.TransactionsViewModel

@Composable
fun TransactionsScreen(vm: TransactionsViewModel = viewModel()) {
    val txns by vm.transactions.collectAsStateWithLifecycle()

    if (txns.isEmpty()) {
        EmptyState("No payments captured yet.\nThey will appear here as payment SMS arrive.")
        return
    }
    LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
        items(txns, key = { it.id }) { TransactionCard(it) }
    }
}

@Composable
private fun TransactionCard(t: TransactionEntity) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "${t.currency} ${Format.money(t.amount)}",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(t.provider.uppercase() + " • " + t.direction, style = MaterialTheme.typography.labelMedium)
            t.counterpartyName?.let { KeyValueRow("From", it) }
            t.counterpartyNumber?.let { KeyValueRow("Number", it) }
            t.reference?.let { KeyValueRow("Ref", it) }
            t.balanceAfter?.let { KeyValueRow("Balance", Format.money(it)) }
            KeyValueRow("Time", Format.date(t.timestamp))
        }
    }
}

@Composable
fun EmptyState(text: String) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center
    ) {
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}
