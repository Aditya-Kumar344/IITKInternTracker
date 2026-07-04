package com.internmail.tracker.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {

    @Query("SELECT * FROM events ORDER BY deadlineEpochMillis ASC")
    fun observeAll(): Flow<List<Event>>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun getById(id: Long): Event?

    @Query("SELECT * FROM events WHERE emailMessageId = :messageId LIMIT 1")
    suspend fun findByMessageId(messageId: String): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: Event): Long

    @Update
    suspend fun update(event: Event)

    @Delete
    suspend fun delete(event: Event)

    @Query("DELETE FROM events WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Events whose deadline day is "today" (caller passes start/end of today in epoch millis)
    @Query("SELECT * FROM events WHERE deadlineEpochMillis BETWEEN :startOfDay AND :endOfDay AND opened = 0")
    suspend fun getEventsDueToday(startOfDay: Long, endOfDay: Long): List<Event>

    @Query("SELECT * FROM events WHERE opened = 0 AND deadlineEpochMillis >= :fromEpoch ORDER BY deadlineEpochMillis ASC")
    suspend fun getUpcoming(fromEpoch: Long): List<Event>
}
