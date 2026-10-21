package com.jrustam.ledgerly.di

import android.content.Context
import androidx.room.Room
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jrustam.ledgerly.data.*
import com.jrustam.ledgerly.domain.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private val Context.preferencesStore by preferencesDataStore(name = "ledgerly_preferences")

@Module
@InstallIn(SingletonComponent::class)
object StorageModule {
    @Provides @Singleton
    fun preferencesStore(@ApplicationContext context: Context): DataStore<Preferences> = context.preferencesStore
    @Provides @Singleton
    fun currencyPreferences(implementation: DataStoreCurrencyPreferences): CurrencyPreferences = implementation
    @Provides @Singleton
    fun languagePreferences(implementation: AppCompatLanguagePreferences): LanguagePreferences = implementation

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): TransactionDatabase =
        Room.databaseBuilder(context, TransactionDatabase::class.java, "ledgerly.db").addMigrations(TransactionDatabase.MIGRATION_1_2).build()

    @Provides fun dao(database: TransactionDatabase): TransactionDao = database.transactionDao()

    @Provides @Singleton
    fun repository(implementation: RoomTransactionRepository): TransactionRepository = implementation
}
