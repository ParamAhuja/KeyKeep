package com.example.keykeep.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [Credential::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, dbKey: ByteArray): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Inject SQLCipher factory with the provided cryptographic key
                val factory = SupportOpenHelperFactory(dbKey)

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "secure-vault.db"
                )
                    .openHelperFactory(factory)
                    .build()

                INSTANCE = instance
                instance
            }
        }

        // Add this new function to securely wipe the cached instance
        fun destroyInstance() {
            INSTANCE?.close()
            INSTANCE = null
        }
    }
}