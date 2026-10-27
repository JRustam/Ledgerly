package com.jrustam.ledgerly.domain.calculation

import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.TransactionType
import com.jrustam.ledgerly.domain.model.Transfer
import com.jrustam.ledgerly.domain.model.Wallet
import java.math.BigInteger

fun walletBalance(
    wallet: Wallet,
    transactions: List<Transaction>,
    transfers: List<Transfer>
): BigInteger {
    val entries = transactions.filter { it.walletId == wallet.id && it.currency == wallet.currency }
    return BigInteger.valueOf(wallet.openingMinor) + entries.fold(BigInteger.ZERO) { sum, t ->
        sum + BigInteger.valueOf(t.amountMinor) * if (t.type == TransactionType.INCOME) BigInteger.ONE else BigInteger.ONE.negate()
    } + transfers.fold(BigInteger.ZERO) { sum, t ->
        sum + (if (t.toId == wallet.id) BigInteger.valueOf(t.creditMinor) else BigInteger.ZERO) -
                (if (t.fromId == wallet.id) BigInteger.valueOf(t.debitMinor) else BigInteger.ZERO)
    }
}
