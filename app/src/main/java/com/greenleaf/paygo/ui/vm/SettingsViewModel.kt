package com.greenleaf.paygo.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.greenleaf.paygo.data.prefs.PaygoSettings
import com.greenleaf.paygo.di.ServiceLocator
import com.greenleaf.paygo.export.DataExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(app: Application) : AndroidViewModel(app) {
    private val settingsStore = ServiceLocator.settingsStore
    private val exporter = DataExporter(app, ServiceLocator.repository)

    val settings = settingsStore.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PaygoSettings())

    fun update(transform: (PaygoSettings) -> PaygoSettings) = viewModelScope.launch {
        settingsStore.update(transform)
    }

    /** Export type: TRANSACTIONS_CSV, MESSAGES_CSV, BACKUP_JSON. */
    fun export(type: ExportType, onReady: (File) -> Unit) = viewModelScope.launch {
        val file = withContext(Dispatchers.IO) {
            when (type) {
                ExportType.TRANSACTIONS_CSV -> exporter.exportTransactionsCsv()
                ExportType.MESSAGES_CSV -> exporter.exportMessagesCsv()
                ExportType.BACKUP_JSON -> exporter.exportBackupJson()
            }
        }
        onReady(file)
    }

}

enum class ExportType { TRANSACTIONS_CSV, MESSAGES_CSV, BACKUP_JSON }
