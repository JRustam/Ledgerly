package com.jrustam.ledgerly.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import com.jrustam.ledgerly.domain.model.Appearance
import com.jrustam.ledgerly.domain.model.ProductPreferences
import com.jrustam.ledgerly.domain.repository.ProductPreferencesRepository

@Singleton
class ProductPreferenceStore @Inject constructor(private val store: DataStore<Preferences>) : ProductPreferencesRepository {
    private val theme = stringPreferencesKey("appearance")
    private val welcome = booleanPreferencesKey("onboarded")
    override val data = store.data.map {
        ProductPreferences(
            runCatching { Appearance.valueOf(it[theme].orEmpty()) }.getOrDefault(
                Appearance.SYSTEM
            ),
            it[welcome] ?: false
        )
    }

    override suspend fun appearance(value: Appearance) {
        store.edit { it[theme] = value.name }
    }

    override suspend fun completeOnboarding() {
        store.edit { it[welcome] = true }
    }
}
