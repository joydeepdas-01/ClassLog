package com.classlog.app.data.repository

import com.classlog.app.data.datastore.AppSettings
import com.classlog.app.data.datastore.SettingsDataStore
import com.classlog.app.data.local.dao.BatchDao
import com.classlog.app.data.local.dao.CategoryDao
import com.classlog.app.data.local.dao.TimeSlotDao
import com.classlog.app.data.local.entity.BatchEntity
import com.classlog.app.data.local.entity.CategoryEntity
import com.classlog.app.data.local.entity.TimeSlotEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing batches, categories, time slots, and app settings.
 */
class ConfigRepository(
    private val batchDao: BatchDao,
    private val categoryDao: CategoryDao,
    private val timeSlotDao: TimeSlotDao,
    private val settingsDataStore: SettingsDataStore
) {
    // --- Batches ---
    fun observeActiveBatches(): Flow<List<BatchEntity>> = batchDao.observeActive()
    fun observeAllBatches(): Flow<List<BatchEntity>> = batchDao.observeAll()

    suspend fun saveBatch(name: String, sortOrder: Int = 0): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        return batchDao.insert(BatchEntity(name = trimmed, isActive = true, sortOrder = sortOrder))
    }

    suspend fun updateBatch(batch: BatchEntity) {
        batchDao.update(batch.copy(name = batch.name.trim()))
    }

    suspend fun toggleBatchActive(batch: BatchEntity) {
        batchDao.update(batch.copy(isActive = !batch.isActive))
    }

    suspend fun deleteBatch(batch: BatchEntity) {
        batchDao.delete(batch)
    }

    // --- Categories ---
    fun observeActiveCategories(): Flow<List<CategoryEntity>> = categoryDao.observeActive()
    fun observeAllCategories(): Flow<List<CategoryEntity>> = categoryDao.observeAll()

    suspend fun saveCategory(name: String, isTeaching: Boolean, sortOrder: Int = 0): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return -1L
        return categoryDao.insert(
            CategoryEntity(name = trimmed, isTeaching = isTeaching, isActive = true, sortOrder = sortOrder)
        )
    }

    suspend fun updateCategory(category: CategoryEntity) {
        categoryDao.update(category.copy(name = category.name.trim()))
    }

    suspend fun toggleCategoryActive(category: CategoryEntity) {
        categoryDao.update(category.copy(isActive = !category.isActive))
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.delete(category)
    }

    // --- Time Slots ---
    fun observeActiveTimeSlots(): Flow<List<TimeSlotEntity>> = timeSlotDao.observeActive()
    fun observeAllTimeSlots(): Flow<List<TimeSlotEntity>> = timeSlotDao.observeAll()

    suspend fun saveTimeSlot(label: String, startMinutes: Int, endMinutes: Int, sortOrder: Int = 0): Long {
        val trimmed = label.trim()
        if (trimmed.isEmpty() || endMinutes <= startMinutes) return -1L
        return timeSlotDao.insert(
            TimeSlotEntity(label = trimmed, startMinutes = startMinutes, endMinutes = endMinutes, isActive = true, sortOrder = sortOrder)
        )
    }

    suspend fun updateTimeSlot(timeSlot: TimeSlotEntity) {
        timeSlotDao.update(timeSlot.copy(label = timeSlot.label.trim()))
    }

    suspend fun toggleTimeSlotActive(timeSlot: TimeSlotEntity) {
        timeSlotDao.update(timeSlot.copy(isActive = !timeSlot.isActive))
    }

    // --- Settings ---
    val settings: Flow<AppSettings> = settingsDataStore.settingsFlow

    suspend fun setUse24HourFormat(use24Hour: Boolean) {
        settingsDataStore.setUse24HourFormat(use24Hour)
    }

    suspend fun setQuickDurations(durations: List<Int>) {
        settingsDataStore.setQuickDurations(durations)
    }

    suspend fun setTeacherName(name: String) {
        settingsDataStore.setTeacherName(name.trim())
    }

    suspend fun setInstitutionName(name: String) {
        settingsDataStore.setInstitutionName(name.trim())
    }

    suspend fun setProfileImagePath(path: String?) {
        settingsDataStore.setProfileImagePath(path)
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        settingsDataStore.setAppLockEnabled(enabled)
    }
}
