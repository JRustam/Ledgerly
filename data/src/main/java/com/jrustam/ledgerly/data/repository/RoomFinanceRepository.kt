package com.jrustam.ledgerly.data.repository

import androidx.room.withTransaction
import com.jrustam.ledgerly.data.TransactionDatabase
import com.jrustam.ledgerly.data.local.entity.WalletEntity
import com.jrustam.ledgerly.data.local.entity.TransferEntity
import com.jrustam.ledgerly.data.local.entity.PlanEntity
import com.jrustam.ledgerly.data.local.entity.ContributionEntity
import com.jrustam.ledgerly.domain.repository.FinanceRepository
import com.jrustam.ledgerly.domain.model.FinanceData
import com.jrustam.ledgerly.domain.model.Wallet
import com.jrustam.ledgerly.domain.model.Transfer
import com.jrustam.ledgerly.domain.model.FinancePlan
import com.jrustam.ledgerly.domain.model.PlanKind
import com.jrustam.ledgerly.domain.model.Category
import com.jrustam.ledgerly.domain.model.Contribution
import com.jrustam.ledgerly.domain.model.LedgerCurrency
import com.jrustam.ledgerly.domain.model.TransactionType
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import javax.inject.Inject

class RoomFinanceRepository @Inject constructor(private val db: TransactionDatabase) :
    FinanceRepository {
    private val dao get() = db.financeDao()
    override fun observe() =
        combine(dao.wallets(), dao.transfers(), dao.plans(), dao.contributions()) { w, t, p, c ->
            FinanceData(
                w.map {
                    Wallet(
                        it.id,
                        it.name,
                        it.bank,
                        it.kind,
                        it.currency,
                        it.openingMinor,
                        it.theme,
                        it.lastFour
                    )
                },
                t.map {
                    Transfer(
                        it.id,
                        it.fromId,
                        it.toId,
                        it.debitMinor,
                        it.creditMinor,
                        LocalDate.ofEpochDay(it.dateEpochDay)
                    )
                },
                p.map {
                    FinancePlan(
                        it.id,
                        PlanKind.valueOf(it.kind),
                        it.name,
                        it.currency,
                        it.amountMinor,
                        Category.valueOf(it.category),
                        LocalDate.ofEpochDay(it.dateEpochDay)
                    )
                },
                c.map {
                    Contribution(
                        it.id,
                        it.goalId,
                        it.amountMinor,
                        LocalDate.ofEpochDay(it.dateEpochDay)
                    )
                })
        }

    override suspend fun saveWallet(wallet: Wallet) = db.withTransaction {
        require(wallet.name.isNotBlank() && wallet.name.length <= 60 && wallet.bank.length <= 60)
        require(wallet.kind in listOf("CASH", "CARD", "BANK"))
        require(wallet.lastFour.isEmpty() || Regex("[0-9]{4}").matches(wallet.lastFour))
        require(wallet.currency in LedgerCurrency.entries.map { it.name })
        // A wallet's denomination is immutable: editing it must never relabel past money.
        require(dao.wallet(wallet.id)?.currency?.let { it == wallet.currency } != false)
        dao.save(
            WalletEntity(
                wallet.id,
                wallet.name.trim(),
                wallet.bank.trim(),
                wallet.kind,
                wallet.currency,
                wallet.openingMinor,
                wallet.theme,
                wallet.lastFour
            )
        )
    }

    override suspend fun transfer(transfer: Transfer) = db.withTransaction {
        require(transfer.fromId != transfer.toId && transfer.debitMinor > 0 && transfer.creditMinor > 0)
        val from = requireNotNull(dao.wallet(transfer.fromId));
        val to = requireNotNull(dao.wallet(transfer.toId))
        require(from.currency != to.currency || transfer.debitMinor == transfer.creditMinor)
        dao.insert(
            TransferEntity(
                transfer.id,
                transfer.fromId,
                transfer.toId,
                transfer.debitMinor,
                transfer.creditMinor,
                transfer.date.toEpochDay()
            )
        )
    }

    override suspend fun deleteTransfer(id: String) = dao.deleteTransfer(id)
    override suspend fun savePlan(plan: FinancePlan) = db.withTransaction {
        require(plan.name.isNotBlank() && plan.name.length <= 60 && plan.amountMinor > 0)
        require(plan.currency in LedgerCurrency.entries.map { it.name })
        require(plan.kind == PlanKind.GOAL || plan.category.type == TransactionType.EXPENSE)
        val old = dao.plan(plan.id)
        require(old == null || old.currency == plan.currency && old.kind == plan.kind.name)
        dao.save(
            PlanEntity(
                plan.id,
                plan.kind.name,
                plan.name.trim(),
                plan.currency,
                plan.amountMinor,
                plan.category.name,
                plan.date.toEpochDay()
            )
        )
    }

    override suspend fun deletePlan(id: String) =
        db.withTransaction { dao.deleteContributions(id); dao.deletePlan(id) }

    override suspend fun contribute(contribution: Contribution) = db.withTransaction {
        require(contribution.amountMinor > 0)
        require(dao.plan(contribution.goalId)?.kind == PlanKind.GOAL.name)
        dao.insert(
            ContributionEntity(
                contribution.id,
                contribution.goalId,
                contribution.amountMinor,
                contribution.date.toEpochDay()
            )
        )
    }
}
