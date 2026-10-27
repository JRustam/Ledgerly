package com.jrustam.ledgerly.domain.repository

import com.jrustam.ledgerly.domain.model.Appearance
import com.jrustam.ledgerly.domain.model.ProductPreferences
import kotlinx.coroutines.flow.Flow

interface ProductPreferencesRepository {
    val data: Flow<ProductPreferences>
    suspend fun appearance(value: Appearance)
    suspend fun completeOnboarding()
}
