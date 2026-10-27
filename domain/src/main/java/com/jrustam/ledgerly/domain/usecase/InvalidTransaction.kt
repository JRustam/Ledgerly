package com.jrustam.ledgerly.domain.usecase

import com.jrustam.ledgerly.domain.model.ValidationErrors

class InvalidTransaction(val errors: ValidationErrors) : IllegalArgumentException()
