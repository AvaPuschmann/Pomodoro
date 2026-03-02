package com.agenticfocus.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.agenticfocus.data.entity.DomainEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainDao {

    @Query("SELECT * FROM domains ORDER BY name ASC")
    fun observeAll(): Flow<List<DomainEntity>>

    @Query("SELECT COUNT(*) FROM domains")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(domains: List<DomainEntity>)

    @Query("DELETE FROM domains WHERE id = :id")
    suspend fun deleteById(id: String)
}
