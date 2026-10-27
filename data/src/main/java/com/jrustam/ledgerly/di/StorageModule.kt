package com.jrustam.ledgerly.di

import com.jrustam.ledgerly.domain.repository.ProfileRepository
import com.jrustam.ledgerly.data.repository.DataStoreProfileRepository
import android.content.Context
import androidx.room.Room
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.jrustam.ledgerly.data.TransactionDatabase
import com.jrustam.ledgerly.data.local.dao.TransactionDao
import com.jrustam.ledgerly.data.repository.RoomFinanceRepository
import com.jrustam.ledgerly.data.repository.RoomTransactionRepository
import com.jrustam.ledgerly.data.repository.DataStoreCurrencyPreferences
import com.jrustam.ledgerly.data.repository.AppCompatLanguagePreferences
import com.jrustam.ledgerly.data.repository.ProductPreferenceStore
import com.jrustam.ledgerly.data.repository.CachedExchangeRates
import com.jrustam.ledgerly.domain.repository.CurrencyPreferences
import com.jrustam.ledgerly.domain.repository.ExchangeRateProvider
import com.jrustam.ledgerly.domain.repository.FinanceRepository
import com.jrustam.ledgerly.domain.repository.LanguagePreferences
import com.jrustam.ledgerly.domain.repository.ProductPreferencesRepository
import com.jrustam.ledgerly.domain.repository.TransactionRepository
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
    fun profileRepository(implementation: DataStoreProfileRepository): ProfileRepository = implementation

    @Provides @Singleton
    fun productPreferences(implementation: ProductPreferenceStore): ProductPreferencesRepository = implementation
    @Provides @Singleton
    fun exchangeRates(implementation: CachedExchangeRates): ExchangeRateProvider = implementation

    @Provides @Singleton
    fun preferencesStore(@ApplicationContext context: Context): DataStore<Preferences> = context.preferencesStore
    @Provides @Singleton
    fun currencyPreferences(implementation: DataStoreCurrencyPreferences): CurrencyPreferences = implementation
    @Provides @Singleton
    fun languagePreferences(implementation: AppCompatLanguagePreferences): LanguagePreferences = implementation

    @Provides @Singleton
    fun database(@ApplicationContext context: Context): TransactionDatabase =
        Room.databaseBuilder(context, TransactionDatabase::class.java, "ledgerly.db").addMigrations(TransactionDatabase.MIGRATION_1_2, TransactionDatabase.MIGRATION_2_3).build()

    @Provides @Singleton
    fun financeRepository(implementation: RoomFinanceRepository): FinanceRepository = implementation

    @Provides fun dao(database: TransactionDatabase): TransactionDao = database.transactionDao()

    @Provides @Singleton
    fun repository(implementation: RoomTransactionRepository): TransactionRepository = implementation
}
