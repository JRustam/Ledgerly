package com.jrustam.ledgerly.domain.usecase

import com.jrustam.ledgerly.domain.calculation.calculateBalances
import com.jrustam.ledgerly.domain.calculation.walletBalance
import com.jrustam.ledgerly.domain.model.FinanceData
import com.jrustam.ledgerly.domain.model.LedgerBalances
import com.jrustam.ledgerly.domain.model.Transaction
import com.jrustam.ledgerly.domain.model.Wallet
import java.math.BigInteger
import javax.inject.Inject

/** Accounts include their baseline and transfers; unassigned legacy rows are included once. */
class CalculateLedgerBalancesUseCase @Inject constructor() {
    operator fun invoke(data: FinanceData, transactions: List<Transaction>): LedgerBalances {
        val wallets = data.wallets.associate { it.id to walletBalance(it, transactions, data.transfers) }
        val unassigned = calculateBalances(transactions.filter { it.walletId == null })
        val currencies = data.wallets.groupBy(Wallet::currency).mapValues { (_, accounts) ->
            accounts.fold(BigInteger.ZERO) { total, wallet -> total + wallets.getValue(wallet.id) }
        }.toMutableMap()
        unassigned.forEach { (currency, summary) ->
            currencies[currency] = currencies.getOrDefault(currency, BigInteger.ZERO) + summary.balanceMinor
        }
        return LedgerBalances(wallets, currencies.toSortedMap(), unassigned)
    }
}
