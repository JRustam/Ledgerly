package com.jrustam.ledgerly.ui.profile

import com.jrustam.ledgerly.domain.model.UserProfile

data class ProfileUiState(
    val profile: UserProfile = UserProfile(),
    val name: String = "",
    val loading: Boolean = true,
    val loadFailed: Boolean = false,
    val busy: Boolean = false,
    val nameError: Boolean = false,
    val saveFailed: Boolean = false,
    val saved: Boolean = false,
)
