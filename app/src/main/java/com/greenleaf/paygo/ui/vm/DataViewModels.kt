package com.greenleaf.paygo.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenleaf.paygo.data.db.entity.CustomerEntity
import com.greenleaf.paygo.di.ServiceLocator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionsViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    val transactions = repo.observeTransactions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

class GroupsViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    val groups = repo.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val customers = repo.observeCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun messagesFor(groupKey: String) = repo.observeMessagesByGroup(groupKey)

    /** Manually link a message to a customer when auto-linking missed it. */
    fun assign(messageId: Long, custId: String) = viewModelScope.launch {
        repo.assignMessageToCustomer(messageId, custId)
    }
}

class CustomersViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    val customers = repo.observeCustomers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun messagesFor(custId: String) = repo.observeMessagesByCustomer(custId)

    fun save(customer: CustomerEntity) = viewModelScope.launch {
        repo.updateCustomer(customer)
    }
}
