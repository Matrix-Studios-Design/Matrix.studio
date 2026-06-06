package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntry>> = historyDao.getAllHistory()

    suspend fun insert(entry: HistoryEntry) {
        historyDao.insertHistory(entry)
    }

    suspend fun deleteById(id: Long) {
        historyDao.deleteHistoryById(id)
    }

    suspend fun clear() {
        historyDao.clearHistory()
    }
}
