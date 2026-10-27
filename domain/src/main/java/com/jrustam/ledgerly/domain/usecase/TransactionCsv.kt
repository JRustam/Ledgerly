package com.jrustam.ledgerly.domain.usecase

import com.jrustam.ledgerly.domain.model.Transaction
import java.math.BigDecimal

fun transactionCsv(rows: List<Transaction>): String {
    fun cell(value: String): String {
        // Prevent spreadsheet formula execution in user-controlled fields.
        val safe = if (value.trimStart().firstOrNull() in listOf(
                '=',
                '+',
                '-',
                '@',
                '\t',
                '\r'
            )
        ) "'$value" else value
        return "\"" + safe.replace("\"", "\"\"") + "\""
    }
    return "date,type,category,amount,currency,wallet_id,notes\r\n" + rows.joinToString("\r\n") {
        listOf(
            it.date.toString(),
            it.type.name,
            it.category.name,
            BigDecimal.valueOf(it.amountMinor, 2).toPlainString(),
            it.currency,
            it.walletId.orEmpty(),
            it.description.orEmpty()
        ).joinToString(",", transform = ::cell)
    }
}
