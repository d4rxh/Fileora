package com.fileora.app.data.repository

import android.content.Context
import android.net.Uri
import com.fileora.app.domain.model.ProcessingHistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

class HistoryRepository(private val context: Context) {
    private val historyFile = File(context.filesDir, "fileora_history.json")
    private val _historyFlow = MutableStateFlow<List<ProcessingHistoryItem>>(emptyList())
    val historyFlow: StateFlow<List<ProcessingHistoryItem>> = _historyFlow.asStateFlow()

    init {
        loadHistory()
    }

    private fun loadHistory() {
        if (!historyFile.exists()) {
            _historyFlow.value = emptyList()
            return
        }
        try {
            val jsonStr = historyFile.readText()
            val array = JSONArray(jsonStr)
            val list = mutableListOf<ProcessingHistoryItem>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val uriStr = obj.optString("outputUri", "")
                list.add(
                    ProcessingHistoryItem(
                        id = obj.getString("id"),
                        toolTitle = obj.getString("toolTitle"),
                        inputName = obj.getString("inputName"),
                        outputName = obj.getString("outputName"),
                        originalSize = obj.getLong("originalSize"),
                        finalSize = obj.getLong("finalSize"),
                        outputUri = if (uriStr.isNotEmpty()) Uri.parse(uriStr) else null,
                        timestamp = obj.getLong("timestamp"),
                        success = obj.optBoolean("success", true)
                    )
                )
            }
            _historyFlow.value = list.sortedByDescending { it.timestamp }
        } catch (_: Exception) {
            _historyFlow.value = emptyList()
        }
    }

    private suspend fun saveHistory(list: List<ProcessingHistoryItem>) = withContext(Dispatchers.IO) {
        val array = JSONArray()
        for (item in list) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("toolTitle", item.toolTitle)
                put("inputName", item.inputName)
                put("outputName", item.outputName)
                put("originalSize", item.originalSize)
                put("finalSize", item.finalSize)
                put("outputUri", item.outputUri?.toString() ?: "")
                put("timestamp", item.timestamp)
                put("success", item.success)
            }
            array.put(obj)
        }
        historyFile.writeText(array.toString())
    }

    suspend fun addRecord(
        toolTitle: String,
        inputName: String,
        outputName: String,
        originalSize: Long,
        finalSize: Long,
        outputUri: Uri?,
        success: Boolean = true
    ) {
        val item = ProcessingHistoryItem(
            id = UUID.randomUUID().toString(),
            toolTitle = toolTitle,
            inputName = inputName,
            outputName = outputName,
            originalSize = originalSize,
            finalSize = finalSize,
            outputUri = outputUri,
            timestamp = System.currentTimeMillis(),
            success = success
        )
        val updated = (listOf(item) + _historyFlow.value).take(100) // keep latest 100
        _historyFlow.value = updated
        saveHistory(updated)
    }

    suspend fun clearHistory() {
        _historyFlow.value = emptyList()
        withContext(Dispatchers.IO) {
            if (historyFile.exists()) {
                historyFile.delete()
            }
        }
    }

    suspend fun deleteItem(id: String) {
        val updated = _historyFlow.value.filterNot { it.id == id }
        _historyFlow.value = updated
        saveHistory(updated)
    }
}
