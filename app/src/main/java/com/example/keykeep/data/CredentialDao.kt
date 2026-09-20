package com.example.keykeep.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCredential(credential: Credential)

    // Used for the new edit functionality
    @Update
    suspend fun updateCredential(credential: Credential)

    @Delete
    suspend fun deleteCredential(credential: Credential)

    @Query("SELECT * FROM credentials ORDER BY serviceName ASC")
    fun getAllCredentials(): Flow<List<Credential>>

    // Add this missing function to resolve the AddCredentialScreen errors
    @Query("SELECT * FROM credentials WHERE id = :id LIMIT 1")
    suspend fun getCredentialById(id: Int): Credential?
}