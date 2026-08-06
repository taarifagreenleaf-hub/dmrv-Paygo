package com.greenleaf.paygo.ui.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.greenleaf.paygo.data.db.entity.ParsingRuleEntity
import com.greenleaf.paygo.data.db.entity.ResponseRuleEntity
import com.greenleaf.paygo.di.ServiceLocator
import com.greenleaf.paygo.parser.ParseResult
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TrainingViewModel : ViewModel() {
    private val repo = ServiceLocator.repository
    private val parser = ServiceLocator.parser

    val parsingRules = repo.observeParsingRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val responseRules = repo.observeResponseRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveParsingRule(rule: ParsingRuleEntity) = viewModelScope.launch {
        repo.upsertParsingRule(rule)
    }

    fun deleteParsingRule(rule: ParsingRuleEntity) = viewModelScope.launch {
        repo.deleteParsingRule(rule)
    }

    fun saveResponseRule(rule: ResponseRuleEntity) = viewModelScope.launch {
        repo.upsertResponseRule(rule)
    }

    fun deleteResponseRule(rule: ResponseRuleEntity) = viewModelScope.launch {
        repo.deleteResponseRule(rule)
    }

    /** Runs the current parsing rules over a sample SMS so the user can verify training. */
    suspend fun testParse(address: String, body: String): ParseResult? {
        val rules = repo.enabledParsingRules()
        return parser.parse(address, body, rules)
    }
}
