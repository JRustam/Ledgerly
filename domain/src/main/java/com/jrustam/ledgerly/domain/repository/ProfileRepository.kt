package com.jrustam.ledgerly.domain.repository

import com.jrustam.ledgerly.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val profile: Flow<UserProfile>
    suspend fun saveName(name: String)
    /** Copies a selected image into private storage; null removes the existing photo. */
    suspend fun setPhoto(source: String?)
}
