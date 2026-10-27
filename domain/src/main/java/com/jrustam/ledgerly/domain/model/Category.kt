package com.jrustam.ledgerly.domain.model

enum class Category(val type: TransactionType) {
    FOOD(TransactionType.EXPENSE), TRANSPORT(TransactionType.EXPENSE),
    SHOPPING(TransactionType.EXPENSE), BILLS(TransactionType.EXPENSE),
    HEALTH(TransactionType.EXPENSE), OTHER_EXPENSE(TransactionType.EXPENSE),
    SALARY(TransactionType.INCOME), FREELANCE(TransactionType.INCOME),
    GIFT(TransactionType.INCOME), OTHER_INCOME(TransactionType.INCOME)
}
